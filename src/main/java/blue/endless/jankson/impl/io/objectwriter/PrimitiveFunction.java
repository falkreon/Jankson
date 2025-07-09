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

package blue.endless.jankson.impl.io.objectwriter;

import blue.endless.jankson.api.SyntaxError;
import blue.endless.jankson.api.document.PrimitiveElement;
import blue.endless.jankson.api.io.StructuredData;

/**
 * Turns this value into a PrimitiveElement. If this value should *not* be a Primitive, throws a
 * SyntaxError. PrimitiveElement supports a number of different flexible marshalling tasks, so there's
 * no need for individual long/String/double/boolean functions.
 */
public class PrimitiveFunction extends SingleValueFunction<PrimitiveElement> {
	PrimitiveElement value = null;
	
	@Override
	public PrimitiveElement getResult() {
		return value;
	}

	@Override
	protected void process(StructuredData data) throws SyntaxError {
		if (data.type() == StructuredData.Type.PRIMITIVE) {
			value = data.asPrimitive();
		} else {
			// If it's not a primitive, but it's a comment or newline, then we're still okay.
			if (data.type().isSemantic()) throw new SyntaxError("Expected primitive value, found "+data.type().name());
		}
	}

}
