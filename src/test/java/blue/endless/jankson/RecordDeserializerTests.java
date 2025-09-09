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
import blue.endless.jankson.api.annotation.SerializedName;
import blue.endless.jankson.api.document.ArrayElement;
import blue.endless.jankson.api.document.ObjectElement;
import blue.endless.jankson.api.document.PrimitiveElement;
import blue.endless.jankson.api.document.ValueElement;
import blue.endless.jankson.api.io.ObjectReaderFactory;
import blue.endless.jankson.api.io.StructuredDataReader;
import blue.endless.jankson.api.io.ValueElementReader;
import blue.endless.jankson.impl.io.objectwriter.RecordDeserializer;

public class RecordDeserializerTests {
	
	@Test
	public void testCloneIsNotNull() throws IOException, SyntaxError {
		record Point(int x, int y) {}
		
		StructuredDataReader reader = new ObjectReaderFactory().getReader(Point.class, new Point(12, 2));
		RecordDeserializer<Point> writer = new RecordDeserializer<>(Point.class);
		reader.transferTo(writer);
		Point result = writer.getResult();
		
		Assertions.assertNotNull(result);
		Assertions.assertEquals(new Point(12, 2), result);
	}
	
	@Test
	public void testValueElementReaderTransfer() throws IOException, SyntaxError {
		record Point(int x, int y) {}
		
		ObjectElement elem = new ObjectElement();
		elem.put("x", PrimitiveElement.of(12));
		elem.put("y", PrimitiveElement.of(2));
		
		StructuredDataReader reader = ValueElementReader.of(elem);
		RecordDeserializer<Point> writer = new RecordDeserializer<>(Point.class);
		reader.transferTo(writer);
		
		Assertions.assertEquals(new Point(12, 2), writer.getResult());
	}
	
	@Test
	public void testRecursiveClosers() throws IOException, SyntaxError {
		record Point(int x, int[] y) {}
		
		ObjectElement elem = new ObjectElement();
		elem.put("x", PrimitiveElement.of(12));
		ArrayElement arr = new ArrayElement();
		arr.add(PrimitiveElement.of(2));
		elem.put("y", arr);
		
		StructuredDataReader reader = ValueElementReader.of(elem);
		RecordDeserializer<Point> writer = new RecordDeserializer<>(Point.class);
		reader.transferTo(writer);
		
		Assertions.assertEquals(12, writer.getResult().x());
		Assertions.assertArrayEquals(new int[] { 2 }, writer.getResult().y());
	}
	
	@Test
	public void testSerializedNameComponents() throws IOException, SyntaxError {
		record Point(int x, @SerializedName("y") int q) {}
		
		ObjectElement elem = new ObjectElement();
		elem.put("x", PrimitiveElement.of(12));
		elem.put("y", PrimitiveElement.of(2));
		
		StructuredDataReader reader = ValueElementReader.of(elem);
		RecordDeserializer<Point> writer = new RecordDeserializer<>(Point.class);
		reader.transferTo(writer);
		
		Assertions.assertEquals(new Point(12, 2), writer.getResult());
	}
}
