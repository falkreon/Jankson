package blue.endless.jankson.api.io;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayDeque;
import java.util.Deque;

import blue.endless.jankson.api.document.CommentType;

public class JsonWriter implements StructuredDataWriter {
	private Writer dest;
	private Deque<ElementType> context = new ArrayDeque<>();
	private boolean isRootWritten = false;
	
	public JsonWriter(Writer destination, JsonWriterOptions options) {
		this.dest = destination;
	}

	@Override
	public void writeComment(String value, CommentType type) throws IOException {
		//For now, ignore commentType
		dest.write("/* ");
		dest.write(value);
		dest.write(" */");
	}

	@Override
	public void writeWhitespace(String value) throws IOException {
		dest.write(value);
	}

	@Override
	public void writeKey(String key) throws IOException {
		dest.write('"');
		dest.write(key);
		dest.write('"');
		dest.write(' ');
	}

	@Override
	public void startValue() throws IOException {
		//TODO: Peek the context to find out whether we're writing objects or arrays
		dest.write(": ");
	}

	@Override
	public void nextValue() throws IOException {
		dest.write(", ");
	}

	@Override
	public void writeObjectStart() throws IOException {
		dest.write("{ ");
	}

	@Override
	public void writeObjectEnd() throws IOException {
		dest.write(" }");
	}

	@Override
	public void writeArrayStart() throws IOException {
		dest.write("[ ");
	}

	@Override
	public void writeArrayEnd() throws IOException {
		dest.write(" ]");
	}

	@Override
	public void writeStringLiteral(String value) throws IOException {
		dest.write('"');
		dest.write(value);
		dest.write('"');
	}

	@Override
	public void writeLongLiteral(long value) throws IOException {
		dest.write(Long.toString(value));
	}

	@Override
	public void writeDoubleLiteral(double value) throws IOException {
		dest.write(Double.toString(value));
	}

	@Override
	public void writeBooleanLiteral(boolean value) throws IOException {
		dest.write(Boolean.toString(value));
	}

	@Override
	public void writeNullLiteral() throws IOException {
		dest.write("null");
	}
}
