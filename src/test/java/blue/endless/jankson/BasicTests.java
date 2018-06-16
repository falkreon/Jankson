package blue.endless.jankson;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

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
}
