package blue.endless.jankson;

import org.junit.jupiter.api.Test;

import blue.endless.jankson.api.SyntaxError;
import blue.endless.jankson.api.io.JsonWriter;
import blue.endless.jankson.api.io.JsonWriterOptions;
import blue.endless.jankson.api.io.TomlReader;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

import org.junit.jupiter.api.Assertions;

public class TestToml {
	@Test
	public void readComment() throws IOException, SyntaxError {
		String subject = """
				# This is a full-line comment
				key = "value"  # This is a comment at the end of a line
				another = "# This is not a comment"
				""";
		
		TomlReader reader = new TomlReader(new StringReader(subject));
		
		StringWriter sw = new StringWriter();
		JsonWriter jsonWriter = new JsonWriter(sw, JsonWriterOptions.STRICT);
		
		reader.transferTo(jsonWriter);
		
		sw.flush();
		String result = sw.getBuffer().toString();
		
		System.out.println(result);
	}
}
