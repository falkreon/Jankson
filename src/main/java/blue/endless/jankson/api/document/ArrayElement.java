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

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;

import blue.endless.jankson.api.document.view.ArrayElementValueView;

public class ArrayElement extends AbstractList<DocumentElement> implements ValueElement {
	protected CommentElement commentBefore;
	protected CommentElement commentAfter;
	protected List<DocumentElement> entries = new ArrayList<>();
	
	@Override
	public ValueElement asValueElement() {
		return this;
	}
	
	/**
	 * Gets a purely-semantic view of this element, ignoring comments and formatting. This can act surprisingly for
	 * inserts, removals, and clears, so be sure that this is really what you want! The returned view is "live", and
	 * will reflect any future changes to the original Document.
	 */
	public List<ValueElement> getValueView() {
		return new ArrayElementValueView(this);
	}
	
	//extends AbstractList<DocumentElement> {
	
		@Override
		public DocumentElement get(int index) {
			return entries.get(index);
		}
	
		@Override
		public int size() {
			return entries.size();
		}
		
		@Override
		public DocumentElement set(int index, DocumentElement element) {
			return entries.set(index, element);
		}
		
		@Override
		public void add(int index, DocumentElement element) {
			entries.add(index, element);
		}
		
		@Override
		public DocumentElement remove(int index) {
			return entries.remove(index);
		}
	
	//}
	
}
