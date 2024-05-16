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

package blue.endless.jankson.impl.io;

import java.io.Writer;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;

import blue.endless.jankson.api.io.StructuredDataWriter;

/**
 * Helper class that handles state consistency for StructuredDataWriters.
 */
public abstract class AbstractStructuredDataWriter implements StructuredDataWriter {
	protected final Writer dest;
	protected Deque<State> context = new ArrayDeque<>();
	protected boolean rootWritten = false;
	
	public AbstractStructuredDataWriter(Writer writer) {
		this.dest = writer;
		context.push(State.ROOT);
	}
	
	/**
	 * Throws an exception if we're not ready to write a key
	 */
	protected void assertKey() {
		State peek = context.peek();
		if (peek!=State.DICTIONARY) throw new IllegalStateException("Attempting to write a key at an invalid location. (State is "+peek+")");
	}
	
	/**
	 * Throws an exception if we're not between the key and value of a keyvalue-pair
	 */
	protected void assertKeyValueDelimiter() {
		State peek = context.peek();
		if (peek!=State.DICTIONARY_BEFORE_DELIMITER) throw new IllegalStateException("Attempting to write a key-value delimiter at an invalid location. (State is "+peek+")");
	}
	
	/**
	 * Throws an exception if we're not ready to begin a value
	 */
	protected void assertValue() {
		State peek = context.peek();
		if (peek == State.ROOT && rootWritten) throw new IllegalStateException("Cannot write multiple values to the document root.");
		
		if (peek == State.ROOT || peek == State.ARRAY || peek == State.DICTIONARY_BEFORE_VALUE) return;
		throw new IllegalStateException("Attempting to write a value at an invalid location. (State is "+peek+")");
	}
	
	/**
	 * Throws an exception if we're not ready to delimit values (i.e. write the comma between keyvalue-pairs or array elements)
	 */
	protected void assertNextValue() {
		State peek = context.peek();
		if (peek == State.DICTIONARY_BEFORE_COMMA || peek == State.ARRAY_BEFORE_COMMA) return;
		throw new IllegalStateException("Attempting to write a comma between values at an invalid location. (State is "+peek+")");
	}
	
	/**
	 * Throws an exception if we're not ready to write the closing brace for an object/dictionary.
	 */
	protected void assertObjectEnd() {
		State peek = context.peek();
		if (peek == State.DICTIONARY || peek == State.DICTIONARY_BEFORE_COMMA) return;
		throw new IllegalStateException("Attempting to end an object-end in an invalid location. (State is "+peek+")");
	}
	
	/**
	 * Throws an exception if we're not ready to write the closing bracket for an array.
	 */
	protected void assertArrayEnd() {
		State peek = context.peek();
		if (peek == State.ARRAY || peek == State.ARRAY_BEFORE_COMMA) return;
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
	
	protected void objectStarted() {
		push(State.DICTIONARY);
	}
	
	protected void arrayStarted() {
		push(State.ARRAY);
	}
	
	protected void keyWritten() {
		push(State.DICTIONARY_BEFORE_DELIMITER);
	}
	
	protected void keyValueDelimiterWritten() {
		pop();
		push(State.DICTIONARY_BEFORE_VALUE);
	}
	
	protected void nextValueWritten() {
		pop();
	}
	
	/**
	 * Perform any state transition that needs to happen when we finish writing an object.
	 */
	protected void objectEndWritten() {
		while(pop() != State.DICTIONARY); //remove any state back to DICTIONARY
		
		valueWritten();
	}
	
	/**
	 * Perform any state transition that needs to happen when we finish writing an array.
	 */
	protected void arrayEndWritten() {
		while(pop() != State.ARRAY); //remove any state back to ARRAY
		
		valueWritten();
	}
	
	protected boolean isWritingRoot() {
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
	
	protected void push(State state) {
		context.push(state);
	}
	
	protected State pop() {
		return context.pop();
	}
	
	protected State peek() {
		return context.peek();
	}
	
	protected static enum State {
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
