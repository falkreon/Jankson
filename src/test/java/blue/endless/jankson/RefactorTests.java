package blue.endless.jankson;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import blue.endless.jankson.api.document.FormattingElement;
import blue.endless.jankson.api.document.KeyValuePairElement;
import blue.endless.jankson.api.document.ObjectElement;
import blue.endless.jankson.api.document.PrimitiveElement;
import blue.endless.jankson.api.io.JsonWriter;
import blue.endless.jankson.api.io.JsonWriterOptions;
import blue.endless.jankson.impl.io.LookaheadCodePointReader;

public class RefactorTests {
	/*
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
	}*/
	
	/*
	@Test
	public void testUnread() throws IOException {
		StringReader s = new StringReader("Foo Bar");
		CodePointReader in = new CodePointReader(s);
		
		System.out.println(in.peekString(4));
		char[] fin = new char[7];
		in.read(fin);
		System.out.println(new String(fin));
	}*/
	
	@Test
	public void codePoints() {
		
		/*
		//String pizza = "\uD83C\uD855";
		String pizza = new String(new int[] { 0x1F355 }, 0, 1);
		System.out.println(pizza);
		byte[] utf8 = pizza.getBytes();
		System.out.println(Arrays.toString(utf8));
		
		List<Integer> points = new ArrayList<>();
		for(char ch : pizza.toCharArray()) points.add((int) ch);
		
		List<String> pointsList = points.stream().map(Integer::toHexString).collect(Collectors.toList());
		
		System.out.println(pointsList);*/
	}
}
