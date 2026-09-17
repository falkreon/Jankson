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

import blue.endless.jankson.api.io.style.CommentStyle;
import blue.endless.jankson.api.io.style.WhitespaceStyle;

/** Immutable writer configuration with an independent builder. */
public final class JsonWriterOptions {
	public static final JsonWriterOptions DEFAULTS = JsonWriterOptions.builder()
			.setUnquotedKeys(true)
			.setWhitespace(WhitespaceStyle.PRETTY)
			.setComments(CommentStyle.STRICT)
			.build();
	
	public static final JsonWriterOptions STRICT = JsonWriterOptions.builder()
			.setUnquotedKeys(false)
			.setComments(CommentStyle.NONE)
			.setWhitespace(WhitespaceStyle.PRETTY)
			.setOmmitCommas(false)
			.build();
	
	public static final JsonWriterOptions ONE_LINE = STRICT.asBuilder()
			.setWhitespace(WhitespaceStyle.SPACES_ONLY)
			.build();
	
	public static final JsonWriterOptions MINIFIED = STRICT.asBuilder()
			.setWhitespace(WhitespaceStyle.COMPACT)
			.build();
	
	public static final JsonWriterOptions INI_SON = JsonWriterOptions.builder()
			.setBareRootObject(true)
			.setKeyValueSeparator('=')
			.setUnquotedKeys(true)
			.setOmmitCommas(true)
			.setWhitespace(WhitespaceStyle.PRETTY)
			.build();
	
	public static Builder builder() {
		return new Builder();
	}
	
	public static final class Builder {
		private boolean bareRootObject = false;
		private boolean unquotedKeys = true;
		private JsonFormat format = null;
		private boolean ommitCommas = false;
		private CommentStyle comments = CommentStyle.STRICT;
		private WhitespaceStyle whitespace = WhitespaceStyle.PRETTY;
		private char keyValueSeparator = ':';
		private String indentValue = "\t";

		public Builder() {}
		
		public Builder(JsonWriterOptions opts) {
			this.bareRootObject = opts.isBareRootObject();
			this.unquotedKeys = opts.isUnquotedKeys();
			this.format = opts.getFormat();
			this.ommitCommas = opts.shouldOmmitCommas();
			this.comments = opts.comments();
			this.whitespace = opts.whitespace();
			this.keyValueSeparator = opts.getKeyValueSeparator();
			this.indentValue = opts.getIndentValue();
		}

		
		public Builder setBareRootObject(boolean value) {
			bareRootObject = value;
			return this;
		}
		
		/** Prefer unquoted keys when the selected syntax permits the literal key. */
		public Builder setUnquotedKeys(boolean value) {
			unquotedKeys = value;
			return this;
		}
		
		/** Selects a document grammar; formatting may be customized without violating it. */
		public Builder setFormat(JsonFormat value) {
			format = Objects.requireNonNull(value);
			unquotedKeys = value != JsonFormat.JSON && value != JsonFormat.JSONC;
			bareRootObject = false;
			ommitCommas = false;
			keyValueSeparator = ':';
			comments = value == JsonFormat.JSON ? CommentStyle.NONE : CommentStyle.STRICT;
			return this;
		}

		public Builder setOmmitCommas(boolean value) {
			ommitCommas = value;
			return this;
		}
		
		public Builder setComments(CommentStyle value) {
			this.comments = value;
			return this;
		}
		
		public Builder setWhitespace(WhitespaceStyle value) {
			this.whitespace = value;
			return this;
		}
		
		public Builder setKeyValueSeparator(char ch) {
			if (Character.isJavaIdentifierPart(ch)) {
				throw new IllegalArgumentException("Java identifier characters are not allowed as key-value separators.");
			}
			
			this.keyValueSeparator = ch;
			return this;
		}
		
		public Builder setIndentValue(String value) {
			this.indentValue = value;
			return this;
		}
		
		public JsonWriterOptions build() {
			return new JsonWriterOptions(this);
		}
	}
	
	private final boolean bareRootObject;
	private final boolean unquotedKeys;
	private final JsonFormat format;
	private final boolean ommitCommas;
	private final CommentStyle comments;
	private final WhitespaceStyle whitespace;
	private final char keyValueSeparator;
	private final String indentValue;

	private JsonWriterOptions(Builder opts) {
		this.bareRootObject = opts.bareRootObject;
		this.unquotedKeys = opts.unquotedKeys;
		this.format = opts.format;
		this.ommitCommas = opts.ommitCommas;
		this.comments = Objects.requireNonNull(opts.comments);
		this.whitespace = Objects.requireNonNull(opts.whitespace);
		this.keyValueSeparator = opts.keyValueSeparator;
		this.indentValue = Objects.requireNonNull(opts.indentValue);
		if (format != null && (keyValueSeparator != ':'
				|| bareRootObject && format != JsonFormat.HJSON
				|| ommitCommas && (format != JsonFormat.HJSON || !whitespace.newlines())
				|| (format == JsonFormat.JSON || format == JsonFormat.JSONC) && unquotedKeys
				|| format == JsonFormat.JSON && comments != CommentStyle.NONE
				|| !indentValue.chars().allMatch(ch -> ch == ' ' || ch == '\t'))) {
			throw new IllegalArgumentException("Writer options conflict with " + format);
		}
	}

	public String getIndent(int count) { return count <= 0 ? "" : indentValue.repeat(count); }
	public boolean isBareRootObject() { return bareRootObject; }
	public boolean isUnquotedKeys() { return unquotedKeys; }
	/** Null selects the legacy output rules. */
	public JsonFormat getFormat() { return format; }
	public boolean shouldOmmitCommas() { return ommitCommas; }
	public CommentStyle comments() { return comments; }
	public WhitespaceStyle whitespace() { return whitespace; }
	public char getKeyValueSeparator() { return keyValueSeparator; }
	public String getIndentValue() { return indentValue; }

	public Builder asBuilder() {
		return new Builder(this);
	}
}
