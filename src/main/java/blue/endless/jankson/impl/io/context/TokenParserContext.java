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

package blue.endless.jankson.impl.io.context;

import java.io.IOException;

import blue.endless.jankson.api.SyntaxError;
import blue.endless.jankson.api.document.PrimitiveElement;
import blue.endless.jankson.api.io.ElementType;
import blue.endless.jankson.impl.io.LookaheadCodePointReader;

public class TokenParserContext implements ParserContext {
	private String result;
	
	@Override
	public ElementType parse(LookaheadCodePointReader reader) throws IOException, SyntaxError {
		StringBuilder result = new StringBuilder();
		
		while(true) {
			int ch = reader.peek();
			if (!Character.isJavaIdentifierPart(ch)) {
				this.result = result.toString();
				return ElementType.PRIMITIVE;
			}
			
			ch = reader.read();
			result.append(Character.toString(ch));
		}
	}

	@Override
	public String getStringValue() {
		return result;
	}

	@Override
	public PrimitiveElement getValue() {
		return switch(result) {
			case "true" -> PrimitiveElement.of(true);
			case "false" -> PrimitiveElement.of(false);
			case "Infinity" -> PrimitiveElement.of(Double.POSITIVE_INFINITY);
			case "-Infinity" -> PrimitiveElement.of(Double.NEGATIVE_INFINITY);
			case "NaN" -> PrimitiveElement.of(Double.NaN);
			
			default -> PrimitiveElement.of(result);
		};
	}

	@Override
	public boolean canEOFHere() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isComplete(LookaheadCodePointReader reader) {
		// TODO Auto-generated method stub
		return false;
	}

	private boolean isTokenTerminator(char ch) {
		return !Character.isJavaIdentifierPart(ch);
	}
}
