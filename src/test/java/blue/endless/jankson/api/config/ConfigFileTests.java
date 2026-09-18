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
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import blue.endless.jankson.api.Jankson;
import blue.endless.jankson.api.document.ObjectElement;
import blue.endless.jankson.api.document.PrimitiveElement;
import blue.endless.jankson.api.document.ValueElement;
import blue.endless.jankson.api.io.ObjectReaderFactory;
import blue.endless.jankson.api.io.json.JsonFormat;
import blue.endless.jankson.api.io.json.JsonWriterOptions;

class ConfigFileTests {
	@TempDir Path directory;
	public record Server(int port, String host) {}
	public static class MutableServer {
		public int port = 25565;
		public String host = "localhost";
	}
	public List<Server> servers;

	@ParameterizedTest @EnumSource(JsonFormat.class)
	void createsAndUsesFixedFormat(JsonFormat format) throws Exception {
		Path path = directory.resolve("settings.conf");
		var file = ConfigFile.builder(path, ConfigCodecs.reflective(Server.class)).format(format).build();
		var original = new Server(25565, "localhost");
		var created = file.loadOrCreate(() -> original);
		assertEquals(ConfigOrigin.CREATED, created.origin());
		assertEquals(format, file.format());
		assertEquals(original, file.load().value());
		Jankson.read(path, format);
		var updated = new Server(25566, "example.org");
		var saved = file.save(updated, created.revision());
		assertEquals(updated, file.load().value());
		assertEquals(saved.revision(), file.load().revision());
		assertThrows(ConfigConflictException.class, () -> file.save(original, created.revision()));
		assertNoTemporaryFiles();
	}

	@Test void selectsExtensionAndRejectsConflictingOptions() {
		assertEquals(JsonFormat.JSONC, document("config.JSONC").format());
		assertThrows(IllegalArgumentException.class, () -> document("config.conf"));
		assertThrows(IllegalArgumentException.class, () -> ConfigFile.builder(directory.resolve("x.jsonc"), ConfigCodecs.document())
				.readerOptions(JsonFormat.HJSON.readerOptions()).build());
		assertThrows(IllegalArgumentException.class, () -> ConfigFile.builder(directory.resolve("x.jsonc"), ConfigCodecs.document())
				.writerOptions(JsonFormat.JSON5.writerOptions()).build());
	}

	@Test void neverDefaultsOnMalformedInputOrDuplicates() throws Exception {
		var file = document("config.jsonc");
		for (String text : List.of("{unquoted: 1}", "{\"key\":1,\"key\":2}", "{} garbage")) {
			Files.writeString(file.path(), text);
			assertThrows(ConfigFileException.class, () -> file.loadOrCreate(() -> { fail("defaults invoked"); return null; }));
			assertEquals(text, Files.readString(file.path()));
		}
		Files.write(file.path(), new byte[] {(byte) 0xc3, 0x28});
		assertThrows(ConfigFileException.class, file::load);
	}

	@Test void jsoncOptionsAndExplicitOverride() throws Exception {
		Path path = directory.resolve("config.hjson");
		Files.writeString(path, "{\"port\":1,}");
		var file = ConfigFile.builder(path, ConfigCodecs.document()).format(JsonFormat.JSONC)
				.readerOptions(JsonFormat.JSONC.readerOptions().asBuilder().setAllowTrailingCommas(true).build()).build();
		file.load();
		Files.writeString(path, "port: 1");
		assertThrows(ConfigFileException.class, file::load);
	}

	@Test void failedSerializationValidationAndMovePreserveFile() throws Exception {
		var file = document("config.json");
		file.overwrite(PrimitiveElement.of(42L));
		String before = Files.readString(file.path());
		assertThrows(ConfigFileException.class, () -> file.overwrite(PrimitiveElement.of(Double.NaN)));
		assertEquals(before, Files.readString(file.path()));
		var failing = ConfigFile.builder(file.path(), ConfigCodecs.document())
				.moveOperation((source, target, atomic) -> { throw new IOException("injected move failure"); }).build();
		assertEquals(ConfigStage.REPLACE, assertThrows(ConfigFileException.class,
				() -> failing.overwrite(PrimitiveElement.of(43L))).stage());
		assertEquals(before, Files.readString(file.path()));
		var invalid = ConfigFile.builder(file.path(), ConfigCodecs.document())
				.validator(value -> { throw new IOException("invalid config"); }).build();
		assertEquals(ConfigStage.VALIDATE, assertThrows(ConfigFileException.class,
				() -> invalid.overwrite(PrimitiveElement.of(43L))).stage());
		assertEquals(before, Files.readString(file.path()));
		assertNoTemporaryFiles();
	}

