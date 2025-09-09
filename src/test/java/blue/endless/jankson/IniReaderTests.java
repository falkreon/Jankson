/*
 * MIT License
 *
 * Copyright (c) 2018-2025 Falkreon (Isaac Ellingson)
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
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import blue.endless.jankson.api.SyntaxError;
import blue.endless.jankson.api.io.ini.IniReader;
import blue.endless.jankson.api.io.json.JsonWriter;
import blue.endless.jankson.api.io.json.JsonWriterOptions;

public class IniReaderTests {
	@Test
	public void testEnumCast() {
		ProcessBuilder.Redirect.Type expected = ProcessBuilder.Redirect.Type.PIPE;
		
		Optional<ProcessBuilder.Redirect.Type> actual = IniReader.castToEnum("pipe", ProcessBuilder.Redirect.Type.class);
		
		Assertions.assertEquals(expected, actual.get());
	}
	
	@Test
	public void testListCast() {
		String subject = "foo, bar, baz bux, blah";
		
		List<String> expected = List.of("foo", "bar", "baz bux", "blah");
		
		List<String> actual = IniReader.castToList(subject);
		
		Assertions.assertEquals(expected, actual);
	}
	
	@Test
	public void testIntCast() {
		String subject = "13";
		
		int expected = 13;
		
		int actual = IniReader.castToInt(subject).getAsInt();
		
		Assertions.assertEquals(expected, actual);
	}
	
	@Test
	public void testFileParse() throws SyntaxError, IOException {
		String iniData = """
		[foo]
		this format = a bunch of key-value pairs
		[bux]
		tests = testEnumCast, testListCast, testIntCast
		quoted = "#ff_000000"
		""";
		
		String expected = """
		{
			"foo": {
				"this format": "a bunch of key-value pairs"
			},
			"bux": {
				"tests": "testEnumCast, testListCast, testIntCast",
				"quoted": "#ff_000000"
			}
		}
		""".trim();
		
		IniReader reader = new IniReader(new StringReader(iniData));
		StringWriter out = new StringWriter();
		JsonWriter writer = new JsonWriter(out, JsonWriterOptions.STRICT);
		reader.transferTo(writer);
		String actual = out.toString();
		
		Assertions.assertEquals(expected, actual);
	}
}
