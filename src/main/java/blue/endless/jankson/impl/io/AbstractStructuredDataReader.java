/*
 * MIT License
 *
 * Copyright (c) 2018-2023 Falkreon (Isaac Ellingson)
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

package blue.endless.jankson.impl.io;

import java.io.Reader;
import java.util.ArrayDeque;
import java.util.Deque;

import javax.annotation.Nullable;

import blue.endless.jankson.api.document.PrimitiveElement;
import blue.endless.jankson.api.io.ElementType;
import blue.endless.jankson.api.io.StructuredDataReader;
import blue.endless.jankson.impl.context.ParserContext;

public class AbstractStructuredDataReader implements StructuredDataReader {
	private final Reader src;
	//private final CodePointReader in;
	private Deque<ParserContext<?>> context = new ArrayDeque<>();
	
	public AbstractStructuredDataReader(Reader src) {
		this.src = src;
		//if (src instanceof CodePointReader r) {
		//	in = r;
		//} else {
		//	in = new CodePointReader(src);
		//}
	}

	@Override
	public PrimitiveElement getValue() {
		// TODO Auto-generated method stub
		return null;
	}

	private @Nullable ElementType nextCharacter() {
		return null;
	}
	
	@Override
	public ElementType next() {
		
		
		
		// TODO Auto-generated method stub
		return null;
	}
}
