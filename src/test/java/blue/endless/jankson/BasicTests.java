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
import java.lang.reflect.Type;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import blue.endless.jankson.api.annotation.SerializedName;
import blue.endless.jankson.api.document.ArrayElement;
import blue.endless.jankson.api.document.KeyValuePairElement;
import blue.endless.jankson.api.document.NonValueElement;
import blue.endless.jankson.api.document.ObjectElement;
import blue.endless.jankson.api.document.PrimitiveElement;
import blue.endless.jankson.api.document.ValueElement;
import blue.endless.jankson.api.Jankson;
import blue.endless.jankson.api.JsonGrammar;
import blue.endless.jankson.api.SyntaxError;
import blue.endless.jankson.api.annotation.Comment;
import blue.endless.jankson.impl.MarshallerImpl;
import blue.endless.jankson.impl.TypeMagic;

@SuppressWarnings("deprecation")
public class BasicTests {
	
	@Test
	public void testPrimitiveEquality() {
		Assertions.assertEquals(PrimitiveElement.box("foo"), PrimitiveElement.of(new String("foo"))); //Ensure that string equality doesn't suffer from interning assumptions
		Assertions.assertEquals(PrimitiveElement.of(42), PrimitiveElement.of(42)); //Ensure that equal objects produce equal primitives
		
		//Non-equal objects should produce non-equal primitives
		Assertions.assertNotEquals(PrimitiveElement.of("foo"), PrimitiveElement.of("bar"));
		Assertions.assertNotEquals(PrimitiveElement.of(42.0), PrimitiveElement.of(42.1));
		
		//Intended quirk behavior: Data types matter in wrapped objects
		Assertions.assertNotEquals(PrimitiveElement.of(42.0), PrimitiveElement.of(42));
	}
	
	
	@Test
	public void testBasicComprehension() throws IOException, SyntaxError {
		String before = "{ 'foo': 'bar', 'baz':'bux' }";
		
		ValueElement after = Jankson.readJson(before);
		if (after instanceof ObjectElement obj) {
			//The above object should deserialize to two keys with specific, known values.
			Assertions.assertEquals(2, obj.size());
			Assertions.assertEquals(2, obj.keySet().size());
			
			Assertions.assertEquals("bar", obj.getPrimitive("foo").asString().get());
			Assertions.assertEquals("bux", obj.getPrimitive("baz").asString().get());
			
			//Keys that are not present should retrieve synthetic null elements.
			Assertions.assertTrue(obj.getPrimitive("bar").isNull());
		} else {
			Assertions.fail();
		}
	}
	
	@Test
	public void testObjectContentCategories() throws IOException, SyntaxError {
		String before = "{ 'a': 'hello', 'b': 42, 'c': 42.0, 'd': {}, 'e': [], 'f': true, 'g': false, 'h': null }";
		
		ValueElement after = Jankson.readJson(before);
		if (after instanceof ObjectElement obj) {
			Assertions.assertEquals(8, obj.size());
			
			Assertions.assertEquals("hello",             obj.getPrimitive("a").getValue().get());
			Assertions.assertEquals(42L,                 obj.getPrimitive("b").asLong().getAsLong());
			Assertions.assertEquals(42.0,                obj.getPrimitive("c").asDouble().getAsDouble(), 0.000000001); //Should be equal to an extremely fine delta
			Assertions.assertEquals(new ObjectElement(), obj.get("d"));
			Assertions.assertEquals(new ArrayElement(),  obj.get("e"));
			Assertions.assertEquals(true,                obj.getPrimitive("f").asBoolean().get());
			Assertions.assertEquals(false,               obj.getPrimitive("g").asBoolean().get());
			Assertions.assertTrue(obj.getPrimitive("h").isNull());
		} else {
			Assertions.fail();
		}
	}
	
	@Test
	public void testArrayContentCategories() throws IOException, SyntaxError {
		String before = "{ 'a': ['hello', 42, 42.0, {}, [], true, false, null] }";
		
		ValueElement after = Jankson.readJson(before);
		
		if (after instanceof ObjectElement obj) {
			Assertions.assertEquals(1, obj.keySet().size());
			
			ArrayElement array = obj.getArray("a");
			Assertions.assertEquals(8, array.size());
			
			Assertions.assertEquals("hello",       array.getPrimitive(0).asString().get());
			Assertions.assertEquals(42,            array.getPrimitive(1).asInt().getAsInt());
			Assertions.assertEquals(42.0,          array.getPrimitive(2).asDouble().getAsDouble());
			Assertions.assertInstanceOf(ObjectElement.class, array.get(3));
			Assertions.assertInstanceOf(ArrayElement.class,  array.get(4));
			Assertions.assertEquals(Boolean.TRUE,  array.getPrimitive(5).asBoolean().get());
			Assertions.assertEquals(Boolean.FALSE, array.getPrimitive(6).asBoolean().get());
			Assertions.assertTrue(array.getPrimitive(7).isNull());
			
		} else {
			Assertions.fail();
		}
	}
	
