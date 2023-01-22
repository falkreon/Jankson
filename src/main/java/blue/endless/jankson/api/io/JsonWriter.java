package blue.endless.jankson.api.io;

import java.io.Writer;
import java.util.ArrayDeque;
import java.util.Deque;

public class JsonWriter {
	private Writer dest;
	private Deque<ElementType> context = new ArrayDeque<>();
	
	public JsonWriter(Writer destination, JsonWriterOptions options) {
		this.dest = destination;
	}
	
	public void writeKey(String key) throws JsonIOException {
		if (context.peekLast()!=ElementType.OBJECT_START) {
			throw new JsonIOException("Cannot write keys unless an object has been started.");
		}
	}
}
