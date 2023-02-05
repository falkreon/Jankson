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

import blue.endless.jankson.api.SyntaxError;
import blue.endless.jankson.impl.io.LookaheadCodePointReader;
import blue.endless.jankson.impl.io.context.StringValueParser;

public class TestStringValueParser {
	
	//Solidus Escape Snakes
	
	@Test
	public void testApostrophe() throws IOException, SyntaxError {
		LookaheadCodePointReader r = new LookaheadCodePointReader(new StringReader("\"\\'\"")); // "\'"
		StringValueParser parser = new StringValueParser();
		
		Assertions.assertTrue(parser.canRead(r));
		Assertions.assertEquals("'", parser.read(r));
	}
	
	@Test
	public void testQuote() throws IOException, SyntaxError {
		LookaheadCodePointReader r = new LookaheadCodePointReader(new StringReader("\"\\\"\"")); // "\""
		StringValueParser parser = new StringValueParser();
		
		Assertions.assertTrue(parser.canRead(r));
		Assertions.assertEquals("\"", parser.read(r));
	}
	
	@Test
	public void testSolidus() throws IOException, SyntaxError {
		LookaheadCodePointReader r = new LookaheadCodePointReader(new StringReader("\"\\\\\"")); // "\\"
		StringValueParser parser = new StringValueParser();
		
		Assertions.assertTrue(parser.canRead(r));
		Assertions.assertEquals("\\", parser.read(r));
	}
	
	@Test
	public void testBackspace() throws IOException, SyntaxError {
		LookaheadCodePointReader r = new LookaheadCodePointReader(new StringReader("\"\\b\"")); // "\b"
		StringValueParser parser = new StringValueParser();
		
		Assertions.assertTrue(parser.canRead(r));
		Assertions.assertEquals("\b", parser.read(r));
	}
	
	@Test
	public void testFormFeed() throws IOException, SyntaxError {
		LookaheadCodePointReader r = new LookaheadCodePointReader(new StringReader("\"\\f\"")); // "\f"
		StringValueParser parser = new StringValueParser();
		
		Assertions.assertTrue(parser.canRead(r));
		Assertions.assertEquals("\f", parser.read(r));
	}
	
	@Test
	public void testNewline() throws IOException, SyntaxError {
		LookaheadCodePointReader r = new LookaheadCodePointReader(new StringReader("\"\\n\"")); // "\n"
		StringValueParser parser = new StringValueParser();
		
		Assertions.assertTrue(parser.canRead(r));
		Assertions.assertEquals("\n", parser.read(r));
	}
	
	@Test
	public void testCarriageReturn() throws IOException, SyntaxError {
		LookaheadCodePointReader r = new LookaheadCodePointReader(new StringReader("\"\\r\"")); // "\r"
		StringValueParser parser = new StringValueParser();
		
		Assertions.assertTrue(parser.canRead(r));
		Assertions.assertEquals("\r", parser.read(r));
	}
	
	@Test
	public void testTab() throws IOException, SyntaxError {
		LookaheadCodePointReader r = new LookaheadCodePointReader(new StringReader("\"\\t\"")); // "\t"
		StringValueParser parser = new StringValueParser();
		
		Assertions.assertTrue(parser.canRead(r));
		Assertions.assertEquals("\t", parser.read(r));
	}
	
	@Test
	public void testVerticalTab() throws IOException, SyntaxError {
		LookaheadCodePointReader r = new LookaheadCodePointReader(new StringReader("\"\\v\"")); // "\v"
		StringValueParser parser = new StringValueParser();
		
		Assertions.assertTrue(parser.canRead(r));
		Assertions.assertEquals(""+(char)0x000B, parser.read(r));
	}
	
	@Test
	public void testNull() throws IOException, SyntaxError {
		LookaheadCodePointReader r = new LookaheadCodePointReader(new StringReader("\"\\0\"")); // "\0"
		StringValueParser parser = new StringValueParser();
		
		Assertions.assertTrue(parser.canRead(r));
		Assertions.assertEquals(""+(char)0x0000, parser.read(r));
	}
	
	@Test
	public void testDigitEscapesNotAllowed() throws IOException, SyntaxError {
		LookaheadCodePointReader r = new LookaheadCodePointReader(new StringReader("\"\\3\"")); // "\3"
		StringValueParser parser = new StringValueParser();
		
		Assertions.assertTrue(parser.canRead(r));
		Assertions.assertThrows(SyntaxError.class, ()->parser.read(r));
	}
	
	@Test
	public void testPizza() throws IOException, SyntaxError {
		LookaheadCodePointReader r = new LookaheadCodePointReader(new StringReader("\"\\ud83c\\udf55\"")); // "\ud83c\udf55"
		StringValueParser parser = new StringValueParser();
		
		Assertions.assertTrue(parser.canRead(r));
		Assertions.assertEquals("\ud83c\udf55", parser.read(r));
	}
	
	@Test
	public void testNotPizza() throws IOException, SyntaxError {
		LookaheadCodePointReader r = new LookaheadCodePointReader(new StringReader("\"\\uud83c\\uudf55\"")); // "\uud83c\uudf55"
		StringValueParser parser = new StringValueParser();
		
		Assertions.assertTrue(parser.canRead(r));
		Assertions.assertEquals("\\ud83c\\udf55", parser.read(r));
	}
	
	@Test
	public void testShortEscape() throws IOException, SyntaxError {
		LookaheadCodePointReader r = new LookaheadCodePointReader(new StringReader("\"\\x20\"")); // "\x20"
		StringValueParser parser = new StringValueParser();
		
		Assertions.assertTrue(parser.canRead(r));
		Assertions.assertEquals(" ", parser.read(r));
	}
	
	@Test
	public void testJson5Newline() throws IOException, SyntaxError {
		LookaheadCodePointReader r = new LookaheadCodePointReader(new StringReader("\"a\\\nb\"")); // "a\(newline)b"
		StringValueParser parser = new StringValueParser();
		
		Assertions.assertTrue(parser.canRead(r));
		Assertions.assertEquals("ab", parser.read(r));
	}
	
	// Other stuff
	
	@Test
	public void testUnescapedNewlineNotAllowed() throws IOException, SyntaxError {
		LookaheadCodePointReader r = new LookaheadCodePointReader(new StringReader("\"stuff\n\"")); // "stuff(newline)"
		StringValueParser parser = new StringValueParser();
		
		Assertions.assertTrue(parser.canRead(r));
		Assertions.assertThrows(SyntaxError.class, ()->parser.read(r));
	}
}
