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

package blue.endless.jankson.api.config;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import blue.endless.jankson.api.Jankson;
import blue.endless.jankson.api.document.ArrayElement;
import blue.endless.jankson.api.document.CommentType;
import blue.endless.jankson.api.document.KeyValuePairElement;
import blue.endless.jankson.api.document.ObjectElement;
import blue.endless.jankson.api.document.PrimitiveElement;
import blue.endless.jankson.api.document.ValueElement;
import blue.endless.jankson.api.io.ObjectReaderFactory;
import blue.endless.jankson.api.io.StructuredData;
import blue.endless.jankson.api.io.StructuredDataBuffer;
import blue.endless.jankson.api.io.StructuredDataReader;
import blue.endless.jankson.api.io.json.JsonFormat;
import blue.endless.jankson.api.io.json.JsonReaderOptions;
import blue.endless.jankson.impl.config.ConfigDepthGuard;
import blue.endless.jankson.impl.io.objectreader.DelegatingStructuredDataReader;

class ConfigPipelineLimitsTests {
	@TempDir Path directory;

	public static class Node {
		public Object child;
		Node(Object child) { this.child = child; }
	}

	@Test void encodeBudgetValidatesPositiveLimitsAndExactTinyBoundaries() throws Exception {
		assertEquals(1_000_000L, ConfigCodecs.DEFAULT_MAX_ENCODE_EVENTS);
		for (long invalid : new long[] {0, -1, Long.MIN_VALUE}) {
			assertThrows(IllegalArgumentException.class,
					() -> ConfigCodecs.reflective(Object.class, new ObjectReaderFactory(), invalid));
		}
		assertEquals(PrimitiveElement.of(1L), limited(1).encode(1L));
		assertEquals(StructuredData.NULL.asPrimitive(), limited(1).encode(null));
		assertNotNull(limited(Long.MAX_VALUE).encode(List.of(1L, 2L)));
		assertEquals(0, ((ArrayElement) limited(2).encode(new Object[0])).size());
		assertThrows(IOException.class, () -> limited(1).encode(new Object[0]));
		assertEquals(2, ((ArrayElement) limited(4).encode(List.of(1L, 2L))).size());
		assertThrows(IOException.class, () -> limited(3).encode(List.of(1L, 2L)));
	}

	@Test void everyNonEofEventCountsBeforeForwardingAndParserTransferIsUncapped() throws Exception {
		List<StructuredData> events = List.of(StructuredData.OBJECT_START,
				StructuredData.objectKey("key"), StructuredData.ARRAY_START,
				StructuredData.comment("comment", CommentType.MULTILINE), StructuredData.whitespace(" "),
				StructuredData.NEWLINE, StructuredData.NULL, StructuredData.ARRAY_END,
				StructuredData.OBJECT_END, StructuredData.EOF);
		for (int budget = 1; budget < events.size() - 1; budget++) {
			int limit = budget;
			List<StructuredData> forwarded = new ArrayList<>();
			assertThrows(IOException.class, () -> ConfigDepthGuard.transfer(events(events), forwarded::add, limit));
			assertEquals(events.subList(0, limit), forwarded);
		}
		List<StructuredData> forwarded = new ArrayList<>();
		ConfigDepthGuard.transfer(events(events), forwarded::add, events.size() - 1);
		assertEquals(events, forwarded); // EOF is forwarded without consuming a slot.
		forwarded.clear();
		ConfigDepthGuard.transfer(events(events), forwarded::add);
		assertEquals(events, forwarded);
		ObjectReaderFactory factory = new ObjectReaderFactory();
		factory.register((java.lang.reflect.Type) Node.class, node -> events(events));
		assertNotNull(ConfigCodecs.<Node>reflective(Node.class, factory, 9).encode(new Node(null)));
		assertThrows(IOException.class,
				() -> ConfigCodecs.<Node>reflective(Node.class, factory, 8).encode(new Node(null)));
	}

