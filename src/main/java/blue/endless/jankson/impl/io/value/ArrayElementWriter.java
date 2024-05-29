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

package blue.endless.jankson.impl.io.value;

import java.io.IOException;

import blue.endless.jankson.api.document.ArrayElement;
import blue.endless.jankson.api.document.KeyValuePairElement;
import blue.endless.jankson.api.document.ValueElement;
import blue.endless.jankson.api.io.StructuredData;

public class ArrayElementWriter implements StrictValueElementWriter {
	
	private boolean initialBracketFound = false;
	private boolean finalBracketFound =  false;
	private ArrayElement value = new ArrayElement();
	private StrictValueElementWriter subordinate;
	
	@Override
	public void write(StructuredData data) throws IOException {
		if (!initialBracketFound) {
			// There is only one valid type, ARRAY_START
			if (data.type() == StructuredData.Type.ARRAY_START) {
				initialBracketFound = true;
			} else {
				throw new IOException("Expected array start, found "+data.type().name());
			}
		} else if (finalBracketFound) {
			if (data.type().isSemantic()) throw new IOException("Anomalous "+data.type().name()+" found after closing bracket of array");
		} else {
			// Anything we receive here is an array element
			
			if (subordinate != null) {
				subordinate.write(data);
				checkSubordinate();
				
				return;
			}
			
			switch(data.type()) {
				case ARRAY_END -> finalBracketFound = true;
				case ARRAY_START -> {
					subordinate = new ArrayElementWriter();
					subordinate.write(data);
					checkSubordinate();
				}
				case OBJECT_START -> {
					subordinate = new ObjectElementWriter();
					subordinate.write(data);
					checkSubordinate();
				}
				case PRIMITIVE -> {
					subordinate = new PrimitiveElementWriter();
					subordinate.write(data);
					checkSubordinate();
				}
				case NEWLINE -> {}
				case EOF -> {}
				case WHITESPACE -> {}
				case COMMENT -> {
					//TODO: Either apply this comment to the previous or next value depending on its type.
				}
				case OBJECT_KEY -> throw new IOException("Expected array element, but found an object key");
				case OBJECT_END -> throw new IOException("Found an object ending brace, but we're inside an array!");
			}
		}
	}

	private void checkSubordinate() throws IOException {
		if (subordinate != null && subordinate.isComplete()) {
			ValueElement result = subordinate.getValue();
			//result.getPrologue().addAll(bufferedValuePreamble);
			//bufferedValuePreamble.clear();
			value.add(result);
			
			subordinate = null;
		}
	}
	
	@Override
	public ArrayElement getValue() {
		return value;
	}

	@Override
	public boolean isComplete() {
		return initialBracketFound && finalBracketFound;
	}

}
