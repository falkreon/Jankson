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

import blue.endless.jankson.api.io.StructuredDataReader;
import blue.endless.jankson.impl.io.objectwriter.SingleValueFunction;

/**
 * Factory for StructuredDataReaders and StructuredDataWriters for a particular type.
 */
public interface StructuredDataCodec {
	/**
	 * Returns true if this codec can be used to create a StructuredData stream about the provided
	 * object. If you're inquiring about going from StructuredData TO an object, you should use
	 * {@link #appliesTo(Type)}.
	 * @param o The object that needs a codec
	 * @return true if this codec can process the supplied Object, otherwise false.
	 */
	public default boolean appliesTo(Object o) {
		return appliesTo(o.getClass());
	}
	
	/**
	 * Returns true if this codec can be used to create and/or process objects of the given type.
	 * @param t The type that needs a codec
	 * @return true if this codec applies to objects of the provided type, otherwise false.
	 */
	public boolean appliesTo(Type t);
	
	/**
	 * Gets a StructuredDataReader which can produce a stream of data that represents the provided
	 * object.
	 * @param o The object to produce a data stream for
	 * @return A stream of StructuredData which represents the provided object
	 */
	public StructuredDataReader getReader(Object o);
	
	/**
	 * Gets a StructuredDataWriter that can consume a stream of structured data and produce an
	 * object of the kind that this codec manages.
	 * @param <T> The type of the object this codec manages
	 * @param existingValue The previous value of the field, which can be reused if the object is
	 *                      mutable. The codec is not obligated to reuse the object, but it MAY
	 *                      decide to.
	 * @return A StructuredDataWriter that can consume a stream for this type.
	 */
	public default <T> SingleValueFunction<T> getWriter(T existingValue) {
		return getWriter();
	}
	
	/**
	 * Gets a StructuredDataWriter that can consume a stream of structured data and produce an
	 * object of the kind that this codec manages.
	 * @param <T> The type of the object this codec manages
	 * @return A StructuredDataWriter that can consume a stream for this type.
	 */
	public <T> SingleValueFunction<T> getWriter();
}
