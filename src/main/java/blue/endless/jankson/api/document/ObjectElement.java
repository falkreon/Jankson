/*
 * MIT License
 *
 * Copyright (c) 2018-2022 Falkreon (Isaac Ellingson)
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

package blue.endless.jankson.api.document;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

public class ObjectElement implements ValueElement {
	protected CommentElement commentBefore = null;
	protected CommentElement commentAfter = null;
	protected List<DocumentElement> entries = new ArrayList<>();
	
	@Nullable
	public ValueElement get(String key) {
		for(DocumentElement entry : entries) {
			if (entry instanceof KeyValuePairElement) {
				if (((KeyValuePairElement) entry).getKey().equals(key)) {
					return ((KeyValuePairElement) entry).getValue();
				}
			}
		}
		
		return null;
	}
	
	public DocumentElement put(String key, ValueElement value) {
		//Validate
		if (
				value instanceof KeyValuePairElement ||
				value instanceof CommentElement) throw new IllegalArgumentException();
		
		for(DocumentElement entry : entries) {
			if (entry instanceof KeyValuePairElement) {
				KeyValuePairElement pair = (KeyValuePairElement) entry;
				
				if (pair.getKey().equals(key)) {
					return pair.setValue(value);
				}
			}
		}
		
		//No matching KeyValueDocumentEntry. Add one at the end of the object's sub-document
		entries.add(new KeyValuePairElement(key, value));
		return null;
	}
	
	public CommentElement commentBefore() {
		return commentBefore;
	}
	
	public CommentElement commentAfter() {
		return commentAfter;
	}
	
	public void setCommentBefore(String comment) {
		commentBefore = new CommentElement(comment);
	}
	
	public void setCommentBefore(CommentElement comment) {
		commentBefore = comment;
	}
	
	public void setCommentAfter(String comment) {
		commentAfter = new CommentElement(comment, CommentType.LINE_END);
	}
	
	public void setCommentAfter(CommentElement comment) {
		commentAfter = comment;
		//Possibly force comment as a lineEndComment but for now assume user knows what they're doing
	}

	@Override
	public boolean isCommentElement() {
		return false;
	}

	@Override
	public CommentElement asCommentElement() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isValueElement() {
		return true;
	}

	@Override
	public ValueElement asValueElement() {
		return this;
	}
}
