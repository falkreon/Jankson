package blue.endless.jankson.impl.io;

import java.io.IOException;

public interface CodePointReader extends AutoCloseable {
	/**
	 * Reads a single code point. This may cause multiple characters to be read from the underlying Reader or InputStream.
	 * @return The code point read, or -1 if the end of the stream has been reached.
	 * @throws IOException if an I/O error occurs
	 */
	int read() throws IOException;
	
	/**
	 * Reads code points into a portion of an array. This method will block until all of the characters are available,
	 * an I/O error occurs, or the end of the stream is reached.
	 * @param buffer the destination buffer
	 * @param start  the offset at which to start storing code points
	 * @param len    the maximum number of code points to read
	 * @return       the number of code points read, or -1 if the end of the stream was reached before reading any
	 *               code points.
	 * @throws IOException if an I/O error occurs
	 */
	default int read(int[] buffer, int start, int len) throws IOException {
		for(int i=0; i<len; i++) {
			int codePoint = read();
			if (codePoint==-1) return i-1; //0-1 is -1, so if the first character was EOF, -1 is returned
			buffer[start+i] = codePoint;
		}
		
		return len;
	}
	
	/**
	 * Reads code points into a String. This method will block until all of the characters are available, an I/O error
	 * occurs, or the end of the stream is reached. If the end of the stream is reached before any code points are read,
	 * an empty String will be returned. Otherwise, the String will represent as many code points as were available, up
	 * to numCodePoints.
	 * @param numCodePoints The maximum number of code points to read
	 * @return              A String representing the accumulated code points.
	 * @throws IOException if an I/O error occurs
	 */
	default String readString(int numCodePoints) throws IOException {
		StringBuilder builder = new StringBuilder();
		
		for(int i=0; i<numCodePoints; i++) {
			int codePoint = read();
			if (codePoint==-1) {
				return builder.toString();
			}
			builder.append(Character.toString(codePoint));
		}
		
		return builder.toString();
	}
	
	/**
	 * Closes this resource, relinquishing any underlying resources. This method is invoked automatically on objects
	 * managed by the try-with-resources statement. If the stream is already closed then invoking this method has no
	 * effect.
	 * @throws IOException if an I/O error occurs
	 */
	@Override
	void close() throws IOException;
}
