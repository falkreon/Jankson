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

package blue.endless.jankson.impl.datastruct;

import java.util.Iterator;
import java.util.function.Function;

public class MappedIteratorView<T, U> implements Iterator<U> {
	private final Iterator<T> delegate;
	private final Function<T, U> mapper;
	
	public MappedIteratorView(Iterator<T> iterator, Function<T, U> mapper) {
		this.delegate = iterator;
		this.mapper = mapper;
	}
	
	public MappedIteratorView(Iterable<T> iterable, Function<T, U> mapper) {
		this.delegate = iterable.iterator();
		this.mapper = mapper;
	}
	
	@Override
	public boolean hasNext() {
		return delegate.hasNext();
	}

	@Override
	public U next() {
		return mapper.apply(delegate.next());
	}
	
	@Override
	public void remove() {
		delegate.remove();
	}
	
	public static <T, U> Iterable<U> iterableOf(Iterable<T> iterable, Function<T, U> mapper) {
		return () -> new MappedIteratorView<T, U>(iterable, mapper);
	}
}
