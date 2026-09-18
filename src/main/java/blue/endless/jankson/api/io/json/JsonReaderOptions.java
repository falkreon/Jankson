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

package blue.endless.jankson.api.io.json;

import java.util.Objects;

/** Immutable reader configuration. Builders have no initialization dependency on this class. */
public final class JsonReaderOptions {
	/** Default source limit, in Unicode code points, for ambiguous HJSON root parsing. */
	public static final int DEFAULT_MAX_BUFFERED_CHARACTERS = 16 * 1024 * 1024;
	/** Default non-EOF event limit for ambiguous HJSON root parsing. */
	public static final long DEFAULT_MAX_BUFFERED_EVENTS = 1_000_000L;
	/**
	 * This is the set of options configured when there are no options specified. Effectively this is the "default
	 * Jankson behavior". Bare root objects are not allowed, unquoted keys are allowed, and commas are ignored.
	 */
	public static final JsonReaderOptions UNSPECIFIED = new Builder().build();
	
	
	private final boolean bareRootObject;
	private final boolean unquotedKeys;
	private final boolean allowTrailingCommas;
	private final JsonFormat format;
	private final char keyValueSeparator;
	private final int maxContainerDepth;
	private final long maxBufferedEvents;
	private final int maxBufferedCharacters;
	public boolean isBareRootObject() { return bareRootObject; }
	public boolean isUnquotedKeys() { return unquotedKeys; }
	/** Whether an explicit format accepts a comma immediately before a closing brace or bracket. */
	public boolean allowsTrailingCommas() { return allowTrailingCommas; }
	/** Null selects the original permissive Jankson parser. */
	public JsonFormat getFormat() { return format; }
	public char getKeyValueSeparator() { return keyValueSeparator; }
	/**
	 * Maximum simultaneously open containers for explicit format profiles (default 256).
	 * A root container counts as one; primitive values do not add a container level.
	 * This is distinct from the configuration pipeline's root-zero value-depth limit.
	 */
	public int getMaxContainerDepth() { return maxContainerDepth; }
	/** Maximum events buffered for ambiguous HJSON root parsing. */
	public long getMaxBufferedEvents() { return maxBufferedEvents; }
	/** Maximum characters buffered for ambiguous HJSON root parsing. */
	public int getMaxBufferedCharacters() { return maxBufferedCharacters; }
	private JsonReaderOptions(Builder opts) {
		bareRootObject = opts.bareRootObject;
		unquotedKeys = opts.unquotedKeys;
		allowTrailingCommas = opts.allowTrailingCommas == null || opts.allowTrailingCommas;
		format = opts.format;
		keyValueSeparator = opts.keyValueSeparator;
		maxContainerDepth = opts.maxContainerDepth;
		maxBufferedEvents = opts.maxBufferedEvents;
		maxBufferedCharacters = opts.maxBufferedCharacters;
		if (format == null && opts.allowTrailingCommas != null) {
			throw new IllegalArgumentException("Trailing-comma options require an explicit JsonFormat");
		}
		if (format != null && (keyValueSeparator != ':'
				|| bareRootObject && format != JsonFormat.HJSON
				|| unquotedKeys && (format == JsonFormat.JSON || format == JsonFormat.JSONC)
				|| allowTrailingCommas && format == JsonFormat.JSON)) {
			throw new IllegalArgumentException("Reader options conflict with " + format);
		}
	}
	public Builder asBuilder() { return new Builder(this); }
	
	public static Builder builder() {
		return new Builder();
	}
	
	public static final class Builder {
		private boolean bareRootObject = false;
		private boolean unquotedKeys = true;
		private Boolean allowTrailingCommas = null;
		private JsonFormat format = null;
		private char keyValueSeparator = ':';
		private int maxContainerDepth = 256;
		private long maxBufferedEvents = DEFAULT_MAX_BUFFERED_EVENTS;
		private int maxBufferedCharacters = DEFAULT_MAX_BUFFERED_CHARACTERS;
		
		public Builder() {}
		
		public Builder(JsonReaderOptions opts) {
			this.bareRootObject = opts.isBareRootObject();
			this.unquotedKeys = opts.isUnquotedKeys();
			this.allowTrailingCommas = opts.getFormat() == null ? null : opts.allowsTrailingCommas();
			this.format = opts.getFormat();
			this.keyValueSeparator = opts.getKeyValueSeparator();
			this.maxContainerDepth = opts.getMaxContainerDepth();
			this.maxBufferedEvents = opts.getMaxBufferedEvents();
			this.maxBufferedCharacters = opts.getMaxBufferedCharacters();
		}

		
		public Builder setBareRootObject(boolean value) { bareRootObject = value; return this; }
		/** Whether to accept unquoted keys permitted by the selected key syntax. */
		public Builder setUnquotedKeys(boolean value) { unquotedKeys = value; return this;}
		/** Allows a comma immediately before a closing brace or bracket. Requires an explicit format profile. */
		public Builder setAllowTrailingCommas(boolean value) { allowTrailingCommas = value; return this; }
		/**
		 * Sets the container limit for explicit format profiles. The root container counts as one.
		 * ConfigFile uses its own fixed value-depth policy and overrides this parser limit.
		 */
		public Builder setMaxContainerDepth(int value) {
			if (value < 1) throw new IllegalArgumentException("Container depth must be positive");
			maxContainerDepth = value;
			return this;
		}
		/** Sets a positive event limit for ambiguous HJSON root buffering. */
		public Builder setMaxBufferedEvents(long value) {
			if (value < 1) throw new IllegalArgumentException("Event limit must be positive");
			maxBufferedEvents = value;
			return this;
		}
		/** Sets a positive character limit for ambiguous HJSON root buffering. */
		public Builder setMaxBufferedCharacters(int value) {
			if (value < 1) throw new IllegalArgumentException("Buffered character limit must be positive");
			maxBufferedCharacters = value;
			return this;
		}
		/** Selects an entire grammar and resets grammar-related switches to its defaults. */
		public Builder setFormat(JsonFormat value) {
			format = Objects.requireNonNull(value);
			unquotedKeys = value != JsonFormat.JSON && value != JsonFormat.JSONC;
			allowTrailingCommas = value == JsonFormat.JSON5 || value == JsonFormat.HJSON;
			bareRootObject = value == JsonFormat.HJSON;
			keyValueSeparator = ':';
			return this;
		}
		public Builder setKeyValueSeparator(char ch) {
			if (Character.isJavaIdentifierPart(ch)) {
				throw new IllegalArgumentException("Java identifier characters are not allowed as key-value separators.");
			}
			this.keyValueSeparator = ch;
			return this;
		}
		
		public JsonReaderOptions build() { return new JsonReaderOptions(this); }
	}
}
