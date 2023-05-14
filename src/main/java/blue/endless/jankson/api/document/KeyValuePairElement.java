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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import blue.endless.jankson.api.io.StructuredDataWriter;

public class KeyValuePairElement implements DocumentElement, Map.Entry<String, ValueElement> {
	protected boolean isDefault = false;
	protected List<NonValueElement> preamble = new ArrayList<>();
	protected String key;
	protected List<NonValueElement> intermission = new ArrayList<>();
	protected ValueElement value;
	
	public KeyValuePairElement(String key, ValueElement value) {
		this.key = key;
		//entries.add(value);
		this.value = value;
	}
	
	public List<NonValueElement> getPreamble() {
		return preamble;
	}
	
	@Override
	public String getKey() {
		return key;
	}
	
	/**
	 * Gets NonValueElements after the key, but before the colon that separates the key and value. Should be left empty
	 * if possible.
	 */
	public List<NonValueElement> getIntermission() {
		return intermission;
	}
	
	@Override
	public ValueElement getValue() {
		return value;
	}
	
	public ValueElement setValue(ValueElement value) {
		ValueElement result = value;
		this.value = value;
		return result;
	}
	
	/**
	 * Clears out any non-semantic data such as comments or FormatElements attached to this KeyValuePairElement. Values
	 * may retain their formatting.
	 * @return this object.
	 */
	public KeyValuePairElement stripFormatting() {
		preamble.clear();
		intermission.clear();
		
		return this;
	}
	
	/**
	 * Clears out any non semantic data such as comments or FormatElements attached to this KeyValuePairElement *and*
	 * its value. No comments will remain.
	 * @return this object.
	 */
	public KeyValuePairElement stripAllFormatting() {
		preamble.clear();
		intermission.clear();
		value.getPrologue().clear();
		value.getEpilogue().clear();
		
		return this;
	}
	
	public KeyValuePairElement clone() {
		KeyValuePairElement result = new KeyValuePairElement(this.key, (ValueElement) this.value.clone());
		result.isDefault = isDefault;
		return result;
	}
	
	@Override
	public boolean isDefault() {
		return isDefault;
	}
	
	@Override
	public void setDefault(boolean isDefault) {
		this.isDefault = isDefault;
	}
	
	@Override
	public void write(StructuredDataWriter writer) throws IOException {
		for(NonValueElement elem : preamble) elem.write(writer);
		
		writer.writeKey(key);
		
		for(NonValueElement elem : intermission) elem.write(writer);
		
		writer.writeKeyValueDelimiter();
		
		value.write(writer);
	}
}
