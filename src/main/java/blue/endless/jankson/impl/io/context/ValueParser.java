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

package blue.endless.jankson.impl.io.context;

import java.io.IOException;

import blue.endless.jankson.api.SyntaxError;
import blue.endless.jankson.impl.io.Lookahead;
import blue.endless.jankson.impl.io.LookaheadCodePointReader;

/**
 * Parses a terminal value from a stream. Terminal values are things like `NaN`, `3.4159`, and `"butter"`. They are
 * almost always destined to be stored in a JsonPrimitive.
 */
public interface ValueParser {
	/**
	 * Returns true if this ValueParser can understand the value at this location in the stream. This is a passive
	 * determination, and SHOULD be possible within about three characters of lookahead.
	 * @param lookahead the stream.
	 * @return true if read should be called using this ValueParser, otherwise false.
	 */
	boolean canRead(Lookahead lookahead) throws IOException;
	
	/**
	 * Read the value at this location in the stream. Lookahead is still permitted.
	 * @param reader the stream.
	 * @return an Object representing the value that was parsed. This is the bare Object, not a PrimitiveElement wrapping it.
	 * @throws IOException if an I/O error occurs
	 * @throws SyntaxError if the value was not properly formed
	 */
	Object read(LookaheadCodePointReader reader) throws IOException, SyntaxError;
}
