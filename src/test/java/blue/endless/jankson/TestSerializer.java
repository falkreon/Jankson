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

import blue.endless.jankson.api.annotation.Serializer;
import blue.endless.jankson.api.document.ArrayElement;
import blue.endless.jankson.api.document.PrimitiveElement;
import blue.endless.jankson.api.document.ValueElement;
import blue.endless.jankson.api.io.JsonWriterOptions;
import blue.endless.jankson.impl.ToStructuredDataFunction;
import blue.endless.jankson.impl.MarshallerImpl;

import java.io.IOException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import blue.endless.jankson.api.Jankson;
import blue.endless.jankson.api.MarshallerException;
import blue.endless.jankson.api.SyntaxError;

public class TestSerializer {
	
	/*
	
	@SuppressWarnings("unused")
	public static class TestObject {
		private int x = 1;
		private String y = "Hello";
	}*/
	
	@Test
	public void testArraySerialization() throws IOException, MarshallerException {
		// TODO: Go back to Marshaller to turn Object->Json
		ValueElement array = ToStructuredDataFunction.toStructuredData(new int[] { 3, 2, 1 });
		/*
		ArrayElement array = new ArrayElement();
		array.add(PrimitiveElement.of(3));
		array.add(PrimitiveElement.of(2));
		array.add(PrimitiveElement.of(1));*/
		
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
	
	/*
	@Test
	public void testArraySerialization() {
		int[] intArray = new int[] {3, 2, 1};
		
		String serializedIntArray = MarshallerImpl.getFallback().serialize(intArray).toString();
		Assertions.assertEquals("[ 3, 2, 1 ]", serializedIntArray);
		
		Void[] voidArray = new Void[] {null, null}; //Yes, I realize this is black magic. We *must not* simply break at the first sign of black magic.
		String serializedVoidArray = MarshallerImpl.getFallback().serialize(voidArray).toString();
		Assertions.assertEquals("[ null, null ]", serializedVoidArray);
		
		List<Double[]> doubleArrayList = new ArrayList<Double[]>();
		doubleArrayList.add(new Double[] {1.0, 2.0, 3.0});
		doubleArrayList.add(new Double[] {4.0, 5.0});
		String serializedDoubleArrayList = MarshallerImpl.getFallback().serialize(doubleArrayList).toString();
		Assertions.assertEquals("[ [ 1.0, 2.0, 3.0 ], [ 4.0, 5.0 ] ]", serializedDoubleArrayList);
	}*/
	
	/*
	@Test
	public void testMapSerialization() {
		HashMap<String, Integer> intHashMap = new HashMap<>();
		intHashMap.put("foo", 1);
		intHashMap.put("bar", 2);
		JsonElement serialized = MarshallerImpl.getFallback().serialize(intHashMap);
		Assertions.assertTrue(serialized instanceof JsonObject);
		JsonObject obj = (JsonObject)serialized;
		Assertions.assertEquals(new JsonPrimitive(1L), obj.get("foo"));
		Assertions.assertEquals(new JsonPrimitive(2L), obj.get("bar"));
	}
	
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