	@Test void atomicFallbackIsOptIn() throws Exception {
		Path path = directory.resolve("config.json");
		Files.writeString(path, "1");
		AtomicInteger fallbacks = new AtomicInteger();
		ConfigFile.MoveOperation mover = (source, target, atomic) -> {
			if (atomic) throw new AtomicMoveNotSupportedException(source.toString(), target.toString(), "test");
			fallbacks.incrementAndGet();
			Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
		};
		var strict = ConfigFile.builder(path, ConfigCodecs.document()).moveOperation(mover).build();
		assertThrows(ConfigFileException.class, () -> strict.overwrite(PrimitiveElement.of(2L)));
		assertEquals("1", Files.readString(path));
		assertEquals(0, fallbacks.get());
		var fallback = ConfigFile.builder(path, ConfigCodecs.document()).moveOperation(mover)
				.atomicWrites(AtomicWritePolicy.ALLOW_NON_ATOMIC_FALLBACK).build();
		fallback.overwrite(PrimitiveElement.of(2L));
		assertEquals(1, fallbacks.get());
		assertNoTemporaryFiles();
	}

	@Test void externalChangesAndDeletionConflict() throws Exception {
		var file = document("config.json");
		var first = file.overwrite(PrimitiveElement.of(1L));
		Files.writeString(file.path(), "2");
		assertThrows(ConfigConflictException.class, () -> file.save(first.value(), first.revision()));
		Files.delete(file.path());
		assertThrows(ConfigConflictException.class, () -> file.save(first.value(), first.revision()));
		assertFalse(Files.exists(file.path()));
		assertThrows(IllegalArgumentException.class, () -> document("other.json").save(first.value(), first.revision()));
		assertNoTemporaryFiles();
	}

	@Test void oversizedAndSymlinkRevisionReplacementsConflictButOtherIoErrorsRemainContextual() throws Exception {
		Path path = directory.resolve("revision.json");
		var file = ConfigFile.builder(path, ConfigCodecs.document()).maxBytes(8).build();
		var loaded = file.overwrite(PrimitiveElement.of(1L));
		Files.writeString(path, "123456789");
		assertThrows(ConfigConflictException.class, () -> file.save(loaded.value(), loaded.revision()));

		Path target = directory.resolve("replacement.json");
		Files.writeString(target, "2");
		Files.delete(path);
		try {
			Files.createSymbolicLink(path, target.getFileName());
		} catch (IOException | UnsupportedOperationException | SecurityException ex) {
			Assumptions.assumeTrue(false, "Symbolic links are unavailable: " + ex);
		}
		assertThrows(ConfigConflictException.class, () -> file.save(loaded.value(), loaded.revision()));
		Files.delete(path);
		Files.createDirectory(path);
		ConfigFileException ioFailure = assertThrows(ConfigFileException.class,
				() -> file.save(loaded.value(), loaded.revision()));
		assertFalse(ioFailure instanceof ConfigConflictException);
		assertEquals(ConfigStage.CHECK_CONFLICT, ioFailure.stage());
	}

	@Test void concurrentCreationDoesNotOverwriteWinner() throws Exception {
		var file = document("config.json");
		var result = file.loadOrCreate(() -> {
			try { Files.writeString(file.path(), "2"); } catch (IOException ex) { throw new RuntimeException(ex); }
			return PrimitiveElement.of(1L);
		});
		assertEquals(ConfigOrigin.FILE, result.origin());
		assertEquals("2", Files.readString(file.path()));
		assertNoTemporaryFiles();
	}

	@ParameterizedTest @EnumSource(ConfigCreationPolicy.class)
	void bothCreationPoliciesPublishCompleteDefaults(ConfigCreationPolicy policy) throws Exception {
		if (policy == ConfigCreationPolicy.REQUIRE_ATOMIC_NO_CLOBBER) assumeHardLinksSupported();
		Path path = directory.resolve("created-" + policy + ".json");
		var original = new Server(25565, "localhost");
		var file = ConfigFile.builder(path, ConfigCodecs.reflective(Server.class))
				.creationPolicy(policy).build();
		var created = file.loadOrCreate(() -> original);
		assertEquals(ConfigOrigin.CREATED, created.origin());
		assertSame(original, created.value());
		assertEquals(original, file.load().value());
		assertNoTemporaryFiles();
	}

