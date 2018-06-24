/*
 * MIT License
 *
 * Copyright (c) 2018 Falkreon
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

package blue.endless.jankson.impl;

import blue.endless.jankson.Jankson;
import blue.endless.jankson.JsonPrimitive;

public class StringParserContext implements ParserContext<JsonPrimitive> {
	private int quote;
	private boolean escape = false;
	private StringBuilder builder = new StringBuilder();
	private boolean complete = false;
	
	public StringParserContext(int quote) {
		this.quote = quote;
	}

	@Override
	public boolean consume(int codePoint, Jankson loader) {
		if (escape) {
			//TODO: Support additional escapes like \t and \n
			builder.append((char)codePoint);
			escape = false;
		} else {
			if (codePoint==quote) {
				complete = true;
				return true;
			}
			
			if (codePoint>0xD800) {
				builder.append((char)codePoint);
			} else {
				//Construct a high and low surrogate pair for this code point
				//TODO: Finish implementing
				int highSurrogate = codePoint - 0x10000;
				
				builder.append((char)codePoint);
			}
			
			
			if (codePoint=='\\') escape=true;
			
		}
		
		return true;
	}

	@Override
	public boolean isComplete() {
		return complete;
	}

	@Override
	public JsonPrimitive getResult() {
		return new JsonPrimitive(builder.toString());
	}

	@Override
	public void eof() throws SyntaxError {
		throw new SyntaxError("Expected to find '"+((char)quote)+"' to end a String, found EOF instead.");
	}
}
