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

package blue.endless.jankson.impl.io.context;

import java.io.IOException;
import java.util.function.BiConsumer;

import javax.annotation.Nullable;

import blue.endless.jankson.api.SyntaxError;
import blue.endless.jankson.api.document.PrimitiveElement;
import blue.endless.jankson.api.io.ElementType;
import blue.endless.jankson.impl.io.LookaheadCodePointReader;

public interface ParserContext {
	
	/**
	 * Parse a small part of the stream, enqueueing elements and their associated values into the elementConsumer.
	 */
	public void parse(LookaheadCodePointReader reader, BiConsumer<ElementType, Object> elementConsumer) throws IOException, SyntaxError;
	
	/**
	 * If there is a String value available at this point in the stream, return it. If not, return null.
	 */
	public @Nullable String getStringValue();
	
	/**
	 * If this Context parses primitive values, and one is available at this point in the stream, return it. If not,
	 * return null.
	 */
	public @Nullable PrimitiveElement getValue();
	
	/**
	 * Returns true if an EOF at this location in the stream would still result in well-formed data. Returns false if
	 * EOF at this location should throw an error.
	 */
	public boolean canEOFHere();
	
	/**
	 * Returns true if the parser has assembled a complete result. This method may trigger lookahead but MUST NOT read.
	 * After this method returns true, the Reader state will change, and {@link #parse(LookaheadCodePointReader)} will
	 * no longer be called.
	 */
	public boolean isComplete(LookaheadCodePointReader reader);
	
	public default void skipWhitespace(LookaheadCodePointReader reader) throws IOException {
		while (true) {
			int ch = reader.peek();
			if (ch==-1 || ch=='\n' || !Character.isWhitespace(ch)) return;
			reader.read(); //It's nonbreaking whitespace. Discard it.
		}
	}
}