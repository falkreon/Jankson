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
import blue.endless.jankson.api.io.JsonReaderOptions;
import blue.endless.jankson.api.io.StructuredData;
import blue.endless.jankson.impl.io.LookaheadCodePointReader;

public class ObjectParserContext implements ParserContext {
	private JsonReaderOptions options;
	private boolean foundStart = false;
	private boolean foundEnd = false;
	
	public ObjectParserContext(JsonReaderOptions options) {
		this.options = options;
	}
	
	@Override
	public void parse(LookaheadCodePointReader reader, Consumer<StructuredData> elementConsumer, Consumer<ParserContext> pusher) throws IOException, SyntaxError {
		emitComments(reader, elementConsumer);
		
		if (!foundStart) {
			int ch = reader.peek();
			if (ch == '{') {
				reader.read();
				foundStart = true;
				elementConsumer.accept(StructuredData.OBJECT_START);
			} else if (ch == '}') {
				throw new SyntaxError("End of object found before start.", reader.getLine(), reader.getCharacter());
			} else {
				throw new SyntaxError("Unexpected input found while looking for an object.", reader.getLine(), reader.getCharacter());
			}
		} else if (!foundEnd) {
			int ch = reader.peek();
			if (ch==-1) {
				throw new IOException("EOF found before object end.");
			}
			if (ch == ',') {
				reader.read(); //We ignore commas
				return;
			}
			if (ch == '}') {
				reader.read();
				foundEnd = true;
				elementConsumer.accept(StructuredData.OBJECT_END);
			} else {
				//This is either a comment or a key.
				//if (CommentValueParser.canReadStatic(reader)) {
					// TODO: This seems to be dead code - emitComments at the top tends to capture any comments we could find here!
				//	CommentValueParser.readStatic(reader);
				//} else {
					//Read a key
					if (StringValueParser.canReadStatic(reader)) {
						//Read a quoted key
						String s = StringValueParser.readStatic(reader);
						elementConsumer.accept(StructuredData.objectKey(s));
					} else {
						//TODO: Accept bare String tokens
						String token = TokenValueParser.readStatic(reader);
						elementConsumer.accept(StructuredData.objectKey(token));
					}
					
					//Look for the colon
					emitComments(reader, elementConsumer);
					ch = reader.peek();
					if (ch==':') {
						//Eat it and proceed to the value parsing
						reader.read();
						
						//elementConsumer.accept(ElementType.OBJECT_KEY_VALUE_SEPARATOR, null);
						
						emitComments(reader, elementConsumer);
						
						handleValue(reader, elementConsumer, pusher, options);
						//TODO: Maybe process the comma.
						//foreach reader
						//if we can read it, do and break.
					} else {
						throw new SyntaxError("Couldn't find key-value separator (:)", reader.getLine(), reader.getCharacter());
					}
					
				//}
			}
		} else {
			//Do nothing - we should stop being called
		}
	}

	@Override
	public boolean isComplete(LookaheadCodePointReader reader) {
		return foundStart && foundEnd;
	}
	
}
