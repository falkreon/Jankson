/*
 * MIT License
 *
 * Copyright (c) 2018-2025 Falkreon (Isaac Ellingson)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package blue.endless.jankson;

import static org.junit.jupiter.api.Assertions.*;

import java.io.StringReader;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.Test;

import blue.endless.jankson.api.SyntaxError;
import blue.endless.jankson.api.annotation.Deserializer;
import blue.endless.jankson.api.annotation.SerializedName;
import blue.endless.jankson.api.document.CommentType;
import blue.endless.jankson.api.document.PrimitiveElement;
import blue.endless.jankson.api.io.ObjectReaderFactory;
import blue.endless.jankson.api.io.ObjectWriter;
import blue.endless.jankson.api.io.StructuredData;
import blue.endless.jankson.api.io.StructuredDataReader;
import blue.endless.jankson.api.io.ValueElementReader;
import blue.endless.jankson.api.io.json.JsonReader;
import blue.endless.jankson.impl.io.objectwriter.MapDeserializer;
import blue.endless.jankson.impl.io.objectwriter.factory.ObjectWrapper;

public class ObjectMappingFixTests {
	@SuppressWarnings("rawtypes")
	private static class GenericTypes<T extends List<String>> {
		List rawList;
		Map rawMap;
		List<?> wildcardList;
		List<? extends String> upperList;
		List<? super String> lowerList;
		Map<String, ?> wildcardMap;
	}

	public static final class ExplodingKey {
		public ExplodingKey(String value) { throw new IllegalArgumentException("bad key: "+value); }
	}

	public record NormalizedKey(String value) {
		public NormalizedKey {
			value = value.toLowerCase(Locale.ROOT);
		}
	}

	private static class SerializedFields {
		List<String> direct = List.of("direct");
		List<List<String>> nested = List.of(List.of("first"), List.of("second"));
		@SuppressWarnings("unchecked")
		List<String>[] array = new List[] {List.of("array")};
	}

	private static class GenericSerializedField<T> {
		T value;
		GenericSerializedField(T value) { this.value = value; }
	}

	@SuppressWarnings("unused")
	private GenericSerializedField<List<String>> genericSerializedFieldType;

	private static class Animal {}
	private static class Dog extends Animal implements Tagged, Named {}
	private interface Tagged {}
	private interface Named {}

	private static class AmbiguousAutomatic {
		private final String value;
		public AmbiguousAutomatic(@SerializedName("value") String value) { this.value = value; }
		public static AmbiguousAutomatic create(@SerializedName("value") String value) {
			return new AmbiguousAutomatic(value);
		}
	}

	private static class SpecificAutomatic {
		private final String value;
		public SpecificAutomatic(@SerializedName("value") Object value) { this.value = "constructor:"+value; }
		public static SpecificAutomatic create(@SerializedName("value") String value) {
			SpecificAutomatic result = new SpecificAutomatic(value);
			return new SpecificAutomatic(result.value.replace("constructor:", "factory:"));
		}
	}

	private static class AmbiguousMarked {
		private final String value;
		private AmbiguousMarked() { value = null; }
		@Deserializer public static AmbiguousMarked first(@SerializedName("value") String value) {
			return new AmbiguousMarked();
		}
		@Deserializer public static AmbiguousMarked second(@SerializedName("value") String value) {
			return new AmbiguousMarked();
		}
	}

	private static class BoxingEquivalentFactories {
		private final int value;
		public BoxingEquivalentFactories(@SerializedName("value") int value) { this.value = value; }
		public static BoxingEquivalentFactories create(@SerializedName("value") Integer value) {
			return new BoxingEquivalentFactories(value);
		}
	}

	public record PublicRecord(@SerializedName("wire") String value) {}

	@Test
	public void mapIgnoresFormattingBetweenKeyAndValue() throws Exception {
		MapDeserializer<String, String> reader = new MapDeserializer<>(String.class, String.class);
		reader.write(StructuredData.OBJECT_START);
		reader.write(StructuredData.objectKey("key"));
		reader.write(StructuredData.comment("comment", CommentType.LINE_END));
		reader.write(StructuredData.whitespace(" "));
		reader.write(StructuredData.NEWLINE);
		reader.write(StructuredData.primitive("value"));
		reader.write(StructuredData.OBJECT_END);
		assertEquals(Map.of("key", "value"), reader.getResult());
	}

	@Test
	public void mapKeyConstructionFailureIsSyntaxErrorWithCause() throws Exception {
		MapDeserializer<ExplodingKey, String> reader = new MapDeserializer<>(ExplodingKey.class, String.class);
		reader.write(StructuredData.OBJECT_START);
		SyntaxError error = assertThrows(SyntaxError.class,
				() -> reader.write(StructuredData.objectKey("broken")));
		assertTrue(error.getMessage().contains("broken"));
		assertTrue(error.getMessage().contains(ExplodingKey.class.getTypeName()));
		assertInstanceOf(IllegalArgumentException.class, error.getCause());
	}

	@Test
	public void mapRejectsDistinctTextKeysThatDecodeToTheSameKey() throws Exception {
		MapDeserializer<NormalizedKey, String> reader = new MapDeserializer<>(NormalizedKey.class, String.class);
		reader.write(StructuredData.OBJECT_START);
		reader.write(StructuredData.objectKey("Alpha"));
		reader.write(StructuredData.primitive("first"));

		SyntaxError error = assertThrows(SyntaxError.class,
				() -> reader.write(StructuredData.objectKey("ALPHA")));
		assertTrue(error.getMessage().contains("Alpha"));
		assertTrue(error.getMessage().contains("ALPHA"));
		assertTrue(error.getMessage().contains(NormalizedKey.class.getTypeName()));
	}

	@Test
	public void mapRejectsRepeatedDecodedKeys() throws Exception {
		MapDeserializer<String, String> reader = new MapDeserializer<>(String.class, String.class);
		reader.write(StructuredData.OBJECT_START);
		reader.write(StructuredData.objectKey("same"));
		reader.write(StructuredData.primitive("first"));

		SyntaxError error = assertThrows(SyntaxError.class,
				() -> reader.write(StructuredData.objectKey("same")));
		assertTrue(error.getMessage().contains("same"));
		assertTrue(error.getMessage().contains("duplicate"));
	}

	@Test
	public void mapDuplicateTrackingDoesNotIncludePreexistingKeys() throws Exception {
		NormalizedKey key = new NormalizedKey("existing");
		Map<NormalizedKey, String> subject = new HashMap<>(Map.of(key, "old"));
		MapDeserializer<NormalizedKey, String> reader = new MapDeserializer<>(NormalizedKey.class, String.class, subject);
		reader.write(StructuredData.OBJECT_START);
		reader.write(StructuredData.objectKey("EXISTING"));
		reader.write(StructuredData.primitive("new"));
		reader.write(StructuredData.OBJECT_END);

		assertEquals(Map.of(key, "new"), reader.getResult());
	}

	@Test
	public void reflectiveSerializationPreservesParameterizedFieldAndElementTypes() throws Exception {
		Type listOfStrings = SerializedFields.class.getDeclaredField("direct").getGenericType();
		ObjectReaderFactory factory = new ObjectReaderFactory();
		factory.setPrecise(true);
		factory.registerSerializer(listOfStrings, ignored -> PrimitiveElement.of("string-list"));

		List<Object> values = semanticValues(factory.getReader(new SerializedFields()));
		assertEquals(List.of("string-list", "string-list", "string-list", "string-list"), values);
	}

	@Test
	public void reflectiveSerializationResolvesParameterizedOwnerFields() throws Exception {
		Type listOfStrings = SerializedFields.class.getDeclaredField("direct").getGenericType();
		Type ownerType = ObjectMappingFixTests.class.getDeclaredField("genericSerializedFieldType").getGenericType();
		ObjectReaderFactory factory = new ObjectReaderFactory();
		factory.setPrecise(true);
		factory.registerSerializer(listOfStrings, ignored -> PrimitiveElement.of("generic-list"));

		GenericSerializedField<List<String>> subject = new GenericSerializedField<>(List.of("value"));
		assertEquals(List.of("generic-list"), semanticValues(factory.getReader(ownerType, subject)));
	}

	@Test
	public void objectTypeRecursivelyUsesJavaContainersAndBoxedPrimitives() throws Exception {
		Object value = read("{name: 'test', values: [1, true, null, {nested: 2.5}]}", Object.class);
		Map<?, ?> object = assertInstanceOf(Map.class, value);
		assertEquals("test", object.get("name"));
		List<?> values = assertInstanceOf(List.class, object.get("values"));
		assertInstanceOf(Number.class, values.get(0));
		assertEquals(Boolean.TRUE, values.get(1));
		assertNull(values.get(2));
		assertEquals(2.5, ((Map<?, ?>) values.get(3)).get("nested"));
	}

	@Test
	public void dynamicCollectionsIgnoreFormattingEvents() throws Exception {
		assertEquals(List.of(1L, 2L), read("[1, /* between */ 2]", Object.class));
		assertEquals(List.of(1L, 2L), read("[1, // between\n 2]", field("rawList")));
	}

	@Test
	public void rawAndWildcardCollectionsAndMapsAreUseful() throws Exception {
		assertEquals(List.of("a", 2L), read("['a', 2]", field("rawList")));
		assertEquals(Map.of("a", 1L), read("{a: 1}", field("rawMap")));
		assertEquals(List.of(Map.of("a", 1L)), read("[{a: 1}]", field("wildcardList")));
		assertEquals(List.of("a", "b"), read("['a', 'b']", field("upperList")));
		assertEquals(List.of("a", 2L), read("['a', 2]", field("lowerList")));
		assertEquals(Map.of("a", List.of(1L)), read("{a: [1]}", field("wildcardMap")));
	}

	@Test
	public void boundedTypeVariableUsesItsErasure() throws Exception {
		Type variable = GenericTypes.class.getTypeParameters()[0];
		assertEquals(List.of("a", "b"), read("['a', 'b']", variable));
	}

	@Test
	public void readerResolutionHonorsDeclaredRuntimePrecisionAndSpecificity() throws Exception {
		Dog dog = new Dog();
		ObjectReaderFactory factory = new ObjectReaderFactory();
		factory.register((Type) Animal.class, ignored -> marker("declared"));
		factory.register((Type) Dog.class, ignored -> marker("runtime"));
		assertEquals("declared", firstValue(factory.getReader(Animal.class, dog)));

		ObjectReaderFactory runtime = new ObjectReaderFactory();
		runtime.register((Type) Dog.class, ignored -> marker("runtime"));
		assertEquals("runtime", firstValue(runtime.getReader(Animal.class, dog)));

		runtime.setPrecise(true);
		assertEquals(StructuredData.Type.OBJECT_START, runtime.getReader(Animal.class, dog).next().type());
		runtime.register((Type) Animal.class, ignored -> marker("declared"));
		assertEquals("declared", firstValue(runtime.getReader(Animal.class, dog)));

		ObjectReaderFactory specific = new ObjectReaderFactory();
		specific.register((Type) Object.class, ignored -> marker("object"));
		specific.register((Type) Animal.class, ignored -> marker("animal"));
		assertEquals("animal", firstValue(specific.getReader(Dog.class, dog)));
	}

	@Test
	public void readerResolutionRejectsIncomparableRegistrations() {
		ObjectReaderFactory factory = new ObjectReaderFactory();
		factory.register((Type) Tagged.class, ignored -> marker("tagged"));
		factory.register((Type) Named.class, ignored -> marker("named"));
		IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
				() -> factory.getReader(Animal.class, new Dog()));
		assertTrue(error.getMessage().contains("Ambiguous"));
		assertTrue(error.getMessage().contains(Tagged.class.getTypeName()));
		assertTrue(error.getMessage().contains(Named.class.getTypeName()));
	}

	@Test
	public void automaticAndMarkedFactoryAmbiguityIsRejected() {
		assertTrue(assertThrows(IllegalArgumentException.class,
				() -> ObjectWrapper.of(AmbiguousAutomatic.class, null)).getMessage().contains("Ambiguous unmarked"));
		assertTrue(assertThrows(IllegalArgumentException.class,
				() -> ObjectWrapper.of(AmbiguousMarked.class, null)).getMessage().contains("Ambiguous @Deserializer"));
		assertTrue(assertThrows(IllegalArgumentException.class,
				() -> ObjectWrapper.of(BoxingEquivalentFactories.class, null)).getMessage().contains("Ambiguous unmarked"));
	}

	@Test
	public void automaticFactoryUsesUniqueMostSpecificParameters() throws Exception {
		SpecificAutomatic result = read("{value: 'selected'}", SpecificAutomatic.class);
		assertEquals("constructor:factory:selected", result.value);
	}

	@Test
	public void publicRecordsSerializeThroughTheirAccessorAndKeepFieldMetadata() throws Exception {
		StructuredDataReader reader = new ObjectReaderFactory().getReader(new PublicRecord("value"));
		assertEquals(StructuredData.Type.OBJECT_START, reader.next().type());
		StructuredData key = reader.next();
		assertEquals(StructuredData.Type.OBJECT_KEY, key.type());
		assertEquals("wire", key.value());
		assertEquals("value", reader.next().value());
	}

	private static Type field(String name) throws NoSuchFieldException {
		return GenericTypes.class.getDeclaredField(name).getGenericType();
	}

	private static StructuredDataReader marker(String value) {
		return ValueElementReader.of(PrimitiveElement.of(value));
	}

	private static Object firstValue(StructuredDataReader reader) throws Exception {
		return reader.next().value();
	}

	private static List<Object> semanticValues(StructuredDataReader reader) throws Exception {
		java.util.ArrayList<Object> result = new java.util.ArrayList<>();
		while (reader.hasNext()) {
			StructuredData event = reader.next();
			if (event.type() == StructuredData.Type.PRIMITIVE) result.add(event.value());
		}
		return result;
	}

	private static <T> T read(String json, Type type) throws Exception {
		ObjectWriter<T> writer = new ObjectWriter<>(type);
		new JsonReader(new StringReader(json)).transferTo(writer);
		return writer.toObject();
	}
}
