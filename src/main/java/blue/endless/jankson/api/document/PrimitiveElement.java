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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;

import blue.endless.jankson.impl.document.BooleanElementImpl;
import blue.endless.jankson.impl.document.DoubleElementImpl;
import blue.endless.jankson.impl.document.LongElementImpl;
import blue.endless.jankson.impl.document.NullElementImpl;
import blue.endless.jankson.impl.document.StringElementImpl;

public abstract class PrimitiveElement implements ValueElement {
	
	protected boolean isDefault = false;
	protected List<NonValueElement> preamble = new ArrayList<>();
	protected List<NonValueElement> epilogue = new ArrayList<>();
	
	@Override
	public List<NonValueElement> getPreamble() {
		return preamble;
	}
	
	@Override
	public List<NonValueElement> getEpilogue() {
		return epilogue;
	}
	
	public abstract Optional<Object> getValue();
	
	public abstract Optional<String> asString();
	public abstract Optional<Boolean> asBoolean();
	public abstract OptionalDouble asDouble();
	public abstract OptionalLong asLong();
	public abstract OptionalInt asInt();
	public abstract Optional<BigInteger> asBigInteger();
	public abstract Optional<BigDecimal> asBigDecimal();
	
	protected void copyNonValueElementsFrom(PrimitiveElement elem) {
		for(NonValueElement nv : elem.preamble) this.preamble.add(nv);
		for(NonValueElement nv : elem.epilogue) this.epilogue.add(nv);
	}
	
	public static PrimitiveElement ofNull() {
		return new NullElementImpl();
	}
	
	public static PrimitiveElement of(String value) {
		if (value==null) return ofNull();
		return new StringElementImpl(value);
	}
	
	public static PrimitiveElement of(boolean value) {
		return new BooleanElementImpl(value);
	}
	
	public static PrimitiveElement of(long value) {
		return new LongElementImpl(value);
	}
	
	public static PrimitiveElement of(double value) {
		return new DoubleElementImpl(value);
	}
	
	public static PrimitiveElement of(BigInteger value) {
		if (value==null) return ofNull();
		return new StringElementImpl(value.toString(16));
	}
	
	public static PrimitiveElement of(BigDecimal value) {
		if (value==null) return ofNull();
		return new StringElementImpl(value.toString());
	}
	
	public static PrimitiveElement box(Object value) throws IllegalArgumentException {
		if (value==null) return ofNull();
		
		if (value instanceof String v)  return of(v);
		if (value instanceof Boolean v) return of(v);
		if (value instanceof Long v)    return of(v);
		if (value instanceof Double v)  return of(v);
		if (value instanceof BigInteger v) return of(v);
		if (value instanceof BigDecimal v) return of(v);
		
		throw new IllegalArgumentException("Objects of type "+value.getClass().getCanonicalName()+" cannot be boxed as a PrimitiveElement.");
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
	public abstract ValueElement clone();
}
