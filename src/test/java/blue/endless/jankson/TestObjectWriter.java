/*
 * MIT License
 *
 * Copyright (c) 2018-2024 Falkreon (Isaac Ellingson)
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
import java.io.StringWriter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import blue.endless.jankson.api.Jankson;
import blue.endless.jankson.api.SyntaxError;
import blue.endless.jankson.api.annotation.SerializedName;
import blue.endless.jankson.api.io.JsonReader;
import blue.endless.jankson.api.io.JsonWriterOptions;
import blue.endless.jankson.api.io.ObjectReaderFactory;
import blue.endless.jankson.api.io.ObjectWriter;
import blue.endless.jankson.impl.magic.ClassHierarchy;

public class TestObjectWriter {
	
	/*
	 * This set of tests checks requested types that canonically correspond to bare values. For
	 * example, Integer.class will always look for a "long" typed primitive (there is no such thing
	 * as int in json or in StructuredData).
	 * 
	 * The test is simple: When presented with a StructuredData Primitive representing the
	 * appropriate bare type, can ObjectWriter correctly detect and convert the value?
	 * 
	 * Technically, these tests are a little more end-to-end than they have to be. They will also
	 * break if JsonReader breaks.
	 */
	
	@Test
	public void testBareInt() throws SyntaxError, IOException {
		String subject = "731";
		JsonReader reader = new JsonReader(new StringReader(subject));
		var writer = new ObjectWriter<>(Integer.class);
		reader.transferTo(writer);
		
		Assertions.assertEquals(731, writer.toObject());
	}
	
	@Test
	public void testBareString() throws SyntaxError, IOException {
		String subject = "\"This is a bare string.\"";
		JsonReader reader = new JsonReader(new StringReader(subject));
		var writer = new ObjectWriter<>(String.class);
		reader.transferTo(writer);
		
		Assertions.assertEquals("This is a bare string.", writer.toObject());
	}
	
	@Test
	public void testBareDouble() throws SyntaxError, IOException {
		String subject = "12.0";
		JsonReader reader = new JsonReader(new StringReader(subject));
		var writer = new ObjectWriter<>(Double.class);
		reader.transferTo(writer);
		
		Assertions.assertEquals(12.0, writer.toObject(), 0.00001);
	}
	
	@Test
	public void testBareFloat() throws SyntaxError, IOException {
		String subject = "13.0";
		JsonReader reader = new JsonReader(new StringReader(subject));
		var writer = new ObjectWriter<>(Float.class);
		reader.transferTo(writer);
		
		Assertions.assertEquals(13.0f, writer.toObject(), 0.00001);
	}
	
	@Test
	public void testBareLong() throws SyntaxError, IOException {
		String subject = "9223372036854775807"; // This happens to be Long.MAX_VALUE, but the important part is that it's big.
		JsonReader reader = new JsonReader(new StringReader(subject));
		var writer = new ObjectWriter<>(Long.class);
		reader.transferTo(writer);
		
		Assertions.assertEquals(9223372036854775807L, writer.toObject());
	}
	
	@Test
	public void testBareBoolean() throws SyntaxError, IOException {
		String subject = "true"; // We don't use "false" here because that's the default value for boolean
		JsonReader reader = new JsonReader(new StringReader(subject));
		var writer = new ObjectWriter<>(Boolean.class);
		reader.transferTo(writer);
		
		Assertions.assertEquals(Boolean.TRUE, writer.toObject());
	}
	
	/*
	 * Records are really great for serialization! We treat them as first-class citizens in 2.x, so
	 * if this breaks it's a Big Deal.
	 */
	
	@Test
	public void testSimpleRecord() throws SyntaxError, IOException {
		record Point(double x, double y, double z) {};
		
		String subject =
				"""
				{ "x": 12.0, "y": 11.0, "z": 6.28 }
				""";
		JsonReader reader = new JsonReader(new StringReader(subject));
		var writer = new ObjectWriter<>(Point.class);
		reader.transferTo(writer);
		
		Point expected = new Point(12, 11, 6.28);
		Point actual = writer.toObject();
		
		Assertions.assertEquals(expected, actual);
	}
	
	/*
	 * These tests kind of belong in a seprate class, but in order to unpack many objects,
	 * ObjectWriter needs to have a great understanding of reified generics. The following test
	 * group ensures that we maintain a correct and thorough understanding of Java's type system.
	 * 
	 * In particular, ObfuscatingList illustrates why we can't just, e.g., rely on the single type
	 * argument of a collection to know its element type, we have to actually walk the type
	 * hierarchy and build a mapping of type-variables to type-arguments as we go.
	 */
	
	private static class ObfuscatingList<T> extends ArrayList<String> {
		private static final long serialVersionUID = 1L;
	}
	
	@Test
	public void testGettingCollectionMemberType() throws Throwable {
		record P(ObfuscatingList<Short> value) {};
		Type genericType = P.class.getRecordComponents()[0].getGenericType();
		
		Type t = ClassHierarchy.getCollectionTypeArgument(genericType);
		Assertions.assertEquals(String.class, t);
	}
	
	@Test
	public void testGettingMapMemberTypes() {
		record P(Map<String, float[]> value) {};
		Type genericType = P.class.getRecordComponents()[0].getGenericType();
		
		var results = ClassHierarchy.getMapTypeArguments(genericType);
		
		Assertions.assertEquals(String.class, results.keyType());
		Assertions.assertEquals(float[].class, results.valueType());
	}
	
	@Test
	public void testNestedGenerics() {
		record P(Map<String, List<Float>> value) {};
		Type genericType = P.class.getRecordComponents()[0].getGenericType();
		
		Type valueType = ClassHierarchy.getMapTypeArguments(genericType).valueType();
		Type elementType = ClassHierarchy.getCollectionTypeArgument(valueType);
		
		Assertions.assertEquals(Float.class, elementType);
	}
	
	
	
	@Test
	public void testCollection() throws SyntaxError, IOException {
		
		String subject =
				"""
				[ 1, 12, 13, 2, 4 ]
				""";
		JsonReader reader = new JsonReader(new StringReader(subject));
		
		record P(List<Integer> value) {}
		Type targetType = P.class.getRecordComponents()[0].getGenericType();
		
		var writer = new ObjectWriter<ArrayList<Integer>>(targetType);
		reader.transferTo(writer);
		
		ArrayList<Integer> actual = writer.toObject();
		
		Assertions.assertEquals(List.of(1, 12, 13, 2, 4), actual);
	}
	
	@Test
	public void testNestedCollection() throws SyntaxError, IOException {
		String subject =
				"""
				{
					"value": [ 1, 12, 13, 2, 4 ]
				}
				""";
		JsonReader reader = new JsonReader(new StringReader(subject));
		
		record P(List<Integer> value) {}
		
		var writer = new ObjectWriter<>(P.class);
		reader.transferTo(writer);
		
		P actual = writer.toObject();
		
		Assertions.assertEquals(new P(List.of(1, 12, 13, 2, 4)), actual);
	}
	
	@Test
	public void testMap() throws SyntaxError, IOException {
		
		String subject =
				"""
				{
					"test": "Everything's going to be fine.",
					"test2": "Electric boogaloo"
				}
				""";
		JsonReader reader = new JsonReader(new StringReader(subject));
		
		record P(Map<String, String> value) {}
		Type targetType = P.class.getRecordComponents()[0].getGenericType();
		
		var writer = new ObjectWriter<Map<String, String>>(targetType);
		reader.transferTo(writer);
		
		Map<String, String> expected = Map.of(
				"test", "Everything's going to be fine.",
				"test2", "Electric boogaloo"
				);
		Map<String, String> actual = writer.toObject();
		
		Assertions.assertEquals(expected, actual);
	}
	
	public static class PojoConfig {
		@SerializedName("port-number")
		public int portNumber;
		
		@SerializedName("host-name")
		public String hostName;
	}
	
	@Test
	public void testConfigPojo() throws SyntaxError, IOException {
		String subject =
				"""
				{
					"port-number": 25565,
					"host-name": "localhost"
				}
				""";
		JsonReader reader = new JsonReader(new StringReader(subject));
		
		var writer = new ObjectWriter<>(PojoConfig.class);
		reader.transferTo(writer);
		
		PojoConfig actual = writer.toObject();
		
		Assertions.assertEquals(25565, actual.portNumber);
		Assertions.assertEquals("localhost", actual.hostName);
	}
	
	public static class LightConfounder {
		public Map<String, List<Integer>> mapOfLists;
	}
	
	@Test
	public void testConfigWithEmbeddedListMap() throws SyntaxError, IOException {
		String subject =
				"""
				{
					"mapOfLists": {
						"a": [ 1, 2, 3]
					}
				}
				""";
		JsonReader reader = new JsonReader(new StringReader(subject));
		
		var writer = new ObjectWriter<>(LightConfounder.class);
		reader.transferTo(writer);
		
		LightConfounder actual = writer.toObject();
		
		Assertions.assertEquals(Map.of("a", List.of(1, 2, 3)), actual.mapOfLists);
	}
	
	@Test
	public void testArrays() throws SyntaxError, IOException {
		String subject =
				"""
				[ 96, 112, 24]
				""";
		JsonReader reader = new JsonReader(new StringReader(subject));
		
		var writer = new ObjectWriter<>(int[].class);
		reader.transferTo(writer);
		
		int[] actual = writer.toObject();
		
		Assertions.assertArrayEquals(new int[] { 96, 112, 24 }, actual);
	}
	
	/**
	 * Reproduction of issue reported via Discord. Fix in progress.
	 */
	
	@Test
	public void testFull() throws SyntaxError, IOException {
		String expected =
			"""
			{
				port-number: 65535,
				host-name: "localhost"
			}
			""".trim(); //Get rid of trailing newline
		
		StringWriter writer = new StringWriter();
		PojoConfig config = new PojoConfig();
		config.hostName = "localhost";
		config.portNumber = 65535;
		Jankson.writeJson(config, new ObjectReaderFactory(), writer, JsonWriterOptions.DEFAULTS);
		String actual = writer.toString();
		
		Assertions.assertEquals(expected, actual);
	}
}
