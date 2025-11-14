package blue.endless.jankson.api.io.ini;

import java.io.IOException;
import java.io.Writer;

import blue.endless.jankson.api.SyntaxError;
import blue.endless.jankson.api.io.StructuredData;
import blue.endless.jankson.api.io.StructuredDataWriter;

public class IniWriter implements StructuredDataWriter {
	private Writer out;
	
	public IniWriter(Writer w) {
		out = w;
	}
	
	// TODO: headers, sub-objects, etc.
	@Override
	public void write(StructuredData data) throws SyntaxError, IOException {
		switch(data.type()) {
			case ARRAY_END -> {}
			case ARRAY_START -> {}
			case COMMENT -> {}
			case EOF -> {}
			case NEWLINE -> { out.write("\n"); }
			case OBJECT_END -> {}
			case OBJECT_KEY -> { out.write(data.value().toString()); out.write(" = "); }
			case OBJECT_START -> {}
			case PRIMITIVE -> out.write(data.value().toString());
			case WHITESPACE -> {}
			default -> throw new IllegalArgumentException("Unexpected value: " + data.type());
		}
	}
	
}