	@Test
	public void testCommentAttribution() throws IOException, SyntaxError {
		/*
		 * A preamble is a list of all the non-value elements that occur before a value element within the same context.
		 */
		String subjectString = "/* 1a */ /* 1b */ { /* 2a */ /* 2b */ 'foo' /* 3a */ /* 3b */ : /* 4a */ /* 4b */ true /* 5a */ /* 5b */ } /* 6a */ /* 6b */";

		ObjectElement subject = Jankson.readJsonObject(subjectString);
		
		Assertions.assertEquals(2, subject.getPreamble().size());
		Assertions.assertEquals("1a", subject.getPreamble().get(0).asCommentElement().getValue());
		Assertions.assertEquals("1b", subject.getPreamble().get(1).asCommentElement().getValue());
		
		Map.Entry<String, ValueElement> entry = subject.entrySet().iterator().next();
		if (entry instanceof KeyValuePairElement kvPair) { //TODO: Oh no! Is there no better way to acquire these objects??
			List<NonValueElement> preamble = kvPair.getPreamble();
			Assertions.assertEquals(2, preamble.size());
			Assertions.assertEquals("2a", preamble.get(0).asCommentElement().getValue());
			Assertions.assertEquals("2b", preamble.get(1).asCommentElement().getValue());
			
			
			List<NonValueElement> intermission = kvPair.getIntermission();
			Assertions.assertEquals(2, intermission.size());
			Assertions.assertEquals("3a", intermission.get(0).asCommentElement().getValue());
			Assertions.assertEquals("3b", intermission.get(1).asCommentElement().getValue());
			
			ValueElement value = kvPair.getValue();
			List<NonValueElement> valuePreamble = value.getPreamble();
			Assertions.assertEquals(2, valuePreamble.size());
			Assertions.assertEquals("4a", valuePreamble.get(0).asCommentElement().getValue());
			Assertions.assertEquals("4b", valuePreamble.get(1).asCommentElement().getValue());
			
			//5a and 5b are technically value epilogues because they're not after a comma
			Assertions.assertEquals(2, value.getEpilogue().size());
			Assertions.assertEquals("5a", value.getEpilogue().get(0).asCommentElement().getValue());
			Assertions.assertEquals("5b", value.getEpilogue().get(1).asCommentElement().getValue());
		}
		
		//TODO: KNOWN ISSUE: Comments after the root object are not kept
		//Assertions.assertEquals(2, subject.getEpilogue().size());
		//Assertions.assertEquals("6a", subject.getEpilogue().get(0).asCommentElement().getValue());
		//Assertions.assertEquals("6b", subject.getEpilogue().get(1).asCommentElement().getValue());
	}
	
	
	
	/* Unported 1.2.x tests */
	
	
	
	//@Test
	//public void testCommentAttribution() {
	//	try {
	//		String before = "{ /* Hello World */ 'foo': true }";
	//		JsonObject after = jankson.load(before);
	//		
	//		Assertions.assertEquals("Hello World", after.getComment("foo"), "Comment should be parsed and attributed to child 'foo'");
	//		
	//		before = "{ /*Hello World */ 'foo': true }";
	//		after = jankson.load(before);
	//		
	//		Assertions.assertEquals("Hello World", after.getComment("foo"), "Comment should still be parsed and attributed to child 'foo'");
	//		
	//		before = "{ //\tHello World \n 'foo': true }";
	//		after = jankson.load(before);
	//		
	//		Assertions.assertEquals("Hello World", after.getComment("foo"), "Single-line comment should be parsed and attributed to child 'foo'");
	//		
	//	} catch (SyntaxError ex) {
	//		Assertions.fail("Should not get a syntax error for a well-formed object: "+ex.getCompleteMessage());
	//	}
	//}
	
