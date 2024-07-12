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

import blue.endless.jankson.api.document.ObjectElement;
import blue.endless.jankson.api.document.ValueElement;
import blue.endless.jankson.api.io.JsonWriter;
import blue.endless.jankson.api.io.JsonWriterOptions;
import blue.endless.jankson.api.io.StructuredDataReader;
import blue.endless.jankson.api.io.ValueElementWriter;
import blue.endless.jankson.impl.ObjectToStructuredDataPipe;
import blue.endless.jankson.impl.io.objectreader.ObjectStructuredDataReader;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import blue.endless.jankson.api.Jankson;
import blue.endless.jankson.api.MarshallerException;
import blue.endless.jankson.api.SyntaxError;

public class TestSerializer {
	
	@Test
	public void testPrimitiveSerialization() throws IOException {
		String actual = Jankson.writeJsonString(2, JsonWriterOptions.ONE_LINE);
		Assertions.assertEquals("2", actual);
	}
	
	@Test
	public void testArraySerialization() throws IOException {
		String actual = Jankson.writeJsonString(new String[] { "foo", "bar" }, JsonWriterOptions.ONE_LINE);
		Assertions.assertEquals("[ \"foo\", \"bar\" ]", actual);
	}
	
	@Test
	public void testCollectionSerialization() throws IOException {
		String listActual = Jankson.writeJsonString(List.of(1, 2, 3), JsonWriterOptions.ONE_LINE);
		Assertions.assertEquals("[ 1, 2, 3 ]", listActual);
		
		// Testing Sets is harder because of undetermined or purposefully randomized iteration order
		// LinkedHashSet fixes this by forcing insertion order
		LinkedHashSet<Integer> testSet = new LinkedHashSet<>();
		testSet.add(1); testSet.add(2); testSet.add(3);
		String setActual = Jankson.writeJsonString(testSet, JsonWriterOptions.ONE_LINE);
		Assertions.assertEquals("[ 1, 2, 3 ]", setActual);
	}
	
	/*
	
	@SuppressWarnings("unused")
	public static class TestObject {
		private int x = 1;
		private String y = "Hello";
	}*/
	
	/*
	@Test
	public void testArraySerialization() throws IOException, MarshallerException {
		ValueElement array = ObjectStructuredDataWriter.toStructuredData(new int[] { 3, 2, 1 });
		
		String actual = Jankson.toJsonString(array, JsonWriterOptions.DEFAULTS);
		
		String expected =
				"""
				[
					3,
					2,
					1
				]
				""";
		Assertions.assertEquals(expected, actual);
	}
	
	@Test
	public void testVoidArraySerialization() throws IOException, MarshallerException {
		Void[] voidArray = new Void[] {null, null}; // We must not simply break at the first sign of black magic.
		ValueElement asValue = ObjectStructuredDataWriter.toStructuredData(voidArray);
		
		String actual = Jankson.toJsonString(asValue, JsonWriterOptions.ONE_LINE);
		
		// Note: This is a change from earlier behavior, which is "[ null, null ]". I felt this additional whitespace was unnecessary.
		Assertions.assertEquals("[null, null]", actual);
	}
	
	@Test
	public void testNestedCollections() throws IOException, MarshallerException {
		List<Double[]> doubleArrayList = new ArrayList<Double[]>();
		doubleArrayList.add(new Double[] {1.0, 2.0, 3.0});
		doubleArrayList.add(new Double[] {4.0, 5.0});
		ValueElement asValue = ObjectStructuredDataWriter.write(doubleArrayList);
		
		String actual = Jankson.toJsonString(asValue, JsonWriterOptions.ONE_LINE);
		
		// Again, change from original behavior which included whitespace around brackets
		Assertions.assertEquals("[[1.0, 2.0, 3.0], [4.0, 5.0]]", actual);
	}
	
	
	@Test
	public void testMapToStructuredData() throws MarshallerException {
		HashMap<String, Integer> intHashMap = new HashMap<>();
		intHashMap.put("foo", 1);
		intHashMap.put("bar", 2);
		ValueElement asValue = ObjectStructuredDataWriter.toStructuredData(intHashMap);
		
		Assertions.assertTrue(asValue instanceof ObjectElement);
		
		ObjectElement obj = (ObjectElement) asValue;
		Assertions.assertEquals(1, obj.getPrimitive("foo").asInt().getAsInt());
		Assertions.assertEquals(2, obj.getPrimitive("bar").asInt().getAsInt());
	}
	*/
	
