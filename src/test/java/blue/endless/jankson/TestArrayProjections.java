package blue.endless.jankson;

import java.io.IOException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import blue.endless.jankson.api.Jankson;
import blue.endless.jankson.api.SyntaxError;
import blue.endless.jankson.api.document.ArrayElement;
import blue.endless.jankson.api.document.ObjectElement;
import blue.endless.jankson.api.document.PrimitiveElement;
import blue.endless.jankson.api.document.ValueElement;

public class TestArrayProjections {
	
	@Test
	@SuppressWarnings("unused")
	public void testObjectProjection() throws IOException, SyntaxError {
		String subject = "[ \"foo\", {}, 42, {}, {}, \"a\" ]";
		ValueElement val = Jankson.readJson(subject);
		Assertions.assertTrue(val instanceof ArrayElement);
		
		int objectCount = 0;
		for(ObjectElement obj : ((ArrayElement) val).asObjectArray()) {
			objectCount++;
		}
		
		Assertions.assertEquals(3, objectCount);
	}
	
	@Test
	public void testPrimitiveProjection() throws IOException, SyntaxError {
		String subject = "[ \"foo\", {}, 40, {}, {}, 2, \"a\" ]";
		ValueElement val = Jankson.readJson(subject);
		Assertions.assertTrue(val instanceof ArrayElement);
		
		int sumOfNumbers = 0;
		for(PrimitiveElement p : ((ArrayElement) val).asPrimitiveArray()) {
			sumOfNumbers += p.orElse(0);
		}
		
		Assertions.assertEquals(42, sumOfNumbers);
	}
}