	@ParameterizedTest @EnumSource(ConfigCreationPolicy.class)
	void confirmedCreationRaceLoadsWinner(ConfigCreationPolicy policy) throws Exception {
		Path path = directory.resolve("winner-" + policy + ".json");
		var file = ConfigFile.builder(path, ConfigCodecs.document()).creationPolicy(policy)
				.publicationOperation((source, target, selected) -> {
					assertEquals(policy, selected);
					Files.writeString(target, "2");
					throw new FileAlreadyExistsException(target.toString());
				}).build();
		var result = file.loadOrCreate(() -> PrimitiveElement.of(1L));
		assertEquals(ConfigOrigin.FILE, result.origin());
		assertEquals(PrimitiveElement.of(2L), result.value());
		assertEquals("2", Files.readString(path));
		assertNoTemporaryFiles();
	}

	@Test void unsupportedStrictCreationIsContextualAndCleansUp() throws Exception {
		Path path = directory.resolve("strict.json");
		var file = ConfigFile.builder(path, ConfigCodecs.document())
				.creationPolicy(ConfigCreationPolicy.REQUIRE_ATOMIC_NO_CLOBBER)
				.publicationOperation((source, target, policy) -> {
					assertEquals(ConfigCreationPolicy.REQUIRE_ATOMIC_NO_CLOBBER, policy);
					throw new UnsupportedOperationException("hard links unsupported");
				}).build();
		var failure = assertThrows(ConfigFileException.class,
				() -> file.loadOrCreate(() -> PrimitiveElement.of(1L)));
		assertEquals(ConfigStage.CREATE, failure.stage());
		assertInstanceOf(UnsupportedOperationException.class, failure.getCause());
		assertFalse(Files.exists(path));
		assertNoTemporaryFiles();
	}

	@Test void defaultsHaveExactStageAndCommittedCleanupFailureDoesNotFail() throws Exception {
		var defaults = document("defaults.json");
		var defaultsFailure = assertThrows(ConfigFileException.class,
				() -> defaults.loadOrCreate(() -> { throw new IllegalStateException("defaults"); }));
		assertEquals(ConfigStage.DEFAULTS, defaultsFailure.stage());
		assertFalse(Files.exists(defaults.path()));

		Path path = directory.resolve("cleanup.json");
		var cleanup = ConfigFile.builder(path, ConfigCodecs.document())
				.cleanupOperation(temporary -> { throw new IOException("cleanup"); }).build();
		var created = cleanup.loadOrCreate(() -> PrimitiveElement.of(1L));
		assertEquals(ConfigOrigin.CREATED, created.origin());
		assertEquals("1", Files.readString(path));
	}

	@ParameterizedTest @ValueSource(booleans = {false, true})
	void committedWritesSurviveImmediateAndExitCleanupFailures(boolean runtimeCleanup) throws Exception {
		Path path = directory.resolve("cleanup-both.json");
		List<Path> temporaries = new ArrayList<>();
		AtomicInteger exitAttempts = new AtomicInteger();
		var file = ConfigFile.builder(path, ConfigCodecs.reflective(Server.class))
				.publicationOperation((source, target, policy) -> {
					assertEquals(ConfigCreationPolicy.PORTABLE_BEST_EFFORT, policy);
					// Deterministically leave the published source for cleanup, without requiring hard links.
					Files.copy(source, target);
				})
				.cleanupOperation(temporary -> {
					temporaries.add(temporary);
					if (runtimeCleanup) throw new SecurityException("cleanup denied");
					throw new IOException("cleanup failed");
				})
				.exitCleanupOperation(temporary -> {
					assertEquals(temporaries.get(temporaries.size() - 1), temporary);
					exitAttempts.incrementAndGet();
					throw new SecurityException("exit cleanup denied");
				}).build();
		try {
			var original = new Server(1, "created");
			var created = file.loadOrCreate(() -> original);
			assertSame(original, created.value());
			assertEquals(ConfigOrigin.CREATED, created.origin());
			assertEquals(created.revision(), file.load().revision());
			assertTrue(Files.exists(temporaries.get(0)));
			var replacement = new Server(2, "saved");
			var saved = file.save(replacement, created.revision());
			assertSame(replacement, saved.value());
			assertEquals(ConfigOrigin.SAVED, saved.origin());
			assertEquals(saved.revision(), file.load().revision());
			assertNotEquals(created.revision(), saved.revision());
			var overwritten = file.overwrite(original);
			assertSame(original, overwritten.value());
			assertEquals(overwritten.revision(), file.load().revision());
			assertEquals(replacement, file.save(replacement, overwritten.revision()).value());
			assertEquals(4, exitAttempts.get());
		} finally {
			for (Path temporary : temporaries) Files.deleteIfExists(temporary);
		}
		assertNoTemporaryFiles();
	}

