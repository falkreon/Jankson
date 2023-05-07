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
import blue.endless.jankson.api.document.CommentElement;
import blue.endless.jankson.api.io.ElementType;
import blue.endless.jankson.api.io.JsonReaderOptions;
import blue.endless.jankson.impl.io.LookaheadCodePointReader;

public class ObjectParserContext implements ParserContext {
	private JsonReaderOptions options;
	private boolean foundStart = false;
	private boolean foundEnd = false;
	
	public ObjectParserContext(JsonReaderOptions options) {
		this.options = options;
	}
	
	@Override
	public void parse(LookaheadCodePointReader reader, BiConsumer<ElementType, Object> elementConsumer, Consumer<ParserContext> pusher) throws IOException, SyntaxError {
		emitComments(reader, elementConsumer);
		
		if (!foundStart) {
			int ch = reader.peek();
			if (ch == '{') {
				reader.read();
				foundStart = true;
				elementConsumer.accept(ElementType.OBJECT_START, null);
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
				elementConsumer.accept(ElementType.OBJECT_END, null);
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
						elementConsumer.accept(ElementType.OBJECT_KEY, s);
					} else {
						//TODO: Accept bare String tokens
						String token = TokenValueParser.readStatic(reader);
						elementConsumer.accept(ElementType.OBJECT_KEY, token);
					}
					
					//Look for the colon
					emitComments(reader, elementConsumer);
					ch = reader.peek();
					if (ch==':') {
						//Eat it and proceed to the value parsing
						reader.read();
						
						elementConsumer.accept(ElementType.OBJECT_KEY_VALUE_SEPARATOR, null);
						
						emitComments(reader, elementConsumer);
						
						handleValue(reader, elementConsumer, pusher);
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

	private void emitComments(LookaheadCodePointReader reader, BiConsumer<ElementType, Object> elementConsumer) throws IOException, SyntaxError {
		skipNonBreakingWhitespace(reader);
		while (CommentValueParser.canReadStatic(reader) || reader.peek()=='\n') {
			if (reader.peek()=='\n') {
				reader.read();
				elementConsumer.accept(ElementType.NEWLINE, null);
			} else {
				CommentElement comment = CommentValueParser.readStatic(reader);
				elementConsumer.accept(ElementType.COMMENT, comment);
			}
			skipNonBreakingWhitespace(reader);
		}
	}

	@Override
	public boolean isComplete(LookaheadCodePointReader reader) {
		return foundStart && foundEnd;
	}
	
	
	public void handleValue(LookaheadCodePointReader reader, BiConsumer<ElementType, Object> elementConsumer, Consumer<ParserContext> pusher) throws IOException, SyntaxError {
		int ch = reader.peek();
		if (ch=='{') {
			pusher.accept(new ObjectParserContext(options));
		} else if (ch=='[') {
			pusher.accept(new ArrayParserContext(options));
		} else if (NumberValueParser.canReadStatic(reader)) {
			Number value = NumberValueParser.readStatic(reader);
			elementConsumer.accept(ElementType.PRIMITIVE, value);
		} else if (BooleanValueParser.canReadStatic(reader)) {
			Boolean value = BooleanValueParser.readStatic(reader);
			elementConsumer.accept(ElementType.PRIMITIVE, value);
		} else if (StringValueParser.canReadStatic(reader)) {
			String value = StringValueParser.readStatic(reader);
			elementConsumer.accept(ElementType.PRIMITIVE, value);
		} else {
			String maybeNull = reader.peekString(4);
			if (maybeNull.equals("null")) {
				reader.readString(4);
				elementConsumer.accept(ElementType.PRIMITIVE, null);
			} else {
				String token = TokenValueParser.readStatic(reader);
				
				elementConsumer.accept(ElementType.PRIMITIVE, token);
				//TODO: Unquoted Strings etc.
				//throw new SyntaxError("Expected a value here, but couldn't decode it.", reader.getLine(), reader.getCharacter());
			}
		}
	}
}
