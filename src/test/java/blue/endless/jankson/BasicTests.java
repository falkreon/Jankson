/*
 * MIT License
 *
 * Copyright (c) 2018-2022 Falkreon (Isaac Ellingson)
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

import java.lang.reflect.Type;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import blue.endless.jankson.api.annotation.NonnullByDefault;
import blue.endless.jankson.api.annotation.Nullable;
import blue.endless.jankson.api.annotation.SerializedName;
import blue.endless.jankson.api.Jankson;
import blue.endless.jankson.api.JsonGrammar;
import blue.endless.jankson.api.SyntaxError;
import blue.endless.jankson.api.annotation.Comment;
import blue.endless.jankson.api.element.JsonArray;
import blue.endless.jankson.api.element.JsonElement;
import blue.endless.jankson.api.element.JsonNull;
import blue.endless.jankson.api.element.JsonObject;
import blue.endless.jankson.api.element.JsonPrimitive;
import blue.endless.jankson.impl.MarshallerImpl;
import blue.endless.jankson.impl.TypeMagic;

@SuppressWarnings("deprecation")
public class BasicTests {
	Jankson jankson;
	
	@Before
	public void setup() {
		jankson = Jankson.builder().build();
	}
	
	@Test
	public void testPrimitiveEquality() {
		Assert.assertEquals("Equal objects should produce equal json primitives", new JsonPrimitive("foo"), new JsonPrimitive(new String("foo"))); //Ensure no interning
		Assert.assertEquals("Equal objects should produce equal json primitives", new JsonPrimitive(Double.valueOf(42)), new JsonPrimitive(Double.valueOf(42)));
		
		Assert.assertNotEquals("Non-Equal objects should produce non-equal json primitives", new JsonPrimitive("foo"), new JsonPrimitive("bar"));
		Assert.assertNotEquals("Non-Equal objects should produce non-equal json primitives", new JsonPrimitive(42.0), new JsonPrimitive(42.1));
		
		Assert.assertNotEquals("Intended quirk behavior: 42.0 != 42", new JsonPrimitive(Double.valueOf(42)), Long.valueOf(42));
	}
	
	
	@Test
	public void testBasicComprehension() {
		String before = "{ 'foo': 'bar', 'baz':'bux' }";
		
		try {
			JsonObject after = jankson.load(before);
			
			Assert.assertTrue("Object should contain two keys", after.keySet().size()==2);
			Assert.assertTrue("Object should contain mapping 'foo': 'bar'", after.get("foo").equals(new JsonPrimitive("bar")));
			Assert.assertTrue("Object should contain mapping 'baz': 'bux'", after.get("baz").equals(new JsonPrimitive("bux")));
			Assert.assertNull("Object shouldn't contain keys that weren't defined", after.get("bar"));
			
		} catch (SyntaxError ex) {
			Assert.fail("Should not get a syntax error for a well-formed object: "+ex.getCompleteMessage());
		}
	}
	
	@Test
	public void testObjectContentCategories() {
		String before = "{ 'a': 'hello', 'b': 42, 'c': 42.0, 'd': {}, 'e': [], 'f': true, 'g': false, 'h': null }";
		
		try {
			JsonObject after = jankson.load(before);
			
			Assert.assertTrue("Object should contain 8 keys", after.keySet().size()==8);
			Assert.assertTrue("Object should contain mapping 'a': 'hello'", after.get("a").equals(new JsonPrimitive("hello")));
			Assert.assertTrue("Object should contain mapping 'b': 42", after.get("b").equals(new JsonPrimitive(Long.valueOf(42))));
			Assert.assertTrue("Object should contain mapping 'c': 42.0", after.get("c").equals(new JsonPrimitive(Double.valueOf(42.0))));
			Assert.assertTrue("Object should contain mapping 'd': {}", after.get("d").equals(new JsonObject()));
			Assert.assertTrue("Object should contain mapping 'e': []", after.get("e").equals(new JsonArray()));
			Assert.assertTrue("Object should contain mapping 'f': true", after.get("f").equals(new JsonPrimitive(Boolean.TRUE)));
			Assert.assertTrue("Object should contain mapping 'g': false", after.get("g").equals(new JsonPrimitive(Boolean.FALSE)));
			Assert.assertTrue("Object should contain mapping 'h': null", after.get("h").equals(JsonNull.INSTANCE));
			
		} catch (SyntaxError ex) {
			Assert.fail("Should not get a syntax error for a well-formed object: "+ex.getCompleteMessage());
		}
	}
	
	@Test
	public void testArrayContentCategories() {
		String before = "{ 'a': ['hello', 42, 42.0, {}, [], true, false, null] }";
		
		try {
			JsonObject after = jankson.load(before);
			
			Assert.assertTrue("Object should contain one key", after.keySet().size()==1);
			
			JsonArray array = after.recursiveGet(JsonArray.class, "a");
			Assert.assertNotNull("Recursive get of just 'a' should obtain an array.", array);
			Assert.assertEquals("Array should contain all declared elements and no more.", 8, array.size());
			
			Assert.assertEquals("Array should contain 'hello' at position 0", new JsonPrimitive("hello"), array.get(0));
			Assert.assertEquals("Array should contain 42 at position 1", new JsonPrimitive(Long.valueOf(42)), array.get(1));
			Assert.assertEquals("Array should contain 42.0 at position 2", new JsonPrimitive(Double.valueOf(42)), array.get(2));
			Assert.assertEquals("Array should contain {} at position 3", new JsonObject(), array.get(3));
			Assert.assertEquals("Array should contain [] at position 4", new JsonArray(), array.get(4));
			Assert.assertEquals("Array should contain true at position 5", new JsonPrimitive(Boolean.TRUE), array.get(5));
			Assert.assertEquals("Array should contain false at position 6", new JsonPrimitive(Boolean.FALSE), array.get(6));
			Assert.assertEquals("Array should contain null at position 7", JsonNull.INSTANCE, array.get(7));
			
		} catch (SyntaxError ex) {
			Assert.fail("Should not get a syntax error for a well-formed object: "+ex.getCompleteMessage());
		}
	}
	
	@Test
	public void testCommentAttribution() {
		try {
			String before = "{ /* Hello World */ 'foo': true }";
			JsonObject after = jankson.load(before);
			
			Assert.assertEquals("Comment should be parsed and attributed to child 'foo'", "Hello World", after.getComment("foo"));
			
			before = "{ /*Hello World */ 'foo': true }";
			after = jankson.load(before);
			
			Assert.assertEquals("Comment should still be parsed and attributed to child 'foo'", "Hello World", after.getComment("foo"));
			
			before = "{ //\tHello World \n 'foo': true }";
			after = jankson.load(before);
			
			Assert.assertEquals("Single-line comment should be parsed and attributed to child 'foo'", "Hello World", after.getComment("foo"));
			
		} catch (SyntaxError ex) {
			Assert.fail("Should not get a syntax error for a well-formed object: "+ex.getCompleteMessage());
		}
	}
	
	@Test
	public void testDeepNesting() {
		String subject = "{ a: { a: { a: { a: { a: { a: { a: { a: 'Hello' } } } } } } } }";
		try {
			JsonObject parsed = jankson.load(subject);
			JsonPrimitive prim = parsed.recursiveGet(JsonPrimitive.class, "a.a.a.a.a.a.a.a");
			
			Assert.assertEquals(new JsonPrimitive("Hello"), prim);
			
			JsonPrimitive notPrim = parsed.recursiveGet(JsonPrimitive.class, "a.a.a.a.a.a.a.a.a");
			Assert.assertNull(notPrim);
			
			notPrim = parsed.recursiveGet(JsonPrimitive.class, "a.a.a.a.a.a.a");
			Assert.assertNull(notPrim);
			
		} catch (SyntaxError ex) {
			Assert.fail("Should not get a syntax error for a well-formed object: "+ex.getCompleteMessage());
		}
	}
	
	@Test
	public void testMarshaller() {
		try {
			String subject = "{ a: { a: { a: 'Hello' } } }";
			String stringResult = jankson.load(subject).recursiveGet(String.class, "a.a.a");
			Assert.assertEquals("Should get the String 'Hello' back", "Hello", stringResult);
			
			subject = "{ a: { a: { a: 42 } } }";
			Integer intResult = jankson.load(subject).recursiveGet(Integer.class, "a.a.a");
			Assert.assertEquals("Should get the Integer 42 back", Integer.valueOf(42), intResult);
			
			Assert.assertEquals("Should get the Double 42 back", Double.valueOf(42), 
					jankson.load(subject).recursiveGet(Double.class, "a.a.a"));
			
			
			
		} catch (SyntaxError ex) {
			Assert.fail("Should not get a syntax error for a well-formed object: "+ex.getCompleteMessage());
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
			
			Assert.assertNull(parsedTwo.get("a"));
		} catch (SyntaxError ex) {
			Assert.fail("Should not get a syntax error for a well-formed object: "+ex.getCompleteMessage());
		}
	}
	
	@Test
	public void testComplexQuirks() {
		try {
			String subject = "{ mods: [{name: 'alf' version:'1.12.2_v143.6'} {name:'bux', version:false}]}";
			JsonObject parsed = jankson.load(subject);
			
			Assert.assertNotNull(parsed);
			Assert.assertNotNull(parsed.get("mods"));
			
			Assert.assertEquals(JsonArray.class, parsed.get("mods").getClass());
			
			JsonArray mods = parsed.recursiveGet(JsonArray.class, "mods");
			Assert.assertEquals(2, mods.size());
			
			//TODO: Add more marshalling logic to arrays
			//JsonElement alfMod = mods.get(0);
			
		} catch (SyntaxError ex) {
			Assert.fail("Should not get a syntax error for a well-formed object: "+ex.getCompleteMessage());
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
		Assert.assertEquals("[ 3, 2, 1 ]", serializedIntArray);
		
		Void[] voidArray = new Void[] {null, null}; //Yes, I realize this is black magic. We *must not* simply break at the first sign of black magic.
		String serializedVoidArray = MarshallerImpl.getFallback().serialize(voidArray).toString();
		Assert.assertEquals("[ null, null ]", serializedVoidArray);
		
		List<Double[]> doubleArrayList = new ArrayList<Double[]>();
		doubleArrayList.add(new Double[] {1.0, 2.0, 3.0});
		doubleArrayList.add(new Double[] {4.0, 5.0});
		String serializedDoubleArrayList = MarshallerImpl.getFallback().serialize(doubleArrayList).toString();
		Assert.assertEquals("[ [ 1.0, 2.0, 3.0 ], [ 4.0, 5.0 ] ]", serializedDoubleArrayList);
	}
	
	@Test
	public void testMapSerialization() {
		HashMap<String, Integer> intHashMap = new HashMap<>();
		intHashMap.put("foo", 1);
		intHashMap.put("bar", 2);
		JsonElement serialized = MarshallerImpl.getFallback().serialize(intHashMap);
		Assert.assertTrue(serialized instanceof JsonObject);
		JsonObject obj = (JsonObject)serialized;
		Assert.assertEquals(new JsonPrimitive(1L), obj.get("foo"));
		Assert.assertEquals(new JsonPrimitive(2L), obj.get("bar"));
	}
	
	private static class CommentedClass {
		@Comment("This is a comment.")
		private String foo = "what?";
	}
	
	@Test
	public void testSerializedComments() {
		CommentedClass commented = new CommentedClass();
		String serialized = MarshallerImpl.getFallback().serialize(commented).toJson(true, false, 0);
		Assert.assertEquals("{ /* This is a comment. */ \"foo\": \"what?\" }", serialized);
	}
	
	private enum ExampleEnum {
		ANT,
		BOX,
		CAT,
		DAY;
	};
	
	@Test
	public void testSerializeEnums() {
		String serialized = MarshallerImpl.getFallback().serialize(ExampleEnum.CAT).toJson();
		
		Assert.assertEquals("\"CAT\"", serialized);
	}
	
	@Test
	public void testDeserializeEnums() {
		String serialized = "{ aProperty: 'DAY' }";
		try {
			JsonObject deserialized = jankson.load(serialized);
			
			ExampleEnum recovered = deserialized.get(ExampleEnum.class, "aProperty");
			Assert.assertEquals(ExampleEnum.DAY, recovered);
			
		} catch (SyntaxError ex) {
			Assert.fail("Should not get a syntax error for a well-formed object: "+ex.getCompleteMessage());
		}
	}
	
	@Test
	public void testDiffAgainstDefaults() {
		try {
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
			
			JsonObject defaultObj = jankson.load("{ a: 'a', b: 'b', c: 'c', d: { e: 'e', f: 'f' }, g: [1, 2], h: [1, 2], i: { j: 'j' } }");
			JsonObject baseObj = jankson.load("{ b: 'b', c: 'test', d: { e: 'e', f: 'test' }, g: [1, 2], h: [2, 3], i: { j: 'j' } }");
			String expected = "{ \"c\": \"test\", \"d\": { \"f\": \"test\" }, \"h\": [ 2, 3 ] }";
			
			String actual = baseObj.getDelta(defaultObj).toJson();
			Assert.assertEquals(expected, actual);
			
		} catch (SyntaxError ex) {
			Assert.fail("Should not get a syntax error for a well-formed object: "+ex.getCompleteMessage());
		}
	}
	
	@Test
	public void testArrayGet() {
		try {
			JsonObject subject = jankson.load("{ a: [1, 2, 3, 4] }");
			int[] maybe = subject.get(int[].class, "a");
			Assert.assertNotNull(maybe);
			Assert.assertArrayEquals(new int[] {1,2,3,4}, maybe);
			
		} catch (SyntaxError ex) {
			Assert.fail("Should not get a syntax error for a well-formed object: "+ex.getCompleteMessage());
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
			
			Assert.assertEquals(subject, result);
		} catch (SyntaxError ex) {
			Assert.fail("Should not get a syntax error for a well-formed object: "+ex.getCompleteMessage());
		}
	}
	
	@Test
	public void recognizeStringEscapes() {
		String subject = "{ foo: 'a\\tb\\nc\\\\'}";
		String expected = "a\tb\nc\\";
		try {
			JsonObject unpacked = jankson.load(subject);
			Assert.assertEquals(expected, unpacked.get(String.class, "foo"));
		} catch (SyntaxError ex) {
			Assert.fail("Should not get a syntax error for a well-formed object: "+ex.getCompleteMessage());
		}
	}
	
	@Test
	public void properlyEscapeStrings() {
		String inputString = "The\nquick\tbrown\ffox\bjumps\"over\\the\rlazy dog.";
		String expected = "{ \"foo\": \"The\\nquick\\tbrown\\ffox\\bjumps\\\"over\\\\the\\rlazy dog.\" }";
		JsonObject subject = new JsonObject();
		subject.put("foo", new JsonPrimitive(inputString));
		String actual = subject.toJson(false, false);
		
		Assert.assertEquals(expected, actual);
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
	
	@Test
	public void testNegativeNumbers() {
		String subject = "{ 'foo': -1, 'bar': [ -1, -3 ] }";
		
		try {
			JsonObject parsed = jankson.load(subject);
			
			Assert.assertEquals(Integer.valueOf(-1), parsed.get(Integer.class, "foo"));
			int[] array = parsed.get(int[].class, "bar");
			Assert.assertArrayEquals(new int[] {-1, -3}, array);
			
		} catch (SyntaxError ex) {
			Assert.fail("Should not get a syntax error for a well-formed object: "+ex.getCompleteMessage());
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
		
		Assert.assertEquals(expected, actual);
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
		
		Assert.assertEquals(expected, actual);
	}
	
	@Test
	public void ensureMultilineCommentsAreIndented() {
		JsonObject subject = new JsonObject();
		subject.put("foo", new JsonPrimitive("bar"), "This is a line\nAnd this is another line.");
		String actual = subject.toJson(JsonGrammar.JSON5);
		String expected =
				"{\n" +
				"	/* This is a line\n" +
				"	   And this is another line.\n" + //Three spaces precede every subsequent line to line comments up
				"	*/\n" + //The end-comment is on its own line
				"	\"foo\": \"bar\",\n" + //Again, trailing comma per JSON5
				"}";
		
		Assert.assertEquals(expected, actual);
	}
	
	@Test
	public void ensureMultilineArrayCommentsAreIndented() {
		JsonArray subject = new JsonArray();
		subject.add(new JsonPrimitive("foo"), "This is a line\nAnd this is another line.");
		String actual = subject.toJson(JsonGrammar.JSON5);
		String expected =
				"[\n" +
				"	/* This is a line\n" +
				"	   And this is another line.\n" + //Three spaces precede every subsequent line to line comments up
				"	*/\n" + //The end-comment is on its own line
				"	\"foo\",\n" + //Trailing comma per JSON5
				"]";
		
		Assert.assertEquals(expected, actual);
	}
	
	private static class TestClass {
		private ArrayList<String> strings;
		private Map<String, Character.UnicodeScript> scripts = new HashMap<>();
		private Queue<String> queue = new ArrayDeque<>();
	}
	
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
	@Test
	public void testDeserializeGenerics() {
		try {
			String serialized = "{ \"strings\": [ \"a\", \"b\", \"c\" ], \"scripts\": { \"arabic\": \"ARABIC\" }, \"queue\": [ \"FUN\" ] }";
			JsonObject subject = jankson.load(serialized);
			TestClass object = jankson.fromJson(subject, TestClass.class);
			Assert.assertEquals("Reserialized form must match original serialized form.", serialized, jankson.toJson(object).toString());
		} catch (SyntaxError ex) {
			Assert.fail("Should not get a syntax error for a well-formed object: "+ex.getCompleteMessage());
		}
	}
	
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
	
	private static class GenericArrayContainer<T> {
		public T[] ts;
		public int[] ints;
		public <U extends T> U[] u() { return null; };
	}
	
	/**
	 * Stresses TypeMagic's array comprehension, and clarifies certain deserializer behaviors and limitations.
	 * 
	 * <li>If a generic array field is found, and an instance is not provided in the constructor or an initializer, it's initialized to Object[].
	 * <li>If a primitive-typed array field is found, it's initialized to exactly its type. Primitive arrays are fully reified in Java.
	 * <li>Wildcard types are tricky. In the tested case, "<U extends T> U[]", it doesn't matter that "U" has type bounds - it's a type variable.
	 *     The variable will be treated as Object for the purposes of deserialization, and an Object[] will be created.
	 */
	@Test
	public void testGenericArrayComprehension() {
		GenericArrayContainer<String> container = new GenericArrayContainer<>();
		Type genericArrayType = container.getClass().getFields()[0].getGenericType();
		Class<?> genericArrayClass = TypeMagic.classForType(genericArrayType);
		Assert.assertEquals("Recovered generic array type should be Object[].", Object[].class, genericArrayClass);
		
		Type intArrayType = container.getClass().getFields()[1].getGenericType();
		Class<?> intArrayClass = TypeMagic.classForType(intArrayType);
		Assert.assertEquals("Recovered array type should be int[].", int[].class, intArrayClass);
		
		Type wildcardType = container.getClass().getMethods()[0].getGenericReturnType();
		Class<?> wildcardArrayClass = TypeMagic.classForType(wildcardType);
		Assert.assertEquals("Recovered wildcard array type should be Object[].", Object[].class, wildcardArrayClass);
	}
	
	@NonnullByDefault
	private static class NullContainer {
		@Nullable
		public String nullable = "";
		
		public String nonnull = "";
	}
	
	/** This test will fail as soon as a key is added for 'nonnull'. 1.2 should fix this. */
	@Test
	public void testDeserializeNulls() {
		String serialized = "{ \"nullable\": null }";
		try {
			NullContainer subject = jankson.fromJson(serialized, NullContainer.class);
			
			Assert.assertNull(subject.nullable);
			Assert.assertNotNull(subject.nonnull);
		} catch (SyntaxError ex) {
			Assert.fail("Should not get a syntax error for a well-formed object: "+ex.getCompleteMessage());
		}
		
	}
	
	
	@Test
	public void testJson5EscapedReturn() {
		String serialized = "{ \"a-multiline-string\": \"foo\\\nbar\" }";
		try {
			JsonObject subject = jankson.load(serialized);
			JsonElement parsed = subject.get("a-multiline-string");
			Assert.assertTrue("String element should be a JsonPrimitive.", parsed instanceof JsonPrimitive); //not the test
			Assert.assertEquals("Multiline String should parse to well-known result.", "foobar", ((JsonPrimitive)parsed).getValue().toString());
			
		} catch (SyntaxError ex) {
			Assert.fail("Should not get a syntax error for a well-formed object: "+ex.getCompleteMessage());
		}
	}
	
	/**
	 * While this isn't really a *normative* example, it's a pretty good example of all the JSON5 quirks in one tidy
	 * package. Jankson is and should remain fully compatible with JSON5 quirks.
	 */
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
			Assert.assertEquals("\"and you can quote me on that\"", subject.get("unquoted").toString());
			Assert.assertEquals("\"I can use \\\"double quotes\\\" here\"", subject.get("singleQuotes").toString());
			Assert.assertEquals("\"Look, Mom! No \\\\n's!\"", subject.get("lineBreaks").toString());
			Assert.assertEquals(Long.toString(0xdecaf), subject.get("hexadecimal").toString());
			//Floating point gets a little hairy, so let's use floating point comparison for this
			double leading = (Double) ((JsonPrimitive)subject.get("leadingDecimalPoint")).getValue();
			double trailing = (Double) ((JsonPrimitive)subject.get("andTrailing")).getValue();
			
			Assert.assertEquals(0.8675309, leading, 0.00000001);
			Assert.assertEquals(8675309.0, trailing, 0.00000001);
			
			long positiveSign = (Long) ((JsonPrimitive)subject.get("positiveSign")).getValue();
			Assert.assertEquals(1L, positiveSign);
			
			Assert.assertEquals("\"in objects\"", subject.get("trailingComma").toString());
			Assert.assertEquals("[ \"arrays\" ]", subject.get("andIn").toString());
			
			Assert.assertEquals("\"with JSON\"", subject.get("backwardsCompatible").toString());
		} catch (SyntaxError ex) {
			Assert.fail("Should not get a syntax error for a well-formed object: "+ex.getCompleteMessage());
		}
	}
	
	
	@Test
	public void testArrayEdgeCase() {
		String arrayDuplicates = "{ \"pattern\": [ \"ss\", \"ss\" ] }";
		try {
			JsonObject subject = jankson.load(arrayDuplicates);
			
			Assert.assertEquals(arrayDuplicates, subject.toJson(false, false));
		} catch (SyntaxError ex) {
			Assert.fail("Should not get a syntax error for a well-formed object: "+ex.getCompleteMessage());
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
		Assert.assertEquals("{ \"bar\": 4 }", elem.toJson(false, false));
		
		try {
			JsonObject subject = jankson.load("{ \"foo\": 1 }");
			StaticAccess deserialized = jankson.fromJson("{ \"foo\": 1 }", StaticAccess.class);
			Assert.assertEquals(8, StaticAccess.foo);
		} catch (SyntaxError ex) {
			Assert.fail("Should not get a syntax error for a well-formed object: "+ex.getCompleteMessage());
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
		Assert.assertNotNull(unpacked.getInner());
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
			Assert.assertEquals(obj.getMarshaller(), adaptedJankson.getMarshaller());
			Assert.assertEquals(((JsonArray)obj.get("enclosed")).getMarshaller(), adaptedJankson.getMarshaller());
			ElementClass test = adaptedJankson.getMarshaller().marshall(ElementClass.class, ((JsonArray)obj.get("enclosed")).get(0));
			Assert.assertEquals("ADAPTER RAN", test.a);
			
			ElementContainerClass result = adaptedJankson.getMarshaller().marshall(ElementContainerClass.class, obj);
			
			//EnclosingAdaptedClass result = adaptedJankson.fromJson("{ 'enclosed': [ {'a': 'foo' } ] }", EnclosingAdaptedClass.class);
			
			
			Assert.assertEquals("ADAPTER RAN", result.enclosed.get(0).a);
		} catch (SyntaxError ex) {
			Assert.fail("Should not get a syntax error for a well-formed object: "+ex.getCompleteMessage());
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
			Assert.fail("Should not get a syntax error for a well-formed object: "+ex.getCompleteMessage());
		}
	}
	
	
	
	private static class NameTest {
		@SerializedName("foo_bar")
		public int fooBar = 0;
	}
	
	/** SerializedName should be preferred over the field's name in both POJO serialization and deserialization */
	@Test
	public void testSerializedName() {
		String subject =
				"{\n" + 
				"	\"foo_bar\": 31,\n" + 
				"}";
		try {
			NameTest object = jankson.fromJson(subject, NameTest.class);
			Assert.assertEquals(31, object.fooBar);
			
			String out = jankson.toJson(object).toJson(JsonGrammar.JSON5);
			Assert.assertEquals(subject, out);
		} catch (SyntaxError ex) {
			Assert.fail("Should not get a syntax error for a well-formed object: "+ex.getCompleteMessage());
		}
	}
	
	/** This makes sure tilde on its own gets processed as a String by the token parser */
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
			
			Assert.assertEquals("~", foo_bar);
			Assert.assertEquals("bux", baz);
		} catch (SyntaxError ex) {
			Assert.fail("Should not get a syntax error for a well-formed object: "+ex.getCompleteMessage());
		}
	}
	
	/**
	 * Issue: #21
	 * Special numerics serializing into quoted String values, so that a save-and-load no longer considers the values numeric.
	 */
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
		
		Assert.assertEquals(expected, actual);
	}
	
	/**
	 * Issue: #22
	 * Offer a grammar option for outputting root objects without braces ( "{}" ) while inner objects retain their delimiters.
	 */
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
		Assert.assertEquals(expected, actual);
	}
	
	/**
	 * Issue: #23
	 * Grammar option to print unquoted keys
	 */
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
		Assert.assertEquals(expected, actual);
	}
}
