/*
 * MIT License
 *
 * Copyright (c) 2018-2019 Falkreon (Isaac Ellingson)
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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import blue.endless.jankson.annotation.Deserializer;
import blue.endless.jankson.api.DeserializationException;
import blue.endless.jankson.api.SyntaxError;

public class TestDeserializer {
	Jankson jankson;
	
	@Before
	public void setup() {
		jankson = Jankson.builder().build();
	}
	
	public static class Foo {
		private String value = "";
		private String opt = "";
		
		@Deserializer
		public static Foo deserialize(String s) {
			Foo result = new Foo();
			result.value = s;
			return result;
		}
		
		@Deserializer
		public static Foo deserialize(JsonArray arr) throws DeserializationException {
			if (arr.size()<1 || arr.size()>2) throw new DeserializationException("Array can have either 1 or 2 elements. Found: "+arr.size());
			Foo result = new Foo();
			result.value = arr.getString(0, "OOPS");
			if (arr.size()>1) result.opt = arr.getString(1, "OOPS");
			return result;
		}
	}
	
	public static class Bar {
		public Foo stringDeserializer;
		public Foo arrayDeserializer;
		public Foo unmappedDeserializer = new Foo();
		public Foo defaultDeserializer;
	}
	
	/**
	 * Make sure that various kinds of self-described deserializers are working alongside each other
	 */
	@Test
	public void testBasicFeatures() throws DeserializationException {
		String subject =
				"{\n" + 
				"	\"stringDeserializer\": \"test\",\n" + 
				"	\"arrayDeserializer\": [ \"someValue\", \"someOptValue\" ],\n" + 
				"	\"unmappedDeserializer\": false,\n" + 
				"	\"defaultDeserializer\": {\n" + 
				"		\"value\": \"someValue\",\n" + 
				"		\"opt\": \"someOptValue\"\n" + 
				"	}\n" + 
				"\n" + 
				"}";
		
		try {
			Bar obj = jankson.fromJson(subject, Bar.class);
			
			Assert.assertNotNull(obj.stringDeserializer);
			Assert.assertNotNull(obj.arrayDeserializer);
			Assert.assertNotNull(obj.unmappedDeserializer);
			Assert.assertNotNull(obj.defaultDeserializer);
			
			Assert.assertEquals("test", obj.stringDeserializer.value);
			Assert.assertEquals("someValue", obj.arrayDeserializer.value);
			Assert.assertEquals("someOptValue", obj.arrayDeserializer.opt);
			Assert.assertEquals("", obj.unmappedDeserializer.value);
			Assert.assertEquals("someValue", obj.defaultDeserializer.value);
			Assert.assertEquals("someOptValue", obj.defaultDeserializer.opt);
		} catch (SyntaxError ex) {
			Assert.fail("Should not get a syntax error for a well-formed object: "+ex.getCompleteMessage());
		}
	}
	
	/**
	 * Just to be clear: If we can't unpack a key for any reason, Carefully should throw an error.
	 */
	@Test(expected = DeserializationException.class)
	public void ensureErrorOnMissingDeserializer() throws SyntaxError, DeserializationException {
		String subject =
				"{\n" + 
				"	\"stringDeserializer\": \"test\",\n" + 
				"	\"arrayDeserializer\": [ \"someValue\", \"someOptValue\" ],\n" + 
				"	\"unmappedDeserializer\": false,\n" + 
				"	\"defaultDeserializer\": {\n" + 
				"		\"value\": \"someValue\",\n" + 
				"		\"opt\": \"someOptValue\"\n" + 
				"	}\n" + 
				"\n" + 
				"}";
		
		Bar obj = jankson.fromJsonCarefully(subject, Bar.class);
	}
	
	public static class Baz {
		public Foo foo;
	}
	
	
	
	
	
	/** This makes sure that values like Infinity, NaN, and -Infinity are parsed properly */
	@Test
	public void testSpecialNumericValues() {
		String subject =
				"{\n" + 
				"	\"a\": Infinity,\n" + 
				"	\"b\": -Infinity,\n" + 
				"	\"c\": NaN,\n" + 
				"}";
		try {
			JsonObject result = jankson.load(subject);
			JsonElement a = result.get("a");
			JsonElement b = result.get("b");
			JsonElement c = result.get("c");
			Assert.assertTrue("'Infinity' parses to a JsonPrimitive", a instanceof JsonPrimitive);
			Assert.assertTrue("'-Infinity' parses to a JsonPrimitive", b instanceof JsonPrimitive);
			Assert.assertTrue("'NaN' parses to a JsonPrimitive", c instanceof JsonPrimitive);
			
			Assert.assertEquals(Double.POSITIVE_INFINITY, ((JsonPrimitive)a).asDouble(-1), 1);
			Assert.assertEquals(Double.NEGATIVE_INFINITY, ((JsonPrimitive)b).asDouble(-1), 1);
			Assert.assertEquals(Double.NaN, ((JsonPrimitive)c).asDouble(-1), 1);
		} catch (SyntaxError ex) {
			Assert.fail("Should not get a syntax error for a well-formed object: "+ex.getCompleteMessage());
		}
	}
}
