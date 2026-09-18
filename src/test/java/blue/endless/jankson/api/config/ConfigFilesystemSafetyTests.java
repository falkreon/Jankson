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
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import blue.endless.jankson.api.document.PrimitiveElement;

class ConfigFilesystemSafetyTests {
	@TempDir Path directory;

	@ParameterizedTest(name = "{displayName} [{index}] {0}") @EnumSource(ConfigCreationPolicy.class)
	void posixStagingAndPublishedFilesAreOwnerOnly(ConfigCreationPolicy policy) throws Exception {
		Assumptions.assumeTrue(Files.getFileStore(directory).supportsFileAttributeView(PosixFileAttributeView.class));
		Path path = directory.resolve("secret.json");
		AtomicInteger stages = new AtomicInteger();
		var file = ConfigFile.builder(path, ConfigCodecs.document()).creationPolicy(policy)
				.beforePublicationOperation(temporary -> {
					assertEquals(PosixFilePermissions.fromString("rw-------"), Files.getPosixFilePermissions(temporary));
					stages.incrementAndGet();
				}).build();
		var created = file.loadOrCreate(() -> PrimitiveElement.of("secret"));
		assertEquals(PosixFilePermissions.fromString("rw-------"), Files.getPosixFilePermissions(path));
		file.save(PrimitiveElement.of("new secret"), created.revision());
		assertEquals(PosixFilePermissions.fromString("rw-------"), Files.getPosixFilePermissions(path));
		assertEquals(2, stages.get());
	}

	@Test void posixStagingRemainsPrivateWhenSaveConflictsAfterStaging() throws Exception {
		Assumptions.assumeTrue(Files.getFileStore(directory).supportsFileAttributeView(PosixFileAttributeView.class));
		Path path = directory.resolve("conflict.json");
		Files.writeString(path, "1");
		AtomicInteger stages = new AtomicInteger();
		var file = ConfigFile.builder(path, ConfigCodecs.document()).beforePublicationOperation(temporary -> {
			assertEquals(PosixFilePermissions.fromString("rw-------"), Files.getPosixFilePermissions(temporary));
			stages.incrementAndGet();
			Files.writeString(path, "2");
		}).build();
		var loaded = file.load();
		assertThrows(ConfigConflictException.class, () -> file.save(PrimitiveElement.of("secret"), loaded.revision()));
		assertEquals(1, stages.get());
		assertEquals("2", Files.readString(path));
		assertNoTemporaryFiles();
	}

	@ParameterizedTest(name = "{displayName} [{index}] {0}") @ValueSource(booleans = {false, true})
	void revisionSaveClassifiesDanglingAndExistingLinksAsConflicts(boolean dangling) throws Exception {
		Path path = directory.resolve("config.json");
		Path target = directory.resolve("target.json");
		Files.writeString(path, "1");
		if (!dangling) Files.writeString(target, "2");
		var file = ConfigFile.builder(path, ConfigCodecs.document()).build();
		var loaded = file.load();
		Files.delete(path);
		try { Files.createSymbolicLink(path, target.getFileName()); }
		catch (IOException | UnsupportedOperationException | SecurityException ex) {
			Assumptions.assumeTrue(false, "Symlinks unavailable: " + ex);
		}
		assertEquals(ConfigStage.CHECK_CONFLICT, assertThrows(ConfigConflictException.class,
				() -> file.save(PrimitiveElement.of(3L), loaded.revision())).stage());
		assertTrue(Files.isSymbolicLink(path));
		if (dangling) assertFalse(Files.exists(target));
		else assertEquals("2", Files.readString(target));
		assertNoTemporaryFiles();
	}

	@ParameterizedTest(name = "{displayName} [{index}] {0}") @ValueSource(booleans = {false, true})
	void changedStagingLengthIsRejectedWithoutPublishing(boolean grow) throws Exception {
		Path path = directory.resolve("size.json");
		Files.writeString(path, "1");
		var file = ConfigFile.builder(path, ConfigCodecs.document()).maxBytes(16)
				.beforePublicationOperation(temporary -> {
					try (var channel = Files.newByteChannel(temporary, StandardOpenOption.WRITE)) {
						if (grow) {
							channel.position(256L * 1024 * 1024);
							channel.write(ByteBuffer.wrap(new byte[] {0}));
						} else channel.truncate(0);
					}
				}).build();
		assertEquals(ConfigStage.WRITE_TEMPORARY, assertThrows(ConfigFileException.class,
				() -> file.overwrite(PrimitiveElement.of(2L))).stage());
		assertEquals("1", Files.readString(path));
		assertNoTemporaryFiles();
	}

