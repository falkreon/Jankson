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
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import blue.endless.jankson.api.annotation.Comment;
import blue.endless.jankson.api.io.json.JsonFormat;

class ConfigManagerTests {
	@TempDir Path directory;

	static class ModConfig {
		@Comment("Chance that a flip occurs")
		float flip_chance = 0.5f;
		@Comment("Elements enabled by default")
		List<String> someElements = List.of("123", "test");
	}

	record ImmutableConfig(int value) {}

	@Test void simpleTypedWorkflowHasNoValueWrapper() throws Exception {
		var manager = ConfigManager.builder(directory.resolve("mod.jsonc"), ModConfig.class).build();
		assertFalse(manager.isLoaded());
		assertThrows(IllegalStateException.class, manager::current);
		assertThrows(IllegalStateException.class, manager::save);

		ModConfig config = manager.loadOrCreate();
		assertSame(config, manager.current());
		assertEquals(0.5f, config.flip_chance);
		String created = Files.readString(manager.path());
		assertTrue(created.contains("Chance that a flip occurs"), created);
		assertTrue(created.contains("Elements enabled by default"), created);

		config.flip_chance = 0.75f;
		config.someElements = List.of("123", "test", "extra");
		manager.save();
		assertSame(config, manager.current());

		var restored = ConfigManager.builder(manager.path(), ModConfig.class).build().load();
		assertEquals(0.75f, restored.flip_chance);
		assertEquals(config.someElements, restored.someElements);
	}

	@ParameterizedTest @ValueSource(booleans = {false, true})
	void cleanupFailuresDoNotPreventManagerStateCommit(boolean runtimeCleanup) throws Exception {
		Path path = directory.resolve("cleanup.jsonc");
		List<Path> temporaries = new ArrayList<>();
		AtomicInteger exitAttempts = new AtomicInteger();
		ModConfig original = new ModConfig();
		var manager = ConfigManager.builder(path, ModConfig.class)
				.creationDefaults(() -> original)
				.publicationOperation((source, target, policy) -> Files.copy(source, target))
				.cleanupOperation(temporary -> {
					temporaries.add(temporary);
					if (runtimeCleanup) throw new SecurityException("cleanup denied");
					throw new IOException("cleanup failed");
				})
				.exitCleanupOperation(temporary -> {
					exitAttempts.incrementAndGet();
					throw new SecurityException("exit cleanup denied");
				}).build();
		var reader = ConfigFile.builder(path, ConfigCodecs.reflective(ModConfig.class)).build();
		try {
			assertSame(original, manager.loadOrCreate());
			assertTrue(manager.isLoaded());
			assertSame(original, manager.current());
			FileRevision created = manager.revision();
			assertEquals(reader.load().revision(), created);
			assertTrue(Files.exists(temporaries.get(0)));

			original.flip_chance = 0.75f;
			manager.save();
			assertSame(original, manager.current());
			FileRevision saved = manager.revision();
			assertNotEquals(created, saved);
			assertEquals(reader.load().revision(), saved);
			assertEquals(0.75f, reader.load().value().flip_chance);

			ModConfig replacement = new ModConfig();
			replacement.flip_chance = 0.25f;
			assertSame(replacement, manager.replaceAndSave(replacement));
			assertSame(replacement, manager.current());
			FileRevision replaced = manager.revision();
			assertNotEquals(saved, replaced);
			assertEquals(reader.load().revision(), replaced);

			replacement.flip_chance = 1.0f;
			manager.save();
			assertSame(replacement, manager.current());
			assertNotEquals(replaced, manager.revision());
			assertEquals(reader.load().revision(), manager.revision());
			assertEquals(1.0f, reader.load().value().flip_chance);
			assertEquals(4, exitAttempts.get());
		} finally {
			for (Path temporary : temporaries) Files.deleteIfExists(temporary);
		}
		try (var files = Files.list(directory)) {
			assertTrue(files.noneMatch(temporary -> temporary.getFileName().toString().startsWith(".jankson-")));
		}
	}

	@Test void explicitFormatOverridesExtension() throws Exception {
		var manager = ConfigManager.builder(directory.resolve("mod.conf"), ModConfig.class)
				.format(JsonFormat.HJSON).build();
		manager.loadOrCreate();
		assertEquals(JsonFormat.HJSON, manager.format());
		assertTrue(Files.readString(manager.path()).contains("Chance that a flip occurs"));
	}

