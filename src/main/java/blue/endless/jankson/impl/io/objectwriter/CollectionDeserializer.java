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

package blue.endless.jankson.impl.io.objectwriter;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Type;
import java.util.Collection;

import blue.endless.jankson.api.SyntaxError;
import blue.endless.jankson.api.io.ObjectWriter;
import blue.endless.jankson.api.io.AbstractDeserializer;
import blue.endless.jankson.api.io.StructuredData;
import blue.endless.jankson.api.io.Deserializer;
import blue.endless.jankson.impl.magic.ClassHierarchy;

public class CollectionDeserializer<V, T extends Collection<V>> extends AbstractDeserializer<Collection<V>>{
	
	private final Type memberType;
	private final T result;
	
	private boolean startFound = false;
	private boolean endFound = false;
	
	private Deserializer<V> delegate = null;
	
	public CollectionDeserializer(T result, Type memberType) {
		this.result = result;
		this.memberType = memberType;
	}
	
	public CollectionDeserializer(Type resultType) throws IllegalArgumentException {
		this(createObject(resultType), ClassHierarchy.getCollectionTypeArgument(resultType));
	}
	
	@Override
	public Collection<V> getResult() {
		return result;
	}
	
	private void checkDelegate() {
		if (delegate == null) return;
		if (delegate.isComplete()) {
			result.add(delegate.getResult());
			delegate = null;
		}
	}
	
	@SuppressWarnings("unchecked")
	@Override
	protected void process(StructuredData data) throws SyntaxError, IOException {
		if (delegate != null) {
			delegate.write(data);
			checkDelegate();
			return;
		}
		
		if (!startFound) {
			if (data.type() == StructuredData.Type.ARRAY_START) {
				startFound = true;
				return;
			} else {
				if (data.type().isSemantic()) throw new SyntaxError("Expected an array, found "+data.type());
			}
		} else if (!endFound) {
			switch(data.type()) {
				case ARRAY_END -> {
					endFound = true;
				}
				
				case EOF -> {
					throw new SyntaxError("Expected a value or end of array. Found EOF instead!");
				}
				
				default -> {
					delegate = (Deserializer<V>) ObjectWriter.getObjectWriter(memberType, data, null);
					delegate.write(data);
					checkDelegate();
				}
			}
			
		} else {
			if (data.type().isSemantic() && data.type() != StructuredData.Type.EOF) {
				throw new SyntaxError("Data found past end of array.");
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	private static <V> V createObject(Type t) throws IllegalArgumentException {
		try {
			Constructor<V> zeroArgConstructor = (Constructor<V>) ClassHierarchy.getErasedClass(t).getConstructor();
			
			boolean access = zeroArgConstructor.canAccess(null);
			if (!access) zeroArgConstructor.setAccessible(true);
			V result = zeroArgConstructor.newInstance();
			if (!access) zeroArgConstructor.setAccessible(false);
			return result;
			
		} catch (Throwable throwable) {
			throw new IllegalArgumentException("Could not create the collection");
		}
	}
}
