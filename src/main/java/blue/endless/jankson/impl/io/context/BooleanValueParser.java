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

import blue.endless.jankson.api.SyntaxError;
import blue.endless.jankson.impl.io.Lookahead;
import blue.endless.jankson.impl.io.LookaheadCodePointReader;

public class BooleanValueParser implements ValueParser {
	
	public static boolean canReadStatic(Lookahead lookahead) throws IOException {
		//TODO: We probably need to peek one more character ahead and make sure that the character after our String is a valid breaking code point
		String maybeFalse = lookahead.peekString(5);
		return maybeFalse.equals("false") || maybeFalse.startsWith("true");
	}
	
	@Override
	public boolean canRead(Lookahead lookahead) throws IOException {
		return canReadStatic(lookahead);
	}

	public static Boolean readStatic(LookaheadCodePointReader reader) throws IOException, SyntaxError {
		String start = reader.readString(4);
		if (start.equals("true")) return Boolean.TRUE;
		start += reader.readString(1);
		if (start.equals("false")) return Boolean.FALSE;
		
		throw new IllegalStateException("Couldn't parse boolean value '"+start+"'.");
	}
	
	@Override
	public Boolean read(LookaheadCodePointReader reader) throws IOException, SyntaxError {
		return readStatic(reader);
	}

}
