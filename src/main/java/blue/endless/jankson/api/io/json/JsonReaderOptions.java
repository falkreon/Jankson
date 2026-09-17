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
	public boolean isBareRootObject() { return bareRootObject; }
	public boolean isUnquotedKeys() { return unquotedKeys; }
	/** Whether an explicit format accepts a comma immediately before a closing brace or bracket. */
	public boolean allowsTrailingCommas() { return allowTrailingCommas; }
	/** Null selects the original permissive Jankson parser. */
	public JsonFormat getFormat() { return format; }
	public char getKeyValueSeparator() { return keyValueSeparator; }
	private JsonReaderOptions(Builder opts) {
		bareRootObject = opts.bareRootObject;
		unquotedKeys = opts.unquotedKeys;
		allowTrailingCommas = opts.allowTrailingCommas == null || opts.allowTrailingCommas;
		format = opts.format;
		keyValueSeparator = opts.keyValueSeparator;
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
		
		public Builder() {}
		
		public Builder(JsonReaderOptions opts) {
			this.bareRootObject = opts.isBareRootObject();
			this.unquotedKeys = opts.isUnquotedKeys();
			this.allowTrailingCommas = opts.getFormat() == null ? null : opts.allowsTrailingCommas();
			this.format = opts.getFormat();
			this.keyValueSeparator = opts.getKeyValueSeparator();
		}

		
		public Builder setBareRootObject(boolean value) { bareRootObject = value; return this; }
		/** Whether to accept unquoted keys permitted by the selected key syntax. */
		public Builder setUnquotedKeys(boolean value) { unquotedKeys = value; return this;}
		/** Allows a comma immediately before a closing brace or bracket. Requires an explicit format profile. */
		public Builder setAllowTrailingCommas(boolean value) { allowTrailingCommas = value; return this; }
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
