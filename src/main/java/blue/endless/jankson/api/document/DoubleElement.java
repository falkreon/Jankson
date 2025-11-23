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

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.function.DoubleFunction;

import blue.endless.jankson.api.SyntaxError;
import blue.endless.jankson.api.io.StructuredData;
import blue.endless.jankson.api.io.StructuredDataWriter;

public final class DoubleElement extends PrimitiveElement {
	private final double value;
	
	public DoubleElement(double value) {
		this.value = value;
	}
	
	@Override
	public DoubleElement copy() {
		DoubleElement result = new DoubleElement(value);
		result.copyNonValueElementsFrom(this);
		return result;
	}

	@Override
	public void write(StructuredDataWriter writer) throws SyntaxError, IOException {
		for(NonValueElement elem : prologue) elem.write(writer);
		writer.write(StructuredData.primitive(this));
		for(NonValueElement elem : epilogue) elem.write(writer);
	}

	@Override
	public Optional<Object> getValue() {
		return Optional.of(value);
	}

	@Override
	public Optional<String> asString() {
		return Optional.of(Double.toString(value));
	}

	@Override
	public Optional<Boolean> asBoolean() {
		return Optional.empty();
	}

	@Override
	public OptionalDouble asDouble() {
		return OptionalDouble.of(value);
	}

	@Override
	public OptionalLong asLong() {
		long result = (long) value;
		if (((double) result) == value) return OptionalLong.of(result);
		
		return OptionalLong.empty();
	}

	@Override
	public OptionalInt asInt() {
		/* We'll do our best. Which is not very good.
		 * You'll notice a "double == double" comparison here. THIS IS INTENDED. We've proactively turned, e.g. "4.0"
		 * into a double precision floating point number. If and only if, after this conversion, we can still
		 * confidently say that it is a whole number (not possible for all whole numbers!), then go ahead and interpret
		 * it as an integer as desired. If there is any uncertainty, we must accurately report that there
		 * is no integer here.
		 */
		int result = (int) value;
		if (((double) result) == value) return OptionalInt.of(result);
		
		return OptionalInt.empty();
	}

	@Override
	public Optional<BigInteger> asBigInteger() {
		return Optional.empty();
	}

	@Override
	public Optional<BigDecimal> asBigDecimal() {
		return Optional.of(BigDecimal.valueOf(value));
	}
	
	@Override
	public boolean orElse(boolean value) {
		return Double.doubleToLongBits(this.value) != 0L;
	}
	
	@Override
	public double orElse(double value) {
		return this.value;
	}
	
	@Override
	public long orElse(long value) {
		long result = (long) this.value;
		if (result == this.value) return result;
		return value;
	}
	
	@Override
	public String orElse(String value) {
		return Double.toString(this.value);
	}
	
	public <T> Optional<T> mapAsDouble(DoubleFunction<T> d) {
		return Optional.of(d.apply(value));
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof DoubleElement v) {
			return super.equals(obj) && v.value == this.value;
		} else {
			return false;
		}
	}
	
	@Override
	public String toString() {
		return Double.toString(value);
	}
}
