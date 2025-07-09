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
import blue.endless.jankson.api.document.ArrayElement;
import blue.endless.jankson.api.document.ObjectElement;
import blue.endless.jankson.api.document.PrimitiveElement;
import blue.endless.jankson.api.document.ValueElement;
import blue.endless.jankson.api.function.CheckedFunction;
import blue.endless.jankson.api.io.StructuredDataFunction;
import blue.endless.jankson.api.io.StructuredDataReader;
import blue.endless.jankson.api.io.ValueElementReader;
import blue.endless.jankson.api.io.ValueElementWriter;

/**
 * Simple codec that converts an object to and from "at-rest" json data in non-streaming mode. This is similar to
 * Jankson 1.2-era serializers and deserializers.
 */
public class JsonValueCodec implements StructuredDataCodec {
	private final Predicate<Type> predicate;
	private final Function<Object, ValueElement> serializer;
	private final CheckedFunction<ValueElement, Object, IOException> deserializer;
	
	@SuppressWarnings("unchecked")
	public <T> JsonValueCodec(Predicate<Type> predicate, Function<T, ValueElement> serializer, CheckedFunction<ValueElement, T, IOException> deserializer) {
		this.predicate = predicate;
		this.serializer = (Function<Object, ValueElement>) serializer;
		this.deserializer = (CheckedFunction<ValueElement, Object, IOException>) deserializer;
	}
	
	@SuppressWarnings("unchecked")
	public <T> JsonValueCodec(Class<T> targetClass, Function<T, ValueElement> serializer, CheckedFunction<ValueElement, T, IOException> deserializer) {
		this.predicate = TypePredicate.ofClass(targetClass);
		this.serializer = (Function<Object, ValueElement>) serializer;
		this.deserializer = (CheckedFunction<ValueElement, Object, IOException>) deserializer;
	}
	
	@Override
	public Predicate<Type> getPredicate() {
		return predicate;
	}
	
	@Override
	public StructuredDataReader getReader(Object o) {
		return ValueElementReader.of(serializer.apply(o));
	}

	@Override
	public <T> StructuredDataFunction<T> getWriter() {
		StructuredDataFunction<ValueElement> function = new ValueElementWriter();
		
		@SuppressWarnings("unchecked")
		CheckedFunction<ValueElement, T, SyntaxError> mapper = (ValueElement val) -> {
			try {
				return (T) deserializer.apply(val);
			} catch (IOException e) {
				throw new SyntaxError("Error applying deserializer function.", e);
			}
		};
		
		return new StructuredDataFunction.Mapper<ValueElement, T>(function, mapper);
	}
	
	public static <T> JsonValueCodec requiringObjects(Class<T> targetClass, Function<T, ObjectElement> serializer, Function<ObjectElement, T> deserializer) {
		Function<T, ValueElement> shimmedSerializer = serializer::apply;
		
		CheckedFunction<ValueElement, T, IOException> shimmedDeserializer = (elem) -> {
			if (elem instanceof ObjectElement object) {
				return deserializer.apply(object);
			} else {
				throw new IOException("Required ObjectElement but found "+elem.getClass().getSimpleName());
			}
		};
		
		return new JsonValueCodec(targetClass, shimmedSerializer, shimmedDeserializer);
	}
	
	public static <T> JsonValueCodec requiringArrays(Class<T> targetClass, Function<T, ArrayElement> serializer, Function<ArrayElement, T> deserializer) {
		Function<T, ValueElement> shimmedSerializer = serializer::apply;
		
		CheckedFunction<ValueElement, T, IOException> shimmedDeserializer = (elem) -> {
			if (elem instanceof ArrayElement array) {
				return deserializer.apply(array);
			} else {
				throw new IOException("Required ArrayElement but found "+elem.getClass().getSimpleName());
			}
		};
		
		return new JsonValueCodec(targetClass, shimmedSerializer, shimmedDeserializer);
	}
	
	public static <T> JsonValueCodec requiringPrimitives(Class<T> targetClass, Function<T, PrimitiveElement> serializer, Function<PrimitiveElement, T> deserializer) {
		Function<T, ValueElement> shimmedSerializer = serializer::apply;
		
		CheckedFunction<ValueElement, T, IOException> shimmedDeserializer = (elem) -> {
			if (elem instanceof PrimitiveElement primitive) {
				return deserializer.apply(primitive);
			} else {
				throw new IOException("Required PrimitiveElement but found "+elem.getClass().getSimpleName());
			}
		};
		
		return new JsonValueCodec(targetClass, shimmedSerializer, shimmedDeserializer);
	}
}
