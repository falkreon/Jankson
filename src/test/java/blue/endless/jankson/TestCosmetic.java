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

public class TestCosmetic {
	Jankson jankson;
	
	@Before
	public void setup() {
		jankson = Jankson.builder().build();
	}
	
	/**
	 * Issue: #16
	 * Closing array brace needs to be indented. Empty array should be kept on the same line.
	 */
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
		
		Assert.assertEquals(expectedJson5, json5);
		Assert.assertEquals(expectedStrict, strict);
		Assert.assertEquals(expectedRootArray, rootArray);
	}
}
