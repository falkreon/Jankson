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

package blue.endless.jankson.api;

import java.lang.reflect.Type;

import blue.endless.jankson.api.document.DocumentElement;
import blue.endless.jankson.api.io.JsonIOException;

public interface Marshaller {
	/* Turns a java object into its json intermediate representation. */
	//JsonElement serialize(Object obj);
	
	/*
	 * Unpacks the provided JsonElement into a new object of type {@code clazz}, making a best
	 * effort to unpack all the fields it can. Any fields that cannot be unpacked will be left in
	 * the state the initializer and no-arg constructor leaves them in.
	 * 
	 * <p>Note: Consider using {@link #marshallCarefully(Class, JsonElement)} to detect errors first,
	 * and then calling this method as a fallback if an error is encountered.
	 * 
	 * @param clazz The class of the object to create and deserialize
	 * @param elem  json intermediate representation of the data to be unpacked.
	 * @param <E>   The type of the object to create and deserialize
	 * @return      A new object of the provided class that represents the data in the json provided.
	 */
	//<E> E marshall(Class<E> clazz, JsonElement elem);
	
	/*
	 * Unpacks the provided JsonElement into an object of the provided Type, and force-casts it to
	 * E.
	 * @param type The type to deserialize to
	 * @param elem json intermediate representation of the data to be unpacked.
	 * @param <E>  The type to force-cast to at the end
	 * @return     A new object of the provided Type that represents the data in the json provided.
	 */
	//<E> E marshall(Type type, JsonElement elem);
	
	/*
	 * Unpacks the provided JsonElement in fail-fast mode. A detailed exception is thrown for any
	 * problem encountered during the unpacking process.
	 * @param clazz The class of the object to create and deserialize
	 * @param elem  json intermediate representation of the data to be unpacked.
	 * @param <E>   The type of the object to create and deserialize
	 * @return      A new object of the provided class that represents the data in the json provided.
	 * @throws JsonIOException if any problems are encountered unpacking the data.
	 */
	//<E> E marshallCarefully(Class<E> clazz, JsonElement elem) throws JsonIOException;
	
	/**
	 * Unpacks the provided DocumentElement into an object of the provided Type, and force-casts it to the return type.
	 * @param <E>  The return type; the type of object this method should produce. Should be assignableFrom type.
	 * @param type The target type to create and convert the data to.
	 * @param elem The source data to convert.
	 * @return An object of type E which is the result of converting the data from elem into a new object of the provided Type and then downcasting to E.
	 * @throws InstantiationException if there was a problem creating the target object. Most often this is because the
	 *                                target Type is not a record type and does not have the required no-arg constructor
	 *                                for non-record types.
	 * @throws MarshallerException    if the data cannot be reconciled with the target class - there's no target field
	 *                                for a piece of source data, or the type can't be marshalled to E.
	 * @throws ClassCastException     if the final cast from the provided Type to the return type fails.
	 */
	<E> E marshall(Type type, DocumentElement elem) throws InstantiationException, MarshallerException, ClassCastException;
}
