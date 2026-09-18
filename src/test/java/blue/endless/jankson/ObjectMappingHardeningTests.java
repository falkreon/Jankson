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

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import blue.endless.jankson.api.Jankson;
import blue.endless.jankson.api.SyntaxError;
import blue.endless.jankson.api.annotation.Immutable;
import blue.endless.jankson.api.annotation.SerializedName;
import blue.endless.jankson.api.document.ArrayElement;
import blue.endless.jankson.api.document.CommentElement;
import blue.endless.jankson.api.document.CommentType;
import blue.endless.jankson.api.document.ObjectElement;
import blue.endless.jankson.api.document.PrimitiveElement;
import blue.endless.jankson.api.io.BufferedStructuredDataWriter;
import blue.endless.jankson.api.io.ObjectReaderFactory;
import blue.endless.jankson.api.io.ObjectWriter;
import blue.endless.jankson.api.io.StructuredData;
import blue.endless.jankson.api.io.StructuredDataReader;
import blue.endless.jankson.api.io.ValueElementReader;
import blue.endless.jankson.api.io.json.JsonReader;
import blue.endless.jankson.api.io.json.JsonWriterOptions;
import blue.endless.jankson.impl.io.objectreader.CollectionStructuredDataReader;
import blue.endless.jankson.impl.io.objectwriter.MapDeserializer;

class ObjectMappingHardeningTests {
	private static final class Node {
		Object child;
		Node(Object child) { this.child = child; }
	}
	private interface Link {}
	private static final class LinkedNode implements Link {
		Link child;
	}

	@Immutable
	private static final class PrivateImmutable {
		private final String first;
		private final int second;

		private PrivateImmutable(@SerializedName("first") String first, @SerializedName("second") int second) {
			this.first = first;
			this.second = second;
		}
	}

	@Test
	void mapSerializationRejectsNullAndDuplicateTextualKeysAsIoErrors() throws Exception {
		Map<Object, Object> nullKey = new LinkedHashMap<>();
		nullKey.put(null, "value");
		assertThrows(IOException.class, () -> drain(new ObjectReaderFactory().getReader(nullKey)));

		Map<Object, Object> duplicateText = new LinkedHashMap<>();
		duplicateText.put(1, "number");
		duplicateText.put("1", "string");
		IOException error = assertThrows(IOException.class,
				() -> drain(new ObjectReaderFactory().getReader(duplicateText)));
		assertTrue(error.getMessage().contains("Duplicate map key"));

		Map<Object, Object> nullText = new LinkedHashMap<>();
		nullText.put(new Object() { @Override public String toString() { return null; } }, "value");
		assertThrows(IOException.class, () -> drain(new ObjectReaderFactory().getReader(nullText)));
	}

	@Test
	void booleanMapKeysAreStrict() throws Exception {
		MapDeserializer<Boolean, String> valid = new MapDeserializer<>(Boolean.class, String.class);
		valid.write(StructuredData.OBJECT_START);
		valid.write(StructuredData.objectKey("true"));
		valid.write(StructuredData.primitive("yes"));
		valid.write(StructuredData.OBJECT_END);
		assertEquals(Map.of(true, "yes"), valid.getResult());

		MapDeserializer<Boolean, String> invalid = new MapDeserializer<>(Boolean.class, String.class);
		invalid.write(StructuredData.OBJECT_START);
		assertThrows(SyntaxError.class, () -> invalid.write(StructuredData.objectKey("TRUE")));
	}

	@Test
	void typedNullAlwaysProducesNullWithoutInvokingSerializer() throws Exception {
		AtomicBoolean invoked = new AtomicBoolean();
		ObjectReaderFactory factory = new ObjectReaderFactory();
		factory.registerSerializer((Type) String.class, value -> {
			invoked.set(true);
			return PrimitiveElement.of((String) value);
		});

		StructuredDataReader reader = factory.getReader((Type) String.class, null);
		StructuredData data = reader.next();
		assertEquals(StructuredData.Type.PRIMITIVE, data.type());
		assertNull(data.value());
		assertFalse(invoked.get());
	}

	@Test
	void immutableMissingArgumentsFailAtObjectEndAndPrivateConstructorIsUsed() throws Exception {
		ObjectWriter<PrivateImmutable> missing = new ObjectWriter<>(PrivateImmutable.class);
		missing.write(StructuredData.OBJECT_START);
		missing.write(StructuredData.objectKey("first"));
		missing.write(StructuredData.primitive("one"));
		IOException error = assertThrows(IOException.class, () -> missing.write(StructuredData.OBJECT_END));
		assertInstanceOf(SyntaxError.class, error.getCause());

		PrivateImmutable value = read("{first: 'one', second: 2}", PrivateImmutable.class);
		assertEquals("one", value.first);
		assertEquals(2, value.second);
	}

