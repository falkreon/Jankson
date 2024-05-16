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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import blue.endless.jankson.api.Jankson;
import blue.endless.jankson.api.SyntaxError;

public class TestCosmetic {

	/**
	 * Issue: #16
	 * Closing array brace needs to be indented. Empty array should be kept on the same line.
	 */
	/*
	@Test
	public void testArrayIndents() {
		
		String expectedJson5 =
				"{\n" + 
				"	\"empty\": [],\n" + //one-line empty array with no whitespace
				"	\"nonempty\": [\n" + //No whitespace after opening brace
				"		\"Foo\",\n" + //Properly indented items, one line per item
				"		42,\n" + //closing comma
				"	],\n" + //indented closing array with comma
				"}";
		
		String expectedStrict =
				"{\n" +
				"	\"empty\": [],\n" + 
				"	\"nonempty\": [\n" + 
				"		\"Foo\",\n" + 
				"		42\n" + //missing comma
				"	]\n" + //missing comma 
				"}";
		
		String expectedRootArray =
				"[\n" + //no indent, no errors
				"	\"Foo\",\n" + //1 indent level
				"	42,\n" + 
				"]"; //no indent
		
		JsonObject subject = new JsonObject();
		JsonArray emptyArr = new JsonArray();
		JsonArray nonEmptyArr = new JsonArray();
		
		subject.put("empty", emptyArr);
		subject.put("nonempty", nonEmptyArr);
		nonEmptyArr.add(0, new JsonPrimitive("Foo"));
		nonEmptyArr.add(1, new JsonPrimitive(42));
		
		String json5 = subject.toJson(JsonGrammar.JSON5);
		String strict = subject.toJson(JsonGrammar.STRICT);
		String rootArray = nonEmptyArr.toJson(JsonGrammar.JSON5);
		
		Assertions.assertEquals(expectedJson5, json5);
		Assertions.assertEquals(expectedStrict, strict);
		Assertions.assertEquals(expectedRootArray, rootArray);
	}*/
	
//	@Test
//	public void testBareRootObjectIndentWithComment() {
//		String expectedJkson =
//				"/* This is a multiline\n" +
//				"   comment.\n" +
//				"*/\n" +
//				"object: {\n" +
//				"\tfoo: true\n" +
//				"}\n";
//		
//		JsonObject subject = new JsonObject();
//		JsonObject sub = new JsonObject();
//		
//		sub.put("foo", JsonPrimitive.TRUE);
//		
//		subject.put("object", sub, "This is a multiline\ncomment.\n");
//		
//		String result = subject.toJson(JsonGrammar.builder()
//				.bareRootObject(true)
//				.printCommas(false)
//				.withComments(true)
//				.printWhitespace(true)
//				.printUnquotedKeys(true)
//				.build());
//		
//		Assertions.assertEquals(expectedJkson, result);
//	}
//	
//	/**
//	 * Issue #44: Adding comments with 'bareRootObject(true)' messes up indentation
//	 * 
//	 * Makes sure that indentation is correct after a single-line comment under "bare root" output
//	 */
//	@Test
//	public void testBareRootObjectIndentWithSingleLineComment() {
//		JsonGrammar grammar = JsonGrammar.builder().bareRootObject(true).build();
//		JsonObject object = new JsonObject();
//		object.put("first_thing", new JsonPrimitive("First Thing"));
//		object.put("second_thing", new JsonPrimitive("Second Thing"), "This has a comment!");
//		object.put("third_thing", new JsonPrimitive("Third Thing"));
//		
//		String expectedJkson =
//			"\"first_thing\": \"First Thing\",\n"
//			+ "// This has a comment!\n"
//			+ "\"second_thing\": \"Second Thing\",\n"
//			+ "\"third_thing\": \"Third Thing\"\n";
//		
//		Assertions.assertEquals(expectedJkson, object.toJson(grammar));
//	}
//	
//	/**
//	 * Issue #36:  Multiline comments sometimes lost in serialization/deserialization cycles
//	 */
//	@Test
//	public void testMultilineCommentsRoundTrip() {
//		JsonObject obj = new JsonObject();
//		JsonPrimitive p = new JsonPrimitive(42);
//		obj.put("thing", p, "this is a multiline\ncomment");
//		
//		String serialized = obj.toJson(JsonGrammar.JSON5);
//		
//		String expectedJkson =
//			"{\n"
//			+ "	/* this is a multiline\n"
//			+ "	   comment\n"
//			+ "	*/\n"
//			+ "	\"thing\": 42,\n"
//			+ "}";
//		
//		Assertions.assertEquals(expectedJkson, serialized);
//	}
//	
//	/**
//	 * Further testing for #36
//	 * @throws SyntaxError 
//	 */
//	@Test
//	public void testMultiCommentDeserialize() throws SyntaxError {
//		String source = "{ /* this is a multiline */ /* comment */ \"thing\": 42 }";
//		
//		JsonObject object = Jankson.builder().build().load(source);
//		
//		String reserialized = object.toJson(JsonGrammar.JSON5);
//		
//		String expectedJkson =
//			"{\n"
//			+ "	/* this is a multiline\n"
//			+ "	   comment\n"
//			+ "	*/\n"
//			+ "	\"thing\": 42,\n"
//			+ "}";
//		
//		Assertions.assertEquals(expectedJkson, reserialized);
//	}
}