	@ParameterizedTest @ValueSource(booleans = {false, true})
	void primaryPublicationFailuresRetainCleanupFailures(boolean runtimeCleanup) throws Exception {
		for (boolean create : new boolean[] {true, false}) {
			Path path = directory.resolve("primary-" + create + ".json");
			if (!create) Files.writeString(path, "1");
			List<Path> temporaries = new ArrayList<>();
			IOException primary = new IOException("publication failed");
			Exception cleanup = runtimeCleanup ? new SecurityException("cleanup denied") : new IOException("cleanup failed");
			SecurityException exitCleanup = new SecurityException("exit cleanup denied");
			var file = ConfigFile.builder(path, ConfigCodecs.document())
					.publicationOperation((source, target, policy) -> { throw primary; })
					.moveOperation((source, target, atomic) -> { throw primary; })
					.cleanupOperation(temporary -> {
						temporaries.add(temporary);
						if (cleanup instanceof IOException io) throw io;
						throw (RuntimeException) cleanup;
					})
					.exitCleanupOperation(temporary -> { throw exitCleanup; }).build();
			try {
				var failure = assertThrows(ConfigFileException.class, () -> {
					if (create) file.loadOrCreate(() -> PrimitiveElement.of(2L));
					else file.overwrite(PrimitiveElement.of(2L));
				});
				assertEquals(create ? ConfigStage.CREATE : ConfigStage.REPLACE, failure.stage());
				assertSame(primary, failure.getCause());
				assertArrayEquals(new Throwable[] {cleanup, exitCleanup}, failure.getSuppressed());
				assertEquals(1, temporaries.size());
				assertTrue(Files.exists(temporaries.get(0)));
				if (create) assertFalse(Files.exists(path));
				else assertEquals("1", Files.readString(path));
			} finally {
				for (Path temporary : temporaries) Files.deleteIfExists(temporary);
			}
		}
		assertNoTemporaryFiles();
	}

	@Test void separateHandlesSerializeRevisionCheckedSaves() throws Exception {
		var file = document("config.json");
		var loaded = file.overwrite(PrimitiveElement.of(0L));
		try (var executor = Executors.newFixedThreadPool(2)) {
			var tasks = List.<java.util.concurrent.Callable<Boolean>>of(
					() -> attemptSave(loaded, 1), () -> attemptSave(loaded, 2));
			var results = executor.invokeAll(tasks);
			assertNotEquals(results.get(0).get(), results.get(1).get());
		}
	}

	private boolean attemptSave(ConfigSnapshot<ValueElement> loaded, long value) throws Exception {
		try { document("config.json").save(PrimitiveElement.of(value), loaded.revision()); return true; }
		catch (ConfigConflictException ex) { return false; }
	}

	@Test void documentCommentsSurviveButTypedSaveIsCanonical() throws Exception {
		var file = document("config.jsonc");
		Files.writeString(file.path(), "{ // retained\n\"port\":1,\"host\":\"localhost\",\"unknown\":42}");
		var loaded = file.load();
		file.save(loaded.value(), loaded.revision());
		assertTrue(Files.readString(file.path()).contains("retained"));
		var typed = ConfigFile.builder(file.path(), ConfigCodecs.reflective(Server.class)).build();
		var server = typed.load();
		typed.save(server.value(), server.revision());
		String text = Files.readString(file.path());
		assertFalse(text.contains("retained"));
		assertFalse(text.contains("unknown"));
	}

	@Test void limitsAndMissingParentsFailWithoutCreation() throws Exception {
		var file = ConfigFile.builder(directory.resolve("config.hjson"), ConfigCodecs.document()).maxBytes(8).build();
		Files.writeString(file.path(), "key: value longer than limit");
		assertThrows(ConfigFileException.class, file::load);
		assertThrows(ConfigFileException.class, () -> document("missing/config.json").loadOrCreate(() -> PrimitiveElement.of(1L)));
		assertFalse(Files.exists(directory.resolve("missing")));
	}

