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

package blue.endless.jankson.api.codec;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.function.Function;
import java.util.function.Predicate;

import blue.endless.jankson.api.SyntaxError;
import blue.endless.jankson.api.function.CheckedFunction;
import blue.endless.jankson.api.io.StructuredData;
import blue.endless.jankson.api.io.StructuredDataBuffer;
import blue.endless.jankson.api.io.Deserializer;
import blue.endless.jankson.api.io.StructuredDataReader;

/**
 * Convenience codec that translates between json strings and some target class. For example, turning
 * <pre><code>
 * "color": "#FFEEAA"
 * </code></pre>
 * into an awt Color object, and the Color object back into the String.
 */
public class JsonStringCodec implements StructuredDataCodec {
	private final Predicate<Type> predicate;
	private final Function<Object, String> encoder;
	private final Function<String, Object> decoder;
	
	@SuppressWarnings("unchecked")
	public <T> JsonStringCodec(Class<T> clazz, Function<T, String> encoder, Function<String, T> decoder) {
		this.predicate = TypePredicate.ofClass(clazz);
		this.encoder = (Function<Object, String>) encoder;
		this.decoder = (Function<String, Object>) decoder;
	}
	
	@Override
	public StructuredDataReader getReader(Object o) {
		StructuredDataBuffer buf = new StructuredDataBuffer();
		buf.write(new StructuredData(StructuredData.Type.PRIMITIVE, encoder.apply(o)));
		return buf;
	}

	@Override
	public <T> Deserializer<T> getWriter() {
		@SuppressWarnings("unchecked")
		CheckedFunction<String, T, SyntaxError> shimmedDecoder = (String encoded) -> (T) decoder.apply(encoded);
		
		return new StringDeserializer().map(shimmedDecoder);
	}
	
	@Override
	public Predicate<Type> getPredicate() {
		return predicate;
	}
	
	private class StringDeserializer implements Deserializer<String> {
		private String result;
		
		@Override
		public boolean isComplete() {
			return (result != null);
		}

		@Override
		public void write(StructuredData data) throws SyntaxError, IOException {
			if (!data.type().isSemantic()) return;
			if (!data.isPrimitive()) throw new SyntaxError("Required: String, found "+data.type());
			
			result = (data.value() instanceof String str) ? str : data.value().toString();
		}

		@Override
		public String getResult() {
			return result;
		}
		
	}
	
}