	@Test void sharedOccurrencesConsumeBudgetWithoutRejectingSharing() throws Exception {
		Object shared = List.of(1L);
		Object pair = List.of(shared, shared); // 2 + 3 + 3 events, despite shared identity.
		assertEquals(2, ((ArrayElement) limited(8).encode(pair)).size());
		assertThrows(IOException.class, () -> limited(7).encode(pair));
		Object dag = 1L;
		for (int i = 0; i < 12; i++) dag = List.of(dag, dag);
		Object expanded = dag;
		assertTrue(assertThrows(IOException.class, () -> limited(100).encode(expanded))
				.getMessage().contains("events"));
	}

	@Test void encodeCountersAreFreshAfterSuccessFailureAndConcurrentCalls() throws Exception {
		ConfigCodec<Object> codec = limited(4);
		var executor = Executors.newFixedThreadPool(4);
		try {
			List<Callable<Void>> tasks = new ArrayList<>();
			for (int i = 0; i < 32; i++) tasks.add(() -> {
				for (int repeat = 0; repeat < 4; repeat++) {
					assertEquals(2, ((ArrayElement) codec.encode(List.of(1L, 2L))).size());
					assertThrows(IOException.class, () -> codec.encode(List.of(1L, 2L, 3L)));
					assertEquals(PrimitiveElement.of(1L), codec.encode(1L));
				}
				return null;
			});
			for (var future : executor.invokeAll(tasks)) future.get();
		} finally {
			executor.shutdownNow();
		}
	}

	@Test void encodeBudgetFailurePreservesManagedPublicationAndMissingCreation() throws Exception {
		Path path = directory.resolve("config.json");
		ConfigCodec<Long[]> codec = ConfigCodecs.reflective(Long[].class, new ObjectReaderFactory(), 3);
		Long[] original = {1L}; // Array start, primitive, array end: exactly three events.
		Long[] replacement = {1L, 2L}; // Four events: exceeds the budget by one.
		var manager = ConfigManager.builder(path, codec).creationDefaults(() -> original).build();
		assertSame(original, manager.loadOrCreate());
		FileRevision revision = manager.revision();
		byte[] bytes = Files.readAllBytes(path);
		IOException replacementFailure = assertInstanceOf(IOException.class,
				assertFailure(ConfigStage.ENCODE, () -> manager.replaceAndSave(replacement)).getCause());
		assertTrue(replacementFailure.getMessage().contains("events"));
		assertSame(original, manager.current());
		assertSame(revision, manager.revision());
		assertArrayEquals(bytes, Files.readAllBytes(path));
		assertNoTemporaryFiles();
		Files.delete(path);
		var missing = ConfigManager.builder(path, codec).creationDefaults(() -> replacement).build();
		IOException creationFailure = assertInstanceOf(IOException.class,
				assertFailure(ConfigStage.ENCODE, missing::loadOrCreate).getCause());
		assertTrue(creationFailure.getMessage().contains("events"));
		assertFalse(missing.isLoaded());
		assertFalse(Files.exists(path));
		assertNoTemporaryFiles();
	}

	@Test void registeredDelegatingSubclassOverridesAreRespectedAtRootAndAsDelegate() throws Exception {
		ObjectReaderFactory factory = new ObjectReaderFactory();
		factory.register((java.lang.reflect.Type) Node.class, node -> new InheritedOverrideReader());
		var codec = ConfigCodecs.<Object>reflective(Object.class, factory, 3);
		assertEquals(PrimitiveElement.of(7L), codec.encode(new Node(null)));
		assertEquals(PrimitiveElement.of(7L), ((ArrayElement) codec.encode(List.of(new Node(null)))).get(0));
		assertThrows(IOException.class, () -> ConfigCodecs.<Object>reflective(Object.class, factory, 2)
				.encode(List.of(new Node(null))));
	}