	/*
	@Test
	public void testDeepNesting() {
		String subject = "{ a: { a: { a: { a: { a: { a: { a: { a: 'Hello' } } } } } } } }";
		try {
			JsonObject parsed = jankson.load(subject);
			JsonPrimitive prim = parsed.recursiveGet(JsonPrimitive.class, "a.a.a.a.a.a.a.a");
			
			Assertions.assertEquals(new JsonPrimitive("Hello"), prim);
			
			JsonPrimitive notPrim = parsed.recursiveGet(JsonPrimitive.class, "a.a.a.a.a.a.a.a.a");
			Assertions.assertNull(notPrim);
			
			notPrim = parsed.recursiveGet(JsonPrimitive.class, "a.a.a.a.a.a.a");
			Assertions.assertNull(notPrim);
			
		} catch (SyntaxError ex) {
			Assertions.fail("Should not get a syntax error for a well-formed object: "+ex.getCompleteMessage());
		}
	}
	
	@Test
	public void testMarshaller() {
		try {
			String subject = "{ a: { a: { a: 'Hello' } } }";
			String stringResult = jankson.load(subject).recursiveGet(String.class, "a.a.a");
			Assertions.assertEquals("Hello", stringResult, "Should get the String 'Hello' back");
			
			subject = "{ a: { a: { a: 42 } } }";
			Integer intResult = jankson.load(subject).recursiveGet(Integer.class, "a.a.a");
			Assertions.assertEquals(Integer.valueOf(42), intResult, "Should get the Integer 42 back");
			
			Assertions.assertEquals(Double.valueOf(42), 
					jankson.load(subject).recursiveGet(Double.class, "a.a.a"),
					"Should get the Double 42 back");
			
			
			
		} catch (SyntaxError ex) {
			Assertions.fail("Should not get a syntax error for a well-formed object: "+ex.getCompleteMessage());
		}
	}
	
	@Test
	public void testForReuseLeaks() {
		try {
			String subjectOne = "{ a: 42 }";
			@SuppressWarnings("unused")
			JsonObject parsedOne = jankson.load(subjectOne);
			
			String subjectTwo = "{ b: 12 }";
			JsonObject parsedTwo = jankson.load(subjectTwo);
			
			Assertions.assertNull(parsedTwo.get("a"));
		} catch (SyntaxError ex) {
			Assertions.fail("Should not get a syntax error for a well-formed object: "+ex.getCompleteMessage());
		}
	}
	
	@Test
	public void testComplexQuirks() {
		try {
			String subject = "{ mods: [{name: 'alf' version:'1.12.2_v143.6'} {name:'bux', version:false}]}";
			JsonObject parsed = jankson.load(subject);
			
			Assertions.assertNotNull(parsed);
			Assertions.assertNotNull(parsed.get("mods"));
			
			Assertions.assertEquals(JsonArray.class, parsed.get("mods").getClass());
			
			JsonArray mods = parsed.recursiveGet(JsonArray.class, "mods");
			Assertions.assertEquals(2, mods.size());
			
			//TODO: Add more marshalling logic to arrays
			//JsonElement alfMod = mods.get(0);
			
		} catch (SyntaxError ex) {
			Assertions.fail("Should not get a syntax error for a well-formed object: "+ex.getCompleteMessage());
		}
	}
	
	@SuppressWarnings("unused")
	public static class TestObject {
		private int x = 1;
		private String y = "Hello";
	}
	
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
	}
	
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
	
	@Test
	public void testDeserializeEnums() {
		String serialized = "{ aProperty: 'DAY' }";
		try {
			JsonObject deserialized = jankson.load(serialized);
			
			ExampleEnum recovered = deserialized.get(ExampleEnum.class, "aProperty");
			Assertions.assertEquals(ExampleEnum.DAY, recovered);
			
		} catch (SyntaxError ex) {
			Assertions.fail("Should not get a syntax error for a well-formed object: "+ex.getCompleteMessage());
		}
	}
	*/
	//@Test
	//public void testDiffAgainstDefaults() {
	//	try {
			/*
			 * A number of specific behaviors are verified here:
			 *  - 'a' is present as a default but not present in the base object. This key is ignored.
			 *  - 'b' is not customized in the base object. This key is ignored.
			 *  - 'c' is customized in the base object. Delta records the customization.
			 *  - 'd' is itself an object, so we do a deep comparison:
			 *    - 'd.e' is not customized. This key is ignored.
			 *    - 'd.f' is customized. This key is recorded, and since the resulting object isn't empty, we know the
			 *            outer key was also customized, so we record all of this.
			 *  - 'g' is a list, and identical to the default. This key is ignored.
			 *  - 'h' is a list, but its value has been customized. Even though the lists share some elements, the
			 *        entire list is represented in the output. This test is effectively a promise to shallow-diff lists
			 *  - 'i' is an object, so it receives a deep comparison. However, it is found to be identical, and so its key is ignored.
			 */
			
	//		JsonObject defaultObj = jankson.load("{ a: 'a', b: 'b', c: 'c', d: { e: 'e', f: 'f' }, g: [1, 2], h: [1, 2], i: { j: 'j' } }");
	//		JsonObject baseObj = jankson.load("{ b: 'b', c: 'test', d: { e: 'e', f: 'test' }, g: [1, 2], h: [2, 3], i: { j: 'j' } }");
	//		String expected = "{ \"c\": \"test\", \"d\": { \"f\": \"test\" }, \"h\": [ 2, 3 ] }";
			
	//		String actual = baseObj.getDelta(defaultObj).toJson();
	//		Assertions.assertEquals(expected, actual);
			
