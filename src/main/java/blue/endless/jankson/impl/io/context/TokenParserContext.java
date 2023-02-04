package blue.endless.jankson.impl.io.context;

import java.io.IOException;

import blue.endless.jankson.api.SyntaxError;
import blue.endless.jankson.api.document.PrimitiveElement;
import blue.endless.jankson.api.io.ElementType;
import blue.endless.jankson.impl.io.LookaheadCodePointReader;

public class TokenParserContext implements ParserContext {
	private String result;
	
	@Override
	public ElementType parse(LookaheadCodePointReader reader) throws IOException, SyntaxError {
		StringBuilder result = new StringBuilder();
		
		while(true) {
			int ch = reader.peek();
			if (!Character.isJavaIdentifierPart(ch)) {
				this.result = result.toString();
				return ElementType.PRIMITIVE;
			}
			
			ch = reader.read();
			result.append(Character.toString(ch));
		}
	}

	@Override
	public String getStringValue() {
		return result;
	}

	@Override
	public PrimitiveElement getValue() {
		return switch(result) {
			case "true" -> PrimitiveElement.of(true);
			case "false" -> PrimitiveElement.of(false);
			case "Infinity" -> PrimitiveElement.of(Double.POSITIVE_INFINITY);
			case "-Infinity" -> PrimitiveElement.of(Double.NEGATIVE_INFINITY);
			case "NaN" -> PrimitiveElement.of(Double.NaN);
			
			default -> PrimitiveElement.of(result);
		};
	}

	@Override
	public boolean canEOFHere() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isComplete(LookaheadCodePointReader reader) {
		// TODO Auto-generated method stub
		return false;
	}

	private boolean isTokenTerminator(char ch) {
		return !Character.isJavaIdentifierPart(ch);
	}
}
