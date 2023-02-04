package blue.endless.jankson.impl.io.context;

import java.io.IOException;

import blue.endless.jankson.api.SyntaxError;
import blue.endless.jankson.impl.io.Lookahead;
import blue.endless.jankson.impl.io.LookaheadCodePointReader;

/**
 * Parses a terminal value from a stream. Terminal values are things like `NaN`, `3.4159`, and `"butter"`. They are
 * almost always destined to be stored in a JsonPrimitive.
 */
public interface ValueParser {
	/**
	 * Returns true if this ValueParser can understand the value at this location in the stream. This is a passive
	 * determination, and SHOULD be possible within about three characters of lookahead.
	 * @param lookahead the stream.
	 * @return true if read should be called using this ValueParser, otherwise false.
	 */
	boolean canRead(Lookahead lookahead) throws IOException;
	
	/**
	 * Read the value at this location in the stream. Lookahead is still permitted.
	 * @param reader the stream.
	 * @return an Object representing the value that was parsed. This is the bare Object, not a PrimitiveElement wrapping it.
	 * @throws IOException if an I/O error occurs
	 * @throws SyntaxError if the value was not properly formed
	 */
	Object read(LookaheadCodePointReader reader) throws IOException, SyntaxError;
}
