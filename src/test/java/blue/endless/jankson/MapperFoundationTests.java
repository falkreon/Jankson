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

import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import blue.endless.jankson.api.SyntaxError;
import blue.endless.jankson.api.annotation.Comment;
import blue.endless.jankson.api.annotation.MutatorFor;
import blue.endless.jankson.api.annotation.SerializedName;
import blue.endless.jankson.api.config.ConfigCodecs;
import blue.endless.jankson.api.document.CommentType;
import blue.endless.jankson.api.document.PrimitiveElement;
import blue.endless.jankson.api.io.ObjectReaderFactory;
import blue.endless.jankson.api.io.ObjectWriter;
import blue.endless.jankson.api.io.StructuredData;
import blue.endless.jankson.api.io.StructuredDataReader;
import blue.endless.jankson.api.io.json.JsonReader;

public class MapperFoundationTests {
	private enum Mode {
		FAST,
		@SerializedName("slow-mode") SLOW
	}

	private static class Parent {
		private String inherited;
		private static String ignoredStatic = "static";
		private transient String ignoredTransient = "transient";
	}

	private static class MutableConfig extends Parent {
		@SerializedName("ratio-value")
		private float ratio;
		private List<String> names;
		private ArrayList<HashMap<String, List<String>>> nested;
		private Mode mode;

		private MutableConfig() {}
	}

	private static class SetterConfig {
		private String value;

		private SetterConfig() {}

		private void setValue(String value) {
			this.value = "set:"+value;
		}
	}

	private record CommentedRecord(@Comment("first\n \nsecond") String value) {}
	private static class GenericBase<T> { private List<T> values; }
	private static class StringChild extends GenericBase<String> { private StringChild() {} }
	private static class NestedGenericBase<T> { private List<T> value; }
	private static class NestedGenericMiddle<T> extends NestedGenericBase<List<T>> {}
	private static class NestedGenericChild extends NestedGenericMiddle<String> { private NestedGenericChild() {} }
	private static class GenericArrayBase<T> { private T[] values; }
	private static class StringArrayChild extends GenericArrayBase<String> { private StringArrayChild() {} }
	private record Box<T>(T value) {}
	public Box<List<String>> boxType;
	private static class MixedFinalConfig {
		private final String kind = "fixed";
		private int port;
		private MixedFinalConfig() {}
	}
	private static class FinalMutatorParent {
		private final List<String> values = new ArrayList<>();

		@MutatorFor("values")
		private void replaceValues(List<String> replacement) {
			values.clear();
			values.addAll(replacement);
		}
	}
	private static class FinalMutatorChild extends FinalMutatorParent { private FinalMutatorChild() {} }
	private static class Animal {}
	private static class Dog extends Animal {}
	private static class Zoo { private Animal animal = new Dog(); }
	private static class LargeNumbers { private BigInteger integer; private BigDecimal decimal; }
	private static class EnumMapConfig { private Map<Mode, String> values; private EnumMapConfig() {} }

	@Test
	public void roundTripsPrivateInheritedAndNestedProperties() throws IOException, SyntaxError {
		String json = """
				{
					inherited: "base",
					ratio-value: 1.25,
					names: ["a", "b"],
					nested: [{key: ["value"]}],
					mode: "slow-mode"
				}
				""";
		MutableConfig value = read(json, MutableConfig.class);

		Assertions.assertEquals("base", ((Parent) value).inherited);
		Assertions.assertEquals(1.25f, value.ratio);
		Assertions.assertEquals(List.of("a", "b"), value.names);
		Assertions.assertEquals(List.of(Map.of("key", List.of("value"))), value.nested);
		Assertions.assertEquals(Mode.SLOW, value.mode);

		List<String> keys = semanticKeys(new ObjectReaderFactory().getReader(value));
		Assertions.assertEquals(Set.of("ratio-value", "names", "nested", "mode", "inherited"), Set.copyOf(keys));
		Assertions.assertFalse(keys.contains("ignoredStatic"));
		Assertions.assertFalse(keys.contains("ignoredTransient"));
	}

	@Test
	public void invokesConventionalSetter() throws IOException, SyntaxError {
		SetterConfig value = read("{value: 'input'}", SetterConfig.class);
		Assertions.assertEquals("set:input", value.value);
	}

