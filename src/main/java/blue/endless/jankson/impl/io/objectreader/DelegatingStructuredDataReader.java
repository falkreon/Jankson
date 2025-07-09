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

package blue.endless.jankson.impl.io.objectreader;

import java.io.IOException;

import blue.endless.jankson.api.SyntaxError;
import blue.endless.jankson.api.io.StructuredData;
import blue.endless.jankson.api.io.StructuredDataBuffer;
import blue.endless.jankson.api.io.StructuredDataReader;

public abstract class DelegatingStructuredDataReader implements StructuredDataReader {
	private StructuredDataReader delegate = null;
	private final StructuredDataBuffer buffer = new StructuredDataBuffer();
	private StructuredData latestEntry = null;
	
	@Override
	public boolean hasNext() {
		
		boolean emptyBuffer = buffer.isEmpty();
		boolean emptyDelegate = delegate == null || !delegate.hasNext();
		
		// Technically this is an illegal scenario, but we SHOULD have thrown by now.
		if (emptyBuffer && emptyDelegate) return false;
		
		// If the pipe is non-empty and the next entry is not EOF, we're ready for reading.
		return !emptyBuffer && buffer.peek().type() != StructuredData.Type.EOF;
	}
	
	@Override
	public StructuredData next() throws SyntaxError, IOException {
		if (buffer.isEmpty()) {
			//Usually this will happen on the first run. Prime our first element
			loadNextElem();
		}
		boolean eof = buffer.isEof();
		// loadNextElem guarantees !pipe.isEmpty()
		latestEntry = buffer.pop();
		
		// If we're not at EOF, load the next item so we always have an answer for hasNext
		if (!eof) loadNextElem();
		
		return latestEntry;
	}
	
	/**
	 * Buffers an element so that it will be presented next, after any previously buffered data
	 * @param value the value to buffer
	 */
	protected void buffer(StructuredData value) {
		buffer.push(value);
	}
	
	protected void setDelegate(StructuredDataReader reader) {
		delegate = reader;
	}
	
	/**
	 * If pipe is empty, pulls in the next piece of data from the current delegate. If there
	 * is no delegate or the current delegate has completed, calls onDelegateEmpty, giving
	 * the concrete subclass a chance to prime the prebuffer data or set a new delegate.
	 * 
	 * <p>One way or another, guarantees !pipe.isEmpty() at return
	 * @throws IOException if a delegate encountered a problem reading data.
	 */
	private void loadNextElem() throws SyntaxError, IOException {
		// If we're already fulfilled, NOP out.
		if (!buffer.isEmpty()) return;
		
		if (delegate == null || !delegate.hasNext()) {
			// We have absolutely no data available. Clear out the delegate if present and call onDelegateEmpty to prime some data
			
			delegate = null;
			onDelegateEmpty();
			
			if (!buffer.isEmpty()) return; // Neat, data was primed directly!
			// No data was directly primed, so we NEED a delegate to continue operating properly.
			if (delegate == null || !delegate.hasNext()) throw new IllegalStateException("No new data was made available from onDelegateEmpty()!");
			
			// We're all set to fill the pipe ourselves
			buffer.push(delegate.next());
			//Not *needed* but helps things short circuit faster
			if (!delegate.hasNext()) {
				delegate = null;
			}
		} else {
			// There's already a delegate, fill the pipe
			buffer.push(delegate.next());
			//Not *needed* but helps things short circuit faster
			if (!delegate.hasNext()) {
				delegate = null;
			}
		}
	}
	
	/**
	 * The reader has just supplied the last element of its current delegate, and is buffering
	 * the next element to supply (or EOF in the case that this reader is dry).
	 * 
	 * <p>If data is pushed with prebuffer(), it will be returned first, before any newly-set
	 * delegate. If no data is prebuffered, the first element of the newly-set delegate is used
	 * next. If this method neither buffers data nor sets a delegate, an IllegalStateException
	 * is thrown.
	 * 
	 * <p>When in doubt, if you have nothing left to buffer, buffer EOF.
	 */
	protected abstract void onDelegateEmpty() throws IOException;
}
