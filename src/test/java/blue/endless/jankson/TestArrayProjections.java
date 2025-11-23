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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import blue.endless.jankson.api.Jankson;
import blue.endless.jankson.api.SyntaxError;
import blue.endless.jankson.api.document.ArrayElement;
import blue.endless.jankson.api.document.ObjectElement;
import blue.endless.jankson.api.document.PrimitiveElement;
import blue.endless.jankson.api.document.ValueElement;

public class TestArrayProjections {
	
	@Test
	@SuppressWarnings("unused")
	public void testObjectProjection() throws IOException, SyntaxError {
		String subject = "[ \"foo\", {}, 42, {}, {}, \"a\" ]";
		ValueElement val = Jankson.readJson(subject);
		Assertions.assertTrue(val instanceof ArrayElement);
		
		int objectCount = 0;
		for(ObjectElement obj : ((ArrayElement) val).asObjectArray()) {
			objectCount++;
		}
		
		Assertions.assertEquals(3, objectCount);
	}
	
	@Test
	public void testPrimitiveProjection() throws IOException, SyntaxError {
		String subject = "[ \"foo\", {}, 40, {}, {}, 2, \"a\" ]";
		ValueElement val = Jankson.readJson(subject);
		Assertions.assertTrue(val instanceof ArrayElement);
		
		int sumOfNumbers = 0;
		for(PrimitiveElement p : ((ArrayElement) val).asPrimitiveArray()) {
			sumOfNumbers += p.orElse(0);
		}
		
		Assertions.assertEquals(42, sumOfNumbers);
	}
}
