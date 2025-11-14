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

package blue.endless.jankson.api.io.ini;

import java.io.IOException;
import java.io.Writer;

import blue.endless.jankson.api.SyntaxError;
import blue.endless.jankson.api.io.StructuredData;
import blue.endless.jankson.api.io.StructuredDataWriter;

public class IniWriter implements StructuredDataWriter {
	private Writer out;
	
	public IniWriter(Writer w) {
		out = w;
	}
	
	// TODO: headers, sub-objects, etc.
	@Override
	public void write(StructuredData data) throws SyntaxError, IOException {
		switch(data.type()) {
			case ARRAY_END -> {}
			case ARRAY_START -> {}
			case COMMENT -> {}
			case EOF -> {}
			case NEWLINE -> { out.write("\n"); }
			case OBJECT_END -> {}
			case OBJECT_KEY -> { out.write(data.value().toString()); out.write(" = "); }
			case OBJECT_START -> {}
			case PRIMITIVE -> out.write(data.value().toString());
			case WHITESPACE -> {}
			default -> throw new IllegalArgumentException("Unexpected value: " + data.type());
		}
	}
	
}