	@Test
	public void handlesNullAccordingToTargetType() throws IOException, SyntaxError {
		record Nullable(String value) {}
		record Primitive(int value) {}
		record Narrow(byte value) {}

		Assertions.assertNull(read("{value: null}", Nullable.class).value());
		IOException error = Assertions.assertThrows(IOException.class, () -> read("{value: null}", Primitive.class));
		Assertions.assertTrue(error.getMessage().contains("Cannot assign null to primitive")
				|| error.getCause() != null && error.getCause().getMessage().contains("Cannot assign null to primitive"));
		IOException overflow = Assertions.assertThrows(IOException.class, () -> read("{value: 128}", Narrow.class));
		Assertions.assertTrue(overflow.getMessage().contains("Byte out of range")
				|| overflow.getCause() != null && overflow.getCause().getMessage().contains("Byte out of range"));
		Assertions.assertThrows(IOException.class, () -> read("{value: -129}", Narrow.class));

		record NarrowShort(short value) {}
		Assertions.assertEquals(Short.MIN_VALUE, read("{value: -32768}", NarrowShort.class).value());
		Assertions.assertEquals(Short.MAX_VALUE, read("{value: 32767}", NarrowShort.class).value());
		Assertions.assertThrows(IOException.class, () -> read("{value: -32769}", NarrowShort.class));
		Assertions.assertThrows(IOException.class, () -> read("{value: 32768}", NarrowShort.class));

		record CharacterValue(char value) {}
		Assertions.assertEquals('x', read("{value: 'x'}", CharacterValue.class).value());
	}

	@Test
	public void rejectsMissingRecordComponents() {
		record Point(int x, int y) {}

		IOException error = Assertions.assertThrows(IOException.class, () -> read("{x: 1}", Point.class));
		Assertions.assertTrue(error.getMessage().contains("Missing required record component")
				|| error.getCause() != null && error.getCause().getMessage().contains("Missing required record component"));
	}

	@Test
	public void emitsRecordCommentLinesImmediatelyBeforeKey() throws IOException, SyntaxError {
		StructuredDataReader reader = new ObjectReaderFactory().getReader(new CommentedRecord("value"));
		List<StructuredData> events = new ArrayList<>();
		while (reader.hasNext()) events.add(reader.next());

		Assertions.assertEquals(StructuredData.Type.COMMENT, events.get(1).type());
		Assertions.assertEquals("first", events.get(1).asComment().getValue());
		Assertions.assertEquals(CommentType.LINE_END, events.get(1).asComment().getCommentType());
		Assertions.assertEquals(StructuredData.Type.COMMENT, events.get(2).type());
		Assertions.assertEquals("second", events.get(2).asComment().getValue());
		Assertions.assertEquals(StructuredData.Type.OBJECT_KEY, events.get(3).type());
		Assertions.assertEquals("value", events.get(3).value());
	}

	@Test
	public void rejectsDuplicateWireNamesInBothDirections() {
		class Duplicate {
			@SerializedName("same") int first;
			@SerializedName("same") int second;
		}

		Assertions.assertThrows(IllegalArgumentException.class,
				() -> new ObjectReaderFactory().getReader(new Duplicate()));
		Assertions.assertThrows(IllegalArgumentException.class,
				() -> new ObjectWriter<>(Duplicate.class).write(StructuredData.OBJECT_START));
	}

	@Test
	public void serializesEnumsUsingWireName() throws IOException, SyntaxError {
		StructuredDataReader reader = new ObjectReaderFactory().getReader(Mode.SLOW);
		StructuredData value = reader.next();
		Assertions.assertEquals(StructuredData.Type.PRIMITIVE, value.type());
		Assertions.assertEquals("slow-mode", value.value());
	}