	@Test
	void reflectiveCyclesFailForPublicWritesAndDirectTraversalButSharedReferencesWork() throws Exception {
		Node node = new Node(null);
		node.child = node;
		Object[] array = new Object[1];
		array[0] = array;
		List<Object> list = new ArrayList<>();
		list.add(list);
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("self", map);

		for (Object cyclic : List.of(node, array, list, map)) {
			IOException direct = assertThrows(IOException.class,
					() -> drain(new ObjectReaderFactory().getReader(cyclic)));
			assertTrue(direct.getMessage().contains("Cyclic"));
			assertThrows(IOException.class, () -> Jankson.writeJsonString(
					cyclic, new ObjectReaderFactory(), JsonWriterOptions.ONE_LINE));
			assertThrows(IOException.class, () -> Jankson.writeJson(
					cyclic, new ObjectReaderFactory(), new StringWriter(), JsonWriterOptions.ONE_LINE));
		}

		Node shared = new Node("leaf");
		String json = Jankson.writeJsonString(List.of(shared, shared),
				new ObjectReaderFactory(), JsonWriterOptions.ONE_LINE);
		assertTrue(json.contains("leaf"));
	}

	@Test
	void traversalIsIterativeForDeepAndLazyCustomReaders() throws Exception {
		Object nested = "leaf";
		for (int i = 0; i < 20_000; i++) nested = new Object[] {nested};
		drain(new ObjectReaderFactory().getReader(nested));

		ObjectReaderFactory factory = new ObjectReaderFactory();
		factory.register((Type) Node.class,
				value -> new CollectionStructuredDataReader(List.of(value), factory));
		assertThrows(IOException.class, () -> drain(factory.getReader(new Node(null))));
	}

	@Test
	void cyclicValueReturnedByCustomSerializerIsRejected() {
		ArrayElement cycle = new ArrayElement();
		cycle.add(cycle);
		ObjectReaderFactory factory = new ObjectReaderFactory();
		factory.registerSerializer((Type) Node.class, ignored -> cycle);
		assertThrows(IOException.class, () -> drain(factory.getReader(new Node(null))));
	}

	@Test
	void eagerRecursiveReaderFactoryIsRejected() {
		ObjectReaderFactory factory = new ObjectReaderFactory();
		factory.register((Type) Node.class, value -> factory.getReader(value));
		assertThrows(IOException.class, () -> drain(factory.getReader(new Node(null))));
	}

	@Test
	void customSerializersCanBreakCyclesAndDelegateAcrossFactories() throws Exception {
		LinkedNode node = new LinkedNode();
		node.child = node;
		ObjectReaderFactory cycleBreaking = new ObjectReaderFactory();
		cycleBreaking.setPrecise(true);
		cycleBreaking.registerSerializer(Link.class,
				(java.util.function.Function<Link, blue.endless.jankson.api.document.ValueElement>)
				ignored -> PrimitiveElement.of("node-id"));
		String json = Jankson.writeJsonString(node, cycleBreaking, JsonWriterOptions.ONE_LINE);
		assertTrue(json.contains("node-id"));

		ObjectReaderFactory inner = new ObjectReaderFactory();
		inner.registerSerializer(Node.class,
				(java.util.function.Function<Node, blue.endless.jankson.api.document.ValueElement>)
				ignored -> PrimitiveElement.of("delegated"));
		ObjectReaderFactory outer = new ObjectReaderFactory();
		outer.register(Node.class,
				(java.util.function.Function<Node, StructuredDataReader>) inner::getReader);
		assertEquals("\"delegated\"", Jankson.writeJsonString(
				new Node(null), outer, JsonWriterOptions.ONE_LINE));
	}

	@Test
	void bufferedValueElementTransferPreservesDecorations() throws Exception {
		ObjectElement value = new ObjectElement();
		value.put("key", PrimitiveElement.of("value"));
		value.getPrologue().add(new CommentElement("before", CommentType.LINE_END));
		AtomicReference<blue.endless.jankson.api.document.ValueElement> captured = new AtomicReference<>();
		ValueElementReader.of(value).transferTo(BufferedStructuredDataWriter.of(captured::set));
		assertSame(value, captured.get());
		assertEquals("before", captured.get().getPrologue().getFirst().asCommentElement().getValue());
	}

	private static void drain(StructuredDataReader reader) throws Exception {
		while (reader.hasNext()) reader.next();
	}

	private static <T> T read(String json, Class<T> type) throws Exception {
		ObjectWriter<T> writer = new ObjectWriter<>(type);
		new JsonReader(new StringReader(json)).transferTo(writer);
		return writer.toObject();
	}
}
