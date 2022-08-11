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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public class PrimitiveElement implements ValueElement {
	public static PrimitiveElement NULL = new PrimitiveElement(null);
	
	protected boolean isDefault = false;
	protected List<NonValueElement> preamble = new ArrayList<>();
	protected Object value;
	protected List<NonValueElement> epilogue = new ArrayList<>();
	
	private PrimitiveElement(Object o) {
		value = o;
	}
	
	@Override
	public List<NonValueElement> getPreamble() {
		return preamble;
	}
	
	public Object getValue() {
		return value;
	}
	
	@Override
	public List<NonValueElement> getEpilogue() {
		return epilogue;
	}
	
	public String asString() {
		if (value==null) return "null";
		return value.toString();
	}
	
	public boolean asBoolean(boolean fallback) {
		return (value instanceof Boolean) ? (Boolean) value : fallback;
	}
	
	public double asDouble(double fallback) {
		return (value instanceof Number) ? ((Number) value).doubleValue() : fallback;
	}
	
	public float asFloat(float fallback) {
		return (value instanceof Number) ? ((Number) value).floatValue() : fallback;
	}
	
	public long asLong(long fallback) {
		return (value instanceof Number) ? ((Number) value).longValue() : fallback;
	}
	
	public int asInt(int fallback) {
		return (value instanceof Number) ? ((Number) value).intValue() : fallback;
	}
	
	public short asShort(short fallback) {
		return (value instanceof Number) ? ((Number) value).shortValue() : fallback;
	}
	
	public byte asByte(byte fallback) {
		return (value instanceof Number) ? ((Number) value).byteValue() : fallback;
	}
	
	public char asChar(char fallback) {
		if (value instanceof Number) {
			return (char)((Number) value).intValue();
		} else if (value instanceof String && ((String) value).length()==1) {
			return ((String) value).charAt(0);
		} else {
			return fallback;
		}
	}
	
	public BigInteger asBigInteger(BigInteger fallback) {
		if (value instanceof Number) {
			return BigInteger.valueOf( ((Number) value).longValue() );
		} else if (value instanceof String) {
			try {
				return new BigInteger((String) value, 16);
			} catch (NumberFormatException ex) {
				return fallback;
			}
		} else {
			return fallback;
		}
	}
	
	public BigDecimal asBigDecimal(BigDecimal fallback) {
		if (value instanceof Number) {
			return BigDecimal.valueOf( ((Number) value).doubleValue() );
		} else if (value instanceof String) {
			try {
				return new BigDecimal((String) value);
			} catch (NumberFormatException ex) {
				return fallback;
			}
		} else {
			return fallback;
		}
	}
	
	@Override
	public PrimitiveElement clone() {
		if (this==NULL) return this;
		
		PrimitiveElement result = new PrimitiveElement(this.value);
		for(NonValueElement elem : preamble) {
			result.preamble.add(elem.clone());
		}
		for(NonValueElement elem : epilogue) {
			result.epilogue.add(elem.clone());
		}
		
		result.isDefault = isDefault;
		
		return result;
	}
	
	public static PrimitiveElement ofNull() {
		return NULL;
	}
	
	public static PrimitiveElement of(String value) {
		if (value==null) return NULL;
		return new PrimitiveElement(value);
	}
	
	public static PrimitiveElement of(boolean value) {
		return new PrimitiveElement(value);
	}
	
	public static PrimitiveElement of(long value) {
		return new PrimitiveElement(value);
	}
	
	public static PrimitiveElement of(double value) {
		return new PrimitiveElement(value);
	}
	
	public static PrimitiveElement of(BigInteger value) {
		if (value==null) return NULL;
		return new PrimitiveElement(value.toString(16));
	}
	
	public static PrimitiveElement of(BigDecimal value) {
		if (value==null) return NULL;
		return new PrimitiveElement(value.toString());
	}
	
	@Override
	public boolean isDefault() {
		return isDefault;
	}
	
	@Override
	public void setDefault(boolean isDefault) {
		this.isDefault = isDefault;
	}
}
