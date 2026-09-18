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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;

import blue.endless.jankson.api.Jankson;
import blue.endless.jankson.api.SyntaxError;
import blue.endless.jankson.api.document.ValueElement;
import blue.endless.jankson.api.io.ValueElementWriter;
import blue.endless.jankson.api.io.json.JsonFormat;
import blue.endless.jankson.api.io.json.JsonReader;
import blue.endless.jankson.api.io.json.JsonReaderOptions;
import blue.endless.jankson.api.io.json.JsonWriterOptions;
import blue.endless.jankson.impl.config.ConfigDepthGuard;

/**
 * An immutable file handle with one fixed format and no cached current value.
 * Revisions detect content changes, not an atomic compare-and-swap against external writers.
 * Target symlinks observed by an operation are rejected; callers must not mutate values during save.
 * The target's parent directory must be trusted against concurrent namespace changes.
 *
 * <p>Configuration values have a maximum depth of 256, with the root at depth zero.
 * Input tokens and returned codec documents are checked before tree construction and serialization,
 * respectively. Custom codecs are responsible for recursion inside their own encode/decode calls.
 */
public final class ConfigFile<T> {
	/** Maximum non-EOF parse events per document by default. */
	public static final long DEFAULT_MAX_PARSE_EVENTS = 1_000_000L;
	// Bounded striped locks coordinate handles without an ever-growing path registry.
	private static final Object[] LOCKS = new Object[64];
	static { for (int i = 0; i < LOCKS.length; i++) LOCKS[i] = new Object(); }
	private final Path path;
	private final JsonFormat format;
	private final JsonReaderOptions reading;
	private final JsonWriterOptions writing;
	private final ConfigCodec<T> codec;
	private final ConfigValidator<T> validator;
	private final AtomicWritePolicy atomicWrites;
	private final ConfigCreationPolicy creationPolicy;
	private final int maxBytes;
	private final long maxParseEvents;
	private final Object lock;
	private final MoveOperation mover;
	private final PublicationOperation publisher;
	private final CleanupOperation cleaner;
	private final Consumer<Path> exitCleaner;
	private final BeforePublicationOperation beforePublication;

	private ConfigFile(Builder<T> builder) {
		path = builder.path.toAbsolutePath().normalize();
		format = builder.format == null ? JsonFormat.fromPath(path) : builder.format;
		// The parser counts containers from one; our token guard counts all values from zero.
		// An empty container at value depth 256 therefore requires 257 open parser containers.
		// Allow one further start token so the guard reports depth 257 before tree construction.
		reading = (builder.reading == null ? format.readerOptions() : builder.reading).asBuilder()
				.setMaxContainerDepth(ConfigDepthGuard.MAX_DEPTH + 2)
				.setMaxBufferedEvents(builder.maxParseEvents)
				.setMaxBufferedCharacters(builder.maxBytes).build();
		writing = builder.writing == null ? format.writerOptions() : builder.writing;
		if (reading.getFormat() != format || writing.getFormat() != format) {
			throw new IllegalArgumentException("Reader and writer options must both use " + format);
		}
		codec = builder.codec;
		validator = builder.validator;
		atomicWrites = builder.atomicWrites;
		creationPolicy = builder.creationPolicy;
		maxBytes = builder.maxBytes;
		maxParseEvents = builder.maxParseEvents;
		mover = builder.mover;
		publisher = builder.publisher;
		cleaner = builder.cleaner;
		exitCleaner = builder.exitCleaner;
		beforePublication = builder.beforePublication;
		lock = LOCKS[Math.floorMod(path.hashCode(), LOCKS.length)];
	}

	public static <T> Builder<T> builder(Path path, ConfigCodec<T> codec) {
		return new Builder<>(path, codec);
	}

	public Path path() { return path; }
	public JsonFormat format() { return format; }

	public ConfigSnapshot<T> load() throws ConfigFileException {
		byte[] bytes;
		synchronized (lock) { bytes = at(ConfigStage.READ, this::readBytes); }
		return decode(bytes, ConfigOrigin.FILE);
	}

	public ConfigSnapshot<T> loadOrCreate(Supplier<? extends T> defaults) throws ConfigFileException {
		Objects.requireNonNull(defaults);
		try { return load(); }
		catch (ConfigFileException ex) {
			if (!isMissing(ex)) throw ex;
		}
		T value = at(ConfigStage.DEFAULTS, defaults::get);
		Prepared prepared = prepare(value);
		try {
			synchronized (lock) { return writePrepared(value, prepared, null, true); }
		} catch (ConfigFileException ex) {
			if (ex.stage() == ConfigStage.CREATE && ex.getCause() instanceof FileAlreadyExistsException) return load();
			throw ex;
		}
	}

