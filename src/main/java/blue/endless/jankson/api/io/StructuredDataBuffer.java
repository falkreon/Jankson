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

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;

/**
 * A FIFO queue / buffer that acts as both a reader and a writer, such that data written will be
 * later visible on reads.
 */
public class StructuredDataBuffer implements StructuredDataWriter, StructuredDataReader {
	private Deque<StructuredData> data = new ArrayDeque<>();
	
	/**
	 * Checks whether there is no data available
	 * @return true if there is no data available in this pipe, otherwise false.
	 */
	public boolean isEmpty() {
		return data.isEmpty();
	}
	
	public boolean isEof() {
		//EOF will ONLY occur when there is data in the pipe, and the data at the head of the pipe is EOF.
		StructuredData data = peek();
		return data != null && data.type() == StructuredData.Type.EOF;
	}
	
	/**
	 * Gets the number of StructuredData entries available
	 * @return the number of StructuredData entries available
	 */
	public int size() {
		return data.size();
	}
	
	/**
	 * Gets the first StructuredData entry that will be retrieved with next()
	 * @return the next StructuredData available, or null if nothing is available.
	 */
	public StructuredData peek() {
		if (data.isEmpty()) return null;
		return data.peekFirst();
	}
	
	public Optional<StructuredData> tryPeek() {
		return Optional.ofNullable(data.pollFirst());
	}
	
	public StructuredData pop() {
		return data.removeFirst();
	}
	
	//TODO: I'm conflicted on this; whether to return Optional.empty on EOF
	public Optional<StructuredData> tryPop() {
		if (data.isEmpty() || peek().type() == StructuredData.Type.EOF) return Optional.empty();
		return Optional.of(data.pop());
	}
	
	public void push(StructuredData.Type elem, Object value) {
		data.addLast(new StructuredData(elem, value));
	}
	
	public void push(StructuredData value) {
		data.addLast(value);
	}

	// implements StructuredDataWriter {
		@Override
		public void write(StructuredData value) {
			data.addLast(value);
		}
	// }
	
	// implements StructuredDataReader {
		@Override
		public StructuredData next() {
			if (data.isEmpty()) return StructuredData.EOF;
			return data.removeFirst();
		}

		@Override
		public boolean hasNext() {
			return !data.isEmpty();
		}
	// }
}
