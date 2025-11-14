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
import java.util.function.LongFunction;

import blue.endless.jankson.api.SyntaxError;
import blue.endless.jankson.api.io.StructuredData;
import blue.endless.jankson.api.io.StructuredDataWriter;

public final class LongElement extends PrimitiveElement {
	private final long value;
	
	public LongElement(long value) {
		this.value = value;
	}
	
	@Override
	public LongElement copy() {
		LongElement result = new LongElement(value);
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
		return Optional.of(Long.toString(value));
	}

	@Override
	public Optional<Boolean> asBoolean() {
		return Optional.of(value != 0L);
	}

	@Override
	public OptionalDouble asDouble() {
		return OptionalDouble.of(value);
	}

	@Override
	public OptionalLong asLong() {
		return OptionalLong.of(value);
	}

	@Override
	public OptionalInt asInt() {
		try {
			return OptionalInt.of(Math.toIntExact(value));
		} catch (ArithmeticException ex) {
			return OptionalInt.empty();
		}
	}
	
	@Override
	public <T> Optional<T> mapAsLong(LongFunction<T> mapper) {
		return Optional.of(mapper.apply(value));
	}
	
	@Override
	public Optional<BigInteger> asBigInteger() {
		return Optional.of(BigInteger.valueOf(value));
	}

	@Override
	public Optional<BigDecimal> asBigDecimal() {
		return Optional.of(BigDecimal.valueOf(value));
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof LongElement v) {
			return super.equals(obj) && v.value == this.value;
		} else {
			return false;
		}
	}
	
	@Override
	public String toString() {
		return Long.toString(value);
	}
}