	//	} catch (SyntaxError ex) {
	//		Assertions.fail("Should not get a syntax error for a well-formed object: "+ex.getCompleteMessage());
	//	}
	//}
	/*
	@Test
	public void testArrayGet() {
		try {
			JsonObject subject = jankson.load("{ a: [1, 2, 3, 4] }");
			int[] maybe = subject.get(int[].class, "a");
			Assertions.assertNotNull(maybe);
			Assertions.assertArrayEquals(new int[] {1,2,3,4}, maybe);
			
		} catch (SyntaxError ex) {
			Assertions.fail("Should not get a syntax error for a well-formed object: "+ex.getCompleteMessage());
		}
	}
	
	
	@Test
	public void preventMangledEmoji() {
		String[] elements = {
			"\uD83C\uDF29", //lightningbolt
			"\uD83D\uDD25", //:fire:
			"\u2668",       //:hotsprings:
			"\uD83C\uDF0A", // :wave:
			"\uD83D\uDC80", //starve
			"\uD83C\uDF35", //cactus
			"\u2BEF️", //fall
			"\uD83D\uDCA8", //flyIntoWall
			"\u2734", //*
			"\uD83D\uDC7B", //???
			"✨ ⚚", //magic
	        "\uD83D\uDD71", //wither
	        "\uD83D\uDC32", //dragonBreath
	        "\uD83C\uDF86", //fireworks
	
	        "\uD83D\uDC80", //mob
	        "\uD83D\uDDE1", //player
	        "\uD83C\uDFF9", //arrow
	        "彡°", //thrown
	        "\uD83C\uDF39", //thorns
	        "\uD83D\uDCA3 \uD83D\uDCA5", //explosion
	        "\uE120" //burger
		};
		
		try {
			JsonObject subject = new JsonObject();
			String serialized = subject.toJson();
			JsonObject result = jankson.load(serialized);
			
			Assertions.assertEquals(subject, result);
		} catch (SyntaxError ex) {
			Assertions.fail("Should not get a syntax error for a well-formed object: "+ex.getCompleteMessage());
		}
	}
	
	@Test
	public void recognizeStringEscapes() {
		String subject = "{ foo: 'a\\tb\\nc\\\\'}";
		String expected = "a\tb\nc\\";
		try {
			JsonObject unpacked = jankson.load(subject);
			Assertions.assertEquals(expected, unpacked.get(String.class, "foo"));
		} catch (SyntaxError ex) {
			Assertions.fail("Should not get a syntax error for a well-formed object: "+ex.getCompleteMessage());
		}
	}
	
	@Test
	public void properlyEscapeStrings() {
		String inputString = "The\nquick\tbrown\ffox\bjumps\"over\\the\rlazy dog.";
		String expected = "{ \"foo\": \"The\\nquick\\tbrown\\ffox\\bjumps\\\"over\\\\the\\rlazy dog.\" }";
		JsonObject subject = new JsonObject();
		subject.put("foo", new JsonPrimitive(inputString));
		String actual = subject.toJson(false, false);
		
		Assertions.assertEquals(expected, actual);
	}
	/*
	@Test
	public void testBaseDeserialization() {
		try {
			JsonObject parsed = jankson.load("{x: 4, y: 4}");
			
			TestObject des = jankson.fromJson(parsed, TestObject.class);
			
			Assert.assertEquals(4, des.x);
			Assert.assertEquals("4", des.y);
			
		} catch (SyntaxError ex) {
			Assert.fail("Should not get a syntax error for a well-formed object: "+ex.getCompleteMessage());
		}
	}
	
	public static class TestContainer {
		TestObject object = new TestObject();
		private String foo = null;
	}
	
	@Test
	public void testNestedDeserialization() {
		try {
			JsonObject parsed = jankson.load("{object:{x: 4, y: 4}, foo:'bar'}");
			
			TestContainer des = jankson.fromJson(parsed, TestContainer.class);
			
			Assert.assertEquals(4, des.object.x);
			Assert.assertEquals("4", des.object.y);
			Assert.assertEquals("bar", des.foo);
			
		} catch (SyntaxError ex) {
			Assert.fail("Should not get a syntax error for a well-formed object: "+ex.getCompleteMessage());
		}
	}*/
	/*
	@Test
	public void testNegativeNumbers() {
		String subject = "{ 'foo': -1, 'bar': [ -1, -3 ] }";
		
		try {
			JsonObject parsed = jankson.load(subject);
			
			Assertions.assertEquals(Integer.valueOf(-1), parsed.get(Integer.class, "foo"));
			int[] array = parsed.get(int[].class, "bar");
			Assertions.assertArrayEquals(new int[] {-1, -3}, array);
			
		} catch (SyntaxError ex) {
			Assertions.fail("Should not get a syntax error for a well-formed object: "+ex.getCompleteMessage());
		}
		
	}
	
	@Test
	public void testAvoidRecursiveGetNPE() {
		JsonObject subject = new JsonObject();
		
		subject.recursiveGetOrCreate(JsonArray.class, "some/random/path", new JsonArray(), "This is a test");
		//Prior test failure is an NPE on the line above.
	}
	
	@Test
	public void testNoInlineArrays() {
		JsonObject subject = new JsonObject();
		JsonArray array = new JsonArray();
		subject.put("array", array);
		JsonObject nested = new JsonObject();
		array.add(nested);
		nested.put("foo", new JsonPrimitive("foo"), "pitiable");
		nested.put("bar", new JsonPrimitive("bar"), "passable");
		
		String actual = subject.toJson(true, true, 0);
		
		String expected =
				"{\n" +
				"	\"array\": [\n" + 
				"		{\n" + 
				"			// pitiable\n" + //Inline comments are now emitted where expedient
				"			\"foo\": \"foo\",\n" + 
				"			// passable\n" + 
				"			\"bar\": \"bar\"\n" + 
				"		}\n" + 
				"	]\n" + 
				"}";
		
		Assertions.assertEquals(expected, actual);
	}
	
	@Test
	public void ensureEmptyCommentsAreOmitted() {
		JsonObject subject = new JsonObject();
		subject.put("foo", new JsonPrimitive("bar"), "");
		subject.put("baz", new JsonPrimitive("bux"), " ");
		String actual = subject.toJson(JsonGrammar.JSON5);
		String expected =
				"{\n" + //Again, this trailing space is subject to change
				"	\"foo\": \"bar\",\n" +
				"	\"baz\": \"bux\",\n" + //Trailing commas are emitted in JSON5 grammar
				"}";
		
		Assertions.assertEquals(expected, actual);
	}
	*/
	//@Test
	//public void ensureMultilineCommentsAreIndented() {
	//	JsonObject subject = new JsonObject();
	//	subject.put("foo", new JsonPrimitive("bar"), "This is a line\nAnd this is another line.");
	//	String actual = subject.toJson(JsonGrammar.JSON5);
	//	String expected =
	//			"{\n" +
	//			"	/* This is a line\n" +
	//			"	   And this is another line.\n" + //Three spaces precede every subsequent line to line comments up
	//			"	*/\n" + //The end-comment is on its own line
	//			"	\"foo\": \"bar\",\n" + //Again, trailing comma per JSON5
	//			"}";
	//	
	//	Assertions.assertEquals(expected, actual);
	//}
	
