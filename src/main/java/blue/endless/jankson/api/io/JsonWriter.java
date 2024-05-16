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

package blue.endless.jankson.api.io;

import java.io.IOException;
import java.io.Writer;

import blue.endless.jankson.api.document.CommentType;
import blue.endless.jankson.impl.io.AbstractStructuredDataWriter;

public class JsonWriter extends AbstractStructuredDataWriter {
	private final JsonWriterOptions options;
	private int indentLevel = 0;
	
	public JsonWriter(Writer destination) {
		this(destination, JsonWriterOptions.DEFAULTS);
	}
	
	public JsonWriter(Writer destination, JsonWriterOptions options) {
		super(destination);
		this.options = options;
	}

	@Override
	public void writeComment(String value, CommentType type) throws IOException {
		switch(type) {
		case LINE_END:
			dest.write("//");
			dest.write(value);
			writeNewline();
			break;
		
		case OCTOTHORPE:
			dest.write("#");
			dest.write(value);
			writeNewline();
			break;
		
		case MULTILINE:
			dest.write("/*");
			dest.write(value);
			dest.write("*/ "); //TODO: Figure out if we should write this extra space here
			break;
		
		case DOC:
			dest.write("/**");
			dest.write(value);
			dest.write("*/ "); //TODO: Figure out if we should write this extra space here
			break;
		}
	}

	@Override
	public void writeWhitespace(String value) throws IOException {
		dest.write(value);
	}

	@Override
	public void writeKey(String key) throws IOException {
		assertKey();
		
		//TODO: escape parts of the key if needed, omit quotes if possible + configured
		boolean quoted = !options.get(JsonWriterOptions.Hint.UNQUOTED_KEYS); //TODO: Check to make sure it CAN be unquoted
		if (quoted) {
			dest.write('"');
		}
		dest.write(key);
		if (quoted) {
			dest.write("\" ");
		}
		
		keyWritten();
	}

	@Override
	public void writeKeyValueDelimiter() throws IOException {
		assertKeyValueDelimiter();
		
		if (options.get(JsonWriterOptions.Hint.KEY_EQUALS_VALUE)) {
			dest.write(" = ");
		} else {
			dest.write(": ");
		}
		
		keyValueDelimiterWritten();
	}

	@Override
	public void nextValue() throws IOException {
		assertNextValue();
		if (!options.get(JsonWriterOptions.Hint.OMIT_COMMAS)) {
			dest.write(", ");
		} else {
			dest.write(" ");
		}
		nextValueWritten();
	}

	@Override
	public void writeObjectStart() throws IOException {
		assertValue();
		if (peek()==State.ROOT && options.get(JsonWriterOptions.Hint.BARE_ROOT_OBJECT)) {
			//Do not write the brace, and do not increase the indent level.
		} else {
			//dest.write("{ ");
			dest.write('{'); //TODO: Consult hints for newline behavior
			indentLevel++;
			writeNewline();
		}
		objectStarted();
	}

	@Override
	public void writeObjectEnd() throws IOException {
		assertObjectEnd();
		
		if (isWritingRoot() && options.get(JsonWriterOptions.Hint.BARE_ROOT_OBJECT)) {
			//Do not write closing brace, and do not decrease the indent level.
		} else {
			indentLevel--;
			writeNewline(); //TODO: Consult hints for newline behavior
			dest.write('}');
		}
		
		objectEndWritten();
	}

	@Override
	public void writeArrayStart() throws IOException {
		assertValue();
		dest.write("[ "); //TODO: Consult hints for newline behavior
		indentLevel++;
		arrayStarted();
	}

	@Override
	public void writeArrayEnd() throws IOException {
		assertArrayEnd();
		dest.write(" ]");
		indentLevel--;
		arrayEndWritten();
	}

	@Override
	public void writeStringLiteral(String value) throws IOException {
		assertValue();
		
		dest.write('"');
		dest.write(value);
		dest.write('"');
		
		valueWritten();
	}

	@Override
	public void writeLongLiteral(long value) throws IOException {
		assertValue();
		dest.write(Long.toString(value));
		valueWritten();
	}

	@Override
	public void writeDoubleLiteral(double value) throws IOException {
		assertValue();
		dest.write(Double.toString(value));
		valueWritten();
	}

	@Override
	public void writeBooleanLiteral(boolean value) throws IOException {
		assertValue();
		dest.write(Boolean.toString(value));
		valueWritten();
	}

	@Override
	public void writeNullLiteral() throws IOException {
		assertValue();
		dest.write("null");
		valueWritten();
	}
	
	private void writeNewline() throws IOException {
		dest.write('\n');
		dest.write("\t".repeat(indentLevel)); //TODO: Get the indent String from options
	}
}
