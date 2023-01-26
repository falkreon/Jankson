package blue.endless.jankson;

import java.io.IOException;
import java.io.StringWriter;

import org.junit.Test;

import blue.endless.jankson.api.document.FormattingElement;
import blue.endless.jankson.api.document.KeyValuePairElement;
import blue.endless.jankson.api.document.ObjectElement;
import blue.endless.jankson.api.document.PrimitiveElement;
import blue.endless.jankson.api.io.JsonWriter;
import blue.endless.jankson.api.io.JsonWriterOptions;

public class RefactorTests {
	
	@Test
	public void basicOperation() throws IOException {
		StringWriter stringWriter = new StringWriter();
		JsonWriter writer = new JsonWriter(stringWriter, JsonWriterOptions.INI_SON);
		
		ObjectElement test = new ObjectElement();
		test.put("foo", PrimitiveElement.of(42));
		
		KeyValuePairElement kvp = new KeyValuePairElement("bar", new ObjectElement());
		kvp.getPreamble().add(FormattingElement.LINE_BREAK);
		test.add(kvp);
		
		test.write(writer);
		
		System.out.println(stringWriter.toString());
	}
}