	/*
	private static class CommentedClass {
		@Comment("This is a comment.")
		private String foo = "what?";
	}
	*/
	//@Test
	//public void testSerializedComments() {
	//	CommentedClass commented = new CommentedClass();
	//	String serialized = MarshallerImpl.getFallback().serialize(commented).toJson(true, false, 0);
	//	Assertions.assertEquals("{ /* This is a comment. */ \"foo\": \"what?\" }", serialized);
	//}
	/*
	private enum ExampleEnum {
		ANT,
		BOX,
		CAT,
		DAY;
	};
	
	@Test
	public void testSerializeEnums() {
		String serialized = MarshallerImpl.getFallback().serialize(ExampleEnum.CAT).toJson();
		
		Assertions.assertEquals("\"CAT\"", serialized);
	}
	*/
	
	
//	Jankson jankson;
//	
//	@BeforeEach
//	public void setup() {
//		jankson = Jankson.builder().build();
//	}
//	
//	/**
//	 * Make sure that characters which lie outside the BMP and/or have complex encodings wind up
//	 * decomposed and escaped properly
//	 */
//	@Test
//	public void testUnicodeEscapes() {
//		String smileyFace = String.valueOf(Character.toChars(0x1F600));
//		String result = new JsonPrimitive(smileyFace).toString();
//		Assertions.assertEquals("\"\\ud83d\\ude00\"", result);
//	}
//	
//	
//	private static class DeclaredSerializerTest {
//		@SuppressWarnings("unused")
//		private String foo = "bar";
//		
//		@Serializer
//		public JsonPrimitive serialize() {
//			return JsonPrimitive.of(42L);
//		}
//	}
//	
//	@Test
//	public void testInternalSerializer() {
//		JsonElement elem = jankson.toJson(new DeclaredSerializerTest());
//		Assertions.assertEquals("42", elem.toJson());
//	}
//	
//	/**
//	 * Issue #34 - switching to Writer caused a small bug where doubles were double-printed
//	 */
//	@Test
//	public void testBareSpecialNumericsDuplication() {
//		JsonObject subject = new JsonObject();
//		subject.put("foo", JsonPrimitive.of(42.0));
//		
//		JsonGrammar grammar = JsonGrammar.builder().bareSpecialNumerics(true).printWhitespace(false).build();
//		
//		Assertions.assertEquals("{ \"foo\": 42.0 }", subject.toJson(grammar));
//	}
//	
//	/** Issues #26 and #38 - ':' isn't force-quoted in object keys */
//	@Test
//	public void testIssue26() {
//		JsonObject obj = new JsonObject();
//		obj.put("test:key", JsonNull.INSTANCE);
//		
//		String result = obj.toJson(JsonGrammar.builder().printWhitespace(false).printUnquotedKeys(true).build());
//		Assertions.assertEquals("{ \"test:key\": null }", result);
//	}
//	
//	/**
//	 * Issue #42: String with russian characters is incorrectly parsed.
//	 * 
//	 * <p>Moved off of a hand-decoding of UTF-8 surrogates onto Reader, which offers a much more robust and future-proof assembly of UTF-8 surrogates into code points.
//	 */
//	
//	@Test
//	public void testCyrillic() {
//		String input =
//				"{\n" + 
//				"  en: \"Play with sound?\",\n" + 
//				"  ru: \"Играть с музыкой?\"\n" + 
//				"}";
//		
//		try {
//			JsonObject obj = Jankson.builder().build().load(input);
//			
//			Assertions.assertEquals("Играть с музыкой?", obj.get(String.class, "ru"));
//		} catch (SyntaxError ex) {
//			Assertions.fail("Should not get a syntax error for a well-formed object: "+ex.getCompleteMessage());
//		}
//	}
}
