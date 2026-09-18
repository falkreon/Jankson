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
import java.util.ArrayDeque;
import java.util.IdentityHashMap;
import java.util.Iterator;

import blue.endless.jankson.api.SyntaxError;
import blue.endless.jankson.api.document.ArrayElement;
import blue.endless.jankson.api.document.KeyValuePairElement;
import blue.endless.jankson.api.document.NonValueElement;
import blue.endless.jankson.api.document.ObjectElement;
import blue.endless.jankson.api.document.PrimitiveElement;
import blue.endless.jankson.api.document.ValueElement;

public class ValueElementReader {
	private ValueElementReader() {}

	/** Reads the same events as ValueElement.write, without recursive tree traversal. */
	public static StructuredDataReader of(ValueElement value) {
		return new TreeReader(value);
	}

	private record Leave(ValueElement value) {}

	private static final class TreeReader implements StructuredDataReader {
		private final ValueElement root;
		private boolean started;
		private final ArrayDeque<Object> pending = new ArrayDeque<>();
		private final IdentityHashMap<ValueElement, Boolean> active = new IdentityHashMap<>();

		TreeReader(ValueElement value) {
			root = value;
			pending.push(value);
		}

		@Override public void transferTo(StructuredDataWriter writer) throws SyntaxError, IOException {
			if (!started && writer instanceof BufferedStructuredDataWriter buffered) {
				// Validate the whole active path before handing the tree to a random-access writer.
				while (hasNext()) next();
				buffered.write(root);
			} else {
				StructuredDataReader.super.transferTo(writer);
			}
		}

		@Override public boolean hasNext() { return !pending.isEmpty(); }

		@Override public StructuredData next() throws SyntaxError, IOException {
			started = true;
			while (!pending.isEmpty()) {
				Object item = pending.pop();
				if (item instanceof StructuredData data) return data;
				if (item instanceof Leave leave) {
					active.remove(leave.value());
				} else if (item instanceof Iterator<?> iterator) {
					if (iterator.hasNext()) {
						pending.push(iterator);
						pending.push(iterator.next());
					}
				} else if (item instanceof NonValueElement decoration) {
					StructuredDataBuffer buffer = new StructuredDataBuffer();
					decoration.write(buffer);
					return buffer.next();
				} else if (item instanceof KeyValuePairElement pair) {
					pending.push(pair.getValue());
					pending.push(StructuredData.objectKey(pair.getKey()));
					pending.push(pair.getPrologue().iterator());
				} else if (item instanceof ValueElement value) {
					if (active.put(value, Boolean.TRUE) != null) {
						throw new IOException("Cyclic object reference while serializing "+value.getClass().getTypeName());
					}
					pending.push(new Leave(value));
					pending.push(value.getEpilogue().iterator());
					if (value instanceof PrimitiveElement primitive) {
						pending.push(StructuredData.primitive(primitive));
					} else if (value instanceof ArrayElement array) {
						pending.push(StructuredData.ARRAY_END);
						pending.push(array.getFooter().iterator());
						pending.push(array.iterator());
						pending.push(StructuredData.ARRAY_START);
					} else if (value instanceof ObjectElement object) {
						pending.push(StructuredData.OBJECT_END);
						pending.push(object.getFooter().iterator());
						pending.push(object.iterator());
						pending.push(StructuredData.OBJECT_START);
					} else {
						throw new IllegalArgumentException("Unknown element type");
					}
					pending.push(value.getPrologue().iterator());
				}
			}
			return StructuredData.EOF;
		}
	}
}
