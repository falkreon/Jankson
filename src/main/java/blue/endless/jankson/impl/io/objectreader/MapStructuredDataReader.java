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

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;

import blue.endless.jankson.api.io.ObjectReaderFactory;
import blue.endless.jankson.api.io.StructuredData;

public class MapStructuredDataReader extends DelegatingStructuredDataReader {
	private final Map<Object, Object> map;
	private final Iterator<Map.Entry<Object, Object>> iterator;
	private final ObjectReaderFactory factory;
	
	@SuppressWarnings("unchecked")
	public MapStructuredDataReader(Map<?, ?> map, ObjectReaderFactory factory) {
		this.map = (Map<Object, Object>) map;
		this.iterator = this.map.entrySet().iterator();
		this.factory = factory;
		
		this.buffer(StructuredData.OBJECT_START);
	}
	
	@Override
	protected void onDelegateEmpty() throws IOException {
		if (!iterator.hasNext()) {
			buffer(StructuredData.OBJECT_END);
			buffer(StructuredData.EOF);
			return;
		}
		
		Map.Entry<Object, Object> entry = iterator.next();
		buffer(StructuredData.objectKey(
				Objects.toString(entry.getKey())
				));
		setDelegate(factory.getReader(entry.getValue()));
	}

}
