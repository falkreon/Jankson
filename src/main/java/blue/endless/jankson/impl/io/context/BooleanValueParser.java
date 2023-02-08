package blue.endless.jankson.impl.io.context;

import java.io.IOException;

import blue.endless.jankson.api.SyntaxError;
import blue.endless.jankson.impl.io.Lookahead;
import blue.endless.jankson.impl.io.LookaheadCodePointReader;

public class BooleanValueParser implements ValueParser {

	@Override
	public boolean canRead(Lookahead lookahead) throws IOException {
		//TODO: We probably need to peek one more character ahead and make sure that the character after our String is a valid breaking code point
		String maybeFalse = lookahead.peekString(5);
		return maybeFalse.equals("false") || maybeFalse.startsWith("true");
	}

	@Override
	public Object read(LookaheadCodePointReader reader) throws IOException, SyntaxError {
		
		String start = reader.readString(4);
		if (start.equals("true")) return Boolean.TRUE;
		start += reader.readString(1);
		if (start.equals("false")) return Boolean.FALSE;
		
		throw new IllegalStateException("Couldn't parse boolean value '"+start+"'.");
	}

}
