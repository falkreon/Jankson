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
import java.util.function.Consumer;

import blue.endless.jankson.api.SyntaxError;
import blue.endless.jankson.api.io.ElementType;
import blue.endless.jankson.impl.io.LookaheadCodePointReader;

public interface ParserContext {
	
	/**
	 * Parse a small part of the stream, enqueueing elements and their associated values into the elementConsumer.
	 * @param reader the stream
	 * @param elementConsumer elements submitted to this consumer will be seen by the reader in the order they are submitted in.
	 * @param pusher submitting a ParserContext to this lambda will cause the parser to call that context until it is complete, and then return to this one.
	 */
	public void parse(LookaheadCodePointReader reader, BiConsumer<ElementType, Object> elementConsumer, Consumer<ParserContext> pusher) throws IOException, SyntaxError;
	
	/**
	 * Returns true if the parser has assembled a complete result. This method may trigger lookahead but MUST NOT read.
	 * After this method returns true, the Reader state will change, and {@link #parse(LookaheadCodePointReader, BiConsumer, Consumer)} will
	 * no longer be called.
	 */
	public boolean isComplete(LookaheadCodePointReader reader);
	
	public default void skipNonBreakingWhitespace(LookaheadCodePointReader reader) throws IOException {
		while (true) {
			int ch = reader.peek();
			if (ch==-1 || ch=='\n' || !Character.isWhitespace(ch)) return;
			reader.read(); //It's nonbreaking whitespace. Discard it.
		}
	}
}