	//@Test
	//public void ensureMultilineArrayCommentsAreIndented() {
	//	JsonArray subject = new JsonArray();
	//	subject.add(new JsonPrimitive("foo"), "This is a line\nAnd this is another line.");
	//	String actual = subject.toJson(JsonGrammar.JSON5);
	//	String expected =
	//			"[\n" +
	//			"	/* This is a line\n" +
	//			"	   And this is another line.\n" + //Three spaces precede every subsequent line to line comments up
	//			"	*/\n" + //The end-comment is on its own line
	//			"	\"foo\",\n" + //Trailing comma per JSON5
	//			"]";
	//	
	//	Assertions.assertEquals(expected, actual);
	//}
	/*
	private static class TestClass {
		private ArrayList<String> strings;
		private Map<String, Character.UnicodeScript> scripts = new HashMap<>();
		private Queue<String> queue = new ArrayDeque<>();
	}*/
	
	/**
	 * This stresses several parts of the POJODeserializer logic. Among other things:
	 * <li> fromJson should be able to call forth arbitrary objects which aren't public or have no public constructor. It
	 *      must only have a no-arg constructor at *all* in order to qualify.
	 * 
	 * <li> private members must likewise pose no threat to the deserializer. [not tested here,] public inherited members
	 *      should be deserialized-to as well.
	 * 
	 * <li> Any field descending from Map or Collection, at the top level, should [de]serialize properly and its type
	 *      parameters should be recovered.
	 * 
	 * <li> When re-serializing, Maps MUST be serialized to JsonObject. Collections MUST be serialized to JsonArray.
	 */
	/*@Test
	public void testDeserializeGenerics() {
		try {
			String serialized = "{ \"strings\": [ \"a\", \"b\", \"c\" ], \"scripts\": { \"arabic\": \"ARABIC\" }, \"queue\": [ \"FUN\" ] }";
			JsonObject subject = jankson.load(serialized);
			TestClass object = jankson.fromJson(subject, TestClass.class);
			Assertions.assertEquals(serialized, jankson.toJson(object).toString(), "Reserialized form must match original serialized form.");
		} catch (SyntaxError ex) {
			Assertions.fail("Should not get a syntax error for a well-formed object: "+ex.getCompleteMessage());
		}
	}*/
	
	//private static class RecursiveGenericsTestClass {
	//	ArrayList<ArrayList<String>> lists;
	//}
	
	/**
	 * This puts additional stress on the POJODeserializer to recover more type information when deserializing. At this
	 * point, generics must be fully recovered, including constructions like {@code List<List<String>>}
	 */
	/*
	@Test
	public void testRecursiveGenerics() {
		String serialized = "{ \"lists\": [ [ \"a\" ], [ ] ] }"; //Lists contains two lists, one contains "a", the other is empty.
		try {
			JsonObject subject = jankson.load(serialized);
			RecursiveGenericsTestClass object = jankson.fromJson(subject, RecursiveGenericsTestClass.class);
			
			//Right now, this test fails, producing { "lists": [ ] } because the inner lists cannot be deserialized.
			//This should be solved by completing Marshaller.marshall(Type, JsonElement) to recover type parameters from ParameterizedTypes instead of losing them in converting to rawtype Classes
			Assert.assertEquals("Reserialized form must match original serialized form.", serialized, jankson.toJson(object).toString());
		} catch (SyntaxError ex) {
			Assert.fail("Should not get a syntax error for a well-formed object: "+ex.getCompleteMessage());
		}
	}*/
	/*
	private static class GenericArrayContainer<T> {
		public T[] ts;
		public int[] ints;
		public <U extends T> U[] u() { return null; };
	}*/
	
