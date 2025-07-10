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

package blue.endless.jankson.api.io;

import java.io.IOException;

import blue.endless.jankson.api.SyntaxError;
import blue.endless.jankson.api.function.CheckedFunction;

/**
 * A Deserializer is a kind of StructuredDataWriter. It's like a job that consumes StructuredData over time (as
 * it is written to this object), and produces a value when writing is complete.
 * @param <T> The kind of object this Deserializer produces
 */
public interface Deserializer<T> extends StructuredDataWriter {
	boolean isComplete();
	void write(StructuredData data) throws SyntaxError, IOException;
	T getResult();
	
	public static <S, T> Deserializer<T> map(Deserializer<S> deserializer, CheckedFunction<S, T, SyntaxError> mapper) {
		return new Mapper<S, T>(deserializer, mapper);
	}
	
	public class Mapper<S, T> implements Deserializer<T> {
		private final Deserializer<S> function;
		private final CheckedFunction<S, T, SyntaxError> mapper;
		private T result = null;
		
		public Mapper(Deserializer<S> function, CheckedFunction<S, T, SyntaxError> mapper) {
			this.function = function;
			this.mapper = mapper;
		}
		
		@Override
		public boolean isComplete() {
			return function.isComplete();
		}

		@Override
		public void write(StructuredData data) throws SyntaxError, IOException {
			function.write(data);
			if (function.isComplete()) result = mapper.apply(function.getResult());
		}

		@Override
		public T getResult() {
			return result;
		}
		
	}
}
