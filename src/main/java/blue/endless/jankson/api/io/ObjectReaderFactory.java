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

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import blue.endless.jankson.api.document.ValueElement;
import blue.endless.jankson.impl.io.objectreader.ObjectStructuredDataReader;

/**
 * This class manages reading arbitrary Java objects as StructuredData. Because not all classes can
 * be recognized or annotated from within, factories may be registered to produce appropriate
 * readers externally.
 */
public class ObjectReaderFactory {
	private Map<Type, Function<Object, StructuredDataReader>> functionMap = new HashMap<>();
	
	/**
	 * Registers a "classic" serializer for the specified type.
	 * @param <T> The type the serializer will apply to
	 * @param type The class the serializer will apply to
	 * @param function A function that will receive an object of the specified type, and produce a
	 *                 ValueElement representing it.
	 */
	@SuppressWarnings("unchecked")
	public <T> void registerSerializer(final Class<T> type, final Function<T, ValueElement> function) {
		registerSerializer((Type) type, (Function<Object, ValueElement>) function);
	}
	
	/**
	 * Registers a "classic" serializer for the specified type
	 * @param type The type to specify a serializer for
	 * @param function A function which will receive an object of the specified type, and produce a
	 *                 ValueElement representing it.
	 */
	public void registerSerializer(final Type type, final Function<Object, ValueElement> function) {
		Function<Object, StructuredDataReader> supplier = (Object obj) -> ValueElementReader.of(function.apply(obj));
		functionMap.put(type, supplier);
	}
	
	/**
	 * Registers a reader factory function for the specified type
	 * @param <T> The type the factory will apply to
	 * @param type The class the factory will apply to
	 * @param function A reader factory function which will receive an object of the specified type,
	 *                 and return a StructuredDataReader representation of it.
	 */
	@SuppressWarnings("unchecked")
	public <T> void register(final Class<T> type, final Function<T, StructuredDataReader> function) {
		register((Type) type, (Function<Object, StructuredDataReader>) function);
	}
	
	/**
	 * Registers a reader factory function for the specified type
	 * @param type The type the factory will apply to
	 * @param function A reader factory function which will receive an object of the specified type,
	 *                 and return a StructuredDataReader representation of it.
	 */
	public void register(final Type type, final Function<Object, StructuredDataReader> function) {
		functionMap.put(type, function);
	}
	
	/**
	 * Gets a reader which will provide a StructuredData representation of the provided Object
	 * @param <T> The type of the object being serialized / read
	 * @param type The class of the object being serialized / read
	 * @param objectOfType The object being serialized / read
	 * @return A StructuredDataReader which will provide data representing the object
	 */
	public <T> StructuredDataReader getReader(final Class<T> type, final T objectOfType) {
		return getReader((Type) type, objectOfType);
	}
	
	/**
	 * Gets a reader which will provide a StructuredData representation of the provided Object
	 * @param type The type of the object being serialized / read
	 * @param objectOfType The object being serialized / read
	 * @return A StructuredDataReader which will provide data representing the object
	 */
	public StructuredDataReader getReader(Type type, final Object objectOfType) {
		// Strip annotations - we don't want to differentiate between String and @Nullable String.
		if (type instanceof AnnotatedType annoType) {
			type = annoType.getType();
		}
		
		Function<Object, StructuredDataReader> function = functionMap.get(type);
		return (function == null) ?
				ObjectStructuredDataReader.of(objectOfType, this) :
				function.apply(objectOfType);
	}
	
	/**
	 * Quick form of {@link #getReader(Type, Object)}. Gets a reader which will provide a
	 * StructuredData representation of the provided Object.
	 * @param object The object being serialized / read
	 * @return A StructuredDataReader which will provide data representing the object
	 */
	public StructuredDataReader getReader(Object object) {
		return getReader(object.getClass(), object);
	}
}
