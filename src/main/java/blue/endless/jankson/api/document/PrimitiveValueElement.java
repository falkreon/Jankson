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

public class PrimitiveValueElement implements ValueElement {
	Object value;
	
	public PrimitiveValueElement(String s) {
		value = s;
	}
	
	public PrimitiveValueElement(long l) {
		value = l; //force it to be a long and then box it ^_^
	}
	
	public PrimitiveValueElement(double d) {
		value = d; //force it into a double and then box it!
	}
	
	@Override
	public ValueElement asValueEntry() {
		//TODO: Convert this document node into a JsonPrimitive or JsonNull
		
		return null;
	}
	
	public String asString() {
		if (value==null) return "null";
		return value.toString();
	}
	
	//TODO: asEverythingElse
}
