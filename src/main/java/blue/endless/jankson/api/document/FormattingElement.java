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

public class FormattingElement implements NonValueElement {
	public static FormattingElement NEWLINE = new FormattingElement("\n");
	//TODO: Should we have additional elements such as INDENT and SPACE?
	
	private String representation;
	
	private FormattingElement(String representation) {
		this.representation = representation;
	}
	
	public String asString() {
		return representation;
	}
	
	@Override
	public boolean isFormattingElement() {
		return true;
	}
	
	@Override
	public FormattingElement asFormattingElement() {
		return this;
	}
	
	@Override
	public FormattingElement clone() {
		//Because formatting elements are singletons, cloning them just returns the same references to the singleton
		return this;
	}
	
	public boolean isDefault() {
		return true;
	}
	
	@Override
	public void setDefault(boolean isDefault) {
		//Ignore. Formatting is always considered default.
	}
	
	@Override
	public void write(StructuredDataWriter writer) throws SyntaxError, IOException {
		if (this == NEWLINE) {
			writer.write(StructuredData.NEWLINE);
		} else {
			writer.write(StructuredData.whitespace(representation));
		}
	}
}