	/**
	 * Stresses TypeMagic's array comprehension, and clarifies certain deserializer behaviors and limitations.
	 * 
	 * <li>If a generic array field is found, and an instance is not provided in the constructor or an initializer, it's initialized to Object[].
	 * <li>If a primitive-typed array field is found, it's initialized to exactly its type. Primitive arrays are fully reified in Java.
	 * <li>Wildcard types are tricky. In the tested case, "<U extends T> U[]", it doesn't matter that "U" has type bounds - it's a type variable.
	 *     The variable will be treated as Object for the purposes of deserialization, and an Object[] will be created.
	 */
	/*
	@Test
	public void testGenericArrayComprehension() {
		GenericArrayContainer<String> container = new GenericArrayContainer<>();
		Type genericArrayType = container.getClass().getFields()[0].getGenericType();
		Class<?> genericArrayClass = TypeMagic.classForType(genericArrayType);
		Assertions.assertEquals(Object[].class, genericArrayClass, "Recovered generic array type should be Object[].");
		
		Type intArrayType = container.getClass().getFields()[1].getGenericType();
		Class<?> intArrayClass = TypeMagic.classForType(intArrayType);
		Assertions.assertEquals(int[].class, intArrayClass, "Recovered array type should be int[].");
		
		Type wildcardType = container.getClass().getMethods()[0].getGenericReturnType();
		Class<?> wildcardArrayClass = TypeMagic.classForType(wildcardType);
		Assertions.assertEquals(Object[].class, wildcardArrayClass, "Recovered wildcard array type should be Object[].");
	}
	
	@ParametersAreNonnullByDefault
	private static class NullContainer {
		@Nullable
		public String nullable = "";
		
		public String nonnull = "";
	}
	*/
	/** This test will fail as soon as a key is added for 'nonnull'. 1.2 should fix this. */
	/*
	@Test
	public void testDeserializeNulls() {
		String serialized = "{ \"nullable\": null }";
		try {
			NullContainer subject = jankson.fromJson(serialized, NullContainer.class);
			
			Assertions.assertNull(subject.nullable);
			Assertions.assertNotNull(subject.nonnull);
		} catch (SyntaxError ex) {
			Assertions.fail("Should not get a syntax error for a well-formed object: "+ex.getCompleteMessage());
		}
		
	}
	
	
	@Test
	public void testJson5EscapedReturn() {
		String serialized = "{ \"a-multiline-string\": \"foo\\\nbar\" }";
		try {
			JsonObject subject = jankson.load(serialized);
			JsonElement parsed = subject.get("a-multiline-string");
			Assertions.assertTrue(parsed instanceof JsonPrimitive, "String element should be a JsonPrimitive."); //not the test
			Assertions.assertEquals("foobar", ((JsonPrimitive)parsed).getValue().toString(), "Multiline String should parse to well-known result.");
			
		} catch (SyntaxError ex) {
			Assertions.fail("Should not get a syntax error for a well-formed object: "+ex.getCompleteMessage());
		}
	}*/
	
