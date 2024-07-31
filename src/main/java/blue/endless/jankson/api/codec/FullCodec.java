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

package blue.endless.jankson.api.codec;

import java.util.function.Function;
import java.util.function.Supplier;

import blue.endless.jankson.api.io.StructuredDataReader;
import blue.endless.jankson.impl.io.objectwriter.SingleValueFunction;

public class FullCodec implements ClassTargetCodec {
	private final Class<?> targetClass;
	private final Function<Object, StructuredDataReader> serializer;
	private final Supplier<SingleValueFunction<Object>> deserializer;
	
	@SuppressWarnings("unchecked")
	public <T> FullCodec(Class<T> targetClass, Function<T, StructuredDataReader> serializer, Supplier<SingleValueFunction<T>> deserializer) {
		this.targetClass = targetClass;
		this.serializer = (Function<Object, StructuredDataReader>) serializer;
		this.deserializer = () -> (SingleValueFunction<Object>) deserializer.get();
	}
	
	@Override
	public StructuredDataReader getReader(Object o) {
		return serializer.apply(o);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> SingleValueFunction<T> getWriter() {
		return (SingleValueFunction<T>) deserializer.get();
	}

	@Override
	public Class<?> getTargetClass() {
		return targetClass;
	}
}
