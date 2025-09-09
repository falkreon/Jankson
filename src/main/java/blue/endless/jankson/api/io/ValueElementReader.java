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

package blue.endless.jankson.api.io;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

import blue.endless.jankson.api.SyntaxError;
import blue.endless.jankson.api.document.ArrayElement;
import blue.endless.jankson.api.document.ObjectElement;
import blue.endless.jankson.api.document.PrimitiveElement;
import blue.endless.jankson.api.document.ValueElement;
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
		private final ArrayElement value;
		private final Iterator<ValueElement> iterator;
		private boolean complete = false;
		
		public ArrayValueReader(ArrayElement value) {
			this.value = value;
			this.iterator = value.iterator();
			buffer(StructuredData.ARRAY_START);
		}
		
		@Override
		protected void onDelegateEmpty() throws IOException {
			if (complete) {
				buffer(StructuredData.EOF);
				return;
			}
			
			if (iterator.hasNext()) {
				ValueElement cur = iterator.next();
				setDelegate(of(cur));
			} else {
				buffer(StructuredData.ARRAY_END);
				complete = true;
			}
		}
		
		@Override
		public void transferTo(StructuredDataWriter writer) throws SyntaxError, IOException {
			if (writer instanceof BufferedStructuredDataWriter buffered) {
				buffered.write(value);
			} else {
				super.transferTo(writer);
			}
		}
	}
	
	private static class ObjectValueReader extends DelegatingStructuredDataReader {
		private final ObjectElement value;
		private final Iterator<Map.Entry<String, ValueElement>> iterator;
		private boolean complete = false;
		
		public ObjectValueReader(ObjectElement value) {
			this.value = value;
			this.iterator = value.entrySet().iterator();
			buffer(StructuredData.OBJECT_START);
		}
		
		@Override
		protected void onDelegateEmpty() throws IOException {
			if (complete) {
				buffer(StructuredData.EOF);
				return;
			}
			
			if (iterator.hasNext()) {
				Map.Entry<String, ValueElement> entry = iterator.next();
				buffer(StructuredData.objectKey(entry.getKey()));
				setDelegate(ValueElementReader.of(entry.getValue()));
			} else {
				buffer(StructuredData.OBJECT_END);
				complete = true;
			}
		}
		
		@Override
		public void transferTo(StructuredDataWriter writer) throws SyntaxError, IOException {
			if (writer instanceof BufferedStructuredDataWriter buffered) {
				buffered.write(value);
			} else {
				super.transferTo(writer);
			}
		}
		
	}
}
