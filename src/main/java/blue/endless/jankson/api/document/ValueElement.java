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

package blue.endless.jankson.api.document;

import java.util.List;

public interface ValueElement extends DocumentElement {
	/**
	 * Returns any NonValueElements between this ValueElement and the one preceding it, excluding any which occur on
	 * the same line as the preceding ValueElement (its epilogue). In other words, all the comments and such above this value.
	 */
	public List<NonValueElement> getPreamble();
	/**
	 * Returns any NonValueElements after this ValueElement, on the same line as the last line this ValueElement occupies.
	 * In other words, trailing comments and such on the same line as this value.
	 */
	public List<NonValueElement> getEpilogue();
	
	@Override
	default boolean isValueElement() {
		return true;
	}
	
	@Override
	default ValueElement asValueElement() {
		return this;
	}
	
	/**
	 * Strips all formatting elements and comments from this ValueElement. This is a shallow operation; wrapped values
	 * may retain their formatting.
	 * @return this object.
	 */
	public default ValueElement stripFormatting() {
		getPreamble().clear();
		getEpilogue().clear();
		
		return this;
	}
	
	/**
	 * Recursively strips all formatting elements and comments from this ValueElement and any child ValueElements.
	 * @return this object.
	 */
	public default ValueElement stripAllFormatting() {
		return stripFormatting();
	}
	
	@Override
	ValueElement clone();
}
