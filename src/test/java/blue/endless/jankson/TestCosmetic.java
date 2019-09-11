package blue.endless.jankson;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TestCosmetic {
	Jankson jankson;
	
	@Before
	public void setup() {
		jankson = Jankson.builder().build();
	}
	
	/**
	 * Issue: #16
	 * Closing array brace needs to be indented. Empty array should be kept on the same line.
	 */
	@Test
	public void testArrayIndents() {
		
		String expectedJson5 =
				"{\n" + 
				"	\"empty\": [],\n" + //one-line empty array with no whitespace
				"	\"nonempty\": [\n" + //No whitespace after opening brace
				"		\"Foo\",\n" + //Properly indented items, one line per item
				"		42,\n" + //closing comma
				"	],\n" + //indented closing array with comma
				"}";
		
		String expectedStrict =
				"{\n" +
				"	\"empty\": [],\n" + 
				"	\"nonempty\": [\n" + 
				"		\"Foo\",\n" + 
				"		42\n" + //missing comma
				"	]\n" + //missing comma 
				"}";
		
		String expectedRootArray =
				"[\n" + //no indent, no errors
				"	\"Foo\",\n" + //1 indent level
				"	42,\n" + 
				"]"; //no indent
		
		JsonObject subject = new JsonObject();
		JsonArray emptyArr = new JsonArray();
		JsonArray nonEmptyArr = new JsonArray();
		
		subject.put("empty", emptyArr);
		subject.put("nonempty", nonEmptyArr);
		nonEmptyArr.add(0, new JsonPrimitive("Foo"));
		nonEmptyArr.add(1, new JsonPrimitive(42));
		
		String json5 = subject.toJson(JsonGrammar.JSON5);
		String strict = subject.toJson(JsonGrammar.STRICT);
		String rootArray = nonEmptyArr.toJson(JsonGrammar.JSON5);
		
		Assert.assertEquals(expectedJson5, json5);
		Assert.assertEquals(expectedStrict, strict);
		Assert.assertEquals(expectedRootArray, rootArray);
	}
}
