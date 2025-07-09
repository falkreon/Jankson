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

package blue.endless.jankson;

import java.io.IOException;
import java.io.StringReader;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import blue.endless.jankson.api.SyntaxError;
import blue.endless.jankson.impl.io.LookaheadCodePointReader;
import blue.endless.jankson.impl.io.context.NumberValueParser;

public class TestNumberValueParsers {

	@Test
	public void testInfinity() throws IOException, SyntaxError {
		LookaheadCodePointReader r = new LookaheadCodePointReader(new StringReader("Infinity"));
		NumberValueParser parser = new NumberValueParser();
		
		Assertions.assertTrue(parser.canRead(r));
		Assertions.assertEquals(Double.POSITIVE_INFINITY, parser.read(r));
	}
	
	//If Infinity is so good, why isn't there an Infinity 2
	@Test
	public void testInfinity2() throws IOException, SyntaxError {
		LookaheadCodePointReader r = new LookaheadCodePointReader(new StringReader("infinity"));
		NumberValueParser parser = new NumberValueParser();
		
		Assertions.assertTrue(parser.canRead(r));
		Assertions.assertEquals(Double.POSITIVE_INFINITY, parser.read(r));
	}
	
	@Test
	public void testNegativeInfinity() throws IOException, SyntaxError {
		LookaheadCodePointReader r = new LookaheadCodePointReader(new StringReader("-Infinity"));
		NumberValueParser parser = new NumberValueParser();
		
		Assertions.assertTrue(parser.canRead(r));
		Assertions.assertEquals(Double.NEGATIVE_INFINITY, parser.read(r));
	}
	
	@Test
	public void testNegativeInfinity2() throws IOException, SyntaxError {
		LookaheadCodePointReader r = new LookaheadCodePointReader(new StringReader("-infinity"));
		NumberValueParser parser = new NumberValueParser();
		
		Assertions.assertTrue(parser.canRead(r));
		Assertions.assertEquals(Double.NEGATIVE_INFINITY, parser.read(r));
	}
	
	@Test
	public void testRejectGarbage() throws IOException, SyntaxError {
		LookaheadCodePointReader r = new LookaheadCodePointReader(new StringReader("0foo"));
		NumberValueParser parser = new NumberValueParser();
		
		Assertions.assertTrue(parser.canRead(r));
		Assertions.assertThrows(SyntaxError.class, ()->parser.read(r));
	}
	
	@Test
	public void testOrdinaryHex() throws IOException, SyntaxError {
		LookaheadCodePointReader r = new LookaheadCodePointReader(new StringReader("0x42"));
		NumberValueParser parser = new NumberValueParser();
		
		Assertions.assertTrue(parser.canRead(r));
		Assertions.assertEquals(0x42L, parser.read(r));
	}
	
	@Test
	public void testNegativeHex() throws IOException, SyntaxError {
		LookaheadCodePointReader r = new LookaheadCodePointReader(new StringReader("-0xC0FFEE"));
		NumberValueParser parser = new NumberValueParser();
		
		Assertions.assertTrue(parser.canRead(r));
		Assertions.assertEquals(-0xC0FFEEL, parser.read(r));
	}
	
	@Test
	public void testNaN() throws IOException, SyntaxError {
		LookaheadCodePointReader r = new LookaheadCodePointReader(new StringReader("NaN"));
		NumberValueParser parser = new NumberValueParser();
		
		Assertions.assertTrue(parser.canRead(r));
		Assertions.assertEquals(Double.NaN, parser.read(r));
	}
	
	@Test
	public void testExponents() throws IOException, SyntaxError {
		LookaheadCodePointReader r = new LookaheadCodePointReader(new StringReader("4e6"));
		NumberValueParser parser = new NumberValueParser();
		
		Assertions.assertTrue(parser.canRead(r));
		Assertions.assertEquals(4e6D, parser.read(r));
	}
	
	@Test
	public void testLeadingDot() throws IOException, SyntaxError {
		LookaheadCodePointReader r = new LookaheadCodePointReader(new StringReader(".3"));
		NumberValueParser parser = new NumberValueParser();
		
		Assertions.assertTrue(parser.canRead(r));
		Assertions.assertEquals(0.3D, parser.read(r));
	}
	
	@Test
	public void testRejectNonNumbers() throws IOException, SyntaxError {
		LookaheadCodePointReader r = new LookaheadCodePointReader(new StringReader("stuff"));
		NumberValueParser parser = new NumberValueParser();
		
		Assertions.assertFalse(parser.canRead(r));
	}
}
