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

package blue.endless.jankson.impl.io.context;

import java.io.IOException;

import blue.endless.jankson.api.SyntaxError;
import blue.endless.jankson.impl.io.Lookahead;
import blue.endless.jankson.impl.io.LookaheadCodePointReader;

public class TokenValueParser implements ValueParser {
	private static final String VALID_UNQUOTED_CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_";
	


	@Override
	public boolean canRead(Lookahead reader) throws IOException {
		int ch = reader.peek();
		return ch != -1 && VALID_UNQUOTED_CHARS.indexOf(ch) != -1;
	}

	@Override
	public String read(LookaheadCodePointReader reader) throws IOException, SyntaxError {
		return readStatic(reader);
	}
	
	public static String readStatic(LookaheadCodePointReader reader) throws IOException, SyntaxError {
		StringBuilder token = new StringBuilder();
		int ch = reader.peek();
		while(ch != -1 && VALID_UNQUOTED_CHARS.indexOf(ch) != -1) {
			token.appendCodePoint(reader.read());
			ch = reader.peek();
		}
		
		if (token.isEmpty()) throw new SyntaxError("Expected unquoted token but found illegal characters.", reader.getLine(), reader.getCharacter());
		return token.toString();
	}
	
}
