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

public final class JsonReaderOptions extends JsonReaderOptionsBuilder {
	/**
	 * This is the set of options configured when there are no options specified. Effectively this is the "default
	 * Jankson behavior". Bare root objects are not allowed, unquoted keys are allowed, and commas are ignored.
	 */
	public static final JsonReaderOptions UNSPECIFIED = new JsonReaderOptionsBuilder().build();
	
	public JsonReaderOptions(JsonReaderOptionsBuilder b) {
		this.bareRootObject = b.bareRootObject;
		this.unquotedKeys = b.unquotedKeys;
		this.keyValueSeparator = b.keyValueSeparator;
	}
	
	@Override
	public void setBareRootObject(boolean value) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public void setKeyValueSeparator(char ch) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public void setUnquotedKeys(boolean value) {
		throw new UnsupportedOperationException();
	}
}
