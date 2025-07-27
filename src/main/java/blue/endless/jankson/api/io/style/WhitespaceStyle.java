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

package blue.endless.jankson.api.io.style;

/**
 * Specifies how much whitespace and what kinds will be emitted by a StructuredDataWriter. While several writers use
 * the same enum, the exact behavior may vary depending on the semantics of the target file format.
 * 
 * <p>
 * Note that this enum does not govern tabs versus spaces. Check your writer options for an indent value.
 */
public enum WhitespaceStyle {
	/**
	 * Emit no unnecessary whitespace. The output will be minified.
	 */
	COMPACT,
	
	/**
	 * Spaces will be used to pad around key/value and make the output more readable, but newlines and indents will not
	 * be used.
	 */
	SPACES_ONLY,
	
	/**
	 * Spaces, newlines, and indents will be used to make the output as readable as possible.
	 */
	PRETTY;
}
