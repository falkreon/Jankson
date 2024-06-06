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

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import blue.endless.jankson.api.io.JsonWriter;
import blue.endless.jankson.api.io.JsonWriterOptions;
import blue.endless.jankson.api.io.TomlReader;
import blue.endless.jankson.api.io.JsonWriterOptions.Hint;

public class TomlReaderTests {
	
	private static final JsonWriterOptions STRICT_ONE_LINE = new JsonWriterOptions(Hint.WRITE_WHITESPACE);
	
	@Test
	public void testKeys() throws IOException {
		String tomlExample = """
		name = "Orange"
		physical.color = "orange"
		physical.shape = "round"
		site."google.com" = true
		""";
		
		String expected = """
		{
			"name": "Orange",
			"physical": {
				"color": "orange",
				"shape": "round"
			},
			"site": {
				"google.com": true
			}
		}
		""";
		
		TomlReader reader = new TomlReader(new StringReader(tomlExample));
		StringWriter out = new StringWriter();
		JsonWriter writer = new JsonWriter(out, JsonWriterOptions.STRICT);
		reader.transferTo(writer);
		String actual = out.toString();
		
		Assertions.assertEquals(expected.stripIndent().trim(), actual);
	}
	
	@Test
	public void testBadlyConceivedKeys() throws IOException {
		String tomlExample = """
		3.14159 = "pi"
		""";
		
		String expected = """
		{ "3": { "14159": "pi" } }
		""";
		
		TomlReader reader = new TomlReader(new StringReader(tomlExample));
		StringWriter out = new StringWriter();
		JsonWriter writer = new JsonWriter(out, STRICT_ONE_LINE);
		reader.transferTo(writer);
		String actual = out.toString();
		
		Assertions.assertEquals(expected.stripIndent().trim(), actual);
	}
	
	@Test
	public void testTable() throws IOException {
		String tomlExample = """
		[dog."tater.man"]
		type.name = "pug"
		""";
		
		String expected = """
		{ "dog": { "tater.man": { "type": { "name": "pug" } } } }
		""";
		
		TomlReader reader = new TomlReader(new StringReader(tomlExample));
		StringWriter out = new StringWriter();
		JsonWriter writer = new JsonWriter(out, STRICT_ONE_LINE);
		reader.transferTo(writer);
		String actual = out.toString();
		
		Assertions.assertEquals(expected.stripIndent().trim(), actual);
	}
	
	/**
	 * Jankson explicitly allows this, in deviation of the spec.
	 */
	@Test
	public void testAllowDuplicateTables() throws IOException {
		String tomlExample = """
		# DO NOT DO THIS (unless you're using jankson)
		
		[fruit]
		apple = "red"
		
		[fruit]
		orange = "orange"
		""";
		
		TomlReader reader = new TomlReader(new StringReader(tomlExample));
		JsonWriter writer = new JsonWriter(new StringWriter(), STRICT_ONE_LINE);
		
		reader.transferTo(writer);
	}
	
	/**
	 * This is disallowed because in one breath we're saying both:
	 * <p><pre>  fruit.apple = "red"</pre>
	 * <p>and:
	 * <p><pre>  fruit.apple = { texture = "smooth" }</pre>
	 * 
	 * <p>Both of these cannot be true, so the value is undefined, and we report an error.
	 */
	@Test
	public void testForbidRedefiningTables() {
		String tomlExample = """
		# DO NOT DO THIS EITHER

		[fruit]
		apple = "red"
		
		[fruit.apple]
		texture = "smooth"
		""";
		
		TomlReader reader = new TomlReader(new StringReader(tomlExample));
		JsonWriter writer = new JsonWriter(new StringWriter(), STRICT_ONE_LINE);
		
		Assertions.assertThrows(IOException.class, ()->reader.transferTo(writer));
	}
}
