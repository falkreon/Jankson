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

public sealed abstract class JsonReaderOptions permits JsonReaderOptions.Builder, JsonReaderOptions.Access {
	/**
	 * This is the set of options configured when there are no options specified. Effectively this is the "default
	 * Jankson behavior". Bare root objects are not allowed, unquoted keys are allowed, and commas are ignored.
	 */
	public static final JsonReaderOptions.Access UNSPECIFIED = new JsonReaderOptions.Builder().build();
	
	
	protected boolean bareRootObject = false;
	protected boolean unquotedKeys = false;
	protected char keyValueSeparator = ':';
	
	public boolean isBareRootObject() { return bareRootObject; }
	public boolean isUnquotedKeys() { return unquotedKeys; }
	public char getKeyValueSeparator() { return keyValueSeparator; }
	
	public JsonReaderOptions() {}
	
	public JsonReaderOptions(JsonReaderOptions opts) {
		this.bareRootObject = opts.bareRootObject;
		this.unquotedKeys = opts.unquotedKeys;
		this.keyValueSeparator = opts.keyValueSeparator;
	}
	
	public static Builder builder() {
		return new Builder();
	}
	
	public static final class Builder extends JsonReaderOptions {
		
		public Builder() {}
		
		public Builder(JsonReaderOptions opts) {
			super(opts);
		}
		
		public Builder setBareRootObject(boolean value) { bareRootObject = value; return this; }
		public Builder setUnquotedKeys(boolean value) { unquotedKeys = value; return this;}
		public Builder setKeyValueSeparator(char ch) {
			if (Character.isJavaIdentifierPart(ch)) {
				throw new IllegalArgumentException("Java identifier characters are not allowed as key-value separators.");
			}
			this.keyValueSeparator = ch;
			return this;
		}
		
		public Access build() { return new Access(this); }
	}
	
	public static final class Access extends JsonReaderOptions {
		
		public Access(JsonReaderOptions opts) {
			super(opts);
		}
		
		public Builder asBuilder() {
			return new Builder(this);
		}
	}
}
