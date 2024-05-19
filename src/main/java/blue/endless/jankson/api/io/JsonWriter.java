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

import static blue.endless.jankson.api.io.JsonWriterOptions.Hint.*;

public class JsonWriter extends AbstractStructuredDataWriter {
	private final JsonWriterOptions options;
	private int indentLevel = 0;
	
	private String resource = "";
	private int line = 0;
	private int column = 0;
	
	public JsonWriter(Writer destination) {
		this(destination, JsonWriterOptions.DEFAULTS);
	}
	
	public JsonWriter(Writer destination, JsonWriterOptions options) {
		super(destination);
		this.options = options;
	}
	
	private void write(char ch) throws IOException {
		if (ch == '\n') {
			line++;
			column = 0;
		}
		dest.write(ch);
	}
	
	private void write(String s) throws IOException {
		for(int i=0; i<s.length(); i++) {
			dest.write(s.charAt(i));
		}
	}
	
	private boolean hint(JsonWriterOptions.Hint hint) {
		return options.get(hint);
	}
	
	public int getLine() {
		return line;
	}
	
	public int getColumn() {
		return column;
	}
	
	@Override
	public void writeComment(String value, CommentType type) throws IOException {
		switch(type) {
		case LINE_END:
			write("//");
			write(value);
			writeNewline();
			break;
		
		case OCTOTHORPE:
			write("#");
			write(value);
			writeNewline();
			break;
		
		case MULTILINE:
			write("/*");
			write(value);
			write("*/ "); //TODO: Figure out if we should write this extra space here
			break;
		
		case DOC:
			write("/**");
			write(value);
			write("*/ "); //TODO: Figure out if we should write this extra space here
			break;
		}
	}

	@Override
	public void writeWhitespace(String value) throws IOException {
		if (hint(WRITE_WHITESPACE)) write(value);
	}

	@Override
	public void writeKey(String key) throws IOException {
		assertKey();
		
		//TODO: escape parts of the key if needed, omit quotes if possible + configured
		boolean quoted = !hint(UNQUOTED_KEYS); //TODO: Check to make sure it CAN be unquoted
		if (quoted) {
			write('"');
		}
		dest.write(key);
		if (quoted) {
			write("\" ");
		}
		
		keyWritten();
	}

	@Override
	public void writeKeyValueDelimiter() throws IOException {
		assertKeyValueDelimiter();
		
		if (hint(KEY_EQUALS_VALUE)) {
			write(" = ");
		} else {
			write(": ");
		}
		
		keyValueDelimiterWritten();
	}

	@Override
	public void nextValue() throws IOException {
		assertNextValue();
		
		if (!hint(OMIT_COMMAS)) {
			write(',');
		}
		
		if (hint(WRITE_NEWLINES)) {
			writeNewline();
		} else {
			write(' ');
		}
		
		nextValueWritten();
	}

	@Override
	public void writeObjectStart() throws IOException {
		assertValue();
		if (peek()==State.ROOT && hint(BARE_ROOT_OBJECT)) {
			//Do not write the brace, and do not increase the indent level.
		} else {
			write('{');
			if (hint(WRITE_NEWLINES)) {
				indentLevel++;
				writeNewline();
			} else {
				if (hint(WRITE_WHITESPACE)) write(' ');
			}
		}
		objectStarted();
	}

	@Override
	public void writeObjectEnd() throws IOException {
		assertObjectEnd();
		
		if (isWritingRoot() && hint(BARE_ROOT_OBJECT)) {
			//Do not write closing brace, and do not decrease the indent level.
		} else {
			indentLevel--;
			writeNewline(); //TODO: Consult hints for newline behavior
			write('}');
		}
		
		objectEndWritten();
	}

	@Override
	public void writeArrayStart() throws IOException {
		assertValue();
		
		write("["); //TODO: Consult hints for newline behavior
		if (hint(WRITE_NEWLINES)) {
			indentLevel++;
			writeNewline();
			
		}
		
		arrayStarted();
	}

	@Override
	public void writeArrayEnd() throws IOException {
		assertArrayEnd();
		
		if (hint(WRITE_NEWLINES)) {
			indentLevel--;
			writeNewline();
		}
		
		write(']');
		if (hint(WRITE_NEWLINES)) {
			writeNewline();
		} else if (hint(WRITE_WHITESPACE)) {
			write(' ');
		}
		
		arrayEndWritten();
	}

	@Override
	public void writeStringLiteral(String value) throws IOException {
		assertValue();
		
		write('"');
		write(value);
		write('"');
		
		valueWritten();
	}

	@Override
	public void writeLongLiteral(long value) throws IOException {
		assertValue();
		write(Long.toString(value));
		valueWritten();
	}

	@Override
	public void writeDoubleLiteral(double value) throws IOException {
		assertValue();
		write(Double.toString(value));
		valueWritten();
	}

	@Override
	public void writeBooleanLiteral(boolean value) throws IOException {
		assertValue();
		write(Boolean.toString(value));
		valueWritten();
	}

	@Override
	public void writeNullLiteral() throws IOException {
		assertValue();
		write("null");
		valueWritten();
	}
	
	private void writeNewline() throws IOException {
		write('\n');
		write(options.getIndent(indentLevel));
	}
}
