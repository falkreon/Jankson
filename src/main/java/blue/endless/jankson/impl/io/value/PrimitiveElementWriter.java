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

package blue.endless.jankson.impl.io.value;

import java.io.IOException;
import java.util.Objects;

import blue.endless.jankson.api.document.PrimitiveElement;
import blue.endless.jankson.api.document.ValueElement;
import blue.endless.jankson.api.io.StructuredData;
import blue.endless.jankson.api.io.Deserializer;

public class PrimitiveElementWriter implements Deserializer<ValueElement> {
	
	private boolean complete = false;
	private PrimitiveElement value = null;
	
	@Override
	public void write(StructuredData data) throws IOException {
		if (data.type() == StructuredData.Type.EOF && value != null) return;
		if (!data.isPrimitive()) throw new IOException("Expected a primitive value, but found "+Objects.toString(data));
		value = data.asPrimitive();
		complete = true;
	}

	@Override
	public PrimitiveElement getResult() {
		return value;
	}

	@Override
	public boolean isComplete() {
		return complete;
	}
	
}
