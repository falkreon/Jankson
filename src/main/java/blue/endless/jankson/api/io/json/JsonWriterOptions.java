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

import blue.endless.jankson.api.io.style.CommentStyle;
import blue.endless.jankson.api.io.style.WhitespaceStyle;

public sealed abstract class JsonWriterOptions permits JsonWriterOptions.Builder, JsonWriterOptions.Access {
	public static final JsonWriterOptions.Access DEFAULTS = JsonWriterOptions.builder()
			.setUnquotedKeys(true)
			.setWhitespace(WhitespaceStyle.PRETTY)
			.setComments(CommentStyle.STRICT)
			.build();
	
	public static final JsonWriterOptions.Access STRICT = JsonWriterOptions.builder()
			.setUnquotedKeys(false)
			.setComments(CommentStyle.NONE)
			.setWhitespace(WhitespaceStyle.PRETTY)
			.setOmmitCommas(false)
			.build();
	
	public static final JsonWriterOptions.Access ONE_LINE = STRICT.asBuilder()
			.setWhitespace(WhitespaceStyle.SPACES_ONLY)
			.build();
	
	public static final JsonWriterOptions.Access MINIFIED = STRICT.asBuilder()
			.setWhitespace(WhitespaceStyle.COMPACT)
			.build();
	
	public static final JsonWriterOptions.Access INI_SON = JsonWriterOptions.builder()
			.setBareRootObject(true)
			.setKeyValueSeparator('=')
			.setUnquotedKeys(true)
			.setOmmitCommas(true)
			.setWhitespace(WhitespaceStyle.PRETTY)
			.build();
	
	protected boolean bareRootObject = false;
	protected boolean unquotedKeys = true;
	protected boolean ommitCommas = false;
	protected CommentStyle comments = CommentStyle.STRICT;
	protected WhitespaceStyle whitespace = WhitespaceStyle.PRETTY;
	
	protected char keyValueSeparator = ':';
	protected String indentValue = "\t";
	
	public JsonWriterOptions() {}
	
	public JsonWriterOptions(JsonWriterOptions opts) {
		this.bareRootObject = opts.bareRootObject;
		this.unquotedKeys = opts.unquotedKeys;
		this.ommitCommas = opts.ommitCommas;
		this.comments = opts.comments;
		this.whitespace = opts.whitespace;
		this.keyValueSeparator = opts.keyValueSeparator;
		this.indentValue = opts.indentValue;
	}
	
	
	public String getIndent(int count) {
		if (count<=0) return "";
		return indentValue.repeat(count);
	}
	
	public boolean isBareRootObject() { return bareRootObject; }
	public boolean isUnquotedKeys() { return unquotedKeys; }
	public boolean shouldOmmitCommas() { return ommitCommas; }
	public CommentStyle comments() { return comments; }
	public WhitespaceStyle whitespace() { return whitespace; }
	public char getKeyValueSeparator() { return keyValueSeparator; }
	public String getIndentValue() { return indentValue; }
	
	public static Builder builder() {
		return new Builder();
	}
	
	public static final class Builder extends JsonWriterOptions {
		public Builder() {}
		
		public Builder(JsonWriterOptions opts) {
			super(opts);
		}
		
		public Builder setBareRootObject(boolean value) {
			bareRootObject = value;
			return this;
		}
		
		public Builder setUnquotedKeys(boolean value) {
			unquotedKeys = value;
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
		
		public Access build() {
			return new Access(this);
		}
	}
	
	public static final class Access extends JsonWriterOptions {
		public Access(JsonWriterOptions opts) {
			super(opts);
		}
		
		public Builder asBuilder() {
			return new Builder(this);
		}
	}
}
