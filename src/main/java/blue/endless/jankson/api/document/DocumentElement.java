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

package blue.endless.jankson.api.document;

import java.io.IOException;

import blue.endless.jankson.api.SyntaxError;
import blue.endless.jankson.api.io.StructuredDataWriter;

/**
 * Represents a Document or part of a Document. Elements can be one of three types: Comment, Value, or Formatting.
 * 
 * <p>CommentElements are comments. They could be single-line or multi-line, and if single-line may occur at the end
 * of an object or key-value pair.
 * 
 * <p>ValueElements are anything semantic - key-value pairs, objects, arrays, and primitives.
 * 
 * <p>FormattingElements, at this moment, are line breaks.
 */
public sealed interface DocumentElement permits ValueElement, NonValueElement, KeyValuePairElement {
	default boolean isCommentElement() { return false; }
	default CommentElement asCommentElement() { throw new UnsupportedOperationException(); }
	default boolean isValueElement() { return false; }
	default ValueElement asValueElement() { throw new UnsupportedOperationException(); }
	default boolean isFormattingElement() { return false; }
	default FormattingElement asFormattingElement() { throw new UnsupportedOperationException(); }
	
	/**
	 * Makes a deep copy of this DocumentElement, formatting and all, and returns it.
	 */
	public DocumentElement copy();
	
	/**
	 * Gets whether this DocumentElement has been marked as a default value. Default values are typically identical to
	 * the equivalent entry in some reference object (such as a default config).
	 */
	public boolean isDefault();
	
	/**
	 * Marks this DocumentElement as default (or non-default) to indicate whether this value matches some equivalent
	 * entry in a reference object (such as a default config).
	 * @param isDefault true to mark this element as default; false to mark this element as non-default.
	 */
	public void setDefault(boolean isDefault);
	
	public void write(StructuredDataWriter writer) throws SyntaxError, IOException;
}