	@Test void byteLimitsUseEncodedUtf8SizeAndPreserveExistingTarget() throws Exception {
		Path path = directory.resolve("bytes.json");
		Files.writeString(path, "\"é\"");
		var exactInput = ConfigFile.builder(path, ConfigCodecs.document()).maxBytes(4).build();
		assertEquals(PrimitiveElement.of("é"), exactInput.load().value());
		var shortInput = ConfigFile.builder(path, ConfigCodecs.document()).maxBytes(3).build();
		assertEquals(ConfigStage.READ, assertThrows(ConfigFileException.class, shortInput::load).stage());

		var unlimited = ConfigFile.builder(path, ConfigCodecs.document()).build();
		unlimited.overwrite(PrimitiveElement.of("é"));
		String saved = Files.readString(path);
		int encodedSize = saved.getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
		var exact = ConfigFile.builder(path, ConfigCodecs.document()).maxBytes(encodedSize).build();
		assertEquals(PrimitiveElement.of("é"), exact.overwrite(PrimitiveElement.of("é")).value());

		var tooSmall = ConfigFile.builder(path, ConfigCodecs.document()).maxBytes(encodedSize - 1).build();
		assertEquals(ConfigStage.SERIALIZE, assertThrows(ConfigFileException.class,
				() -> tooSmall.overwrite(PrimitiveElement.of("é"))).stage());
		assertEquals(saved, Files.readString(path));
	}

	@Test void rejectsExistingAndDanglingSymbolicLinks() throws Exception {
		Path target = directory.resolve("target.json");
		Files.writeString(target, "1");
		Path link = directory.resolve("link.json");
		try {
			Files.createSymbolicLink(link, target.getFileName());
		} catch (IOException | UnsupportedOperationException | SecurityException ex) {
			Assumptions.assumeTrue(false, "Symbolic links are unavailable: " + ex);
		}

		var linked = ConfigFile.builder(link, ConfigCodecs.document()).build();
		assertEquals(ConfigStage.READ, assertThrows(ConfigFileException.class, linked::load).stage());
		assertEquals(ConfigStage.WRITE_TEMPORARY, assertThrows(ConfigFileException.class,
				() -> linked.overwrite(PrimitiveElement.of(2L))).stage());
		assertEquals("1", Files.readString(target));

		Files.delete(target);
		assertEquals(ConfigStage.READ, assertThrows(ConfigFileException.class, linked::load).stage());
		assertEquals(ConfigStage.WRITE_TEMPORARY, assertThrows(ConfigFileException.class,
				() -> linked.overwrite(PrimitiveElement.of(2L))).stage());
	}

	@Test void genericAndMutableMappingsRoundTrip() throws Exception {
		var type = ConfigFileTests.class.getField("servers").getGenericType();
		var file = ConfigFile.builder(directory.resolve("list.json5"), ConfigCodecs.<List<Server>>reflective(type)).build();
		var servers = List.of(new Server(1, "one"), new Server(2, "two"));
		file.overwrite(servers);
		assertEquals(servers, file.load().value());
		var mutable = ConfigFile.builder(directory.resolve("mutable.json"), ConfigCodecs.reflective(MutableServer.class)).build();
		Files.writeString(mutable.path(), "{}");
		assertEquals(25565, mutable.load().value().port);
		var server = mutable.load().value();
		server.port = 42;
		mutable.overwrite(server);
		assertEquals(42, mutable.load().value().port);
	}

	@Test void shapeCompatibleCustomReflectiveSerializerRoundTripsThroughConfigFile() throws Exception {
		ObjectReaderFactory serializers = new ObjectReaderFactory();
		serializers.registerSerializer(Server.class, (java.util.function.Function<Server, ValueElement>) server -> {
			ObjectElement result = new ObjectElement();
			result.put("host", PrimitiveElement.of(server.host()));
			result.put("port", PrimitiveElement.of(server.port()));
			return result;
		});
		Path path = directory.resolve("custom-reflective.json");
		var file = ConfigFile.builder(path, ConfigCodecs.<Server>reflective(Server.class, serializers)).build();
		Server expected = new Server(25565, "example.org");
		file.overwrite(expected);
		assertEquals(expected, file.load().value());
		assertTrue(Files.readString(path).indexOf("host") < Files.readString(path).indexOf("port"));
	}

	@SuppressWarnings("deprecation")
	@Test void correctlySpelledCommaOptionsAliasLegacyMethods() {
		JsonWriterOptions options = JsonWriterOptions.builder().setOmitCommas(true).build();
		assertTrue(options.shouldOmitCommas());
		assertTrue(options.shouldOmmitCommas());
		assertFalse(options.asBuilder().setOmmitCommas(false).build().shouldOmitCommas());
	}