	@Test void eitherPublicOverrideDisablesBaseProtocolBypass() throws Exception {
		StructuredDataReader nextOnly = new DelegatingStructuredDataReader() {
			{ buffer(StructuredData.NULL); }
			@Override public StructuredData next() { return StructuredData.primitive(7L); }
			@Override protected void onDelegateEmpty() { fail("Must respect next override"); }
		};
		assertEquals(StructuredData.primitive(7L), DelegatingStructuredDataReader.iterative(nextOnly).next());
		StructuredDataReader hasNextOnly = new DelegatingStructuredDataReader() {
			{ buffer(StructuredData.NULL); }
			@Override public boolean hasNext() { return false; }
			@Override protected void onDelegateEmpty() { fail("Must respect hasNext override"); }
		};
		var adapted = DelegatingStructuredDataReader.iterative(hasNextOnly);
		assertEquals(StructuredData.EOF, adapted.next());
		assertFalse(adapted.hasNext());
	}

	private static class OverrideReader extends DelegatingStructuredDataReader {
		private boolean available = true;
		@Override public boolean hasNext() { return available; }
		@Override public StructuredData next() {
			if (!available) return StructuredData.EOF;
			available = false;
			return StructuredData.primitive(7L);
		}
		@Override protected void onDelegateEmpty() { fail("Must respect public reader overrides"); }
	}

	private static final class InheritedOverrideReader extends OverrideReader {}

	private static ConfigCodec<Object> limited(long budget) {
		return ConfigCodecs.reflective(Object.class, new ObjectReaderFactory(), budget);
	}

	private static StructuredDataReader events(List<StructuredData> events) {
		StructuredDataBuffer result = new StructuredDataBuffer();
		for (StructuredData event : events) result.write(event);
		return result;
	}

	@ParameterizedTest @ValueSource(strings = {"array", "object", "mixed"})
	void exactLoadDepthIncludesPrimitivesAndEmptyContainers(String shape) throws Exception {
		var file = document();
		for (String leaf : List.of("0", "null", "[]", "{}")) {
			Files.writeString(file.path(), nestedText(256, shape, leaf));
			assertNotNull(file.load().value());
			Files.writeString(file.path(), nestedText(257, shape, leaf));
			assertFailure(ConfigStage.PARSE, file::load);
		}
		assertNoTemporaryFiles();
	}

	@ParameterizedTest @ValueSource(strings = {"array", "object", "mixed"})
	void veryDeepLoadFailsContextuallyBeforeRecursiveTreeConstruction(String shape) throws Exception {
		var file = document();
		String text = nestedText(20_000, shape, "0");
		Files.writeString(file.path(), text);
		ConfigFileException failure = assertFailure(ConfigStage.PARSE, file::load);
		assertTrue(failure.getCause().getMessage().contains("256"));
		assertEquals(text, Files.readString(file.path()));
		assertNoTemporaryFiles();
	}

	@Test void duplicatesAreRejectedRecursively() throws Exception {
		var file = document();
		Files.writeString(file.path(), "{\"a\":[{\"b\":1,\"b\":2}]}");
		assertTrue(assertFailure(ConfigStage.PARSE, file::load).getCause().getMessage().contains("Duplicate"));
		ObjectElement duplicate = new ObjectElement();
		duplicate.add(new KeyValuePairElement("b", PrimitiveElement.of(1L)));
		duplicate.add(new KeyValuePairElement("b", PrimitiveElement.of(2L)));
		ArrayElement root = new ArrayElement();
		root.add(duplicate);
		assertFailure(ConfigStage.SERIALIZE, () -> file.overwrite(root));
		assertEquals("{\"a\":[{\"b\":1,\"b\":2}]}", Files.readString(file.path()));
	}

	@ParameterizedTest @EnumSource(JsonFormat.class)
	void configDepthBoundarySurvivesGeneratedDocumentParseForEveryFormat(JsonFormat format) throws Exception {
		Path path = directory.resolve("boundary.conf");
		var file = ConfigFile.builder(path, ConfigCodecs.document()).format(format).build();
		for (ValueElement leaf : List.of(new ArrayElement(), new ObjectElement())) {
			ValueElement accepted = nestedDocument(256, leaf);
			assertSame(accepted, file.overwrite(accepted).value());
			assertNotNull(file.load().value());
			byte[] before = Files.readAllBytes(path);
			ValueElement rejected = nestedDocument(257, leaf);
			assertEquals(ConfigStage.SERIALIZE,
					assertThrows(ConfigFileException.class, () -> file.overwrite(rejected)).stage());
			assertArrayEquals(before, Files.readAllBytes(path));
		}
		assertNoTemporaryFiles();
	}

