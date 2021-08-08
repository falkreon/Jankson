/*
 * MIT License
 *
 * Copyright (c) 2018-2020 Falkreon (Isaac Ellingson)
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

package blue.endless.jankson.impl.context.json;

import java.util.Arrays;

import blue.endless.jankson.api.Jankson;
import blue.endless.jankson.api.SyntaxError;
import blue.endless.jankson.api.document.ObjectElement;
import blue.endless.jankson.api.io.DeserializerOptions;
import blue.endless.jankson.impl.context.ElementContext;
import blue.endless.jankson.impl.context.ParserContext;

public class ObjectElementContext implements ElementContext<ObjectElement> {
	/**
	 * These characters are pretty much always treated as whitespace. If we're skipping all whitespace, we're skipping these.
	 */
	private static final char[] CHARS_WHITESPACE = {
		0x09, //tab
		0x0A, 0x0B, 0x0C, 0x0D, // LF, VT, FF, CR
		0x20, //space
		0x85, //NEL
		0xA0, //NBSP
		0x1680, //the space from Ogham script
		0x2000, 0x2001, 0x2002, 0x2003, 0x2004, 0x2005, 0x2006, 0x2007, 0x2008, 0x2009, 0x200A, //bunch of en and em spaces, figure space, etc.
		0x2028, 0x2029, //line-sep, para-sep
		0x202F, 0x205F, //narrow-nbsp, medium-math-space
		0x3000, //ideographic space
	};
	
	/**
	 * In certain contexts, especially HOCON, these additional chars are either blocking or skipped depending on where you are in parsing.
	 */
	private static final char[] CHARS_JOINER = {
		0x180E, 0x200B, 0x200C, 0x200D, 0x2060, 0xFEFF,
	};
	
	private ObjectElement result = new ObjectElement();
	private boolean openBraced;
	private boolean closeBraced;
	
	public ObjectElementContext() {
		//openBraced = options.hasHint(DeserializerOptions.Hint.ALLOW_BARE_ROOTS);
	}
	
	@Override
	public boolean consume(char codePoint, int line, int column, DeserializerOptions options) throws SyntaxError {
		if (!openBraced & !options.hasHint(DeserializerOptions.Hint.ALLOW_BARE_ROOTS)) {
			//We can only accept whitespace, comments, and open braces.
			if (codePoint=='{') {
				openBraced = true;
				return true;
			//} else if (codePoint=='/') {
				//TODO: Launch into comment context
			} else {
				//skip over whitespace
				if (Arrays.binarySearch(CHARS_WHITESPACE, codePoint)>=0) {
					return true;
				}
				//Skip over not-strictly-whitespace joiners, breaks, and BOMs
				if (Arrays.binarySearch(CHARS_JOINER, codePoint)>=0) {
					return true;
				}
			}
		} else if (!closeBraced) {
			//We're inside the object body. we can accept anything that could start a key (and then kick off the keyValue context)
			if (!openBraced && codePoint=='=') {
				throw new SyntaxError("Found close brace for a bare object.", line, column);
			}
			
		} else {
			//We're after the object body. We can't continue past a newline, including an end-of-line comment,
			//and we can't consume anything but line-end comments or whitespace.
			
		}
		
		
		
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void eof() throws SyntaxError {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean isComplete() {
		return false;
	}

	@Override
	public ObjectElement getResult() throws SyntaxError {
		return result;
	}

}
