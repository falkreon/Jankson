package blue.endless.jankson.impl.io.context;

import java.io.IOException;

import blue.endless.jankson.api.SyntaxError;
import blue.endless.jankson.impl.io.Lookahead;
import blue.endless.jankson.impl.io.LookaheadCodePointReader;

public class TokenValueParser implements ValueParser {
	private static final String VALID_UNQUOTED_CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_";
	


	@Override
	public boolean canRead(Lookahead reader) throws IOException {
		int ch = reader.peek();
		return ch != -1 && VALID_UNQUOTED_CHARS.indexOf(ch) != -1;
	}

	@Override
	public String read(LookaheadCodePointReader reader) throws IOException, SyntaxError {
		return readStatic(reader);
	}
	
	public static String readStatic(LookaheadCodePointReader reader) throws IOException, SyntaxError {
		StringBuilder token = new StringBuilder();
		int ch = reader.peek();
		while(ch != -1 && VALID_UNQUOTED_CHARS.indexOf(ch) != -1) {
			token.appendCodePoint(reader.read());
			ch = reader.peek();
		}
		
		if (token.isEmpty()) throw new SyntaxError("Expected unquoted token but found illegal characters.", reader.getLine(), reader.getCharacter());
		return token.toString();
	}
	
}
