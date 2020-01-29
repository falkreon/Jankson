package blue.endless.jankson;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import blue.endless.jankson.annotation.Serializer;

public class TestSerializer {
	Jankson jankson;
	
	@Before
	public void setup() {
		jankson = Jankson.builder().build();
	}
	
	/**
	 * Make sure that characters which lie outside the BMP and/or have complex encodings wind up
	 * decomposed and escaped properly
	 */
	@Test
	public void testUnicodeEscapes() {
		String smileyFace = String.valueOf(Character.toChars(0x1F600));
		String result = new JsonPrimitive(smileyFace).toString();
		Assert.assertEquals("\"\\ud83d\\ude00\"", result);
	}
	
	
	private static class DeclaredSerializerTest {
		private String foo = "bar";
		
		@Serializer
		public JsonPrimitive serialize() {
			return JsonPrimitive.of(42L);
		}
	}
	
	@Test
	public void testInternalSerializer() {
		JsonElement elem = jankson.toJson(new DeclaredSerializerTest());
		Assert.assertEquals("42", elem.toJson());
	}
}
