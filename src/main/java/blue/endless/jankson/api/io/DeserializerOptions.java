/*
 * MIT License
 *
 * Copyright (c) 2018-2020 Falkreon (Isaac Ellingson)
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
public class DeserializerOptions {
	private final EnumSet<Hint> hints = EnumSet.noneOf(Hint.class);
	private final Marshaller marshaller;
	
	public DeserializerOptions(Hint... hints) {
		this.marshaller = MarshallerImpl.getFallback();
	}
	
	public DeserializerOptions(Marshaller marshaller, Hint... hints) {
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
		ALLOW_BARE_ROOTS, ALLOW_UNQUOTED_KEYS;
	}
}
