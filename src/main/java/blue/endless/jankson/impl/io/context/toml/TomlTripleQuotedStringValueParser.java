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

package blue.endless.jankson.impl.io.context.toml;

import java.io.IOException;

import blue.endless.jankson.api.SyntaxError;
import blue.endless.jankson.impl.io.Lookahead;
import blue.endless.jankson.impl.io.LookaheadCodePointReader;
import blue.endless.jankson.impl.io.context.StringValueParser;
import blue.endless.jankson.impl.io.context.ValueParser;

public class TomlTripleQuotedStringValueParser implements ValueParser {
	private static final String TRIPLE_QUOTE = "\"\"\"";
	private static final String TRIPLE_APOSTROPHE = "'''";
	
	@Override
	public boolean canRead(Lookahead lookahead) throws IOException {
		return canReadStatic(lookahead);
	}

	@Override
	public String read(LookaheadCodePointReader reader) throws IOException, SyntaxError {
		return readStatic(reader);
	}
	
	public static boolean canReadStatic(Lookahead lookahead) throws IOException {
		String str = lookahead.peekString(3);
		return str.equals(TRIPLE_QUOTE) || str.equals(TRIPLE_APOSTROPHE);
	}

	
	public static String readStatic(LookaheadCodePointReader reader) throws IOException, SyntaxError {
		String initial = reader.readString(3); // Consume the initial triple-quote
		boolean literal = (initial.equals(TRIPLE_APOSTROPHE));
		
		StringBuilder result = new StringBuilder();
		
		int firstPeek = reader.peek();
		if (firstPeek == '\n' || firstPeek == '\r') {
			reader.read(); // If the first character after the triple quote is a newline, discard it.
		}
		
		while(!reader.peekString(3).equals(initial)) {
			int ch = reader.read();
			if (ch == '\\' && !literal) {
				// This is an escaped character
				int peek = reader.peek();
				if (peek == '\n' || peek == '\r') {
					reader.read(); // Discard the newline
					discardUntilNextNonWhitespace(reader);
				} else {
					// This is an ordinary String escape, use the regular method to handle it
					StringValueParser.readEscapeSequence(reader, result);
				}
			} else {
				result.appendCodePoint(ch);
			}
		}
		
		return result.toString();
	}
	
	/**
	 * Discard all whitespace, including line breaks, up to the next non-whitespace character
	 */
	public static void discardUntilNextNonWhitespace(LookaheadCodePointReader reader) throws IOException {
		int peek = reader.peek();
		while (Character.isWhitespace(peek)) {
			reader.read();
			peek = reader.peek();
		}
	}
}
