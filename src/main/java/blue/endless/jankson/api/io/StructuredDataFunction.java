/*
 * MIT License
 *
 * Copyright (c) 2018-2024 Falkreon (Isaac Ellingson)
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
import java.util.Optional;
import java.util.function.Function;

import blue.endless.jankson.api.SyntaxError;

/**
 * A StructuredDataFunction is a job that consumes StructuredData over time, and produces a value
 * when complete.
 * @param <T> The return type of the function.
 */
public interface StructuredDataFunction<T> extends StructuredDataWriter {
	boolean isComplete();
	void write(StructuredData data) throws SyntaxError, IOException;
	T getResult();
	
	public class Mapper<S, T> implements StructuredDataFunction<T> {
		private final StructuredDataFunction<S> function;
		private final Function<S, Optional<T>> mapper;
		
		public Mapper(StructuredDataFunction<S> function, Function<S, Optional<T>> mapper) {
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
		}

		@Override
		public T getResult() {
			Optional<T> result = mapper.apply(function.getResult());
			return result.orElse(null);
		}
		
	}
}
