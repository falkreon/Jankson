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

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import blue.endless.jankson.api.document.FormattingElement;
import blue.endless.jankson.api.document.KeyValuePairElement;
import blue.endless.jankson.api.document.ObjectElement;
import blue.endless.jankson.api.document.PrimitiveElement;
import blue.endless.jankson.api.io.JsonWriter;
import blue.endless.jankson.api.io.JsonWriterOptions;
import blue.endless.jankson.impl.io.LookaheadCodePointReader;

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
	
	/*
	@Test
	public void testUnread() throws IOException {
		StringReader s = new StringReader("Foo Bar");
		CodePointReader in = new CodePointReader(s);
		
		System.out.println(in.peekString(4));
		char[] fin = new char[7];
		in.read(fin);
		System.out.println(new String(fin));
	}*/
	
	@Test
	public void codePoints() {
		
		/*
		//String pizza = "\uD83C\uD855";
		String pizza = new String(new int[] { 0x1F355 }, 0, 1);
		System.out.println(pizza);
		byte[] utf8 = pizza.getBytes();
		System.out.println(Arrays.toString(utf8));
		
		List<Integer> points = new ArrayList<>();
		for(char ch : pizza.toCharArray()) points.add((int) ch);
		
		List<String> pointsList = points.stream().map(Integer::toHexString).collect(Collectors.toList());
		
		System.out.println(pointsList);*/
	}
}
