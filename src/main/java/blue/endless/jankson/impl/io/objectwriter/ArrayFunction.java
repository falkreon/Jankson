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

package blue.endless.jankson.impl.io.objectwriter;

import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Type;
import java.util.ArrayList;

import blue.endless.jankson.api.SyntaxError;
import blue.endless.jankson.api.io.ObjectWriter;
import blue.endless.jankson.api.io.StructuredData;
import blue.endless.jankson.api.io.StructuredDataFunction;
import blue.endless.jankson.impl.magic.ClassHierarchy;

public class ArrayFunction<V> extends SingleValueFunction<Object> {
	
	private ArrayList<V> result = new ArrayList<>();
	//private final Class<?> arrayType;
	private final Type elementType;
	private boolean foundStart = false;
	private boolean foundEnd = false;
	private StructuredDataFunction<V> delegate;
	
	public ArrayFunction(Class<?> arrayType) {
		//this.arrayType = arrayType;
		if (!arrayType.isArray()) throw new IllegalArgumentException("Expected: Array type, got "+arrayType.getCanonicalName()+" instead.");
		this.elementType = arrayType.getComponentType();
	}
	
	//@SuppressWarnings("unchecked")
	@Override
	public Object getResult() {
		Object resultArray = Array.newInstance(ClassHierarchy.getErasedClass(elementType), result.size());
		for(int i=0; i<result.size(); i++) {
			V v = result.get(i);
			Array.set(resultArray, i, v);
		}
		return resultArray;
	}
	
	private void checkDelegate() {
		if (delegate != null && delegate.isComplete() && !foundEnd) {
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
		
		if (!foundStart) {
			if (data.type() == StructuredData.Type.ARRAY_START) {
				foundStart = true;
				return;
			} else {
				if (data.type().isSemantic()) throw new SyntaxError("Expected an array, found "+data.type());
			}
		} else if (!foundEnd) {
			switch(data.type()) {
				case ARRAY_END -> {
					foundEnd = true;
				}
				
				case EOF -> {
					throw new SyntaxError("Expected a value or end of array. Found EOF instead!");
				}
				
				default -> {
					if (!data.type().isSemantic()) return;
					delegate = (StructuredDataFunction<V>) ObjectWriter.getObjectWriter(elementType, data, null);
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

}
