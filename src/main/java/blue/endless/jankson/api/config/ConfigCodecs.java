/*
 * MIT License
 *
 * Copyright (c) 2018-2025 Falkreon (Isaac Ellingson)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package blue.endless.jankson.api.config;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Objects;
import blue.endless.jankson.api.SyntaxError;
import blue.endless.jankson.api.document.ValueElement;
import blue.endless.jankson.api.io.ObjectReaderFactory;
import blue.endless.jankson.api.io.ObjectWriter;
import blue.endless.jankson.api.io.StructuredData;
import blue.endless.jankson.api.io.ValueElementReader;
import blue.endless.jankson.api.io.ValueElementWriter;
import blue.endless.jankson.impl.config.ConfigDepthGuard;
import blue.endless.jankson.impl.io.objectreader.DelegatingStructuredDataReader;

public final class ConfigCodecs {
	/** Maximum expanded non-EOF events per reflective encode by default. */
	public static final long DEFAULT_MAX_ENCODE_EVENTS = 1_000_000L;

	private ConfigCodecs() {}

	/** Preserves document decorations when the selected output format supports them. */
	public static ConfigCodec<ValueElement> document() {
		return new ConfigCodec<>() {
			public ValueElement decode(ValueElement document) { return document; }
			public ValueElement encode(ValueElement value) { return Objects.requireNonNull(value); }
		};
	}

	public static <T> ConfigCodec<T> reflective(Class<T> type) {
		return reflective((Type) type);
	}

	public static <T> ConfigCodec<T> reflective(Type type) {
		return reflective(type, new ObjectReaderFactory());
	}

	/**
	 * Canonical mapping: annotations are regenerated, but source comments and unknown fields are not retained.
	 * Built-in readers are depth-bounded while streaming. Custom serializers/readers must bound any
	 * recursion inside their own callbacks; their returned stream is checked before tree construction.
	 */
	public static <T> ConfigCodec<T> reflective(Type type, ObjectReaderFactory factory) {
		return reflective(type, factory, DEFAULT_MAX_ENCODE_EVENTS);
	}

	/**
	 * Canonical reflective mapping with a positive, per-encode expanded event budget.
	 * Every non-EOF event counts, including keys, comments, formatting, and container boundaries.
	 * Shared references are allowed, but each occurrence consumes the budget independently.
	 * Exceeding the budget throws IOException before the excess event reaches the tree writer.
	 * Custom callbacks must bound their own recursion and allocations, including inside hasNext().
	 */
	public static <T> ConfigCodec<T> reflective(Type type, ObjectReaderFactory factory, long maxEncodeEvents) {
		if (maxEncodeEvents <= 0) throw new IllegalArgumentException("maxEncodeEvents must be positive");
		Objects.requireNonNull(type);
		ObjectReaderFactory serializers = Objects.requireNonNull(factory).copy();
		return new ConfigCodec<>() {
			public T decode(ValueElement document) throws IOException, SyntaxError {
				ObjectWriter<T> writer = new ObjectWriter<>(type);
				ValueElementReader.of(document).transferTo(writer);
				return writer.toObject();
			}
			public ValueElement encode(T value) throws IOException, SyntaxError {
				ValueElementWriter writer = new ValueElementWriter();
				var reader = value == null ? serializers.getReader(null) : serializers.getReader(type, value);
				ConfigDepthGuard.transfer(DelegatingStructuredDataReader.iterative(reader), writer, maxEncodeEvents);
				writer.write(StructuredData.EOF);
				return writer.getResult();
			}
		};
	}
}
