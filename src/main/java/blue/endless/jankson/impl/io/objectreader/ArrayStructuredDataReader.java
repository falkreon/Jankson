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

package blue.endless.jankson.impl.io.objectreader;

import java.lang.reflect.Array;

import blue.endless.jankson.api.io.ObjectReaderFactory;
import blue.endless.jankson.api.io.StructuredData;

class ArrayStructuredDataReader extends DelegatingStructuredDataReader {
	
	private final Object arr;
	private final ObjectReaderFactory factory;
	private int index = 0;
	
	public ArrayStructuredDataReader(Object array, ObjectReaderFactory factory) {
		if (!array.getClass().isArray()) throw new IllegalArgumentException("This class can only be used with arrays.");
		
		this.arr = array;
		this.factory = (factory == null) ? new ObjectReaderFactory() : factory;
		buffer(StructuredData.ARRAY_START);
	}

	@Override
	protected void onDelegateEmpty() {
		if (index >= Array.getLength(arr)) {
			buffer(StructuredData.ARRAY_END);
			buffer(StructuredData.EOF);
		} else {
			Object o = Array.get(arr, index);
			index++;
			setDelegate(factory.getReader(o));
		}
	}
}