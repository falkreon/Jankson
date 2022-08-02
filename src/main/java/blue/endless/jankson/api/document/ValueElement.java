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

package blue.endless.jankson.api.document;

import java.util.List;

public interface ValueElement extends DocumentElement {
	/**
	 * If there is a comment before this element which satisfies certain conditions, returns a List containing the
	 * comment and any non-semantic elements between the comment and this ValueElement. The conditions follow:
	 * <ul>
	 *   <li>The comment must precede this ValueElement
	 *   <li>There must be no ValueElements between the comment and this ValueElement
	 *   <li>The comment must be in the same "scope" (i.e. it must be within the same container which contains this
	 *       ValueElement, and must not be in any additional containers that this ValueElement is not in)
	 * </ul>
	 * 
	 * <p>Note that this is an editable list; however, undefined and undesirable behavior will result if you insert a
	 * ValueElement in here.
	 */
	//TODO: Find a good typesafe way to exclude ValueElements from this list
	public List<DocumentElement> getPreamble();
	
	@Override
	default boolean isValueElement() {
		return true;
	}
	
	@Override
	default ValueElement asValueElement() {
		return this;
	}
}
