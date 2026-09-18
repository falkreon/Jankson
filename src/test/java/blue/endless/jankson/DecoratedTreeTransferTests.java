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
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import blue.endless.jankson.api.SyntaxError;
import blue.endless.jankson.api.document.ArrayElement;
import blue.endless.jankson.api.document.CommentElement;
import blue.endless.jankson.api.document.FormattingElement;
import blue.endless.jankson.api.document.ObjectElement;
import blue.endless.jankson.api.document.PrimitiveElement;
import blue.endless.jankson.api.document.ValueElement;
import blue.endless.jankson.api.io.BufferedStructuredDataWriter;
import blue.endless.jankson.api.io.ObjectReaderFactory;
import blue.endless.jankson.api.io.StructuredData;
import blue.endless.jankson.api.io.StructuredDataBuffer;
import blue.endless.jankson.api.io.StructuredDataReader;
import blue.endless.jankson.api.io.ValueElementReader;
import blue.endless.jankson.api.io.json.JsonWriter;
import blue.endless.jankson.api.io.toml.TomlWriter;

class DecoratedTreeTransferTests {
	private record Serialized(ValueElement tree) {}

	private static StructuredDataReader registered(ValueElement tree) {
		ObjectReaderFactory factory = new ObjectReaderFactory();
		factory.registerSerializer(Serialized.class, Serialized::tree);
		return factory.getReader(new Serialized(tree));
	}

	private static <T extends ValueElement> T decorated(T value, String name) {
		value.getPrologue().add(new CommentElement(name+"-before"));
		value.getEpilogue().add(new CommentElement(name+"-after"));
		return value;
	}

	private static List<ValueElement> trees() {
		ValueElement primitive = decorated(PrimitiveElement.of(7), "primitive");
		ArrayElement array = decorated(new ArrayElement(), "array");
		array.add(decorated(PrimitiveElement.of("entry"), "entry"));
		array.getFooter().add(new CommentElement("array-footer"));
		ObjectElement object = decorated(new ObjectElement(), "object");
		object.put("items", array);
		object.getKeyValuePair("items").orElseThrow().getPrologue().add(new CommentElement("property-before"));
		object.getFooter().add(new CommentElement("object-footer"));
		return List.of(primitive, array,
				decorated(new ArrayElement(), "empty-array"),
				decorated(new ObjectElement(), "empty-object"),
				decorated(PrimitiveElement.ofNull(), "null"), object);
	}

	private static String json(ValueElement tree) throws Exception {
		StringWriter output = new StringWriter();
		JsonWriter writer = new JsonWriter(output);
		tree.write(writer);
		writer.write(StructuredData.EOF);
		return output.toString();
	}

	private static String json(StructuredDataReader reader) throws Exception {
		StringWriter output = new StringWriter();
		JsonWriter writer = new JsonWriter(output);
		reader.transferTo(writer);
		writer.write(StructuredData.EOF);
		return output.toString();
	}

	@Test void directAndRegisteredReadersMatchTreeJsonIncludingEveryComment() throws Exception {
		for (ValueElement tree : trees()) {
			assertEquals(json(tree), json(ValueElementReader.of(tree)));
			assertEquals(json(tree), json(registered(tree)));
		}
	}

	@Test void bufferedCallbacksSeeTrailingCommentsAndRunExactlyOnce() throws Exception {
		for (ValueElement tree : trees()) {
			for (StructuredDataReader reader : List.of(ValueElementReader.of(tree), registered(tree))) {
				List<String> rendered = new ArrayList<>();
				BufferedStructuredDataWriter writer = BufferedStructuredDataWriter.of(value -> {
					try { rendered.add(json(value)); }
					catch (Exception ex) { throw new IOException(ex); }
				});
				reader.transferTo(writer);
				writer.write(StructuredData.EOF);
				assertEquals(List.of(json(tree)), rendered);
				assertFalse(reader.hasNext());
			}
		}
	}

	@Test void eventTraversalPreservesWhitespaceAndPropertyDecorationsInOrder() throws Exception {
		ValueElement tree = trees().getLast();
		tree.getPrologue().add(FormattingElement.NEWLINE);
		tree.getEpilogue().add(FormattingElement.NEWLINE);
		List<StructuredData> expected = new ArrayList<>();
		tree.write(expected::add);
		for (StructuredDataReader reader : List.of(ValueElementReader.of(tree), registered(tree))) {
			List<StructuredData> actual = new ArrayList<>();
			reader.transferTo(data -> { if (data.type() != StructuredData.Type.EOF) actual.add(data); });
			assertEquals(expected, actual);
		}
	}

	@Test void directEventsDoNotRedeliverAfterEofOrTrivia() throws Exception {
		for (ValueElement tree : trees()) {
			AtomicInteger calls = new AtomicInteger();
			BufferedStructuredDataWriter writer = BufferedStructuredDataWriter.of(value -> calls.incrementAndGet());
			tree.write(writer);
			writer.write(StructuredData.EOF);
			writer.write(new StructuredData(StructuredData.Type.COMMENT, new CommentElement("late")));
			writer.write(StructuredData.NEWLINE);
			writer.write(StructuredData.EOF);
			assertEquals(1, calls.get());
			assertThrows(SyntaxError.class, () -> writer.write(StructuredData.NULL));
		}
	}

