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

import blue.endless.jankson.api.document.ValueElement;
import blue.endless.jankson.impl.io.value.ArrayElementWriter;
import blue.endless.jankson.impl.io.value.ObjectElementWriter;
import blue.endless.jankson.impl.io.value.PrimitiveElementWriter;
import blue.endless.jankson.impl.io.value.StrictValueElementWriter;

/**
 * StructuredDataWriter that assembles a ValueElement. Much like StringWriter, this "captures" data
 * that would normally be serialized and assembles it into intermediate state.
 */
public class ValueElementWriter implements StructuredDataWriter {
	
	private StrictValueElementWriter delegate = null;
	private ValueElement result = null;
	
	@Override
	public void write(StructuredData data) throws IOException {
		if (delegate != null) {
			// After we've completed our data, we could potentially consume a trailer
			if (delegate.isComplete()) {
				
			}
			
			delegate.write(data);
			if (delegate.isComplete()) {
				result = delegate.getValue();
				delegate = null;
			}
		} else {
		
			switch(data.type()) {
				case ARRAY_END -> throw new IOException("Illegal Array-End found");
				case ARRAY_START -> delegate = new ArrayElementWriter();
				case COMMENT -> {
					//TODO: Handle prologues and epilogues
				}
				case EOF -> {}
				case NEWLINE -> {}
				case OBJECT_END -> throw new IOException("Illegal Object-End found");
				case OBJECT_KEY -> throw new IOException("Illegal Object-Key found");
				case OBJECT_START -> delegate = new ObjectElementWriter();
				case PRIMITIVE -> delegate = new PrimitiveElementWriter();
				case WHITESPACE -> {}
			}
			if (delegate != null) {
				//We just set a delegate, consume the data
				delegate.write(data);
			}
		
		}
		
	}
	
	public ValueElement toValueElement() {
		return result;
	}
}
