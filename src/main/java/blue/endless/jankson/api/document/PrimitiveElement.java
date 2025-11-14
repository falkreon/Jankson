/*
 * MIT License
 *
 * Copyright (c) 2018-2025 Falkreon (Isaac Ellingson)
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
import java.util.function.DoubleFunction;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.LongFunction;

public abstract sealed class PrimitiveElement implements ValueElement permits BooleanElement, DoubleElement, LongElement, StringElement, NullElement {
	
	protected boolean isDefault = false;
	protected List<NonValueElement> prologue = new ArrayList<>();
	protected List<NonValueElement> epilogue = new ArrayList<>();
	
	@Override
	public List<NonValueElement> getPrologue() {
		return prologue;
	}
	
	@Override
	public List<NonValueElement> getEpilogue() {
		return epilogue;
	}
	
	/**
	 * Gets the value represented by this element, or empty if this element is a null literal or a synthetic missing-key element.
	 */
	public abstract Optional<Object> getValue();
	/**
	 * Returns true if this element represents a null literal, otherwise returns false.
	 */
	public boolean isNull() { return false; }
	
	/**
	 * If this value is a String, or an element stored in json as a String such as BigDecimal or BigInteger, returns the
	 * value as a String. Otherwise, this method returns empty.
	 */
	public abstract Optional<String> asString();
	
	/**
	 * Returns this value as a boolean if and only if it is a boolean value. Otherwise, this method returns empty.
	 */
	public abstract Optional<Boolean> asBoolean();
	
	/**
	 * If this value is a floating point value, or an integer or long type value which can be upcast to double with
	 * normal java semantics, this method will return that double value. Otherwise, empty will be returned.
	 */
	public abstract OptionalDouble asDouble();
	
	/**
	 * If this value is an integer or long value, this method will return that value. Otherwise, empty will be returned.
	 * String values that represent valid integers DO NOT count as integers here. If an integer value is specified with
	 * fractional precision like 42.0, it is considered a double and empty will be returned by this method.
	 */
	public abstract OptionalLong asLong();
	
	/**
	 * If the value is an integer value, that value will be returned. Otherwise, this method returns empty. Note that
	 * integer types do not exist at all in normal json5, and all integer types in Jankson have long precision, so some
	 * examples follow to clarify quirks behavior:
	 * <ul>
	 * <li>520 will return its exact value (it is a long value, but its exact value fits in an int)
	 * <li>2.0 will return empty (it is a double value)
	 * <li>"8" will return empty (it is a string value)
	 * <li>Infinity will return empty (it is a double value)
	 * <li>2147483648 will return empty (it is too big for its exact value to fit in an int)
	 * </ul>
	 */
	public abstract OptionalInt asInt();
	
	/**
	 * If this value can be interpreted as a boolean, map it and return a new optional with the
	 * mapped value. If not, returns an empty optional.
	 * Identical to {@code asBoolean().map(mapper)}.
	 * @param <T> The destination type of the mapper
	 * @param mapper A function to convert the value into an object of a different type
	 * @return The mapped object, or empty if the value cannot be interpreted this way.
	 */
	public <T> Optional<T> mapAsBoolean(Function<Boolean, T> mapper) {
		return asBoolean().map(mapper);
	}
	
	/**
	 * If this value can be interpreted as a double, map it and return a new optional with the
	 * mapped value. If not, returns an empty optional.
	 * @param <T> The destination type of the mapper
	 * @param mapper A function to convert the value into an object of a different type
	 * @return The mapped object, or empty if the value cannot be interpreted this way.
	 */
	public <T> Optional<T> mapAsDouble(DoubleFunction<T> mapper) {
		OptionalDouble doubleValue = asDouble();
		if (!doubleValue.isPresent()) return Optional.empty();
		
		return Optional.of(mapper.apply(doubleValue.getAsDouble()));
	}
	
	/**
	 * If this value can be interpreted as an integer, map it and return a new optional with the
	 * mapped value. If not, returns an empty optional.
	 * @param <T> The destination type of the mapper
	 * @param mapper A function to convert the value into an object of a different type
	 * @return The mapped object, or empty if the value cannot be interpreted this way.
	 */
	public <T> Optional<T> mapAsInt(IntFunction<T> mapper) {
		OptionalInt intValue = asInt();
		if (!intValue.isPresent()) return Optional.empty();
		
		return Optional.of(mapper.apply(intValue.getAsInt()));
	}
	
	/**
	 * If this value can be interpreted as a long, map it and return a new optional with the
	 * mapped value. If not, returns an empty optional.
	 * @param <T> The destination type of the mapper
	 * @param mapper A function to convert the value into an object of a different type
	 * @return The mapped object, or empty if the value cannot be interpreted this way.
	 */
	public <T> Optional<T> mapAsLong(LongFunction<T> mapper) {
		OptionalLong longValue = asLong();
		if (!longValue.isPresent()) return Optional.empty();
		
		return Optional.of(mapper.apply(longValue.getAsLong()));
	}
	
	/**
	 * If this value can be interpreted as a String, map it and return a new optional with the
	 * mapped value. If not, returns an empty optional. Null values will always return empty.
	 * Identical to {@code asString().map(mapper)}.
	 * @param <T> The destination type of the mapper
	 * @param mapper A function to convert the value into an object of a different type
	 * @return The mapped object, or empty if the value cannot be interpreted this way.
	 */
	public <T> Optional<T> mapAsString(Function<String, T> mapper) {
		return asString().map(mapper);
	}
	
	/**
	 * If this value is a base-16 String which can be parsed as a valid BigInteger, this method returns that value.
	 * If this is a long value, its BigInteger representation will be returned. Otherwise, the result will be empty.
	 */
	public abstract Optional<BigInteger> asBigInteger();
	
	/**
	 * If this value is String whose contents conform to BigDecimal's canonical String representation, its corresponding
	 * BigDecimal value will be returned. If this is a long or double value, it will be wrapped and returned as a
	 * BigDecimal. Otherwise, the result will be empty.
	 * @see BigDecimal#toString()
	 */
	public abstract Optional<BigDecimal> asBigDecimal();
	
	protected void copyNonValueElementsFrom(PrimitiveElement elem) {
		for(NonValueElement nv : elem.prologue) this.prologue.add(nv);
		for(NonValueElement nv : elem.epilogue) this.epilogue.add(nv);
	}
	
	public static PrimitiveElement ofNull() {
		return new NullElement();
	}
	
	public static PrimitiveElement of(String value) {
		if (value==null) return ofNull();
		return new StringElement(value);
	}
	
	public static PrimitiveElement of(boolean value) {
		return new BooleanElement(value);
	}
	
	public static PrimitiveElement of(long value) {
		return new LongElement(value);
	}
	
	public static PrimitiveElement of(double value) {
		return new DoubleElement(value);
	}
	
	public static PrimitiveElement of(BigInteger value) {
		if (value==null) return ofNull();
		return new StringElement(value.toString(16));
	}
	
	public static PrimitiveElement of(BigDecimal value) {
		if (value==null) return ofNull();
		return new StringElement(value.toString());
	}
	
	public static boolean canBox(Object value) {
		return
				value == null ||
				value instanceof String ||
				value instanceof Boolean ||
				value instanceof Character ||
				value instanceof Byte ||
				value instanceof Short ||
				value instanceof Integer ||
				value instanceof Long ||
				value instanceof Float ||
				value instanceof Double ||
				value instanceof BigInteger ||
				value instanceof BigDecimal;
	}
	
	public static PrimitiveElement box(Object value) throws IllegalArgumentException {
		if (value == null) return ofNull();
		
		if (value instanceof String v)     return of(v);
		if (value instanceof Boolean v)    return of(v.booleanValue());
		if (value instanceof Character v)  return of(""+v); // We could have chosen Long but 1-character String is semantically closer
		if (value instanceof Byte v)       return of(v);
		if (value instanceof Short v)      return of(v);
		if (value instanceof Integer v)    return of(v);
		if (value instanceof Long v)       return of(v);
		if (value instanceof Float v)      return of(v);
		if (value instanceof Double v)     return of(v);
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
	public abstract PrimitiveElement copy();
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof PrimitiveElement prim) {
			return
					prologue.equals(prim.prologue) &&
					epilogue.equals(prim.epilogue);
		} else {
			return false;
		}
	}
}