	@Test void readersWithoutEofStillFinalizePrimitivesAndTrailingComments() throws Exception {
		StructuredDataBuffer events = new StructuredDataBuffer();
		ValueElement tree = trees().getFirst();
		tree.write(events);
		List<String> comments = new ArrayList<>();
		events.transferTo(BufferedStructuredDataWriter.of(value ->
				comments.add(value.getEpilogue().getFirst().asCommentElement().getValue())));
		assertEquals(List.of("primitive-after"), comments);
		AtomicInteger calls = new AtomicInteger();
		StructuredDataReader.of(StructuredData.primitive(PrimitiveElement.of(1)))
				.transferTo(BufferedStructuredDataWriter.of(value -> calls.incrementAndGet()));
		assertEquals(1, calls.get());
	}

	@Test void registeredTomlOutputIsNotDuplicatedByEof() throws Exception {
		ObjectElement tree = decorated(new ObjectElement(), "root");
		tree.put("answer", PrimitiveElement.of(42));
		StringWriter expected = new StringWriter();
		new TomlWriter(expected).write(tree);
		for (StructuredDataReader reader : List.of(ValueElementReader.of(tree), registered(tree))) {
			StringWriter actual = new StringWriter();
			reader.transferTo(new TomlWriter(actual));
			assertEquals(expected.toString(), actual.toString());
		}
	}

	@Test void partialTransfersDoNotReplayConsumedEventsOrAnExhaustedRoot() throws Exception {
		ObjectElement tree = new ObjectElement();
		tree.put("answer", PrimitiveElement.of(42));
		for (StructuredDataReader reader : List.of(ValueElementReader.of(tree), registered(tree))) {
			StringWriter output = new StringWriter();
			JsonWriter writer = new JsonWriter(output);
			writer.write(reader.next());
			reader.transferTo(writer);
			writer.write(StructuredData.EOF);
			assertEquals(json(tree), output.toString());
			AtomicInteger calls = new AtomicInteger();
			reader.transferTo(BufferedStructuredDataWriter.of(value -> calls.incrementAndGet()));
			assertEquals(0, calls.get());
		}
		StructuredDataReader partial = ValueElementReader.of(tree);
		partial.next();
		assertThrows(IOException.class, () -> partial.transferTo(BufferedStructuredDataWriter.of(value -> fail("Replayed root"))));
		StructuredDataReader continuing = ValueElementReader.of(tree);
		AtomicInteger calls = new AtomicInteger();
		BufferedStructuredDataWriter buffered = BufferedStructuredDataWriter.of(value -> calls.incrementAndGet());
		buffered.write(continuing.next());
		continuing.transferTo(buffered);
		assertEquals(1, calls.get());
	}

	@Test void deeplyNestedTreeTraversalUsesAnExplicitStack() throws Exception {
		ValueElement tree = PrimitiveElement.of(1);
		for (int i = 0; i < 20_000; i++) {
			ArrayElement parent = new ArrayElement();
			parent.add(tree);
			tree = parent;
		}
		for (StructuredDataReader reader : List.of(ValueElementReader.of(tree), registered(tree))) {
			AtomicInteger treeEvents = new AtomicInteger();
			AtomicInteger eofEvents = new AtomicInteger();
			reader.transferTo(data -> {
				if (data.type() == StructuredData.Type.EOF) eofEvents.incrementAndGet();
				else if (data.type().isSemantic()) treeEvents.incrementAndGet();
			});
			assertEquals(40_001, treeEvents.get());
			assertEquals(1, eofEvents.get());
		}
	}

	@Test void deferredBufferedTransferRejectsASecondRoot() throws Exception {
		StructuredDataBuffer events = new StructuredDataBuffer();
		events.write(StructuredData.OBJECT_START);
		events.write(StructuredData.OBJECT_END);
		events.write(StructuredData.NULL);
		assertThrows(SyntaxError.class, () -> events.transferTo(BufferedStructuredDataWriter.of(value -> fail("Delivered invalid stream"))));
	}

	@Test void cyclesAreRejectedBeforeBufferedDeliveryButSharedChildrenAreAllowed() throws Exception {
		ArrayElement cycle = new ArrayElement();
		cycle.add(cycle);
		for (StructuredDataReader reader : List.of(ValueElementReader.of(cycle), registered(cycle))) {
			assertThrows(IOException.class, () -> reader.transferTo(BufferedStructuredDataWriter.of(value -> fail("Delivered cyclic tree"))));
		}
		assertThrows(IOException.class, () -> json(ValueElementReader.of(cycle)));
		assertThrows(IOException.class, () -> json(registered(cycle)));
		ArrayElement shared = new ArrayElement();
		ValueElement child = trees().get(1);
		shared.add(child);
		shared.add(child);
		assertEquals(json(shared), json(registered(shared)));
	}
}
