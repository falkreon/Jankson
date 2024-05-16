/*
 * MIT License
 *
 * Copyright (c) 2018-2024 Falkreon (Isaac Ellingson)
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

import blue.endless.jankson.api.Marshaller;
import blue.endless.jankson.impl.MarshallerImpl;

@SuppressWarnings("deprecation")
public class JsonReaderOptions {
	/**
	 * This is the set of options configured when there are no options specified. Effectively this is the "default
	 * Jankson behavior". Bare root objects are not allowed, unquoted keys are allowed, and commas are ignored.
	 */
	public static final JsonReaderOptions UNSPECIFIED = new JsonReaderOptions(Hint.ALLOW_UNQUOTED_KEYS);
	
	private final EnumSet<Hint> hints = EnumSet.noneOf(Hint.class);
	private final Marshaller marshaller;
	
	public JsonReaderOptions(Hint... hints) {
		this.marshaller = MarshallerImpl.getFallback();
	}
	
	public JsonReaderOptions(Marshaller marshaller, Hint... hints) {
		for(Hint hint : hints) this.hints.add(hint);
		this.marshaller = marshaller;
	}
	
	public boolean hasHint(Hint hint) {
		return hints.contains(hint);
	}
	
	public Marshaller getMarshaller() {
		return this.marshaller;
	}
	
	public enum Hint {
		/** Allow the root object of a document to omit its delimiters / braces */
		ALLOW_BARE_ROOT_OBJECT,
		/** Allow keys in key value pairs to occur without quotes */
		ALLOW_UNQUOTED_KEYS,
		/** This is the HOCON behavior of combining objects declared on the same key. Will have no effect during granular
		 * JsonReader access. */
		MERGE_DUPLICATE_OBJECTS,
		/** This is the HOCON behavior of allowing equals ('=') to replace colons between keys and values */
		ALLOW_KEY_EQUALS_VALUE;
	}
}
