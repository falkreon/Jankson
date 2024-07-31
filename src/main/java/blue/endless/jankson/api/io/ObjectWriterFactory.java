/*
 * MIT License
 *
 * Copyright (c) 2018-2024 Falkreon (Isaac Ellingson)
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

package blue.endless.jankson.api.io;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import blue.endless.jankson.api.SyntaxError;
import blue.endless.jankson.api.document.ValueElement;
import blue.endless.jankson.api.function.CheckedFunction;

public class ObjectWriterFactory {
	private final Map<Type, ReaderDeserializer<?>> functionMap = new HashMap<>();
	
	public <T> void register(Class<T> type, ReaderDeserializer<T> function) {
		functionMap.put(type, function);
	}
	
	public void register(Type type, ReaderDeserializer<?> function) {
		functionMap.put(type, function);
	}
	
	public <T> void registerDeserializer(Class<T> type, ValueDeserializer<T> function) {
		functionMap.put(type, ReaderDeserializer.of(function));
	}
	
	public void registerDeserializer(Type type, ValueDeserializer<?> function) {
		functionMap.put(type, ReaderDeserializer.of(function));
	}
	
	@FunctionalInterface
	public static interface ValueDeserializer<T> extends CheckedFunction<ValueElement, T, IOException> {}
	
	@FunctionalInterface
	public static interface ReaderDeserializer<T> extends CheckedFunction<StructuredDataReader, T, IOException> {
		public static <T> ReaderDeserializer<T> of(ValueDeserializer<T> function) {
			return (reader) -> {
				ValueElementWriter writer = new ValueElementWriter();
				try {
					reader.transferTo(writer);
				} catch (SyntaxError e) {
					throw new IOException(e);
				}
				return function.apply(writer.toValueElement());
			};
		}
	}
}
