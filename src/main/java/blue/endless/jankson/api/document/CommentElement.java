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

package blue.endless.jankson.api.document;

import java.io.IOException;

import blue.endless.jankson.api.SyntaxError;
import blue.endless.jankson.api.io.StructuredData;
import blue.endless.jankson.api.io.StructuredDataWriter;

public final class CommentElement implements NonValueElement {
	protected boolean isDefault = false;
	protected String value;
	protected CommentType commentType;
	
	public CommentElement(String comment) {
		value = comment;
		commentType = CommentType.MULTILINE;
	}
	
	public CommentElement(String comment, CommentType commentType) {
		value = comment;
		this.commentType = commentType;
	}
	
	public String getValue() { return value; }
	
	public String setValue(String value) {
		String result = this.value;
		this.value = value;
		return result;
	}
	
	public CommentType getCommentType() {
		return commentType;
	}
	
	@Override
	public boolean isCommentElement() {
		return true;
	}

	@Override
	public CommentElement asCommentElement() {
		return this;
	}
	
	public CommentElement copy() {
		CommentElement result = new CommentElement(this.value);
		result.value = this.value;
		result.commentType = this.commentType;
		result.isDefault = isDefault;
		return result;
	}
	
	@Override
	public boolean isDefault() {
		return isDefault;
	}
	
	@Override
	public void setDefault(boolean isDefault) {
		this.isDefault = isDefault;
	}
	
	@Override
	public void write(StructuredDataWriter writer) throws SyntaxError, IOException {
		writer.write(new StructuredData(StructuredData.Type.COMMENT, this));
	}
	
	@Override
	public String toString() {
		switch(commentType) {
		case OCTOTHORPE:
			return "# "+this.value;
		case MULTILINE:
			return "/*"+this.value+"*/";
		case LINE_END:
			return "//"+this.value;
		case DOC:
			return "/**"+this.value+"*/";
		default:
			return this.value;
		}
	}
}
