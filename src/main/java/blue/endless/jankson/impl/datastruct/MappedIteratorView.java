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
