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
import java.util.Iterator;
import java.util.Map;

import blue.endless.jankson.api.document.ArrayElement;
import blue.endless.jankson.api.document.ObjectElement;
import blue.endless.jankson.api.document.PrimitiveElement;
import blue.endless.jankson.api.document.ValueElement;
import blue.endless.jankson.impl.io.StructuredDataBuffer;
import blue.endless.jankson.impl.io.objectreader.DelegatingStructuredDataReader;

public class ValueElementReader {
	
	private ValueElementReader() {}
	
	public static StructuredDataReader of(ValueElement val) {
		if (val instanceof PrimitiveElement primitive) {
			StructuredDataBuffer buf = new StructuredDataBuffer();
			buf.write(StructuredData.primitive(primitive));
			return buf;
		} else if (val instanceof ArrayElement array) {
			return new ArrayValueReader(array);
		} else if (val instanceof ObjectElement object) {
			return new ObjectValueReader(object);
		} else {
			throw new IllegalArgumentException("Unknown element type");
		}
	}
	
	private static class ArrayValueReader extends DelegatingStructuredDataReader {
		private final Iterator<ValueElement> iterator;
		
		public ArrayValueReader(ArrayElement value) {
			this.iterator = value.iterator();
		}
		
		@Override
		protected void onDelegateEmpty() throws IOException {
			if (iterator.hasNext()) {
				ValueElement cur = iterator.next();
				setDelegate(of(cur));
			} else {
				buffer(StructuredData.EOF);
			}
		}
	}
	
	private static class ObjectValueReader extends DelegatingStructuredDataReader {
		private final Iterator<Map.Entry<String, ValueElement>> iterator;
		
		public ObjectValueReader(ObjectElement value) {
			iterator = value.entrySet().iterator();
		}
		
		@Override
		protected void onDelegateEmpty() throws IOException {
			if (iterator.hasNext()) {
				Map.Entry<String, ValueElement> entry = iterator.next();
				buffer(StructuredData.objectKey(entry.getKey()));
				setDelegate(ValueElementReader.of(entry.getValue()));
			} else {
				buffer(StructuredData.EOF);
			}
		}
		
	}
}
