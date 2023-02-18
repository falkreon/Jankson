package blue.endless.jankson.impl.io.context;

import java.io.IOException;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import blue.endless.jankson.api.SyntaxError;
import blue.endless.jankson.api.document.CommentElement;
import blue.endless.jankson.api.io.ElementType;
import blue.endless.jankson.api.io.JsonReaderOptions;
import blue.endless.jankson.impl.io.LookaheadCodePointReader;

public class ArrayParserContext implements ParserContext {
	private JsonReaderOptions options;
	private boolean foundStart = false;
	private boolean foundEnd = false;
	
	public ArrayParserContext(JsonReaderOptions options) {
		this.options = options;
	}
	
	@Override
	public void parse(LookaheadCodePointReader reader, BiConsumer<ElementType, Object> elementConsumer, Consumer<ParserContext> pusher) throws IOException, SyntaxError {
		emitComments(reader, elementConsumer);
		
		if (!foundStart) {
			int ch = reader.peek();
			if (ch=='[') {
				reader.read();
				foundStart = true;
				elementConsumer.accept(ElementType.ARRAY_START, null);
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
					elementConsumer.accept(ElementType.ARRAY_END, null);
				} else {
					handleValue(reader, elementConsumer, pusher);
				}
			} else {
				//Do nothing. We shouldn't have been called.
			}
			
		}
	}
	
	//TODO: Not very DRY. See if we can fold this together with ObjectParserContext's and hoist to ParserContext.
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
	
	//TODO: Also not very DRY. See if we can fold this together with ObjectParserContext's
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
			//TODO: Unquoted Strings etc.
			throw new SyntaxError("Expected a value here, but couldn't decode it.", reader.getLine(), reader.getCharacter());
		}
	}

	@Override
	public boolean isComplete(LookaheadCodePointReader reader) {
		return foundStart && foundEnd;
	}

}