	@Test
	public void resolvesInheritedAndRecordGenericTypes() throws Exception {
		StringChild child = read("{values: ['a', 'b']}", StringChild.class);
		Assertions.assertEquals(List.of("a", "b"), ((GenericBase<String>) child).values);

		Type type = MapperFoundationTests.class.getField("boxType").getGenericType();
		ObjectWriter<Box<List<String>>> writer = new ObjectWriter<>(type);
		new JsonReader(new StringReader("{value: ['a', 'b']}")).transferTo(writer);
		Assertions.assertEquals(List.of("a", "b"), writer.toObject().value());

		NestedGenericChild nested = read("{value: [['a'], ['b']]}", NestedGenericChild.class);
		Assertions.assertEquals(List.of(List.of("a"), List.of("b")), ((NestedGenericBase<List<String>>) nested).value);

		StringArrayChild array = read("{values: ['a', 'b']}", StringArrayChild.class);
		Assertions.assertArrayEquals(new String[] {"a", "b"}, ((GenericArrayBase<String>) array).values);
	}

	@Test
	public void keepsInitializedFinalFieldsWhileLoadingMutableFields() throws Exception {
		MixedFinalConfig value = read("{kind: 'attempted-change', port: 25565}", MixedFinalConfig.class);
		Assertions.assertEquals("fixed", value.kind);
		Assertions.assertEquals(25565, value.port);

		FinalMutatorChild mutated = read("{values: ['updated']}", FinalMutatorChild.class);
		Assertions.assertEquals(List.of("updated"), ((FinalMutatorParent) mutated).values);
	}

	@Test
	public void runtimeSubtypeSerializerStillAppliesToFields() throws Exception {
		ObjectReaderFactory factory = new ObjectReaderFactory();
		factory.registerSerializer(Dog.class,
				(java.util.function.Function<Dog, blue.endless.jankson.api.document.ValueElement>) dog ->
						blue.endless.jankson.api.document.PrimitiveElement.of("custom-dog"));
		StructuredDataReader reader = factory.getReader(new Zoo());
		List<StructuredData> events = new ArrayList<>();
		while (reader.hasNext()) events.add(reader.next());
		Assertions.assertTrue(events.stream().anyMatch(event -> event.type() == StructuredData.Type.PRIMITIVE
				&& "custom-dog".equals(event.value())));
		Assertions.assertEquals(PrimitiveElement.of("custom-dog"),
				ConfigCodecs.<Animal>reflective(Animal.class, factory).encode(new Dog()));
	}

	@Test
	public void roundTripsEnumMapKeysUsingWireNames() throws Exception {
		EnumMapConfig value = read("{values: {'slow-mode': 'selected'}}", EnumMapConfig.class);
		Assertions.assertEquals(Map.of(Mode.SLOW, "selected"), value.values);

		StructuredDataReader reader = new ObjectReaderFactory().getReader(value);
		List<String> keys = new ArrayList<>();
		while (reader.hasNext()) {
			StructuredData event = reader.next();
			if (event.type() == StructuredData.Type.OBJECT_KEY) keys.add(event.value().toString());
		}
		Assertions.assertTrue(keys.contains("slow-mode"));
	}

	@Test
	public void retainsClassConstructorForRecordDeserializerCompatibility() throws Exception {
		Assertions.assertNotNull(blue.endless.jankson.impl.io.objectwriter.RecordDeserializer.class
				.getConstructor(Class.class));
	}

	@Test
	public void mapsLargeNumberTypesWithoutInvalidCasts() throws Exception {
		LargeNumbers value = read("{integer: 'ff', decimal: '123.456'}", LargeNumbers.class);
		Assertions.assertEquals(new BigInteger("ff", 16), value.integer);
		Assertions.assertEquals(new BigDecimal("123.456"), value.decimal);
	}

	private static <T> T read(String json, Class<T> type) throws IOException, SyntaxError {
		JsonReader reader = new JsonReader(new StringReader(json));
		ObjectWriter<T> writer = new ObjectWriter<>(type);
		reader.transferTo(writer);
		return writer.toObject();
	}

	private static List<String> semanticKeys(StructuredDataReader reader) throws IOException, SyntaxError {
		List<String> result = new ArrayList<>();
		int depth = 0;
		while (reader.hasNext()) {
			StructuredData event = reader.next();
			if (event.type() == StructuredData.Type.OBJECT_START || event.type() == StructuredData.Type.ARRAY_START) depth++;
			if (event.type() == StructuredData.Type.OBJECT_KEY && depth == 1) result.add(event.value().toString());
			if (event.type() == StructuredData.Type.OBJECT_END || event.type() == StructuredData.Type.ARRAY_END) depth--;
		}
		return result;
	}
}
