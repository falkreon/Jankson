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
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Objects;

import org.junit.jupiter.api.Test;

import blue.endless.jankson.api.Jankson;
import blue.endless.jankson.api.SyntaxError;
import blue.endless.jankson.api.document.ValueElement;
import blue.endless.jankson.api.io.ElementType;
import blue.endless.jankson.api.io.JsonReader;
import blue.endless.jankson.api.io.JsonWriter;

public class RefactorTests {
	/*
	@Test
	public void basicOperation() throws IOException {
		StringWriter stringWriter = new StringWriter();
		JsonWriter writer = new JsonWriter(stringWriter, JsonWriterOptions.INI_SON);
		
		ObjectElement test = new ObjectElement();
		test.put("foo", PrimitiveElement.of(42));
		
		KeyValuePairElement kvp = new KeyValuePairElement("bar", new ObjectElement());
		kvp.getPreamble().add(FormattingElement.LINE_BREAK);
		test.add(kvp);
		
		test.write(writer);
		
		System.out.println(stringWriter.toString());
	}*/
	
	@Test
	public void spam() throws IOException, SyntaxError {
		String subject = "{ \"foo\": 42, /* stuff */ \"bar\": {} }";
		
		ValueElement value = Jankson.readJson(subject);
		
		System.out.println(value);
		
	}
}
