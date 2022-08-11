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
import java.util.List;

public class KeyValuePairElement implements DocumentElement {
	protected CommentElement commentBefore = null;
	protected CommentElement commentAfter = null;
	//protected List<DocumentElement> entries = new ArrayList<>();
	protected String key;
	protected ValueElement valueEntry;
	
	public KeyValuePairElement(String key, ValueElement value) {
		this.key = key;
		//entries.add(value);
		valueEntry = value;
	}
	
	public KeyValuePairElement(String key, DocumentElement value, String comment) {
		commentBefore = new CommentElement(comment);
	}
	
	public String getKey() {
		return key;
	}
	
	public ValueElement getValue() {
		return valueEntry;
	}
	
	public DocumentElement setValue(ValueElement value) {
		ValueElement result = valueEntry;
		/*
		for(int i=0; i<entries.size(); i++) {
			if (valueEntry==entries.get(i)) { //Because of this, keeping valueEntry and entries consistent is VERY IMPORTANT
				entries.set(i, value);
				valueEntry = value;
				return result;
			}
		}*/
		
		//No existing value entry?!?
		//entries.add(value);
		valueEntry = value;
		return null;
	}
	
	public KeyValuePairElement clone() {
		KeyValuePairElement result = new KeyValuePairElement(this.key, (ValueElement) this.valueEntry.clone());
		
		return result;
	}
}
