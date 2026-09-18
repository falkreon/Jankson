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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

import blue.endless.jankson.api.io.json.JsonFormat;
import blue.endless.jankson.api.io.json.JsonReaderOptions;
import blue.endless.jankson.api.io.json.JsonWriterOptions;

/**
 * Stateful, application-facing management of one configuration file.
 *
 * <p>Operations are serialized, but the returned mutable value is not made thread-safe. Callers must not mutate it
 * concurrently with save operations. A successful {@link #reload()} or {@link #replaceAndSave(Object)} replaces the
 * managed reference; previously returned references then become stale.
 */
public final class ConfigManager<T> {
	private final ConfigFile<T> file;
	private final Supplier<? extends T> creationDefaults;
	private ConfigSnapshot<T> current;

	private ConfigManager(Builder<T> builder) {
		file = builder.file.build();
		creationDefaults = builder.creationDefaults;
	}

	public static <T> Builder<T> builder(Path path, Class<T> type) {
		Objects.requireNonNull(type);
		return new Builder<>(ConfigFile.builder(path, nonNull(ConfigCodecs.reflective(type))), () -> instantiate(type));
	}

	public static <T> Builder<T> builder(Path path, ConfigCodec<T> codec) {
		return new Builder<>(ConfigFile.builder(path, nonNull(Objects.requireNonNull(codec))), null);
	}

	public Path path() { return file.path(); }
	public JsonFormat format() { return file.format(); }

	public synchronized boolean isLoaded() { return current != null; }

	public synchronized T current() {
		return requireLoaded().value();
	}

	public synchronized FileRevision revision() {
		return requireLoaded().revision();
	}

	/** Loads once; use {@link #reload()} for an explicit subsequent disk read. */
	public synchronized T load() throws ConfigFileException {
		if (current == null) current = file.load();
		return current.value();
	}

	/** Loads once or creates a missing file from the configured creation defaults. */
	public synchronized T loadOrCreate() throws ConfigFileException {
		if (current == null) {
			if (creationDefaults != null) {
				current = file.loadOrCreate(() -> Objects.requireNonNull(
						creationDefaults.get(), "Creation defaults returned null"));
			} else {
				try {
					current = file.load();
				} catch (ConfigFileException ex) {
					if (ex.stage() == ConfigStage.READ
							&& ex.getCause() instanceof java.nio.file.NoSuchFileException) {
						throw new IllegalStateException("No creation defaults were configured");
					}
					throw ex;
				}
			}
		}
		return current.value();
	}

	/** Replaces the managed reference only after a complete successful disk read. */
	public synchronized T reload() throws ConfigFileException {
		requireLoaded();
		ConfigSnapshot<T> loaded = file.load();
		current = loaded;
		return loaded.value();
	}

	/** Conflict-checked save which retains the identity of the managed mutable value. */
	public synchronized void save() throws ConfigFileException {
		ConfigSnapshot<T> state = requireLoaded();
		current = file.save(state.value(), state.revision());
	}

	/** Conflict-checked immutable replacement; the candidate becomes current only after success. */
	public synchronized T replaceAndSave(T replacement) throws ConfigFileException {
		Objects.requireNonNull(replacement, "replacement");
		ConfigSnapshot<T> state = requireLoaded();
		ConfigSnapshot<T> saved = file.save(replacement, state.revision());
		current = saved;
		return replacement;
	}

	/** Explicit last-writer-wins save of the current value. */
	public synchronized void overwrite() throws ConfigFileException {
		ConfigSnapshot<T> state = requireLoaded();
		current = file.overwrite(state.value());
	}

	private ConfigSnapshot<T> requireLoaded() {
		if (current == null) throw new IllegalStateException("Configuration has not been loaded");
		return current;
	}

	// Enforce this at the codec boundary, including the generated-document decode before publication.
	private static <T> ConfigCodec<T> nonNull(ConfigCodec<T> codec) {
		return new ConfigCodec<>() {
			@Override
			public T decode(blue.endless.jankson.api.document.ValueElement document)
					throws java.io.IOException, blue.endless.jankson.api.SyntaxError {
				return Objects.requireNonNull(codec.decode(document), "A managed configuration root cannot be null");
			}

			@Override
			public blue.endless.jankson.api.document.ValueElement encode(T value)
					throws java.io.IOException, blue.endless.jankson.api.SyntaxError {
				return codec.encode(Objects.requireNonNull(value, "A managed configuration root cannot be null"));
			}
		};
	}

	private static <T> T instantiate(Class<T> type) {
		Constructor<T> constructor;
		try {
			constructor = type.getDeclaredConstructor();
		} catch (NoSuchMethodException ex) {
			throw new IllegalStateException("No creation defaults were configured and " + type.getTypeName()
					+ " has no no-argument constructor", ex);
		}
		boolean accessible = constructor.canAccess(null);
		try {
			if (!accessible && !constructor.trySetAccessible()) {
				throw new IllegalStateException("Cannot access the no-argument constructor of " + type.getTypeName());
			}
			return constructor.newInstance();
		} catch (InstantiationException | IllegalAccessException | InvocationTargetException ex) {
			throw new IllegalStateException("Could not create defaults for " + type.getTypeName(), ex);
		} finally {
			if (!accessible) constructor.setAccessible(false);
		}
	}

	public static final class Builder<T> {
		private final ConfigFile.Builder<T> file;
		private Supplier<? extends T> creationDefaults;

		private Builder(ConfigFile.Builder<T> file, Supplier<? extends T> creationDefaults) {
			this.file = file;
			this.creationDefaults = creationDefaults;
		}

		public Builder<T> creationDefaults(Supplier<? extends T> value) {
			creationDefaults = Objects.requireNonNull(value);
			return this;
		}
		public Builder<T> format(JsonFormat value) { file.format(value); return this; }
		public Builder<T> readerOptions(JsonReaderOptions value) { file.readerOptions(value); return this; }
		public Builder<T> writerOptions(JsonWriterOptions value) { file.writerOptions(value); return this; }
		public Builder<T> validator(ConfigValidator<T> value) { file.validator(value); return this; }
		public Builder<T> atomicWrites(AtomicWritePolicy value) { file.atomicWrites(value); return this; }
		public Builder<T> creationPolicy(ConfigCreationPolicy value) { file.creationPolicy(value); return this; }
		public Builder<T> maxBytes(int value) { file.maxBytes(value); return this; }
		/** Limits non-EOF parser events, including comments, keys, formatting, and container boundaries. */
		public Builder<T> maxParseEvents(long value) { file.maxParseEvents(value); return this; }
		Builder<T> publicationOperation(ConfigFile.PublicationOperation value) { file.publicationOperation(value); return this; }
		Builder<T> cleanupOperation(ConfigFile.CleanupOperation value) { file.cleanupOperation(value); return this; }
		Builder<T> exitCleanupOperation(Consumer<Path> value) { file.exitCleanupOperation(value); return this; }

		public ConfigManager<T> build() { return new ConfigManager<>(this); }
	}
}