	@Test void sameContentStagingReplacementIsRejectedByIdentity() throws Exception {
		Path path = directory.resolve("identity.json");
		Path replacement = directory.resolve("replacement.json");
		Files.writeString(path, "1");
		AtomicBoolean distinctFileKeys = new AtomicBoolean(true);
		AtomicInteger publications = new AtomicInteger();
		var file = ConfigFile.builder(path, ConfigCodecs.document())
				.beforePublicationOperation(temporary -> {
					byte[] expected = Files.readAllBytes(temporary);
					Files.copy(temporary, replacement);
					Object stagedKey = Files.readAttributes(temporary, BasicFileAttributes.class,
							LinkOption.NOFOLLOW_LINKS).fileKey();
					Object replacementKey = Files.readAttributes(replacement, BasicFileAttributes.class,
							LinkOption.NOFOLLOW_LINKS).fileKey();
					distinctFileKeys.set(stagedKey != null && replacementKey != null && !stagedKey.equals(replacementKey));
					if (!distinctFileKeys.get()) {
						// Abort through normal cleanup; the assumption is evaluated outside ConfigFile's error wrapping.
						throw new IOException("Provider does not expose distinguishing file keys");
					}
					Files.move(replacement, temporary, StandardCopyOption.REPLACE_EXISTING);
					assertEquals(replacementKey, Files.readAttributes(temporary, BasicFileAttributes.class,
							LinkOption.NOFOLLOW_LINKS).fileKey());
					assertArrayEquals(expected, Files.readAllBytes(temporary));
				})
				.moveOperation((source, target, atomic) -> publications.incrementAndGet()).build();
		var failure = assertThrows(ConfigFileException.class, () -> file.overwrite(PrimitiveElement.of(2L)));
		Assumptions.assumeTrue(distinctFileKeys.get(), "Provider does not expose distinguishing file keys");
		assertEquals(ConfigStage.WRITE_TEMPORARY, failure.stage());
		assertTrue(failure.getCause().getMessage().contains("Staged configuration was replaced before publication"));
		assertEquals(0, publications.get());
		assertEquals("1", Files.readString(path));
		assertNoTemporaryFiles();
	}

	@Test void directoryIsRejectedAsNonRegularTarget() throws Exception {
		Path path = Files.createDirectory(directory.resolve("directory.json"));
		var file = ConfigFile.builder(path, ConfigCodecs.document()).build();
		assertEquals(ConfigStage.READ, assertThrows(ConfigFileException.class, file::load).stage());
		assertEquals(ConfigStage.WRITE_TEMPORARY, assertThrows(ConfigFileException.class,
				() -> file.overwrite(PrimitiveElement.of(1L))).stage());
		assertTrue(Files.isDirectory(path));
		assertNoTemporaryFiles();
	}

	@Test @Timeout(value = 10, unit = TimeUnit.SECONDS, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
	void fifoIsRejectedBeforeBlockingOpen() throws Exception {
		Assumptions.assumeTrue(System.getProperty("os.name").toLowerCase(java.util.Locale.ROOT).contains("linux"));
		Path path = directory.resolve("pipe.json");
		Process mkfifo = new ProcessBuilder("mkfifo", path.toString()).start();
		try {
			assertTrue(mkfifo.waitFor(5, TimeUnit.SECONDS), "mkfifo did not complete");
			assertEquals(0, mkfifo.exitValue());
		} finally { mkfifo.destroyForcibly(); }
		var file = ConfigFile.builder(path, ConfigCodecs.document()).build();
		var failure = assertThrows(ConfigFileException.class, file::load);
		assertEquals(ConfigStage.READ, failure.stage());
		assertTrue(failure.getCause().getMessage().contains("not a regular file"));
	}

	private void assertNoTemporaryFiles() throws IOException {
		try (var files = Files.list(directory)) {
			assertTrue(files.noneMatch(path -> path.getFileName().toString().startsWith(".jankson-")));
		}
	}
}
