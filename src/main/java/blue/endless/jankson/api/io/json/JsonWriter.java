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

package blue.endless.jankson.api.io.json;

import static blue.endless.jankson.api.io.json.JsonWriterOptions.Hint.*;

import java.io.IOException;
import java.io.Writer;

import blue.endless.jankson.api.document.CommentElement;
import blue.endless.jankson.api.document.CommentType;
import blue.endless.jankson.api.io.StructuredData;
import blue.endless.jankson.impl.io.AbstractStructuredDataWriter;

public class JsonWriter extends AbstractStructuredDataWriter {
	private final JsonWriterOptions options;
	private int indentLevel = 0;
	
	private String resource = "";
	private int line = 0;
	private int column = 0;
	private boolean skipNewline = false;
	
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
	public void write(StructuredData data) throws IOException {
		switch(data.type()) {
			case PRIMITIVE -> {
				if (data.value() == null) {
					writeNullLiteral();
				} else if (data.value() instanceof String val) {
					writeStringLiteral(val);
				} else if (data.value() instanceof Long val) {
					writeLongLiteral(val);
				} else if (data.value() instanceof Double val) {
					writeDoubleLiteral(val);
				} else if (data.value() instanceof Boolean val) {
					writeBooleanLiteral(val);
				} else {
					throw new IOException("Found illegal value in a PRIMITIVE StructuredData element");
				}
			}
			case ARRAY_START -> writeArrayStart();
			case ARRAY_END -> writeArrayEnd();
			case OBJECT_START -> writeObjectStart();
			case OBJECT_END -> writeObjectEnd();
			case OBJECT_KEY -> writeKey(data.value().toString());
			case COMMENT -> {
				if (data.value() == null) {
					writeComment("", CommentType.MULTILINE);
				} else if (data.value() instanceof CommentElement c) {
					writeComment(c.getValue(), c.getCommentType());
				} else {
					writeComment(data.value().toString(), CommentType.MULTILINE);
				}
			}
			case WHITESPACE -> {
				writeWhitespace(
						(data.value() == null) ? " " : data.value().toString()
						);
			}
			case NEWLINE -> writeNewline();
			case EOF -> { return; }
		}
	}
	
	private void writeComment(String value, CommentType type) throws IOException {
		switch(type) {
		case LINE_END:
			addCommas();
			write("//");
			write(value);
			skipNewline = false;
			writeNewline();
			break;
		
		case OCTOTHORPE:
			addCommas();
			if (hint(WRITE_NEWLINES)) writeNewline();
			write("#");
			write(value);
			skipNewline = false;
			writeNewline();
			break;
		
		case MULTILINE:
			addCommas();
			if (hint(WRITE_NEWLINES)) writeNewline();
			write("/*");
			write(value);
			write("*/ "); //TODO: Figure out if we should write this extra space here
			break;
		
		case DOC:
			addCommas();
			if (hint(WRITE_NEWLINES)) writeNewline();
			write("/**");
			write(value);
			write("*/ "); //TODO: Figure out if we should write this extra space here
			break;
		}
		
		State peek = peek();
		if (peek == State.ARRAY_BEFORE_COMMA || peek == State.DICTIONARY_BEFORE_COMMA) {
			pop();
		}
	}
	
	private void writeWhitespace(String value) throws IOException {
		if (hint(WRITE_WHITESPACE)) write(value);
	}
	
	private void writeKey(String key) throws IOException {
		addCommas();
		
		assertKey();
		
		//TODO: escape parts of the key if needed, omit quotes if possible + configured
		boolean quoted = !hint(UNQUOTED_KEYS); //TODO: Check to make sure it CAN be unquoted
		if (quoted) {
			write('"');
		}
		dest.write(key);
		if (quoted) {
			write('"');
		}
		
		if (hint(KEY_EQUALS_VALUE)) {
			write(" = ");
		} else {
			write(": ");
		}
		
		keyWritten();
	}
	
	private void addCommas() throws IOException {
		State peek = context.peek();
		
		if (peek == State.DICTIONARY || peek == State.ARRAY) {
			if (hint(WRITE_NEWLINES)) {
				writeNewline();
			} else {
				if (hint(WRITE_WHITESPACE)) write(' ');
			}
		} else if (peek == State.ARRAY_BEFORE_COMMA || peek == State.DICTIONARY_BEFORE_COMMA) {
			// fixCommas is called before a value is written; a comma is needed here
			if (!hint(OMIT_COMMAS)) {
				write(',');
			} else {
				write(' ');
			}
			
			if (hint(WRITE_NEWLINES)) {
				skipNewline = false;
				writeNewline();
			} else {
				write(' ');
			}
			
			context.pop();
		}
	}
	
	private void writeObjectStart() throws IOException {
		addCommas();
		
		assertValue();
		if (!isWritingRoot() || !hint(BARE_ROOT_OBJECT)) {
			write('{');
			indentLevel++;
		}
		
		objectStarted();
		skipNewline = false;
	}
	
	private void writeObjectEnd() throws IOException {
		assertObjectEnd();
		
		skipNewline = false;
		if (!isWritingRoot() || !hint(BARE_ROOT_OBJECT)) {
			indentLevel--;
		}
		if (peek() == State.DICTIONARY) { // No values yet
			if (hint(WRITE_WHITESPACE)) write(' ');
		} else {
			if (hint(WRITE_NEWLINES)) {
				writeNewline();
			} else {
				if (hint(WRITE_WHITESPACE)) write(' ');
			}
		}
		
		if (!isWritingRoot() || !hint(BARE_ROOT_OBJECT)) {
			write('}');
		}
		
		objectEndWritten();
	}
	
	private void writeArrayStart() throws IOException {
		addCommas();
		
		assertValue();
		
		write("[");
		indentLevel++;
		
		arrayStarted();
		skipNewline = false;
	}
	
	private void writeArrayEnd() throws IOException {
		assertArrayEnd();
		
		skipNewline = false;
		indentLevel--;
		if (hint(WRITE_NEWLINES)) {
			writeNewline();
		} else {
			if (hint(WRITE_WHITESPACE)) write(' ');
		}
		
		write(']');
		
		arrayEndWritten();
	}

	private void writeStringLiteral(String value) throws IOException {
		addCommas();
		
		assertValue();
		
		write('"');
		write(value);
		write('"');
		
		valueWritten();
	}
	
	private void writeLongLiteral(long value) throws IOException {
		addCommas();
		
		assertValue();
		write(Long.toString(value));
		valueWritten();
	}
	
	private void writeDoubleLiteral(double value) throws IOException {
		addCommas();
		
		assertValue();
		write(Double.toString(value));
		valueWritten();
	}
	
	private void writeBooleanLiteral(boolean value) throws IOException {
		addCommas();
		
		assertValue();
		write(Boolean.toString(value));
		valueWritten();
	}
	
	private void writeNullLiteral() throws IOException {
		addCommas();
		
		assertValue();
		write("null");
		valueWritten();
	}
	
	private void writeNewline() throws IOException {
		if (skipNewline) return;
		write('\n');
		write(options.getIndent(indentLevel));
		skipNewline = true;
	}
}