	/**
	 * While this isn't really a *normative* example, it's a pretty good example of all the JSON5 quirks in one tidy
	 * package. Jankson is and should remain fully compatible with JSON5 quirks.
	 */
	/*
	@Test
	public void parseJson5InformativeExample() {
		String serialized = "{\n" + 
				"  // comments\n" + 
				"  unquoted: 'and you can quote me on that',\n" + 
				"  singleQuotes: 'I can use \"double quotes\" here',\n" + 
				"  lineBreaks: \"Look, Mom! \\\n" + 
				"No \\\\n's!\",\n" + 
				"  hexadecimal: 0xdecaf,\n" + 
				"  leadingDecimalPoint: .8675309, andTrailing: 8675309.,\n" + 
				"  positiveSign: +1,\n" + 
				"  trailingComma: 'in objects', andIn: ['arrays',],\n" + 
				"  \"backwardsCompatible\": \"with JSON\",\n" + 
				"}";
		
		try {
			JsonObject subject = jankson.load(serialized);
			Assertions.assertEquals("\"and you can quote me on that\"", subject.get("unquoted").toString());
			Assertions.assertEquals("\"I can use \\\"double quotes\\\" here\"", subject.get("singleQuotes").toString());
			Assertions.assertEquals("\"Look, Mom! No \\\\n's!\"", subject.get("lineBreaks").toString());
			Assertions.assertEquals(Long.toString(0xdecaf), subject.get("hexadecimal").toString());
			//Floating point gets a little hairy, so let's use floating point comparison for this
			double leading = (Double) ((JsonPrimitive)subject.get("leadingDecimalPoint")).getValue();
			double trailing = (Double) ((JsonPrimitive)subject.get("andTrailing")).getValue();
			
			Assertions.assertEquals(0.8675309, leading, 0.00000001);
			Assertions.assertEquals(8675309.0, trailing, 0.00000001);
			
			long positiveSign = (Long) ((JsonPrimitive)subject.get("positiveSign")).getValue();
			Assertions.assertEquals(1L, positiveSign);
			
			Assertions.assertEquals("\"in objects\"", subject.get("trailingComma").toString());
			Assertions.assertEquals("[ \"arrays\" ]", subject.get("andIn").toString());
			
			Assertions.assertEquals("\"with JSON\"", subject.get("backwardsCompatible").toString());
		} catch (SyntaxError ex) {
			Assertions.fail("Should not get a syntax error for a well-formed object: "+ex.getCompleteMessage());
		}
	}
	
	
	@Test
	public void testArrayEdgeCase() {
		String arrayDuplicates = "{ \"pattern\": [ \"ss\", \"ss\" ] }";
		try {
			JsonObject subject = jankson.load(arrayDuplicates);
			
			Assertions.assertEquals(arrayDuplicates, subject.toJson(false, false));
		} catch (SyntaxError ex) {
			Assertions.fail("Should not get a syntax error for a well-formed object: "+ex.getCompleteMessage());
		}
	}
	
	private static class StaticAccess {
		static transient int foo = 8;
		int bar = 4;
		transient int baz = 3;
	}
	
	@Test
	public void preventStaticAccess() {
		JsonElement elem = jankson.toJson(new StaticAccess());
		Assertions.assertEquals("{ \"bar\": 4 }", elem.toJson(false, false));
		
		try {
			JsonObject subject = jankson.load("{ \"foo\": 1 }");
			StaticAccess deserialized = jankson.fromJson("{ \"foo\": 1 }", StaticAccess.class);
			Assertions.assertEquals(8, StaticAccess.foo);
		} catch (SyntaxError ex) {
			Assertions.fail("Should not get a syntax error for a well-formed object: "+ex.getCompleteMessage());
		}
	}
	
	private static class NestedObjectInner {
		protected String id;
		public NestedObjectInner() { this.id = null; } //Needed for TypeMagic
		public NestedObjectInner(String id) {
			this.id = id;
		}
	}
	
	private static class NestedObjectOuter {
		NestedObjectInner inner;
		public void setInner(NestedObjectInner inner) {
			this.inner = inner;
		}
		public NestedObjectInner getInner() { return inner; }
	}
	
	@Test
	public void ensureNestedObjectMarshalling() {
		NestedObjectOuter subject = new NestedObjectOuter();
		subject.setInner(new NestedObjectInner("A unique string"));
		JsonElement serialized = jankson.toJson(subject);
		
		NestedObjectOuter unpacked = jankson.fromJson((JsonObject)serialized, NestedObjectOuter.class);
		Assertions.assertNotNull(unpacked.getInner());
	}
	
	private static class ElementContainerClass {
		public List<ElementClass> enclosed = new ArrayList<>();
	}
	
	private static class ElementClass {
		String a = "ADAPTER DID NOT RUN";
	}
	//private static boolean adapterRan = false;
	
	@Test
	public void ensureTypeAdaptersAreCalled() {
		Jankson adaptedJankson = Jankson.builder().registerTypeAdapter(ElementClass.class, (obj)->{
			//adapterRan = true;
			ElementClass result = new ElementClass();
			result.a = "ADAPTER RAN";
			return result;
		}).build();
		try {
			JsonObject obj = adaptedJankson.load("{ 'enclosed': [ {'a': 'foo' } ] }");
			Assertions.assertEquals(obj.getMarshaller(), adaptedJankson.getMarshaller());
			Assertions.assertEquals(((JsonArray)obj.get("enclosed")).getMarshaller(), adaptedJankson.getMarshaller());
			ElementClass test = adaptedJankson.getMarshaller().marshall(ElementClass.class, ((JsonArray)obj.get("enclosed")).get(0));
			Assertions.assertEquals("ADAPTER RAN", test.a);
			
			ElementContainerClass result = adaptedJankson.getMarshaller().marshall(ElementContainerClass.class, obj);
			
			//EnclosingAdaptedClass result = adaptedJankson.fromJson("{ 'enclosed': [ {'a': 'foo' } ] }", EnclosingAdaptedClass.class);
			
			
			Assertions.assertEquals("ADAPTER RAN", result.enclosed.get(0).a);
		} catch (SyntaxError ex) {
			Assertions.fail("Should not get a syntax error for a well-formed object: "+ex.getCompleteMessage());
		}
		//Assert.assertTrue(adapterRan);
	}
	
	@Test
	public void testUnifontCommentTestCase() {
		String subject =
				"{\n" + 
				"	\"anArray\": [\n" + 
				"		\"foo\" // A COMMENT\n" + 
				"	]\n" + 
				"}";
		
		try {
			JsonObject result = jankson.load(subject);
		} catch (SyntaxError ex) {
			Assertions.fail("Should not get a syntax error for a well-formed object: "+ex.getCompleteMessage());
		}
	}
	
	
	
	private static class NameTest {
		@SerializedName("foo_bar")
		public int fooBar = 0;
	}*/
	