	@Test void parserContainerLimitDefaultsAndOptionCopiesRemainIndependentOfConfigValueDepth() throws Exception {
		JsonReaderOptions defaults = JsonFormat.JSON.readerOptions();
		assertEquals(256, defaults.getMaxContainerDepth());
		String containers = nestedText(256, "array", "[]");
		assertThrows(IOException.class, () -> Jankson.readJson(containers, defaults));
		JsonReaderOptions expanded = defaults.asBuilder().setMaxContainerDepth(257).build();
		assertEquals(257, expanded.asBuilder().build().getMaxContainerDepth());
		assertNotNull(Jankson.readJson(containers, expanded));
		assertEquals(256, defaults.getMaxContainerDepth());
		assertThrows(IllegalArgumentException.class, () -> defaults.asBuilder().setMaxContainerDepth(0));
		assertThrows(IllegalArgumentException.class, () -> defaults.asBuilder().setMaxContainerDepth(-1));
	}

	@Test void returnedDocumentDepthIsCheckedBeforeSerialization() throws Exception {
		var file = document();
		ValueElement accepted = nestedDocument(256, PrimitiveElement.of(0L));
		assertSame(accepted, file.overwrite(accepted).value());
		byte[] before = Files.readAllBytes(file.path());
		for (int depth : new int[] {257, 20_000}) {
			ValueElement rejected = nestedDocument(depth, PrimitiveElement.of(0L));
			assertFailure(ConfigStage.SERIALIZE, () -> file.overwrite(rejected));
			assertArrayEquals(before, Files.readAllBytes(file.path()));
		}
		assertNoTemporaryFiles();
	}

	@Test void cyclicDocumentsFailButSharedNoncyclicNodesWork() throws Exception {
		var file = document();
		file.overwrite(PrimitiveElement.of(3L));
		ArrayElement array = new ArrayElement();
		array.add(array);
		ObjectElement object = new ObjectElement();
		object.add(new KeyValuePairElement("self", object));
		for (ValueElement cyclic : List.of(array, object)) {
			assertTrue(assertFailure(ConfigStage.SERIALIZE, () -> file.overwrite(cyclic))
					.getCause().getMessage().contains("Cyclic"));
		}
		assertEquals("3", Files.readString(file.path()).trim());
		ArrayElement shared = new ArrayElement();
		shared.add(PrimitiveElement.of(7L));
		ArrayElement root = new ArrayElement();
		root.add(shared);
		root.add(shared);
		assertSame(root, file.overwrite(root).value());
		assertEquals(2, ((ArrayElement) file.load().value()).size());
		assertNoTemporaryFiles();
	}

	@Test void sharedSubtreeDepthIsCheckedAtEachOccurrenceWithoutExpandingTheDag() throws Exception {
		ValueElement shared = nestedDocument(2, PrimitiveElement.of(1L));
		ArrayElement root = new ArrayElement();
		root.add(shared);
		root.add(nestedDocument(254, shared)); // deepest primitive: 1 + 254 + 2
		assertThrows(IOException.class, () -> ConfigDepthGuard.checkDocument(root));
		ValueElement dag = PrimitiveElement.of(1L);
		for (int i = 0; i < 256; i++) {
			ArrayElement parent = new ArrayElement();
			parent.add(dag);
			parent.add(dag);
			dag = parent;
		}
		ConfigDepthGuard.checkDocument(dag);
	}

