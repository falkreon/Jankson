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

package blue.endless.jankson.api.document.view;

import java.util.AbstractList;

import blue.endless.jankson.api.document.ArrayElement;
import blue.endless.jankson.api.document.DocumentElement;
import blue.endless.jankson.api.document.ValueElement;

/**
 * EXPERIMENTAL, NEEDS OPTIMIZING: Value-only view of the underlying document element.
 */
public class ArrayElementValueView extends AbstractList<ValueElement> {
	private ArrayElement parent;
	
	public ArrayElementValueView(ArrayElement parent) {
		this.parent = parent;
	}
	
	private int getIndex(int valueIndex) {
		int index = -1;
		for(int i=0; i<parent.size(); i++) {
			if (parent.get(i).isValueElement()) index++;
			if (index==valueIndex) return i;
		}
		return -1;
	}
	
	@Override
	public ValueElement get(int index) {
		return (ValueElement) parent.get(getIndex(index));
	}
	
	@Override
	public ValueElement set(int index, ValueElement element) {
		DocumentElement result = parent.set(getIndex(index), element);
		return (result.isValueElement()) ? result.asValueElement() : null;
	}
	
	@Override
	public void add(int index, ValueElement element) {
		parent.add(getIndex(index), element);
	}
	
	@Override
	public ValueElement remove(int index) {
		return (ValueElement) parent.remove(getIndex(index));
	}
	
	@Override
	public int size() {
		int result = 0;
		for(DocumentElement elem : parent) {
			if (elem.isValueElement()) result++;
		}
		return result;
	}
}
