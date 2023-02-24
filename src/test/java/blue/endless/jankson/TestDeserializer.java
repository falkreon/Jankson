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

import blue.endless.jankson.api.annotation.Deserializer;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import blue.endless.jankson.api.Jankson;
import blue.endless.jankson.api.JsonGrammar;
import blue.endless.jankson.api.Marshaller;
import blue.endless.jankson.api.SyntaxError;
import blue.endless.jankson.api.io.JsonIOException;

public class TestDeserializer {
	
//	public static class Foo {
//		private String value = "";
//		private String opt = "";
//		
//		@Deserializer
//		public static Foo deserialize(String s) {
//			Foo result = new Foo();
//			result.value = s;
//			return result;
//		}
//		
//		// the unused Marshaller parameter is for testing that it gets recognised properly
//		@Deserializer
//		public static Foo deserialize(JsonArray arr, Marshaller marshaller) throws JsonIOException {
//			if (arr.size()<1 || arr.size()>2) throw new JsonIOException("Array can have either 1 or 2 elements. Found: "+arr.size());
//			Foo result = new Foo();
//			result.value = arr.getString(0, "OOPS");
//			if (arr.size()>1) result.opt = arr.getString(1, "OOPS");
//			return result;
//		}
//	}
//	
//	public static class Bar {
//		public Foo stringDeserializer;
//		public Foo arrayDeserializer;
//		public Foo unmappedDeserializer = new Foo();
//		public Foo defaultDeserializer;
//	}
//	
//	/**
//	 * Make sure that various kinds of self-described deserializers are working alongside each other
//	 */
//	@Test
//	public void testBasicFeatures() throws JsonIOException {
//		String subject =
//				"{\n" + 
//				"	\"stringDeserializer\": \"test\",\n" + 
//				"	\"arrayDeserializer\": [ \"someValue\", \"someOptValue\" ],\n" + 
//				"	\"unmappedDeserializer\": false,\n" + 
//				"	\"defaultDeserializer\": {\n" + 
//				"		\"value\": \"someValue\",\n" + 
//				"		\"opt\": \"someOptValue\"\n" + 
//				"	}\n" + 
//				"\n" + 
//				"}";
//		
//		try {
//			Bar obj = jankson.fromJson(subject, Bar.class);
//			
//			Assertions.assertNotNull(obj.stringDeserializer);
//			Assertions.assertNotNull(obj.arrayDeserializer);
//			Assertions.assertNotNull(obj.unmappedDeserializer);
//			Assertions.assertNotNull(obj.defaultDeserializer);
//			
//			Assertions.assertEquals("test", obj.stringDeserializer.value);
//			Assertions.assertEquals("someValue", obj.arrayDeserializer.value);
//			Assertions.assertEquals("someOptValue", obj.arrayDeserializer.opt);
//			Assertions.assertEquals("", obj.unmappedDeserializer.value);
//			Assertions.assertEquals("someValue", obj.defaultDeserializer.value);
//			Assertions.assertEquals("someOptValue", obj.defaultDeserializer.opt);
//		} catch (SyntaxError ex) {
//			Assertions.fail("Should not get a syntax error for a well-formed object: "+ex.getCompleteMessage());
//		}
//	}
//	
//	/**
//	 * Just to be clear: If we can't unpack a key for any reason, Carefully should throw an error.
//	 */
//	@Test
//	public void ensureErrorOnMissingDeserializer() throws SyntaxError, JsonIOException {
//		String subject =
//				"{\n" + 
//				"	\"stringDeserializer\": \"test\",\n" + 
//				"	\"arrayDeserializer\": [ \"someValue\", \"someOptValue\" ],\n" + 
//				"	\"unmappedDeserializer\": false,\n" + 
//				"	\"defaultDeserializer\": {\n" + 
//				"		\"value\": \"someValue\",\n" + 
//				"		\"opt\": \"someOptValue\"\n" + 
//				"	}\n" + 
//				"\n" + 
//				"}";
//		
//		Assertions.assertThrows(
//				JsonIOException.class,
//				()->jankson.fromJsonCarefully(subject, Bar.class)
//				);
//	}
//	
//	public static class Baz {
//		public Foo foo;
//	}
//	
//	
//	
//	
//	
//	/** This makes sure that values like Infinity, NaN, and -Infinity are parsed properly */
//	@Test
//	public void testSpecialNumericValues() {
//		String subject =
//				"{\n" + 
//				"	\"a\": Infinity,\n" + 
//				"	\"b\": -Infinity,\n" + 
//				"	\"c\": NaN,\n" + 
//				"}";
//		try {
//			JsonObject result = jankson.load(subject);
//			JsonElement a = result.get("a");
//			JsonElement b = result.get("b");
//			JsonElement c = result.get("c");
//			Assertions.assertTrue(a instanceof JsonPrimitive, "'Infinity' parses to a JsonPrimitive");
//			Assertions.assertTrue(b instanceof JsonPrimitive, "'-Infinity' parses to a JsonPrimitive");
//			Assertions.assertTrue(c instanceof JsonPrimitive, "'NaN' parses to a JsonPrimitive");
//			
//			Assertions.assertEquals(Double.POSITIVE_INFINITY, ((JsonPrimitive)a).asDouble(-1), 1);
//			Assertions.assertEquals(Double.NEGATIVE_INFINITY, ((JsonPrimitive)b).asDouble(-1), 1);
//			Assertions.assertEquals(Double.NaN, ((JsonPrimitive)c).asDouble(-1), 1);
//		} catch (SyntaxError ex) {
//			Assertions.fail("Should not get a syntax error for a well-formed object: "+ex.getCompleteMessage());
//		}
//	}
//	
//	/**
//	 * Problems reported: loadElement returns jsonNull for an array or a bare numeric element.
//	 */
//	@Test
//	public void testLoadElementNonnull() throws SyntaxError {
//		Object x;
//		x = jankson.loadElement("1");
//		Assertions.assertTrue(x instanceof JsonPrimitive, "'1' must be a JsonPrimitive");
//		x = jankson.loadElement("[1]");
//		Assertions.assertTrue(x instanceof JsonArray, "'[1]' must be a jsonArray");
//	}
//	
//	/**
//	 * Issue #31: Unicode escape sequences aren't deserialized correctly
//	 * 
//	 * <p>StringParserContext was fitted with a new unicode escape parser to cover this case.
//	 */
//	@Test
//	public void testUnicodeEscapes() throws SyntaxError {
//		String example = "{ \"test\": \"\\u003C0\" }";
//		Assertions.assertEquals("\"<0\"", jankson.load(example).get("test").toJson(), "Unicode escapes must be parsed correctly.");
//		
//		String slightlyUnescaped = ((JsonPrimitive)jankson.load("{'test': \"\\uu003C\" }").get("test")).asString();
//		Assertions.assertEquals("\\u003c", slightlyUnescaped, "Unicode escapes which are themselves escaped must be unescaped exactly one level."); //implied here is that hex digit case MUST be lost
//	}
//	
//	/**
//	 * Issue #35: Serialize/deserialize cycles of multiline comments cause whitespace of lines after the first to creep larger and larger
//	 * 
//	 * <p>Now leading whitespace is stripped from each line of multiline comments.
//	 */
//	@Test
//	public void testMultilineCommentReads() throws SyntaxError {
//		JsonObject obj = new JsonObject();
//		JsonPrimitive p = new JsonPrimitive(42);
//		obj.put("thing", p, "this is a multiline\ncomment");
//		
//		JsonObject recyc = jankson.load(obj.toJson(JsonGrammar.JSON5));
//		
//		Assertions.assertEquals(obj.toJson(JsonGrammar.JSON5), recyc.toJson(JsonGrammar.JSON5));
//	}
//
//	@Test
//	public void testOmitRootBraces() throws JsonIOException {
//		String subject =
//				"\"stringDeserializer\": \"test\",\n" +
//				"\"arrayDeserializer\": [ \"someValue\", \"someOptValue\" ],\n" +
//				"\"unmappedDeserializer\": false,\n" +
//				"\"defaultDeserializer\": {\n" +
//				"	\"value\": \"someValue\",\n" +
//				"	\"opt\": \"someOptValue\"\n" +
//				"}";
//
//		try {
//			Bar obj = Jankson.builder().allowBareRootObject().build().fromJson(subject, Bar.class);
//
//			Assertions.assertNotNull(obj.stringDeserializer);
//			Assertions.assertNotNull(obj.arrayDeserializer);
//			Assertions.assertNotNull(obj.unmappedDeserializer);
//			Assertions.assertNotNull(obj.defaultDeserializer);
//
//			Assertions.assertEquals("test", obj.stringDeserializer.value);
//			Assertions.assertEquals("someValue", obj.arrayDeserializer.value);
//			Assertions.assertEquals("someOptValue", obj.arrayDeserializer.opt);
//			Assertions.assertEquals("", obj.unmappedDeserializer.value);
//			Assertions.assertEquals("someValue", obj.defaultDeserializer.value);
//			Assertions.assertEquals("someOptValue", obj.defaultDeserializer.opt);
//		} catch (SyntaxError ex) {
//			Assertions.fail("Should not get a syntax error for a well-formed object: "+ex.getCompleteMessage());
//		}
//		try {
//			Jankson.builder().build().fromJson(subject, Bar.class);
//			Assertions.fail("Should not successfully load bare root object without enabling option");
//		} catch (SyntaxError ex) {
//		}
//	}
}
