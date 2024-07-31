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

package blue.endless.jankson.impl.io.objectwriter;

import blue.endless.jankson.api.io.StructuredData;
import static blue.endless.jankson.api.io.StructuredData.Type.*;

import java.io.IOException;

import blue.endless.jankson.api.SyntaxError;

public abstract class SingleValueFunction<T> implements StructuredDataFunction<T> {
	private int nestingLevel = 0;
	private boolean complete = false;
	
	@Override
	public boolean isComplete() {
		return complete;
	}

	@Override
	public void write(StructuredData data) throws SyntaxError, IOException {
		if (complete) return;
		
		process(data);
		
		if (nestingLevel == 0 && data.type() == PRIMITIVE) {
			// Simple case: Our entire value data stream consists of one StructuredData element.
			complete = true;
			
		} else if (data.type() == OBJECT_START || data.type() == ARRAY_START) {
			// We are opening an object or array. We want to continue to process data until we walk
			// back to the root level of the tree
			
			nestingLevel++;
		} else if (data.type() == OBJECT_END || data.type() == ARRAY_END) {
			// Continue to keep track of the nesting level as we return from nested objects
			
			nestingLevel--;
			
			// The instant we return to the nesting level we started at, we MUST have collected all
			// the data for a single ValueElement. Stop accepting data immediately.
			if (nestingLevel <= 0) {
				complete = true;
			}
		}
	}
	
	protected abstract void process(StructuredData data) throws SyntaxError, IOException;
	
	public static <T> SingleValueFunction<T> discard() {
		return new Discard<T>();
	}
	
	private static class Discard<T> extends SingleValueFunction<T> {
		@Override
		public T getResult() { return null; }
		
		@Override
		protected void process(StructuredData data) throws SyntaxError {}
	}
}
