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

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayDeque;
import java.util.Deque;

import blue.endless.jankson.api.SyntaxError;
import blue.endless.jankson.api.io.ElementType;
import blue.endless.jankson.api.io.StructuredDataReader;
import blue.endless.jankson.impl.io.context.ParserContext;

public abstract class AbstractStructuredDataReader implements StructuredDataReader {
	protected final LookaheadCodePointReader src;
	private final Deque<PendingOutput> readQueue = new ArrayDeque<>();
	private final Deque<ParserContext> contextStack = new ArrayDeque<>();
	private Object latestValue = null;
	
	public AbstractStructuredDataReader(Reader src) {
		this.src = new LookaheadCodePointReader(src);
	}

	@Override
	public Object getLatestValue() {
		return latestValue;
	}
	
	protected void setLatestValue(Object o) {
		this.latestValue = o;
	}
	
	protected ParserContext getContext() {
		return contextStack.peek();
	}
	
	protected void pushContext(ParserContext context) {
		contextStack.push(context);
	}
	
	protected void popContext() {
		contextStack.pop();
	}
	
	protected void enqueueOutput(ElementType elem, Object obj) {
		readQueue.addFirst(new PendingOutput(elem, obj));
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
	
	protected abstract void readNext() throws IOException, SyntaxError;
	
	@Override
	public boolean hasNext() {
		if (readQueue.isEmpty()) return true;
		
		return readQueue.peekLast().elementType() != ElementType.EOF;
	}
	
	@Override
	public ElementType next() throws IOException, SyntaxError {
		while(readQueue.isEmpty()) readNext();
		
		PendingOutput pending = readQueue.peekLast();
		if (pending.elementType==ElementType.EOF) return ElementType.EOF;
		
		pending = readQueue.removeLast();
		//Should we update pending value?
		if (pending.elementType==ElementType.COMMENT || pending.elementType==ElementType.OBJECT_KEY || pending.elementType==ElementType.PRIMITIVE) {
			this.latestValue = pending.value();
		}
		return pending.elementType();
	}
	
	private static record PendingOutput(ElementType elementType, Object value) {}
}
