package blue.endless.jankson.api.io;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;

import blue.endless.jankson.api.document.CommentType;

public class JsonWriter implements StructuredDataWriter {
	private Writer dest;
	private Deque<State> context = new ArrayDeque<>();
	private boolean rootWritten = false;
	private final JsonWriterOptions options;
	private int indentLevel = 0;
	
	public JsonWriter(Writer destination) {
		this(destination, JsonWriterOptions.DEFAULTS);
	}
	
	public JsonWriter(Writer destination, JsonWriterOptions options) {
		this.dest = destination;
		context.push(State.ROOT);
		this.options = options;
	}

	@Override
	public void writeComment(String value, CommentType type) throws IOException {
		//For now, ignore commentType
		dest.write("/* ");
		dest.write(value);
		dest.write(" */");
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
		context.push(State.DICTIONARY_BEFORE_DELIMITER);
	}

	@Override
	public void writeKeyValueDelimiter() throws IOException {
		assertKeyValueDelimiter();
		
		if (options.get(JsonWriterOptions.Hint.KEY_EQUALS_VALUE)) {
			dest.write(" = ");
		} else {
			dest.write(": ");
		}
		context.pop();
		context.push(State.DICTIONARY_BEFORE_VALUE);
	}

	@Override
	public void nextValue() throws IOException {
		assertNextValue();
		if (!options.get(JsonWriterOptions.Hint.OMIT_COMMAS)) {
			dest.write(", ");
		} else {
			dest.write(" ");
		}
		context.pop();
	}

	@Override
	public void writeObjectStart() throws IOException {
		assertValue();
		if (context.peek()==State.ROOT && options.get(JsonWriterOptions.Hint.BARE_ROOT_OBJECT)) {
			//Do not write the brace, and do not increase the indent level.
		} else {
			dest.write("{ ");
		}
		context.push(State.DICTIONARY);
	}

	@Override
	public void writeObjectEnd() throws IOException {
		assertObjectEnd();
		
		if (isWritingRoot() && options.get(JsonWriterOptions.Hint.BARE_ROOT_OBJECT)) {
			//Do not write closing brace, and do not decrease the indent level.
		} else {
			dest.write(" }");
		}
		
		while(context.pop() != State.DICTIONARY); //remove any state back to DICTIONARY
		
		valueWritten();
	}

	@Override
	public void writeArrayStart() throws IOException {
		assertValue();
		
		dest.write("[ ");
		context.push(State.ARRAY);
	}

	@Override
	public void writeArrayEnd() throws IOException {
		assertArrayEnd();
		
		dest.write(" ]");
		context.pop();
		
		valueWritten();
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
	
	/**
	 * Checks to see if it's okay to write an object key.
	 */
	protected void assertKey() {
		State peek = context.peek();
		if (peek!=State.DICTIONARY) throw new IllegalStateException("Attempting to write a key at an invalid location. (State is "+peek+")");
	}
	
	/**
	 * Checks to see if it's okay to write the colon between a key and a value.
	 */
	protected void assertKeyValueDelimiter() {
		State peek = context.peek();
		if (peek!=State.DICTIONARY_BEFORE_DELIMITER) throw new IllegalStateException("Attempting to write a key-value delimiter at an invalid location. (State is "+peek+")");
	}
	
	/**
	 * Checks to see if it's okay to write a value.
	 */
	protected void assertValue() {
		State peek = context.peek();
		if (peek == State.ROOT && rootWritten) throw new IllegalStateException("Cannot write multiple values to the document root.");
		
		if (peek == State.ROOT || peek == State.ARRAY || peek == State.DICTIONARY_BEFORE_VALUE) return;
		throw new IllegalStateException("Attempting to write a value at an invalid location. (State is "+peek+")");
	}
	
	protected void assertNextValue() {
		State peek = context.peek();
		if (peek == State.DICTIONARY_BEFORE_COMMA || peek == State.ARRAY_BEFORE_COMMA) return;
		throw new IllegalStateException("Attempting to write a comma between values at an invalid location. (State is "+peek+")");
	}
	
	protected void assertObjectEnd() {
		State peek = context.peek();
		if (peek == State.DICTIONARY || peek == State.DICTIONARY_BEFORE_COMMA) return;
		throw new IllegalStateException("Attempting to end an object-end in an invalid location. (State is "+peek+")");
	}
	
	protected void assertArrayEnd() {
		State peek = context.peek();
		if (peek == State.ARRAY) return;
		throw new IllegalStateException("Attempting to end an array-end in an invalid location. (State is "+peek+")");
	}
	
	/**
	 * Perform any state transition that needs to happen when a value has been written.
	 */
	protected void valueWritten() {
		State peek = context.peek();
		if (peek == State.ROOT) {
			rootWritten = true;
		} else if (peek == State.ARRAY) {
			context.push(State.ARRAY_BEFORE_COMMA);
		} else if (peek == State.DICTIONARY_BEFORE_VALUE) {
			context.pop();
			context.push(State.DICTIONARY_BEFORE_COMMA);
		} else {
			throw new IllegalStateException("A value was just written but the writer state has become invalid. (State stack: "+context.toString()+")");
		}
	}
	
	private boolean isWritingRoot() {
		Iterator<State> iter = context.iterator();
		State a = (iter.hasNext()) ? iter.next() : State.ROOT;
		State b = (iter.hasNext()) ? iter.next() : State.ROOT;
		State c = (iter.hasNext()) ? iter.next() : State.ROOT;
		
		if (a==State.ROOT) return true;
		
		if (a==State.DICTIONARY && b==State.ROOT) return true;
		if (a==State.DICTIONARY_BEFORE_COMMA     && b==State.DICTIONARY && c==State.ROOT) return true;
		if (a==State.DICTIONARY_BEFORE_DELIMITER && b==State.DICTIONARY && c==State.ROOT) return true;
		if (a==State.DICTIONARY_BEFORE_VALUE     && b==State.DICTIONARY && c==State.ROOT) return true;
		
		if (a==State.ARRAY && b==State.ROOT) return true;
		if (a==State.ARRAY_BEFORE_COMMA && b==State.ROOT) return true;
		
		return false;
	}
	
	private static enum State {
		/**
		 * Only "root-approved" values can be written here. Depending on settings, this may disallow things like String
		 * literals. If an object is started here, settings may direct its braces to be omitted.
		 */
		ROOT,
		
		/**
		 * This is an array, and if this is the top element of the context stack, nothing has been written yet. Any
		 * valid value can be written with a writeXLiteral method, objectStart, or arrayStart.
		 */
		ARRAY,
		
		/**
		 * The only things valid to write here are nextValue or arrayEnd.
		 */
		ARRAY_BEFORE_COMMA,
		
		/**
		 * This is a json object or other dictionary/map. The valid actions here are objectEnd and writeKey.
		 */
		DICTIONARY,
		
		/**
		 * We're writing an object, and have written a key. We are waiting for the delimiter to be written and that is
		 * the only valid action.
		 */
		DICTIONARY_BEFORE_DELIMITER,
		
		/**
		 * We're writing an object and have written a key and delimiter. Any valid value is accepted using
		 * writeXLiteral, objectStart, or arrayStart.
		 */
		DICTIONARY_BEFORE_VALUE,
		
		/**
		 * We're writing an object and have written the key, delimiter, and value. Valid actions here are nextValue or
		 * objectEnd.
		 */
		DICTIONARY_BEFORE_COMMA;
	}
}