	@ParameterizedTest @ValueSource(strings = {"array", "collection", "map", "pojo"})
	void reflectiveEncodingChecksDepthBeforeRecursiveDelegateLookahead(String shape) throws Exception {
		ConfigCodec<Object> codec = ConfigCodecs.reflective(Object.class);
		Object accepted = nestedObject(256, shape);
		ConfigDepthGuard.checkDocument(codec.encode(accepted));
		var file = ConfigFile.builder(directory.resolve("config.json"), codec).build();
		Files.writeString(file.path(), "0");
		for (int depth : new int[] {257, 20_000}) {
			Object rejected = nestedObject(depth, shape);
			assertFailure(ConfigStage.ENCODE, () -> file.overwrite(rejected));
		}
		assertEquals("0", Files.readString(file.path()));
		assertNoTemporaryFiles();
	}

	@Test void reflectiveCyclesAreBoundedAndSharedReferencesAreAccepted() throws Exception {
		Node node = new Node(null);
		node.child = node;
		Object[] array = new Object[1];
		array[0] = array;
		List<Object> list = new ArrayList<>();
		list.add(list);
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("self", map);
		var codec = ConfigCodecs.reflective(Object.class);
		var file = ConfigFile.builder(directory.resolve("config.json"), codec).build();
		Files.writeString(file.path(), "0");
		for (Object cycle : new Object[] {node, array, list, map}) {
			assertFailure(ConfigStage.ENCODE, () -> file.overwrite(cycle));
		}
		Node shared = new Node(1L);
		ArrayElement result = (ArrayElement) codec.encode(List.of(shared, shared));
		assertEquals(2, result.size());
		assertEquals(((ObjectElement) result.get(0)).get("child"), ((ObjectElement) result.get(1)).get("child"));
		assertEquals("0", Files.readString(file.path()));
		assertNoTemporaryFiles();
	}

	@Test void customSerializerReturnedDeepOrCyclicDocumentIsGuarded() throws Exception {
		ObjectReaderFactory factory = new ObjectReaderFactory();
		ArrayElement cycle = new ArrayElement();
		cycle.add(cycle);
		factory.registerSerializer((java.lang.reflect.Type) Node.class, node -> (ValueElement) ((Node) node).child);
		var file = ConfigFile.builder(directory.resolve("config.json"),
				ConfigCodecs.<Node>reflective(Node.class, factory)).build();
		for (ValueElement invalid : List.of(cycle, nestedDocument(20_000, PrimitiveElement.of(0L)))) {
			assertFailure(ConfigStage.ENCODE, () -> file.overwrite(new Node(invalid)));
		}
		assertFalse(Files.exists(file.path()));
		assertNoTemporaryFiles();
	}

	@Test void generatedNullDecodeCannotPublishOrReplaceManagedState() throws Exception {
		NullSwitchCodec codec = new NullSwitchCodec();
		Path path = directory.resolve("config.json");
		var manager = ConfigManager.builder(path, codec).creationDefaults(() -> new String("initial")).build();
		String original = manager.loadOrCreate();
		FileRevision revision = manager.revision();
		byte[] bytes = Files.readAllBytes(path);
		codec.decodeNull = true;
		assertFailure(ConfigStage.DECODE, manager::save);
		assertFailure(ConfigStage.DECODE, manager::overwrite);
		assertFailure(ConfigStage.DECODE, () -> manager.replaceAndSave("replacement"));
		assertFailure(ConfigStage.DECODE, manager::reload);
		assertSame(original, manager.current());
		assertSame(revision, manager.revision());
		assertArrayEquals(bytes, Files.readAllBytes(path));
		codec.decodeNull = false;
		manager.save();
		assertSame(original, manager.current());
		assertNoTemporaryFiles();
	}

	@Test void nullDecodeDuringCreationOrInitialLoadLeavesManagerUnloaded() throws Exception {
		NullSwitchCodec codec = new NullSwitchCodec();
		codec.decodeNull = true;
		Path path = directory.resolve("config.json");
		var manager = ConfigManager.builder(path, codec).creationDefaults(() -> "initial").build();
		assertFailure(ConfigStage.DECODE, manager::loadOrCreate);
		assertFalse(Files.exists(path));
		assertFalse(manager.isLoaded());
		Files.writeString(path, "\"on disk\"");
		assertFailure(ConfigStage.DECODE, manager::load);
		assertFalse(manager.isLoaded());
		assertEquals("\"on disk\"", Files.readString(path));
		assertNoTemporaryFiles();
	}