	public ConfigSnapshot<T> save(T value, FileRevision expectedRevision) throws ConfigFileException {
		Objects.requireNonNull(expectedRevision);
		if (!path.equals(expectedRevision.path())) throw new IllegalArgumentException("Revision belongs to another path");
		Prepared prepared = prepare(value);
		synchronized (lock) { return writePrepared(value, prepared, expectedRevision, false); }
	}

	/** Explicit last-writer-wins replacement, without revision checking. */
	public ConfigSnapshot<T> overwrite(T value) throws ConfigFileException {
		Prepared prepared = prepare(value);
		synchronized (lock) { return writePrepared(value, prepared, null, false); }
	}

	private ConfigSnapshot<T> decode(byte[] bytes, ConfigOrigin origin) throws ConfigFileException {
		ValueElement document = at(ConfigStage.PARSE, () -> parse(bytes));
		T value = at(ConfigStage.DECODE, () -> codec.decode(document));
		at(ConfigStage.VALIDATE, () -> { validator.validate(value); return null; });
		return new ConfigSnapshot<>(value, FileRevision.of(path, bytes), origin);
	}

	private static boolean isMissing(ConfigFileException ex) {
		return ex.stage() == ConfigStage.READ && ex.getCause() instanceof NoSuchFileException;
	}

	private byte[] readBytes() throws IOException {
		requireRegularTarget();
		try (InputStream input = Files.newInputStream(path, LinkOption.NOFOLLOW_LINKS)) {
			byte[] result = input.readNBytes(maxBytes + 1);
			if (result.length > maxBytes) throw new ChangedStateException("Configuration exceeds " + maxBytes + " bytes");
			return result;
		}
	}

	private void rejectSymlink() throws IOException {
		if (Files.isSymbolicLink(path)) throw new ChangedStateException("Symbolic link targets are not supported: " + path);
	}

