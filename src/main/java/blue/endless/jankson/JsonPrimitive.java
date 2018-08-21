/*
 * MIT License
 *
 * Copyright (c) 2018 Falkreon
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

package blue.endless.jankson;

import java.util.Objects;
import java.util.regex.Matcher;

import javax.annotation.Nonnull;

public class JsonPrimitive extends JsonElement {
	@Nonnull
	private Object value;
	
	public JsonPrimitive(@Nonnull Object value) {
		this.value = value;
	}

	@Nonnull
	public String asString() {
		if (value==null) return "null";
		return value.toString();
	}
	
	@Nonnull
	public String toString() {
		return toJson();
	}
	
	@Nonnull
	public Object getValue() {
		return value;
	}
	
	@Override
	public boolean equals(Object other) {
		if (other==null) return false;
		if (other instanceof JsonPrimitive) {
			return Objects.equals(value, ((JsonPrimitive)other).value);
		} else {
			return false;
		}
	}
	
	@Override
	public int hashCode() {
		return value.hashCode();
	}
	
	public static String escape(String s) {
		StringBuilder result = new StringBuilder();
		for(int i=0; i<s.length(); i++) {
			char ch = s.charAt(i);
			
			switch(ch) {
			case '\u0008':
				result.append("\\b");
				break;
			case '\f':
				result.append("\\f");
				break;
			case '\n':
				result.append("\\n");
				break;
			case '\r':
				result.append("\\r");
				break;
			case '\t':
				result.append("\\t");
				break;
			case '"':
				result.append("\\\"");
				break;
			case '\\':
				result.append("\\\\");
				break;
			default:
				result.append(ch);
			}
		}
		
		return result.toString();
	}
	
	@Override
	public String toJson(boolean comments, boolean newlines, int depth) {
		if (value==null) return "null";
		if (value instanceof Number) {
			return value.toString();
		}
		if (value instanceof Boolean) return value.toString();
		
		return '\"'+escape(value.toString())+'\"';
	}
	
	//IMPLEMENTATION for Cloneable
	@Override
	public JsonPrimitive clone() {
		return this;
	}
}
