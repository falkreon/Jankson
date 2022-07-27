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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ArrayElement implements ValueElement {
	protected CommentElement commentBefore;
	protected CommentElement commentAfter;
	protected List<DocumentElement> entries = new ArrayList<>();
	
	public DocumentElement get(int index) {
		int cur = 0;
		for(DocumentElement entry : entries) {
			if (entry.isValueEntry()) {
				if (index==cur) return entry;
				cur++;
			}
		}
		
		throw new IndexOutOfBoundsException("Index: "+index+", Size: "+cur);
	}
	
	public int size() {
		int result = 0;
		for(DocumentElement entry : entries) {
			if (entry.isValueEntry()) result++;
		}
		
		return result;
	}
	
	/*
	public DocumentEntry iterator() {
		//TODO: Implement
	}*/
	
	public void add(DocumentElement entry) {
		this.entries.add(entry);
	}
	
	public int entrySize() {
		return entries.size();
	}
	
	public DocumentElement getEntry(int i) {
		return entries.get(i);
	}
	
	public Iterator<DocumentElement> entryIterator() {
		return entries.iterator();
	}
	
	public int elementCount() {
		int counter = 0;
		for(DocumentElement entry : entries) {
			if (!(entry instanceof CommentElement)) counter++;
			
			/*
			//Alternate way to do this
			if (entry instanceof ArrayDocumentEntry) counter++;
			if (entry instanceof ObjectDocumentEntry) counter++;
			if (entry instanceof PrimitiveValueDocumentEntry) counter++;
			*/
		}
		
		return counter;
	}

	@Override
	public ValueElement asValueEntry() {
		return this;
	}
}