	@Test void codecFailuresAreContextualAndDoNotTouchTarget() throws Exception {
		Path path = directory.resolve("custom.json");
		Files.writeString(path, "1");
		ConfigCodec<String> codec = new ConfigCodec<>() {
			public String decode(ValueElement document) throws IOException { throw new IOException("decode"); }
			public ValueElement encode(String value) throws IOException { throw new IOException("encode"); }
		};
		var file = ConfigFile.builder(path, codec).build();
		assertEquals(ConfigStage.DECODE, assertThrows(ConfigFileException.class, file::load).stage());
		assertEquals(ConfigStage.ENCODE, assertThrows(ConfigFileException.class, () -> file.overwrite("value")).stage());
		assertEquals("1", Files.readString(path));
		assertNoTemporaryFiles();
	}

	@Test void generatedOutputIsDecodedAndValidatedBeforeTargetMutation() throws Exception {
		Path decodePath = directory.resolve("decode.json");
		Files.writeString(decodePath, "1");
		ConfigCodec<String> rejectingDecoder = new ConfigCodec<>() {
			public String decode(ValueElement document) throws IOException { throw new IOException("generated decode"); }
			public ValueElement encode(String value) { return PrimitiveElement.of(value); }
		};
		var decodeFile = ConfigFile.builder(decodePath, rejectingDecoder).build();
		assertEquals(ConfigStage.DECODE, assertThrows(ConfigFileException.class,
				() -> decodeFile.overwrite("new")).stage());
		assertEquals("1", Files.readString(decodePath));

		Path validatePath = directory.resolve("validate.json");
		Files.writeString(validatePath, "1");
		ValueElement original = PrimitiveElement.of(2L);
		var validateFile = ConfigFile.builder(validatePath, ConfigCodecs.document())
				.validator(value -> {
					if (value != original) throw new IOException("generated validation");
				}).build();
		assertEquals(ConfigStage.VALIDATE, assertThrows(ConfigFileException.class,
				() -> validateFile.overwrite(original)).stage());
		assertEquals("1", Files.readString(validatePath));
		assertNoTemporaryFiles();
	}

	@Test void successfulWritesPreserveSuppliedValueIdentity() throws Exception {
		var overwrittenFile = ConfigFile.builder(directory.resolve("identity-save.json"),
				ConfigCodecs.reflective(MutableServer.class)).build();
		var overwritten = new MutableServer();
		assertSame(overwritten, overwrittenFile.overwrite(overwritten).value());

		var createdFile = ConfigFile.builder(directory.resolve("identity-create.json"),
				ConfigCodecs.reflective(MutableServer.class)).build();
		var created = new MutableServer();
		assertSame(created, createdFile.loadOrCreate(() -> created).value());
	}

	@Test void generatedOutputMustMatchConfiguredReader() throws Exception {
		var file = ConfigFile.builder(directory.resolve("config.hjson"), ConfigCodecs.document())
				.readerOptions(JsonFormat.HJSON.readerOptions().asBuilder().setUnquotedKeys(false).build()).build();
		Files.writeString(file.path(), "{}");
		var document = Jankson.read("{host: 'localhost'}", JsonFormat.JSON5);
		assertEquals(ConfigStage.PARSE, assertThrows(ConfigFileException.class,
				() -> file.overwrite(document)).stage());
		assertEquals("{}", Files.readString(file.path()));
		assertNoTemporaryFiles();
	}

	private ConfigFile<ValueElement> document(String name) {
		return ConfigFile.builder(directory.resolve(name), ConfigCodecs.document()).build();
	}
	private void assumeHardLinksSupported() throws IOException {
		Path source = directory.resolve("hard-link-source.tmp");
		Path link = directory.resolve("hard-link-probe.tmp");
		Files.writeString(source, "probe");
		try {
			Files.createLink(link, source);
		} catch (IOException | UnsupportedOperationException | SecurityException ex) {
			Assumptions.assumeTrue(false, "Hard links are unavailable: " + ex);
		} finally {
			Files.deleteIfExists(link);
			Files.deleteIfExists(source);
		}
	}
	private void assertNoTemporaryFiles() throws IOException {
		try (var files = Files.list(directory)) {
			assertTrue(files.noneMatch(path -> path.getFileName().toString().startsWith(".jankson-")));
		}
	}
}