	@Test void initializationIsIdempotentAndReloadIsExplicit() throws Exception {
		var manager = ConfigManager.builder(directory.resolve("mod.json"), ModConfig.class).build();
		ModConfig original = manager.loadOrCreate();
		Files.writeString(manager.path(), "{\"flip_chance\":0.25,\"someElements\":[]}");
		assertSame(original, manager.load());
		ModConfig reloaded = manager.reload();
		assertNotSame(original, reloaded);
		assertEquals(0.25f, reloaded.flip_chance);
		original.flip_chance = 1.0f;
		assertEquals(0.25f, manager.current().flip_chance);
	}

	@Test void failedReloadAndSaveRetainManagerAssignments() throws Exception {
		var manager = ConfigManager.builder(directory.resolve("mod.json"), ModConfig.class).build();
		ModConfig config = manager.loadOrCreate();
		FileRevision revision = manager.revision();
		Files.writeString(manager.path(), "malformed");
		assertThrows(ConfigFileException.class, manager::reload);
		assertSame(config, manager.current());
		assertEquals(revision, manager.revision());
		assertThrows(ConfigConflictException.class, manager::save);
		assertSame(config, manager.current());
		assertEquals(revision, manager.revision());
	}

	@Test void separateManagersDetectConflicts() throws Exception {
		Path path = directory.resolve("mod.json");
		var first = ConfigManager.builder(path, ModConfig.class).build();
		var second = ConfigManager.builder(path, ModConfig.class).build();
		first.loadOrCreate();
		second.load();
		first.current().flip_chance = 0.1f;
		first.save();
		second.current().flip_chance = 0.2f;
		assertThrows(ConfigConflictException.class, second::save);
		second.overwrite();
		assertEquals(0.2f, ConfigManager.builder(path, ModConfig.class).build().load().flip_chance);
	}

	@Test void immutableReplacementInstallsOnlyAfterSuccess() throws Exception {
		Path path = directory.resolve("immutable.json");
		var manager = ConfigManager.builder(path, ImmutableConfig.class)
				.creationDefaults(() -> new ImmutableConfig(1)).build();
		ImmutableConfig initial = manager.loadOrCreate();
		ImmutableConfig replacement = manager.replaceAndSave(new ImmutableConfig(2));
		assertEquals(2, replacement.value());
		assertSame(replacement, manager.current());
		Files.writeString(path, "{\"value\":3}");
		assertThrows(ConfigConflictException.class,
				() -> manager.replaceAndSave(new ImmutableConfig(4)));
		assertSame(replacement, manager.current());
		assertEquals(1, initial.value());
	}

	@Test void defaultsAreCreationOnlyAndNullRootsAreRejected() throws Exception {
		Path path = directory.resolve("custom.json");
		AtomicInteger calls = new AtomicInteger();
		ConfigCodec<ModConfig> codec = ConfigCodecs.reflective(ModConfig.class);
		var manager = ConfigManager.builder(path, codec).creationDefaults(() -> {
			calls.incrementAndGet();
			return new ModConfig();
		}).build();
		manager.loadOrCreate();
		assertEquals(1, calls.get());
		assertSame(manager.current(), manager.loadOrCreate());
		assertEquals(1, calls.get());

		var noDefaults = ConfigManager.builder(directory.resolve("none.json"), codec).build();
		assertThrows(IllegalStateException.class, noDefaults::loadOrCreate);
		Files.writeString(noDefaults.path(), "null");
		assertEquals(ConfigStage.DECODE,
				assertThrows(ConfigFileException.class, noDefaults::load).stage());
		assertFalse(noDefaults.isLoaded());
	}

	@Test void codecWithoutDefaultsCanLoadAnExistingFile() throws Exception {
		Path path = directory.resolve("existing.json");
		Files.writeString(path, "{}");
		var manager = ConfigManager.builder(path, ConfigCodecs.reflective(ModConfig.class)).build();
		assertNotNull(manager.loadOrCreate());
		assertTrue(manager.isLoaded());
	}

	@Test void codecThatRoundTripsToNullCannotPublishManagedConfig() throws Exception {
		Path path = directory.resolve("null-output.json");
		ConfigCodec<ModConfig> codec = new ConfigCodec<>() {
			public ModConfig decode(blue.endless.jankson.api.document.ValueElement document) { return null; }
			public blue.endless.jankson.api.document.ValueElement encode(ModConfig value) {
				return blue.endless.jankson.api.document.PrimitiveElement.ofNull();
			}
		};
		var manager = ConfigManager.builder(path, codec).creationDefaults(ModConfig::new).build();
		assertEquals(ConfigStage.DECODE,
				assertThrows(ConfigFileException.class, manager::loadOrCreate).stage());
		assertFalse(Files.exists(path));
		assertFalse(manager.isLoaded());
	}
}