	private void requireRegularTarget() throws IOException {
		BasicFileAttributes attributes = Files.readAttributes(path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
		if (attributes.isSymbolicLink()) throw new ChangedStateException("Symbolic link targets are not supported: " + path);
		if (!attributes.isRegularFile()) throw new IOException("Configuration target is not a regular file: " + path);
	}

	private void checkRevision(FileRevision expected) throws ConfigFileException {
		FileRevision actual;
		try { actual = at(ConfigStage.CHECK_CONFLICT, () -> FileRevision.of(path, readBytes())); }
		catch (ConfigFileException ex) {
			if (ex.getCause() instanceof NoSuchFileException || ex.getCause() instanceof ChangedStateException) {
				throw new ConfigConflictException(path, format);
			}
			throw ex;
		}
		if (!expected.equals(actual)) throw new ConfigConflictException(path, format);
	}

	private ValueElement parse(byte[] bytes) throws IOException, SyntaxError {
		JsonReader reader = new JsonReader(new ByteArrayInputStream(bytes), reading);
		ValueElementWriter writer = new ValueElementWriter();
		ConfigDepthGuard.transfer(reader, writer, maxParseEvents);
		return writer.getResult();
	}

	private Prepared prepare(T value) throws ConfigFileException {
		at(ConfigStage.VALIDATE, () -> { validator.validate(value); return null; });
		ValueElement document = at(ConfigStage.ENCODE, () -> Objects.requireNonNull(codec.encode(value)));
		byte[] bytes = at(ConfigStage.SERIALIZE, () -> {
			ConfigDepthGuard.checkDocument(document);
			LimitedOutputStream output = new LimitedOutputStream(maxBytes);
			OutputStreamWriter writer = new OutputStreamWriter(output, StandardCharsets.UTF_8);
			Jankson.writeJson(document, writer, writing);
			writer.flush();
			return output.toByteArray();
		});
		ValueElement generated = at(ConfigStage.PARSE, () -> parse(bytes));
		T decoded = at(ConfigStage.DECODE, () -> codec.decode(generated));
		at(ConfigStage.VALIDATE, () -> { validator.validate(decoded); return null; });
		return new Prepared(bytes);
	}

	private ConfigSnapshot<T> writePrepared(T value, Prepared prepared, FileRevision expected, boolean create)
			throws ConfigFileException {
		byte[] bytes = prepared.bytes();
		if (expected != null) checkRevision(expected);
		StagedTemporary staged = at(ConfigStage.WRITE_TEMPORARY, () -> {
			// Revision-checked writes classify observed target changes in checkRevision instead.
			if (expected == null) {
				try { requireRegularTarget(); }
				catch (NoSuchFileException missing) { /* A new target is allowed. */ }
			}
			return stage(bytes);
		});
		Path temporary = staged.path();
		ConfigFileException failure = null;
		try {
			at(ConfigStage.WRITE_TEMPORARY, () -> {
				beforePublication.run(temporary);
				validateStaged(staged, bytes);
				return null;
			});
			if (expected != null) checkRevision(expected);
			if (create) {
				at(ConfigStage.CREATE, () -> { publisher.publish(temporary, path, creationPolicy); return null; });
			} else {
				at(ConfigStage.REPLACE, () -> { rejectSymlink(); replace(temporary); return null; });
			}
			return new ConfigSnapshot<>(value, FileRevision.of(path, bytes), create ? ConfigOrigin.CREATED : ConfigOrigin.SAVED);
		} catch (ConfigFileException ex) {
			failure = ex;
			throw ex;
		} finally {
			try { cleaner.cleanup(temporary); }
			catch (IOException | RuntimeException ex) {
				if (failure != null) failure.addSuppressed(ex);
				try { exitCleaner.accept(temporary); }
				catch (RuntimeException exitFailure) {
					// Publication has already committed, or the primary failure must remain authoritative.
					if (failure != null) failure.addSuppressed(exitFailure);
				}
			}
		}
	}

	private StagedTemporary stage(byte[] bytes) throws IOException {
		FileAttribute<?>[] attributesAtCreation = Files.getFileStore(path.getParent())
				.supportsFileAttributeView(PosixFileAttributeView.class)
				? new FileAttribute<?>[] { PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")) }
				: new FileAttribute<?>[0];
		while (true) {
			Path candidate = path.getParent().resolve(".jankson-" + UUID.randomUUID() + ".tmp");
			SeekableByteChannel channel;
			try {
				channel = Files.newByteChannel(candidate,
						Set.of(StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE, LinkOption.NOFOLLOW_LINKS),
						attributesAtCreation);
			} catch (FileAlreadyExistsException ex) {
				// UUID collisions and pre-existing names are harmless; choose another exclusive name.
				continue;
			}
			try (channel) {
				ByteBuffer buffer = ByteBuffer.wrap(bytes);
				while (buffer.hasRemaining()) channel.write(buffer);
				BasicFileAttributes attributes = Files.readAttributes(candidate, BasicFileAttributes.class,
						LinkOption.NOFOLLOW_LINKS);
				if (!attributes.isRegularFile() || attributes.isSymbolicLink()) {
					throw new ChangedStateException("Staged configuration is not a regular file: " + candidate);
				}
				return new StagedTemporary(candidate, attributes.fileKey());
			} catch (IOException | RuntimeException ex) {
				try { Files.deleteIfExists(candidate); }
				catch (IOException | RuntimeException cleanupFailure) { ex.addSuppressed(cleanupFailure); }
				throw ex;
			}
		}
	}

	private void validateStaged(StagedTemporary staged, byte[] expectedBytes) throws IOException {
		BasicFileAttributes attributes = Files.readAttributes(staged.path(), BasicFileAttributes.class,
				LinkOption.NOFOLLOW_LINKS);
		if (!attributes.isRegularFile() || attributes.isSymbolicLink()) {
			throw new ChangedStateException("Staged configuration is not a regular file: " + staged.path());
		}
		if (staged.fileKey() != null && !Objects.equals(staged.fileKey(), attributes.fileKey())) {
			throw new ChangedStateException("Staged configuration was replaced before publication: " + staged.path());
		}
		if (attributes.size() != expectedBytes.length) {
			throw new ChangedStateException("Staged configuration content changed before publication: " + staged.path());
		}
		try (InputStream input = Files.newInputStream(staged.path(), LinkOption.NOFOLLOW_LINKS)) {
			// Bound the read even if the same inode grows after the metadata check.
			if (!Arrays.equals(expectedBytes, input.readNBytes(expectedBytes.length)) || input.read() != -1) {
				throw new ChangedStateException("Staged configuration content changed before publication: " + staged.path());
			}
		}
	}

	private record Prepared(byte[] bytes) {}
	private record StagedTemporary(Path path, Object fileKey) {}
	private static final class ChangedStateException extends IOException {
		private static final long serialVersionUID = 1L;
		private ChangedStateException(String message) { super(message); }
	}

	private static final class LimitedOutputStream extends OutputStream {
		private final int limit;
		private final ByteArrayOutputStream delegate = new ByteArrayOutputStream();

		private LimitedOutputStream(int limit) { this.limit = limit; }

		@Override public void write(int value) throws IOException {
			ensureCapacity(1);
			delegate.write(value);
		}

		@Override public void write(byte[] bytes, int offset, int length) throws IOException {
			ensureCapacity(length);
			delegate.write(bytes, offset, length);
		}

		private void ensureCapacity(int additional) throws IOException {
			if ((long) delegate.size() + additional > limit) {
				throw new IOException("Configuration exceeds " + limit + " bytes");
			}
		}

		private byte[] toByteArray() { return delegate.toByteArray(); }
	}

	private void replace(Path temporary) throws IOException {
		try { mover.move(temporary, path, true); }
		catch (AtomicMoveNotSupportedException ex) {
			if (atomicWrites == AtomicWritePolicy.REQUIRE_ATOMIC) throw ex;
			mover.move(temporary, path, false);
		}
	}

	private <R> R at(ConfigStage stage, Operation<R> operation) throws ConfigFileException {
		try { return operation.run(); }
		catch (IOException | SyntaxError | RuntimeException ex) {
			throw new ConfigFileException(path, format, stage, ex);
		}
	}

	@FunctionalInterface private interface Operation<R> { R run() throws IOException, SyntaxError; }
	@FunctionalInterface interface MoveOperation { void move(Path source, Path target, boolean atomic) throws IOException; }
	@FunctionalInterface interface PublicationOperation {
		void publish(Path source, Path target, ConfigCreationPolicy policy) throws IOException;
	}
	@FunctionalInterface interface CleanupOperation { void cleanup(Path temporary) throws IOException; }
	@FunctionalInterface interface BeforePublicationOperation { void run(Path temporary) throws IOException; }

	public static final class Builder<T> {
		private final Path path;
		private final ConfigCodec<T> codec;
		private JsonFormat format;
		private JsonReaderOptions reading;
		private JsonWriterOptions writing;
		private ConfigValidator<T> validator = value -> {};
		private AtomicWritePolicy atomicWrites = AtomicWritePolicy.REQUIRE_ATOMIC;
		private ConfigCreationPolicy creationPolicy = ConfigCreationPolicy.PORTABLE_BEST_EFFORT;
		private int maxBytes = 16 * 1024 * 1024;
		private long maxParseEvents = DEFAULT_MAX_PARSE_EVENTS;
		private MoveOperation mover = (source, target, atomic) -> {
			if (atomic) Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
			else Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
		};
		private PublicationOperation publisher = (source, target, policy) -> {
			if (policy == ConfigCreationPolicy.REQUIRE_ATOMIC_NO_CLOBBER) Files.createLink(target, source);
			else Files.move(source, target);
		};
		private CleanupOperation cleaner = Files::deleteIfExists;
		private Consumer<Path> exitCleaner = temporary -> temporary.toFile().deleteOnExit();
		private BeforePublicationOperation beforePublication = temporary -> {};

		private Builder(Path path, ConfigCodec<T> codec) {
			this.path = Objects.requireNonNull(path);
			this.codec = Objects.requireNonNull(codec);
		}
		public Builder<T> format(JsonFormat value) { format = Objects.requireNonNull(value); return this; }
		/** Uses the supplied grammar options; configuration depth, event, and HJSON buffer limits override resource limits. */
		public Builder<T> readerOptions(JsonReaderOptions value) { reading = Objects.requireNonNull(value); return this; }
		public Builder<T> writerOptions(JsonWriterOptions value) { writing = Objects.requireNonNull(value); return this; }
		public Builder<T> validator(ConfigValidator<T> value) { validator = Objects.requireNonNull(value); return this; }
		public Builder<T> atomicWrites(AtomicWritePolicy value) { atomicWrites = Objects.requireNonNull(value); return this; }
		public Builder<T> creationPolicy(ConfigCreationPolicy value) { creationPolicy = Objects.requireNonNull(value); return this; }
		public Builder<T> maxBytes(int value) {
			if (value < 1 || value == Integer.MAX_VALUE) throw new IllegalArgumentException("Invalid byte limit");
			maxBytes = value; return this;
		}
		/** Limits non-EOF parser events, including comments, keys, formatting, and container boundaries. */
		public Builder<T> maxParseEvents(long value) {
			if (value < 1) throw new IllegalArgumentException("Parse event limit must be positive");
			maxParseEvents = value; return this;
		}
		Builder<T> moveOperation(MoveOperation value) { mover = Objects.requireNonNull(value); return this; }
		Builder<T> publicationOperation(PublicationOperation value) { publisher = Objects.requireNonNull(value); return this; }
		Builder<T> cleanupOperation(CleanupOperation value) { cleaner = Objects.requireNonNull(value); return this; }
		Builder<T> exitCleanupOperation(Consumer<Path> value) { exitCleaner = Objects.requireNonNull(value); return this; }
		Builder<T> beforePublicationOperation(BeforePublicationOperation value) {
			beforePublication = Objects.requireNonNull(value); return this;
		}
		public ConfigFile<T> build() { return new ConfigFile<>(this); }
	}
}
