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

package blue.endless.jankson.impl.io;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayDeque;
import java.util.Deque;

import blue.endless.jankson.api.SyntaxError;
import blue.endless.jankson.api.io.ElementType;
import blue.endless.jankson.api.io.StructuredDataReader;

public abstract class AbstractStructuredDataReader implements StructuredDataReader {
	protected final LookaheadCodePointReader src;
	private final Deque<ElementType> readQueue = new ArrayDeque<>();
	private final Deque<ReaderState> stateStack = new ArrayDeque<>();
	private Object latestValue = null;
	//private Deque<ParserContext<?>> context = new ArrayDeque<>();
	
	public AbstractStructuredDataReader(Reader src) {
		this.src = new LookaheadCodePointReader(src);
		pushState(ReaderState.ROOT);
	}

	@Override
	public Object getLatestValue() {
		return latestValue;
	}
	
	protected void setLatestValue(Object o) {
		this.latestValue = o;
	}
	
	protected void pushState(ReaderState elem) {
		stateStack.push(elem);
	}
	
	protected ReaderState peekState() {
		return stateStack.peek();
	}
	
	protected ReaderState popState() {
		return stateStack.pop();
	}
	
	protected void enqueueOutput(ElementType elem) {
		readQueue.addFirst(elem);
	}
	
	protected void skipNonBreakingWhitespace() throws IOException {
		while(true) {
			int ch = src.peek();
			if (ch==-1) return;
			if (ch=='\n') return;
			if (!Character.isWhitespace(ch)) return;
			src.read();
		}
	}
	
	protected abstract void nextCharacter() throws IOException, SyntaxError;
	
	@Override
	public ElementType next() throws IOException, SyntaxError {
		while(readQueue.isEmpty()) nextCharacter();
		
		if (readQueue.peekLast()==ElementType.EOF) return ElementType.EOF;
		
		return readQueue.removeLast();
	}
	
	public static enum ReaderState {
		ROOT,
		OBJECT,
		ARRAY;
	}
}
