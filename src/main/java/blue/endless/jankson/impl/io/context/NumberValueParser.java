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

package blue.endless.jankson.impl.io.context;

import java.io.IOException;
import java.util.Arrays;
import java.util.Locale;

import blue.endless.jankson.api.SyntaxError;
import blue.endless.jankson.impl.io.Lookahead;
import blue.endless.jankson.impl.io.LookaheadCodePointReader;

public class NumberValueParser implements ValueParser {
	
	public static boolean canReadStatic(Lookahead lookahead) throws IOException {
		String infTest = lookahead.peekString(8);
		if (infTest.equals("Infinity") || infTest.equals("infinity")) return true;
		if (infTest.toLowerCase(Locale.ROOT).startsWith("nan")) return true;
		
		int ch = lookahead.peek();
		return Arrays.binarySearch(ParserConstants.NUMBER_VALUE_START, ch) >= 0;
	}
	
	@Override
	public boolean canRead(Lookahead lookahead) throws IOException {
		return canReadStatic(lookahead);
	}
	
	public static Number readStatic(LookaheadCodePointReader reader) throws IOException, SyntaxError {
		int startLine = reader.getLine();
		int startChar = reader.getCharacter();
		
		StringBuilder sb = new StringBuilder();
		int ch = reader.read();
		sb.appendCodePoint(ch);
		
		ch = reader.peek();
		while(Arrays.binarySearch(ParserConstants.NUMBER_VALUE_CHAR, ch) >= 0) {
			ch = reader.read();
			sb.appendCodePoint(ch);
			ch = reader.peek();
		}
		
		if (sb.charAt(0)=='.') sb.insert(0, '0');
		
		String result = sb.toString();
		if (
			result.equals("Infinity") ||
			result.equals("infinity") ||
			result.equals("+Infinity") ||
			result.equals("+infinity")
			) return Double.POSITIVE_INFINITY;
		
		if (
			result.equals("-Infinity") ||
			result.equals("-infinity")
			) return Double.NEGATIVE_INFINITY;
		
		if (result.toLowerCase().equals("nan")) return Double.NaN;
		
		if (result.startsWith("0x")) {
			try {
				result = result.substring(2);
				return Long.parseLong(result, 16);
			} catch (NumberFormatException ex) {
				SyntaxError err = new SyntaxError("Invalid number format for '"+result+"'.", ex);
				err.setStartParsing(startLine, startChar);
				err.setEndParsing(reader.getLine(), reader.getCharacter());
				throw err;
			}
		}
		
		if (result.startsWith("-0x")) {
			try {
				result = result.substring(3);
				return -Long.parseLong(result, 16);
			} catch (NumberFormatException ex) {
				SyntaxError err = new SyntaxError("Invalid number format for '"+result+"'.", ex);
				err.setStartParsing(startLine, startChar);
				err.setEndParsing(reader.getLine(), reader.getCharacter());
				throw err;
			}
		}
		
		try {
			if (result.indexOf(".")>=0 || result.indexOf("e")>=0) {
				return Double.parseDouble(result);
			} else {
				return Long.parseLong(sb.toString());
			}
		} catch (NumberFormatException ex) {
			SyntaxError err = new SyntaxError("Invalid number format for '"+result+"'.", ex);
			err.setStartParsing(startLine, startChar);
			err.setEndParsing(reader.getLine(), reader.getCharacter());
			throw err;
		}
	}
	
	@Override
	public Number read(LookaheadCodePointReader reader) throws IOException, SyntaxError {
		return readStatic(reader);
	}
}
