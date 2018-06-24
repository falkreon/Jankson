/*
 * MIT License
 *
 * Copyright (c) 2018 Falkreon
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import blue.endless.jankson.impl.Marshaller;
import blue.endless.jankson.impl.SyntaxError;

public class BasicTests {
	Jankson jankson;
	
	@Before
	public void setup() {
		jankson = Jankson.builder().build();
	}
	
	@Test
	public void testPrimitiveEquality() {
		Assert.assertEquals("Equal objects should produce equal json primitives", new JsonPrimitive("foo"), new JsonPrimitive(new String("foo"))); //Ensure no interning
		Assert.assertEquals("Equal objects should produce equal json primitives", new JsonPrimitive(Double.valueOf(42)), new JsonPrimitive(new Double(42)));
		
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
	
	public static class TestObject {
		private int x = 1;
		private String y = "Hello";
	}
	
	@Test
	public void testArraySerialization() {
		int[] intArray = new int[] {3, 2, 1};
		String serializedIntArray = Marshaller.getFallback().serialize(intArray).toString();
		Assert.assertEquals("[ 3, 2, 1 ]", serializedIntArray);
		
		Void[] voidArray = new Void[] {null, null}; //Yes, I realize this is black magic. We *must not* simply break at the first sign of black magic.
		String serializedVoidArray = Marshaller.getFallback().serialize(voidArray).toString();
		Assert.assertEquals("[ null, null ]", serializedVoidArray);
		
		List<Double[]> doubleArrayList = new ArrayList<Double[]>();
		doubleArrayList.add(new Double[] {1.0, 2.0, 3.0});
		doubleArrayList.add(new Double[] {4.0, 5.0});
		String serializedDoubleArrayList = Marshaller.getFallback().serialize(doubleArrayList).toString();
		Assert.assertEquals("[ [ 1.0, 2.0, 3.0 ], [ 4.0, 5.0 ] ]", serializedDoubleArrayList);
	}
	
	@Test
	public void testMapSerialization() {
		HashMap<String, Integer> intHashMap = new HashMap<>();
		intHashMap.put("foo", 1);
		intHashMap.put("bar", 2);
		JsonElement serialized = Marshaller.getFallback().serialize(intHashMap);
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
		String serialized = Marshaller.getFallback().serialize(commented).toJson(true, false);
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
		String serialized = Marshaller.getFallback().serialize(ExampleEnum.CAT).toJson();
		
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
}
