package blue.endless.jankson.api.io;

import java.io.IOException;

import blue.endless.jankson.api.document.CommentType;

public interface StructuredDataWriter {
	
	public void writeComment(String value, CommentType type) throws IOException;
	
	public void writeWhitespace(String value) throws IOException;
	
	public void writeKey(String key) throws IOException;
	
	/**
	 * If we have just written a key and zero or more comments, emit the delimiter between keys and values and prepare the state to write the corresponding value.
	 */
	public void writeKeyValueDelimiter() throws IOException;
	
	/**
	 * Writes the delimiter between values in lists or the delimiter between key-value pairs in objects.
	 */
	public void nextValue() throws IOException;
	
	public void writeObjectStart() throws IOException;
	
	public void writeObjectEnd() throws IOException;
	
	public void writeArrayStart() throws IOException;
	
	public void writeArrayEnd() throws IOException;
	
	public void writeStringLiteral(String value) throws IOException;
	
	public void writeLongLiteral(long value) throws IOException;
	
	public void writeDoubleLiteral(double value) throws IOException;
	
	public void writeBooleanLiteral(boolean value) throws IOException;
	
	public void writeNullLiteral() throws IOException;
}
