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

package blue.endless.jankson;

import java.io.IOException;
import java.io.StringReader;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import blue.endless.jankson.impl.io.LookaheadCodePointReader;

public class TestLookaheadReader {
	
	public static final String VALID_STRING = "This is a valid\u2705 String with some extended Unicode\uD83D\uDD22 code points.";
	public static final String INVALID_STRING = "[\uD83D\uD83D\uDD22]";
	public static final String INVALID_STRING_FIXED = "[\uFFFD\uD83D\uDD22]"; //Extra high surrogate replaced with "replacement character" U+FFFD
	
	@Test
	public void peekEntireString() throws IOException {
		LookaheadCodePointReader r = new LookaheadCodePointReader(new StringReader(VALID_STRING), 128);
		
		Assertions.assertEquals(VALID_STRING, r.peekString(128));
	}
	
	@Test
	public void peekThenReadEntire() throws IOException {
		LookaheadCodePointReader r = new LookaheadCodePointReader(new StringReader(VALID_STRING), 16);
		
		String a = r.peekString(16);
		String b = r.readString(128);
		Assertions.assertEquals("This is a valid\u2705", a);
		Assertions.assertEquals(VALID_STRING, b);
	}
	
	@Test
	public void readStringPastEnd() throws IOException {
		LookaheadCodePointReader r = new LookaheadCodePointReader(new StringReader(VALID_STRING), 16);
		
		String a = r.readString(128);
		String b = r.readString(16);
		
		Assertions.assertEquals(VALID_STRING, a);
		Assertions.assertEquals("", b);
	}
	
	@Test
	public void peekTwoCharCodePoint() throws IOException {
		LookaheadCodePointReader r = new LookaheadCodePointReader(new StringReader(VALID_STRING), 128);
		
		String a = r.peekString(51);
		Assertions.assertEquals("This is a valid\u2705 String with some extended Unicode\uD83D\uDD22", a);
		
		String b = r.readString(51);
		Assertions.assertEquals("This is a valid\u2705 String with some extended Unicode\uD83D\uDD22", b);
	}
	
	@Test
	public void peekThenDualRead() throws IOException {
		LookaheadCodePointReader r = new LookaheadCodePointReader(new StringReader(VALID_STRING), 16);
		String a = r.peekString(8);
		String b = r.readString(16);
		String c = r.readString(128);
		
		Assertions.assertEquals(VALID_STRING, b+c);
	}
	
	@Test
	public void readLessThenMoreThanPeek() throws IOException {
		LookaheadCodePointReader r = new LookaheadCodePointReader(new StringReader(VALID_STRING), 16);
		
		String a = r.peekString(16);
		String b = r.readString(8);
		String c = r.readString(128);
		
		Assertions.assertEquals(VALID_STRING, b+c);
	}
	
	@Test
	public void fixInvalidString() throws IOException {
		LookaheadCodePointReader r = new LookaheadCodePointReader(new StringReader(INVALID_STRING), 16);
		
		String result = r.readString(128);
		
		Assertions.assertEquals(INVALID_STRING_FIXED, result);
	}
	
	@Test
	public void peekChar() throws IOException {
		LookaheadCodePointReader r = new LookaheadCodePointReader(new StringReader(VALID_STRING), 32);
		
		Assertions.assertEquals(' ', r.peek(17)); //This is the space after the checkmark
	}
	
	@Test
	public void failExcessivePeek() throws IOException {
		LookaheadCodePointReader r = new LookaheadCodePointReader(new StringReader(VALID_STRING), 8);
		
		r.peekString(2);
		r.peekString(3);
		Assertions.assertThrows(IllegalArgumentException.class, ()->r.peek(9));
	}
}
