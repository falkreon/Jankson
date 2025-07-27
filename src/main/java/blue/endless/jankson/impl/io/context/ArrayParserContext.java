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

package blue.endless.jankson.impl.io.context;

import java.io.IOException;
import java.util.function.Consumer;

import blue.endless.jankson.api.SyntaxError;
import blue.endless.jankson.api.io.StructuredData;
import blue.endless.jankson.api.io.json.JsonReaderOptions;
import blue.endless.jankson.impl.io.LookaheadCodePointReader;

public class ArrayParserContext implements ParserContext {
	private JsonReaderOptions options;
	private boolean foundStart = false;
	private boolean foundEnd = false;
	
	public ArrayParserContext(JsonReaderOptions options) {
		this.options = options;
	}
	
	@Override
	public void parse(LookaheadCodePointReader reader, Consumer<StructuredData> elementConsumer, Consumer<ParserContext> pusher) throws IOException, SyntaxError {
		emitComments(reader, elementConsumer);
		
		if (!foundStart) {
			int ch = reader.peek();
			if (ch=='[') {
				reader.read();
				foundStart = true;
				elementConsumer.accept(StructuredData.ARRAY_START);
			} else {
				throw new SyntaxError("Unexpected input found while looking for an array.", reader.getLine(), reader.getCharacter());
			}
		} else {
			if (!foundEnd) {
				int ch = reader.peek();
				if (ch==',') {
					reader.read();
					return;
				}
				if (ch==']') {
					reader.read();
					foundEnd = true;
					elementConsumer.accept(StructuredData.ARRAY_END);
				} else {
					handleValue(reader, elementConsumer, pusher, options);
				}
			} else {
				//Do nothing. We shouldn't have been called.
			}
			
		}
	}

	@Override
	public boolean isComplete(LookaheadCodePointReader reader) {
		return foundStart && foundEnd;
	}

}
