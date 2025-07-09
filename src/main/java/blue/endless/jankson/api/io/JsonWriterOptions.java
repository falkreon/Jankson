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

package blue.endless.jankson.api.io;

import java.util.EnumSet;

public class JsonWriterOptions {
	public static final JsonWriterOptions DEFAULTS = new JsonWriterOptions(Hint.UNQUOTED_KEYS, Hint.WRITE_COMMENTS, Hint.WRITE_NEWLINES, Hint.WRITE_WHITESPACE);
	public static final JsonWriterOptions ONE_LINE = new JsonWriterOptions(Hint.UNQUOTED_KEYS, Hint.WRITE_COMMENTS, Hint.WRITE_WHITESPACE);
	public static final JsonWriterOptions STRICT = new JsonWriterOptions(Hint.WRITE_NEWLINES, Hint.WRITE_WHITESPACE); // TODO: Add strict hints
	public static final JsonWriterOptions INI_SON = new JsonWriterOptions(Hint.BARE_ROOT_OBJECT, Hint.KEY_EQUALS_VALUE, Hint.UNQUOTED_KEYS, Hint.OMIT_COMMAS, Hint.WRITE_NEWLINES, Hint.WRITE_WHITESPACE);
	
	private final EnumSet<Hint> hints = EnumSet.noneOf(Hint.class);
	private final String indentString;
	
	public JsonWriterOptions(Hint... hints) {
		this("\t", hints);
	}
	
	public JsonWriterOptions(String indentString, Hint... hints) {
		for(Hint hint : hints) this.hints.add(hint);
		this.indentString = indentString;
	}
	
	public boolean get(Hint hint) {
		return hints.contains(hint);
	}
	
	public String getIndent(int count) {
		if (count<=0) return "";
		return indentString.repeat(count);
	}
	
	public static enum Hint {
		/**
		 * If the root element is an object, omit the curly braces around it.
		 */
		BARE_ROOT_OBJECT,
		/**
		 * Do not quote keys unless they contain spaces or other special characters that would make them ambiguous.
		 */
		UNQUOTED_KEYS,
		/**
		 * Write key=value instead of key:value.
		 */
		KEY_EQUALS_VALUE,
		/**
		 * Don't write commas between elements that don't need them.
		 */
		OMIT_COMMAS,
		
		WRITE_COMMENTS,
		
		WRITE_WHITESPACE,
		
		WRITE_NEWLINES;
	}
}
