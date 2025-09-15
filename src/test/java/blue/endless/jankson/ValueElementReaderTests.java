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

import blue.endless.jankson.api.SyntaxError;
import blue.endless.jankson.api.document.ObjectElement;
import blue.endless.jankson.api.document.PrimitiveElement;
import blue.endless.jankson.api.io.StructuredData;
import blue.endless.jankson.api.io.StructuredDataReader;
import blue.endless.jankson.api.io.ValueElementReader;

public class ValueElementReaderTests {
	
	@Test
	public void testKnownObject() throws IOException, SyntaxError {
		ObjectElement obj = new ObjectElement();
		obj.put("test", PrimitiveElement.of(true));
		obj.put("test2", PrimitiveElement.of("foo"));
		
		// This should produce 6 tokens:
		/* This should produce 6 tokens:
		 * 
		 * OBJECT_START
		 * OBJECT_KEY "test"
		 * PRIMITIVE true
		 * OBJECT_KEY "test2"
		 * PRIMITIVE "foo"
		 * OBJECT_END
		 * (any subsequent reads should produce EOF)
		 */
		StructuredDataReader reader = ValueElementReader.of(obj);
		StructuredData objStart = reader.next();
		Assertions.assertEquals(StructuredData.OBJECT_START, objStart);
		StructuredData testKey = reader.next();
		Assertions.assertEquals(StructuredData.Type.OBJECT_KEY, testKey.type());
		Assertions.assertEquals("test", testKey.value());
		StructuredData testValue = reader.next();
		Assertions.assertEquals(StructuredData.Type.PRIMITIVE, testValue.type());
		Assertions.assertEquals(Boolean.TRUE, testValue.value());
		StructuredData test2Key = reader.next();
		Assertions.assertEquals(StructuredData.Type.OBJECT_KEY, test2Key.type());
		Assertions.assertEquals("test2", test2Key.value());
		StructuredData test2Value = reader.next();
		Assertions.assertEquals(StructuredData.Type.PRIMITIVE, test2Value.type());
		Assertions.assertEquals("foo", test2Value.value());
		StructuredData objEnd = reader.next();
		Assertions.assertEquals(StructuredData.OBJECT_END, objEnd);
		StructuredData eof = reader.next();
		Assertions.assertEquals(StructuredData.EOF, eof);
		StructuredData eof2 = reader.next();
		Assertions.assertEquals(StructuredData.EOF, eof2);
	}
	
	@Test
	public void testFlexibleTypes() throws IOException, SyntaxError {
		PrimitiveElement prim = PrimitiveElement.of(4.0);
		Assertions.assertEquals(4, prim.asInt().getAsInt());
		boolean b = PrimitiveElement.of(1);
	}
}
