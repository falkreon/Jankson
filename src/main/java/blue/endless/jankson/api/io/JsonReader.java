/*
 * MIT License
 *
 * Copyright (c) 2018-2022 Falkreon (Isaac Ellingson)
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

import java.io.Reader;

import javax.annotation.Nullable;

import blue.endless.jankson.api.document.PrimitiveElement;
import blue.endless.jankson.impl.io.AbstractStructuredDataReader;

public class JsonReader extends AbstractStructuredDataReader {
private final JsonReaderOptions options;
	
	private ElementType cur = ElementType.WHITESPACE;
	private PrimitiveElement value = PrimitiveElement.NULL;
	
	public JsonReader(Reader source) {
		this(source, new JsonReaderOptions());
	}
	
	public JsonReader(Reader source, JsonReaderOptions options) {
		super(source);
		this.options = options;
	}
	
	/**
	 * Gets the value at this location in the document.
	 * @return the value of the element we just parsed, if it has a value. Otherwise, PrimitiveElement.NULL.
	 */
	public PrimitiveElement getValue() {
		return (value==null) ? PrimitiveElement.NULL : value;
	}
	
	/**
	 * Advance one character and adjust the parser state.
	 * @return an ElementType if we're ready to pause and give the API user some information.
	 */
	private @Nullable ElementType advance() {
		return null;
	}
	
	@Override
	public ElementType next() {
		
		
		return ElementType.OBJECT_END;
	}
}
