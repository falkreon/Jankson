/*
 * MIT License
 *
 * Copyright (c) 2018-2020 Falkreon (Isaac Ellingson)
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

import java.util.regex.Pattern;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import blue.endless.jankson.api.annotation.Serializer;
import blue.endless.jankson.api.Jankson;
import blue.endless.jankson.api.JsonGrammar;
import blue.endless.jankson.api.SyntaxError;
import blue.endless.jankson.api.element.JsonElement;
import blue.endless.jankson.api.element.JsonNull;
import blue.endless.jankson.api.element.JsonObject;
import blue.endless.jankson.api.element.JsonPrimitive;

public class TestSerializer {
	Jankson jankson;
	
	@Before
	public void setup() {
		jankson = Jankson.builder().build();
	}
	
	/**
	 * Make sure that characters which lie outside the BMP and/or have complex encodings wind up
	 * decomposed and escaped properly
	 */
	@Test
	public void testUnicodeEscapes() {
		String smileyFace = String.valueOf(Character.toChars(0x1F600));
		String result = new JsonPrimitive(smileyFace).toString();
		Assert.assertEquals("\"\\ud83d\\ude00\"", result);
	}
	
	
	private static class DeclaredSerializerTest {
		private String foo = "bar";
		
		@Serializer
		public JsonPrimitive serialize() {
			return JsonPrimitive.of(42L);
		}
	}
	
	@Test
	public void testInternalSerializer() {
		JsonElement elem = jankson.toJson(new DeclaredSerializerTest());
		Assert.assertEquals("42", elem.toJson());
	}
	
	/**
	 * Issue #34 - switching to Writer caused a small bug where doubles were double-printed
	 */
	@Test
	public void testBareSpecialNumericsDuplication() {
		JsonObject subject = new JsonObject();
		subject.put("foo", JsonPrimitive.of(42.0));
		
		JsonGrammar grammar = JsonGrammar.builder().bareSpecialNumerics(true).printWhitespace(false).build();
		
		Assert.assertEquals("{ \"foo\": 42.0 }", subject.toJson(grammar));
	}
	
	/** Issues #26 and #38 - ':' isn't force-quoted in object keys */
	@Test
	public void testIssue26() {
		JsonObject obj = new JsonObject();
		obj.put("test:key", JsonNull.INSTANCE);
		
		String result = obj.toJson(JsonGrammar.builder().printWhitespace(false).printUnquotedKeys(true).build());
		Assert.assertEquals("{ \"test:key\": null }", result);
	}
	
	/**
	 * Issue #42: String with russian characters is incorrectly parsed.
	 * 
	 * <p>Moved off of a hand-decoding of UTF-8 surrogates onto Reader, which offers a much more robust and future-proof assembly of UTF-8 surrogates into code points.
	 */
	
	@Test
	public void testCyrillic() {
		String input =
				"{\n" + 
				"  en: \"Play with sound?\",\n" + 
				"  ru: \"Играть с музыкой?\"\n" + 
				"}";
		
		try {
			JsonObject obj = Jankson.builder().build().load(input);
			
			Assert.assertEquals("Играть с музыкой?", obj.get(String.class, "ru"));
		} catch (SyntaxError ex) {
			Assert.fail("Should not get a syntax error for a well-formed object: "+ex.getCompleteMessage());
		}
	}
}