	/** SerializedName should be preferred over the field's name in both POJO serialization and deserialization */
	/*
	@Test
	public void testSerializedName() {
		String subject =
				"{\n" + 
				"	\"foo_bar\": 31,\n" + 
				"}";
		try {
			NameTest object = jankson.fromJson(subject, NameTest.class);
			Assertions.assertEquals(31, object.fooBar);
			
			String out = jankson.toJson(object).toJson(JsonGrammar.JSON5);
			Assertions.assertEquals(subject, out);
		} catch (SyntaxError ex) {
			Assertions.fail("Should not get a syntax error for a well-formed object: "+ex.getCompleteMessage());
		}
	}*/
	
	/** This makes sure tilde on its own gets processed as a String by the token parser */
	/*
	@Test
	public void testUnquotedStrings() {
		String subject =
				"{\n" + 
				"	\"foo_bar\": ~,\n" + 
				"	~: bux,\n" + 
				"}";
		try {
			JsonObject obj = jankson.fromJson(subject, JsonObject.class);
			String foo_bar = obj.get(String.class, "foo_bar");
			String baz = obj.get(String.class, "~");
			
			Assertions.assertEquals("~", foo_bar);
			Assertions.assertEquals("bux", baz);
		} catch (SyntaxError ex) {
			Assertions.fail("Should not get a syntax error for a well-formed object: "+ex.getCompleteMessage());
		}
	}*/
	
	/**
	 * Issue: #21
	 * Special numerics serializing into quoted String values, so that a save-and-load no longer considers the values numeric.
	 */
	/*
	@Test
	public void testSerializeSpecialNumerics() {
		JsonObject obj = new JsonObject();
		obj.put("foo", new JsonPrimitive(Double.NaN));
		obj.put("bar", new JsonPrimitive(Double.POSITIVE_INFINITY));
		obj.put("baz", new JsonPrimitive(Double.NEGATIVE_INFINITY));
		
		String expected =
				"{\n" + 
				"	\"foo\": NaN,\n" + 
				"	\"bar\": Infinity,\n" + 
				"	\"baz\": -Infinity,\n" + 
				"}";
		
		String actual = obj.toJson(JsonGrammar.JSON5);
		
		Assertions.assertEquals(expected, actual);
	}*/
	
	/**
	 * Issue: #22
	 * Offer a grammar option for outputting root objects without braces ( "{}" ) while inner objects retain their delimiters.
	 */
	/*
	@Test
	public void testBareObject() {
		JsonGrammar BARE = JsonGrammar.builder().bareRootObject(true).build();
		JsonObject obj = new JsonObject();
		obj.put("foo", new JsonPrimitive("bar"));
		obj.put("baz", new JsonPrimitive(42));
		JsonObject nested = new JsonObject();
		JsonArray moreNested = new JsonArray();
		nested.put("boo", moreNested);
		moreNested.add(new JsonPrimitive(3));
		obj.put("bux", nested);
		
		String expected =
				"\"foo\": \"bar\",\n" + 
				"\"baz\": 42,\n" +
				"\"bux\": {\n" +
				"	\"boo\": [\n" +
				"		3\n" +
				"	]\n" +
				"}\n";
		
		String actual = obj.toJson(BARE);
		Assertions.assertEquals(expected, actual);
	}*/
	
	/**
	 * Issue: #23
	 * Grammar option to print unquoted keys
	 */
	/*
	@Test
	public void testUnquotedKeys() {
		JsonGrammar UNQUOTED = JsonGrammar.builder().printUnquotedKeys(true).build();
		JsonObject obj = new JsonObject();
		obj.put("foo", new JsonPrimitive("bar"));
		obj.put("baz", new JsonPrimitive(42));
		JsonObject nested = new JsonObject();
		JsonArray moreNested = new JsonArray();
		nested.put("boo", moreNested);
		moreNested.add(new JsonPrimitive(3));
		obj.put("bux", nested);
		
		String expected =
				"{\n" +
				"	foo: \"bar\",\n" + 
				"	baz: 42,\n" +
				"	bux: {\n" +
				"		boo: [\n" +
				"			3\n" +
				"		]\n" +
				"	}\n" +
				"}";
		String actual = obj.toJson(UNQUOTED);
		Assertions.assertEquals(expected, actual);
	}*/
}
