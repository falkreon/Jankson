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
import blue.endless.jankson.api.io.json.JsonFormat;
import blue.endless.jankson.impl.io.LookaheadCodePointReader;

public class ObjectParserContext implements ParserContext {
	private JsonReaderOptions options;
	private boolean foundStart = false;
	private boolean foundEnd = false;
	private final boolean braced;
	private final int depth;
	private boolean lineTerminatorAfterValue;
	private enum State { START, KEY_OR_END, KEY_AFTER_COMMA, COLON, VALUE, COMMA_OR_END, COMPLETE }
	private State state = State.START;
	
	public ObjectParserContext(JsonReaderOptions options) {
		this(options, true, 1);
	}
	ObjectParserContext(JsonReaderOptions options, boolean braced, int depth) {
		this.options = options;
		this.braced = braced;
		this.depth = depth;
	}
	
	@Override
	public void parse(LookaheadCodePointReader reader, Consumer<StructuredData> elementConsumer, Consumer<ParserContext> pusher) throws IOException, SyntaxError {
		if (options.getFormat() != null) { parseFormatted(reader, elementConsumer, pusher); return; }
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
					elementConsumer.accept(StructuredData.objectKey(KeyRules.read(reader, options)));
					
					//Look for the colon
					emitComments(reader, elementConsumer);
					ch = reader.peek();
					if (ch==options.getKeyValueSeparator()) {
						//Eat it and proceed to the value parsing
						reader.read();
						
						//elementConsumer.accept(ElementType.OBJECT_KEY_VALUE_SEPARATOR, null);
						
						emitComments(reader, elementConsumer);
						
						handleValue(reader, elementConsumer, pusher, options);
						//TODO: Maybe process the comma.
						//foreach reader
						//if we can read it, do and break.
					} else {
						throw new SyntaxError("Couldn't find key-value separator ("+options.getKeyValueSeparator()+")", reader.getLine(), reader.getCharacter());
					}
					
				//}
			}
		} else {
			//Do nothing - we should stop being called
		}
	}

	@Override
	public boolean isComplete(LookaheadCodePointReader reader) {
		return options.getFormat() == null ? foundStart && foundEnd : state == State.COMPLETE;
	}
	private void parseFormatted(LookaheadCodePointReader r, Consumer<StructuredData> out, Consumer<ParserContext> push) throws IOException, SyntaxError {
		JsonFormat format = options.getFormat();
		if (JsonGrammar.trivia(r, format, out, () -> lineTerminatorAfterValue = true)) return;
		int ch = r.peek();
		boolean end = braced ? ch == '}' : ch == -1;
		switch (state) {
			case START -> {
				if (braced && r.read() != '{') throw JsonGrammar.error(r, "Expected '{'.");
				out.accept(StructuredData.OBJECT_START); state = State.KEY_OR_END; lineTerminatorAfterValue = false;
			}
			case KEY_OR_END, KEY_AFTER_COMMA -> {
				if (end) {
					if (state == State.KEY_AFTER_COMMA && !options.allowsTrailingCommas()) {
						throw JsonGrammar.error(r, "Trailing commas are not allowed in " + format + ".");
					}
					finish(r, out);
				} else {
					out.accept(StructuredData.objectKey(KeyRules.read(r, options))); state = State.COLON;
				}
			}
			case COLON -> { if (r.read() != ':') throw JsonGrammar.error(r, "Expected ':'."); state = State.VALUE; }
			case VALUE -> { JsonGrammar.value(r, options, depth, out, push); state = State.COMMA_OR_END; lineTerminatorAfterValue = false; }
			case COMMA_OR_END -> {
				if (end) finish(r, out);
				else if (ch == ',') { r.read(); state = State.KEY_AFTER_COMMA; lineTerminatorAfterValue = false; }
				else if (format == JsonFormat.HJSON && lineTerminatorAfterValue) state = State.KEY_OR_END;
				else throw JsonGrammar.error(r, "Expected ',' between object members.");
			}
			case COMPLETE -> { }
		}
	}
	private void finish(LookaheadCodePointReader r, Consumer<StructuredData> out) throws IOException {
		if (braced) r.read();
		out.accept(StructuredData.OBJECT_END); state = State.COMPLETE;
	}
	
}
