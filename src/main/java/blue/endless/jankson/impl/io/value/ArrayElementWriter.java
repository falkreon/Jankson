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
import java.util.ArrayList;

import blue.endless.jankson.api.SyntaxError;
import blue.endless.jankson.api.document.ArrayElement;
import blue.endless.jankson.api.document.CommentElement;
import blue.endless.jankson.api.document.CommentType;
import blue.endless.jankson.api.document.FormattingElement;
import blue.endless.jankson.api.document.NonValueElement;
import blue.endless.jankson.api.document.ValueElement;
import blue.endless.jankson.api.io.StructuredData;
import blue.endless.jankson.api.io.Deserializer;

public class ArrayElementWriter implements Deserializer<ValueElement> {
	
	private boolean initialBracketFound = false;
	private boolean finalBracketFound =  false;
	private ArrayElement value = new ArrayElement();
	private Deserializer<ValueElement> delegate;
	private ArrayList<NonValueElement> bufferedValuePrologue = new ArrayList<>();
	
	@Override
	public void write(StructuredData data) throws SyntaxError, IOException {
		if (!initialBracketFound) {
			// There is only one valid type, ARRAY_START
			if (data.type() == StructuredData.Type.ARRAY_START) {
				initialBracketFound = true;
			} else if (data.isComment()) {
				value.getPrologue().add(data.asComment());
			} else {
				throw new SyntaxError("Expected array start, found "+data.type().name());
			}
		} else if (finalBracketFound) {
			if (data.type().isSemantic()) throw new SyntaxError("Anomalous "+data.type().name()+" found after closing bracket of array");
		} else {
			// Anything we receive here is an array element
			
			if (delegate != null) {
				delegate.write(data);
				checkSubordinate();
				
				return;
			}
			
			switch(data.type()) {
				case ARRAY_END -> {
					finalBracketFound = true;
					if (!bufferedValuePrologue.isEmpty()) {
						value.getFooter().addAll(bufferedValuePrologue);
						bufferedValuePrologue.clear();
					}
				}
				case ARRAY_START -> {
					delegate = new ArrayElementWriter();
					delegate.write(data);
					checkSubordinate();
				}
				case OBJECT_START -> {
					delegate = new ObjectElementWriter();
					delegate.write(data);
					checkSubordinate();
				}
				case PRIMITIVE -> {
					delegate = new PrimitiveElementWriter();
					delegate.write(data);
					checkSubordinate();
				}
				case NEWLINE -> bufferedValuePrologue.add(FormattingElement.NEWLINE);
				case EOF -> {}
				case WHITESPACE -> {} //bufferedValuePrologue.add(new FormattingElement(data.value()));
				case COMMENT -> {
					// Prefer to make line-end comments part of the previous value's epilogue - - if there is a previous value!
					// Note that if we have even one thing in the epilogue, bump it down to the prologue of the next element.
					CommentElement comment = data.asComment();
					if (!value.isEmpty() && value.getLast().getEpilogue().isEmpty() && (comment.getCommentType() == CommentType.LINE_END || comment.getCommentType() == CommentType.OCTOTHORPE)) {
						value.getLast().getEpilogue().add(comment);
					} else {
						bufferedValuePrologue.add(comment);
					}
				}
				case OBJECT_KEY -> throw new SyntaxError("Expected array element, but found an object key");
				case OBJECT_END -> throw new SyntaxError("Found an object ending brace, but we're inside an array!");
			}
		}
	}

	private void checkSubordinate() throws IOException {
		if (delegate != null && delegate.isComplete()) {
			ValueElement result = delegate.getResult();
			result.getPrologue().addAll(bufferedValuePrologue);
			bufferedValuePrologue.clear();
			value.add(result);
			
			delegate = null;
		}
	}
	
	@Override
	public ArrayElement getResult() {
		return value;
	}

	@Override
	public boolean isComplete() {
		return initialBracketFound && finalBracketFound;
	}

}
