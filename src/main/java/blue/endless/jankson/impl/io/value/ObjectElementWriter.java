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
import java.util.ArrayList;
import java.util.List;

import blue.endless.jankson.api.SyntaxError;
import blue.endless.jankson.api.document.CommentElement;
import blue.endless.jankson.api.document.CommentType;
import blue.endless.jankson.api.document.FormattingElement;
import blue.endless.jankson.api.document.KeyValuePairElement;
import blue.endless.jankson.api.document.NonValueElement;
import blue.endless.jankson.api.document.ObjectElement;
import blue.endless.jankson.api.document.ValueElement;
import blue.endless.jankson.api.io.StructuredData;
import blue.endless.jankson.api.io.StructuredDataFunction;

public class ObjectElementWriter implements StructuredDataFunction<ValueElement> {
	
	private boolean initialBraceFound = false;
	private boolean finalBraceFound = false;
	private ObjectElement value = new ObjectElement();
	private StructuredDataFunction<ValueElement> subordinate;
	
	private List<NonValueElement> bufferedKeyPreamble = new ArrayList<>();
	private List<NonValueElement> bufferedValuePreamble = new ArrayList<>();
	private String bufferedKey;
	
	@Override
	public void write(StructuredData data) throws SyntaxError, IOException {
		
		if (subordinate != null) {
			subordinate.write(data);
			checkSubordinate();
		} else {
			if (!initialBraceFound) {
				// Only accept OBJECT_START
				if (data.isComment()) {
					System.out.println("Dealing with comment");
					value.getPrologue().add(data.asComment());
				} else {
					if (data.type() != StructuredData.Type.OBJECT_START) throw new SyntaxError("Required to start an object: OBJECT_START. Found: "+data.type().name());
					initialBraceFound = true;
				}
			} else if (!finalBraceFound) {
				
				if (bufferedKey == null) {
					// Expected: Key or comment
					switch(data.type()) {
						case COMMENT -> {
							if (data.value() instanceof CommentElement comment) {
								bufferedKeyPreamble.add(comment);
							} else {
								bufferedKeyPreamble.add(new CommentElement(data.value().toString(), CommentType.MULTILINE));
							}
						}
						
						case OBJECT_KEY -> bufferedKey = data.value().toString();
						
						case OBJECT_END -> {
							finalBraceFound = true;
							value.getFooter().addAll(bufferedKeyPreamble);
							bufferedKeyPreamble.clear();
						}
						
						default -> {
							if (data.type().isSemantic()) {
								throw new SyntaxError("Expected object key but found "+data.type().name());
							}
						}
					}
					
				} else {
					// Expected: Value
					
					switch (data.type()) {
						
						
						case PRIMITIVE -> {
							subordinate = new PrimitiveElementWriter();
							subordinate.write(data);
							checkSubordinate();
						}
						
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
						
						case COMMENT -> bufferedValuePreamble.add(data.asComment());
						case NEWLINE -> bufferedValuePreamble.add(FormattingElement.NEWLINE);
						case WHITESPACE -> {
							// If we ever enable whitespace, just uncomment the following:
							// bufferedValuePreamble.add(new FormattingElement(data.value().toString()));
						}
						
						case OBJECT_KEY -> throw new SyntaxError("Found two object keys in a row!");
						case OBJECT_END -> throw new SyntaxError("Found anomalous object end");
						case ARRAY_END -> throw new SyntaxError("Found anomalous array end");
						case EOF -> throw new SyntaxError("Stream ended before object was closed!");
					}
				}
			} else {
				// No semantic data is allowed after the ending brace
				if (data.type().isSemantic()) {
					throw new SyntaxError("Illegal "+data.type().name()+" found after end of object body");
				} else {
					if (data.isComment()) {
						value.getEpilogue().add(data.asComment());
					} else if (data.type() == StructuredData.Type.NEWLINE) {
						// This should technically be placed in the preamble of the next value, but if it's
						// been presented to us, better to absorb it than lose it.
						value.getEpilogue().add(FormattingElement.NEWLINE);
					}
				}
			}
		}
		
	}
	
	private void checkSubordinate() throws SyntaxError, IOException {
		if (subordinate != null && subordinate.isComplete()) {
			if (bufferedKey == null) throw new SyntaxError("Invalid writer state: we don't have a key for an object value");
			ValueElement result = subordinate.getResult();
			result.getPrologue().addAll(bufferedValuePreamble);
			bufferedValuePreamble.clear();
			KeyValuePairElement kvPair = new KeyValuePairElement(bufferedKey, result);
			kvPair.getPrologue().addAll(bufferedKeyPreamble);
			bufferedKeyPreamble.clear();
			
			value.add(kvPair);
			
			subordinate = null;
			bufferedKey = null;
		}
	}

	@Override
	public ObjectElement getResult() {
		return value;
	}

	@Override
	public boolean isComplete() {
		return initialBraceFound && finalBraceFound;
	}

}
