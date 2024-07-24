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

import blue.endless.jankson.api.io.StructuredData;
import blue.endless.jankson.api.io.StructuredDataReader;
import blue.endless.jankson.impl.io.context.ParserContext;

public abstract class AbstractStructuredDataReader implements StructuredDataReader {
	protected final LookaheadCodePointReader src;
	protected final StructuredDataBuffer readQueue = new StructuredDataBuffer();
	private final Deque<ParserContext> contextStack = new ArrayDeque<>();
	
	public AbstractStructuredDataReader(Reader src) {
		this.src = new LookaheadCodePointReader(src);
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
	
	protected void enqueueOutput(StructuredData.Type elem, Object obj) {
		readQueue.push(elem, obj);
	}
	
	protected void enqueueOutput(StructuredData value) {
		readQueue.push(value);
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
	
	protected abstract void readNext() throws IOException;
	
	@Override
	public boolean hasNext() {
		if (readQueue.isEmpty()) return true;
		
		return readQueue.peek().type() != StructuredData.Type.EOF;
	}
	
	@Override
	public StructuredData next() throws IOException {
		while(readQueue.isEmpty()) {
			readNext();
		}
		if (hasNext()) {
			return readQueue.pop();
		} else {
			return StructuredData.EOF;
		}
	}
}
