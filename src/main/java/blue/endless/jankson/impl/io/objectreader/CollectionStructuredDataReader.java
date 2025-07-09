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

import java.util.Collection;
import java.util.Iterator;

import blue.endless.jankson.api.io.ObjectReaderFactory;
import blue.endless.jankson.api.io.StructuredData;

public class CollectionStructuredDataReader extends DelegatingStructuredDataReader {
	private final Iterator<?> iter;
	private final ObjectReaderFactory factory;
	
	public CollectionStructuredDataReader(Collection<?> collection, ObjectReaderFactory factory) {
		iter = collection.iterator();
		this.factory = factory;
		buffer(StructuredData.ARRAY_START);
	}

	@Override
	protected void onDelegateEmpty() {
		if (!iter.hasNext()) {
			buffer(StructuredData.ARRAY_END);
			buffer(StructuredData.EOF);
		} else {
			this.setDelegate(factory.getReader(iter.next()));
		}
	}
}