	@Test void temporaryFileIsRemovedAfterPublicationFailure() throws Exception {
		Path path = directory.resolve("config.json");
		Files.writeString(path, "0");
		var file = ConfigFile.builder(path, ConfigCodecs.document())
				.moveOperation((source, target, atomic) -> { throw new IOException("injected replacement failure"); }).build();
		assertFailure(ConfigStage.REPLACE, () -> file.overwrite(PrimitiveElement.of(1L)));
		assertEquals("0", Files.readString(path));
		assertNoTemporaryFiles();
	}

	@Test void failedDepthSaveAndReloadPreserveManagedIdentityAndRevision() throws Exception {
		Path path = directory.resolve("config.json");
		ArrayElement original = new ArrayElement();
		var manager = ConfigManager.builder(path, ConfigCodecs.document()).creationDefaults(() -> original).build();
		assertSame(original, manager.loadOrCreate());
		FileRevision revision = manager.revision();
		byte[] before = Files.readAllBytes(path);
		ValueElement deep = nestedDocument(20_000, PrimitiveElement.of(0L));
		assertFailure(ConfigStage.SERIALIZE, () -> manager.replaceAndSave(deep));
		assertArrayEquals(before, Files.readAllBytes(path));
		assertSame(original, manager.current());
		assertSame(revision, manager.revision());
		String external = nestedText(20_000, "mixed", "0");
		Files.writeString(path, external);
		assertFailure(ConfigStage.PARSE, manager::reload);
		assertSame(original, manager.current());
		assertSame(revision, manager.revision());
		assertEquals(external, Files.readString(path));
		assertNoTemporaryFiles();
	}

	private static final class NullSwitchCodec implements ConfigCodec<String> {
		boolean decodeNull;
		public String decode(ValueElement document) { return decodeNull ? null : "loaded"; }
		public ValueElement encode(String value) { return PrimitiveElement.of(value); }
	}

	private ConfigFile<ValueElement> document() {
		return ConfigFile.builder(directory.resolve("config.json"), ConfigCodecs.document()).build();
	}

	private ConfigFileException assertFailure(ConfigStage stage, org.junit.jupiter.api.function.Executable operation) {
		ConfigFileException failure = assertThrows(ConfigFileException.class, operation);
		assertEquals(stage, failure.stage());
		assertEquals(directory.resolve("config.json").toAbsolutePath().normalize(), failure.path());
		assertEquals(JsonFormat.JSON, failure.format());
		return failure;
	}

	private void assertNoTemporaryFiles() throws IOException {
		try (var paths = Files.list(directory)) {
			assertFalse(paths.anyMatch(path -> path.getFileName().toString().startsWith(".jankson-")));
		}
	}

	private static ValueElement nestedDocument(int depth, ValueElement leaf) {
		for (int i = 0; i < depth; i++) {
			ArrayElement parent = new ArrayElement();
			parent.add(leaf);
			leaf = parent;
		}
		return leaf;
	}

	private static Object nestedObject(int depth, String shape) {
		Object value = 0L;
		for (int i = 0; i < depth; i++) {
			value = switch (shape) {
				case "array" -> new Object[] {value};
				case "collection" -> List.of(value);
				case "map" -> Map.of("child", value);
				default -> new Node(value);
			};
		}
		return value;
	}

	private static String nestedText(int depth, String shape, String leaf) {
		StringBuilder result = new StringBuilder();
		for (int i = 0; i < depth; i++) result.append(isObject(shape, i) ? "{\"child\":" : "[");
		result.append(leaf);
		for (int i = depth - 1; i >= 0; i--) result.append(isObject(shape, i) ? "}" : "]");
		return result.toString();
	}

	private static boolean isObject(String shape, int index) {
		return shape.equals("object") || shape.equals("mixed") && index % 2 == 0;
	}
}
