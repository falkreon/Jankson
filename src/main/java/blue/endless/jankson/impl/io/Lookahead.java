package blue.endless.jankson.impl.io;

import java.io.IOException;


public interface Lookahead {
	/**
	 * Peeks a String of length codepoints ahead of the read pointer. This will result in a string of at least length
	 * characters, possibly more.
	 * @param length the number of codepoints to peek at
	 * @return       the String representation of those codepoints
	 * @throws IOException if an I/O error occurs
	 */
	public String peekString(int length) throws IOException;
	
	/**
	 * Gets the next codepoint in the stream, "before" it has been read. Implementations MUST have at least one
	 * codepoint of lookahead available.
	 * @return The next codepoint in the stream, or -1 if we have reached the end of the stream.
	 * @throws IOException if an I/O error occurs
	 */
	public int peek() throws IOException;
	
	/**
	 * Returns a codepoint from the stream, but one that is ahead of the read pointer. This codepoint will later be
	 * visible from a read method, but will not be consumed by this method.
	 * @param distanceAhead The number of characters ahead of the read pointer to peek. 1 is the next character in the
	 *                      stream.
	 * @return              The code point at this location, or -1 if the end of the file has been reached.
	 * @throws IOException  if an I/O error occurs
	 * @throws IllegalArgumentException if the lookahead is less than 1 or beyond this stream's lookahead buffer size
	 */
	public int peek(int distanceAhead) throws IOException;
}
