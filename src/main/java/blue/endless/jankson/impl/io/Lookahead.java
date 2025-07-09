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

package blue.endless.jankson.impl.io;

import java.io.IOException;


public interface Lookahead {
	/**
	 * Peeks a String of length codepoints ahead of the read pointer. This will result in a string of at least length
	 * characters, possibly more.
	 * @param length the number of codepoints to peek at
	 * @return       the String representation of those codepoints
	 * @throws IOException if an I/O error occurs
	 */
	public String peekString(int length) throws IOException;
	
	/**
	 * Gets the next codepoint in the stream, "before" it has been read. Implementations MUST have at least one
	 * codepoint of lookahead available.
	 * @return The next codepoint in the stream, or -1 if we have reached the end of the stream.
	 * @throws IOException if an I/O error occurs
	 */
	public int peek() throws IOException;
	
	/**
	 * Returns a codepoint from the stream, but one that is ahead of the read pointer. This codepoint will later be
	 * visible from a read method, but will not be consumed by this method.
	 * @param distanceAhead The number of characters ahead of the read pointer to peek. 1 is the next character in the
	 *                      stream.
	 * @return              The code point at this location, or -1 if the end of the file has been reached.
	 * @throws IOException  if an I/O error occurs
	 * @throws IllegalArgumentException if the lookahead is less than 1 or beyond this stream's lookahead buffer size
	 */
	public int peek(int distanceAhead) throws IOException;
}
