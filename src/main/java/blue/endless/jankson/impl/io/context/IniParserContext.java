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
import blue.endless.jankson.api.document.CommentType;
import blue.endless.jankson.api.io.StructuredData;
import blue.endless.jankson.impl.io.LookaheadCodePointReader;

public class IniParserContext implements ParserContext {
	
	private String sectionName = null;
	private boolean complete = false;
	
	@Override
	public void parse(LookaheadCodePointReader reader, Consumer<StructuredData> elementConsumer, Consumer<ParserContext> pusher) throws IOException, SyntaxError {
		
		int ch = reader.read();
		switch (ch) {
			case -1 -> {
				if (sectionName != null) {
					elementConsumer.accept(StructuredData.OBJECT_END);
					sectionName = null;
				}
				complete = true;
				return;
			}
		
			case '\n' -> {
				//Blank line or a trailing newline we need to discard
				discardTrailingWhitespace(reader);
				return;
			}
		
			case '#', ';' -> {
				String comment = readRestOfLine(reader);
				elementConsumer.accept(StructuredData.comment(comment, CommentType.OCTOTHORPE));
				discardTrailingWhitespace(reader);
				return;
			}
		
			case '[' -> {
				if (sectionName != null) {
					elementConsumer.accept(StructuredData.OBJECT_END);
				}
				
				sectionName = readLineUntil(reader, ']');
				int endBrace = reader.peek();
				if (endBrace != ']') throw new SyntaxError("Expected ']' but found end of line instead", reader.getLine(), reader.getCharacter());
				
				elementConsumer.accept(StructuredData.objectKey(sectionName));
				elementConsumer.accept(StructuredData.OBJECT_START);
				
				discardTrailingWhitespace(reader);
				return;
			}
		}
		
		StringBuilder key = new StringBuilder();
		
		while(ch != '\n' && ch != -1 && ch != '=') {
			key.appendCodePoint(ch);
			
			ch = reader.read();
		}
		
		elementConsumer.accept(StructuredData.objectKey(key.toString().trim()));
		
		if (ch != '=') throw new SyntaxError("Expected '=', but found end of line instead", reader.getLine(), reader.getCharacter());
		
		if (NumberValueParser.canReadStatic(reader)) {
			Number n = NumberValueParser.readStatic(reader);
			elementConsumer.accept(StructuredData.primitive(n));
		} else if (BooleanValueParser.canReadStatic(reader)) {
			Boolean b = BooleanValueParser.readStatic(reader);
			elementConsumer.accept(StructuredData.primitive(b));
		} else if (StringValueParser.canReadStatic(reader)) {
			String s = StringValueParser.readStatic(reader);
			elementConsumer.accept(StructuredData.primitive(s));
		} else {
			String s = readRestOfLine(reader);
			elementConsumer.accept(StructuredData.primitive(s));
		}
		
	}

	private static String readUntil(LookaheadCodePointReader reader, int codePoint) throws IOException {
		StringBuilder result = new StringBuilder();
		
		while(true) {
			int cur = reader.peek();
			if (cur == -1 || cur == codePoint) return result.toString();
			result.appendCodePoint(reader.read());
		}
	}
	
	private static String readLineUntil(LookaheadCodePointReader reader, int codePoint) throws IOException {
		StringBuilder result = new StringBuilder();
		
		while(true) {
			int cur = reader.peek();
			if (cur == -1 || cur == codePoint || cur == '\n') return result.toString();
			result.appendCodePoint(reader.read());
		}
	}
	
	private static String readRestOfLine(LookaheadCodePointReader reader) throws IOException {
		StringBuilder result = new StringBuilder();
		
		while(true) {
			int cur = reader.peek();
			if (cur == -1 || cur == '\n') return result.toString();
			result.appendCodePoint(reader.read());
		}
	}
	
	private static void discardTrailingWhitespace(LookaheadCodePointReader reader) throws IOException {
		//whitespace includes \r and \n
		while (Character.isWhitespace(reader.peek())) {
			reader.read();
		}
	}
	
	@Override
	public boolean isComplete(LookaheadCodePointReader reader) {
		return complete;
	}

}
