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

import java.lang.reflect.Type;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import blue.endless.jankson.api.io.StructuredDataFunction;
import blue.endless.jankson.api.io.StructuredDataReader;

/**
 * Understands the structure of certain java object types, and can create streams to produce or
 * consume StructuredData. You can think of this as a factory for serializers and deserializers.
 */
public interface CodecManager {
	/**
	 * Gets a StructuredDataReader which can produce a stream of data that represents the provided
	 * object.
	 * @param o The object to produce a data stream for
	 * @return A stream of StructuredData which represents the provided object, or null if the object
	 *         isn't understood by this manager or any of its delegates.
	 */
	public @Nullable StructuredDataReader getReader(Object o);
	
	/**
	 * Gets a StructuredDataWriter that can consume a stream of data and produce an object of the
	 * same type as the provided object.
	 * @param <T> The type of the object that should be produced
	 * @param existingValue The previous value of the field, which can be reused if the object is
	 *                      mutable. The codec is not obligated to reuse the object, but it MAY
	 *                      decide to.
	 * @return A StructuredDataWriter that can consume a stream for this object's type, or null if
	 *         the type isn't understood by this manager or any of its delegates.
	 */
	public @Nullable <T> StructuredDataFunction<T> getWriter(@Nonnull T existingValue);
	
	/**
	 * Gets a StructuredDataWriter that can consume a stream of data and produce an object of the
	 * provided type.
	 * @param <T> The type of object to produce
	 * @param t The type of object to produce
	 * @return A StructuredDataWriter that can consume data for this type and produce an instance of
	 *         it.
	 */
	public @Nullable <T> StructuredDataFunction<T> getWriter(Type t);
}
