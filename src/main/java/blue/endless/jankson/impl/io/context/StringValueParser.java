/*
 * MIT License
 *
 * Copyright (c) 2018-2023 Falkreon (Isaac Ellingson)
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


/**
 * Value Parser for any quoted String except the triple-quoted "multiline string".
 */
public class StringValueParser implements ValueParser {
	
	@Override
	public boolean canRead(Lookahead lookahead) throws IOException {
		String test = lookahead.peekString(3);
		if (test.equals("\"\"\"")) return false; //Disclaim responsibility for triple-quotes
		
		int start = lookahead.peek();
		return start=='\'' || start=='"';
	}

	@Override
	public Object read(LookaheadCodePointReader reader) throws IOException, SyntaxError {
		int openQuote = reader.read();
		
		StringBuilder result = new StringBuilder();
		int ch = reader.read();
		
		while(ch!=openQuote) {
			if (ch==-1) throw new SyntaxError("Unmatched quote on a String value.");
			if (ch=='\n') throw new SyntaxError("Unescaped newline in a String value.");
			
			if (ch=='\\') {
				readEscapeSequence(reader, result);
			} else {
				result.appendCodePoint(ch);
			}
			
			ch = reader.read();
		}
		
		return result.toString();
	}
	
	/**
	 * Assuming the opening slash has already been read, consume an escape sequence from in and emit the result to out.
	 * @param in the stream
	 * @param out the result
	 */
	private void readEscapeSequence(LookaheadCodePointReader in, StringBuilder out) throws IOException, SyntaxError {
		int escape = in.read();
		switch(escape) {
			case 'b':
				out.append('\b');
				break;
			case 'f':
				out.append('\f');
				break;
			case 'n':  // regular \n
				out.append('\n');
				break;
			case 'r':
				out.append('\r');
				break;
			case 't':
				out.append('\t');
				break;
			case 'v':
				out.appendCodePoint(0x000B); //vertical tab
				break;
			case '0':
				out.appendCodePoint(0x0000); //null. TODO: Should this be forbidden? I super don't like seeing these in Strings.
				break;
			case '"':
				out.append('"');
				break;
			case '\'':
				out.append('\'');
				break;
			case '\\':
				out.append('\\');
				break;
			case '\n': // JSON5 multiline string - \ followed by a newline prevents that newline from being emitted
				break;
			case 'u':
			case 'U':
				int unicode = in.peek();
				if (unicode=='u' || unicode=='U') {
					// Unwrap one 'u' from this String, which was most likely meant to illustrate a unicode escape without invoking that unicode escape.
					in.read(); //eat that
					out.append('\\'); //We ate the slash so emit a replacement
					out.appendCodePoint(unicode); //emit the *second* 'u' in whatever case the user used. Canonically this would be lowercase.
				} else {
					StringBuilder sb = new StringBuilder();
					
					try {
						sb.appendCodePoint(in.read());
						sb.appendCodePoint(in.read());
						sb.appendCodePoint(in.read());
						sb.appendCodePoint(in.read());
						int code = Integer.parseInt(sb.toString(), 16);
						out.appendCodePoint(code);
					} catch (NumberFormatException ex) {
						throw new SyntaxError("Invalid unicode escape sequence '"+sb.toString()+"'");
					}
				}
				break;
			case 'x':
			case 'X':
				int unicodeX = in.peek();
				if (unicodeX=='x' || unicodeX=='X') {
					in.read();
					out.append('\\');
					out.appendCodePoint(unicodeX);
				} else {
					StringBuilder xb = new StringBuilder();
					
					try {
						xb.appendCodePoint(in.read());
						xb.appendCodePoint(in.read());
						int code = Integer.parseInt(xb.toString(), 16);
						out.appendCodePoint(code);
					} catch (NumberFormatException ex) {
						throw new SyntaxError("Invalid unicode escape sequence '"+xb.toString()+"'");
					}
				}
				break;
			default:
				if (Character.isDigit(escape)) {
					throw new SyntaxError("Numeric escapes are forbidden ('\\"+Character.toString(escape)+"')");
				}
				
				out.appendCodePoint(escape);
		}
	}

}
