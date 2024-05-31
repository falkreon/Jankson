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
import java.util.function.Consumer;

import blue.endless.jankson.api.SyntaxError;
import blue.endless.jankson.api.io.JsonReaderOptions;
import blue.endless.jankson.api.io.StructuredData;
import blue.endless.jankson.impl.io.LookaheadCodePointReader;

public class RootParserContext implements ParserContext {
	
	private final JsonReaderOptions options;
	private boolean complete = false;
	
	private String bufferedKey = null;
	
	public RootParserContext(JsonReaderOptions options) {
		this.options = options;
	}
	
	@Override
	public void parse(LookaheadCodePointReader reader, Consumer<StructuredData> elementConsumer, Consumer<ParserContext> pusher) throws IOException, SyntaxError {
		emitComments(reader, elementConsumer);
		
		int ch = reader.peek();
		switch (ch) {
			case -1 -> {
				complete = true;
				if (bufferedKey != null) {
					// We buffered a String we found, but it seems to be the whole object value.
					elementConsumer.accept(StructuredData.primitive(bufferedKey));
					bufferedKey = null;
				}
				elementConsumer.accept(StructuredData.EOF);
			}
			case ':' -> {
				if (bufferedKey != null && options.hasHint(JsonReaderOptions.Hint.ALLOW_BARE_ROOT_OBJECT)) {
					elementConsumer.accept(StructuredData.objectKey(bufferedKey));
					bufferedKey = null;
					// Next thing will be a Value, but it'll be caught by the next parse call.
				}
			}
			case '{' -> pusher.accept(new ObjectParserContext(options));
			case '[' -> pusher.accept(new ArrayParserContext(options));
			default -> {
				if (NumberValueParser.canReadStatic(reader)) {
					Number value = NumberValueParser.readStatic(reader);
					elementConsumer.accept(StructuredData.primitive(value));
				} else if (BooleanValueParser.canReadStatic(reader)) {
					Boolean value = BooleanValueParser.readStatic(reader);
					elementConsumer.accept(StructuredData.primitive(value));
				} else if (checkForNullLiteral(reader)) {
					reader.readString(4); //Consume the null literal
					elementConsumer.accept(StructuredData.NULL);
				} else if (StringValueParser.canReadStatic(reader)) {
					String s = StringValueParser.readStatic(reader);
					if (options.hasHint(JsonReaderOptions.Hint.ALLOW_BARE_ROOT_OBJECT)) {
						// This could be either a key of a bare root object, or it could be a primitive String root object.
						// Buffer it for now - if we find a colon later, it's a key.
						bufferedKey = s;
					} else {
						elementConsumer.accept(StructuredData.primitive(s));
					}
				}
			}
		}
	}
	
	private boolean checkForNullLiteral(LookaheadCodePointReader reader) throws IOException {
		String maybeNull = reader.peekString(4);
		int extra = reader.peek(5);
		if (Character.isLetterOrDigit(extra)) return false; //some token *starts with* "null" but is not null.
		return maybeNull.equals("null");
	}
	
	@Override
	public boolean isComplete(LookaheadCodePointReader reader) {
		return complete;
	}
	
}
