package blue.endless.jankson.impl.datastruct;

import java.util.Iterator;

public class FilteredIteratorView<T, U extends T> implements Iterator<U> {
	private final Iterator<T> delegate;
	private final Class<U> clazz;
	private U next;
	
	public FilteredIteratorView(Iterator<T> delegate, Class<U> clazz) {
		this.delegate = delegate;
		this.clazz = clazz;
		
		loadNext();
	}
	
	@SuppressWarnings("unchecked")
	private void loadNext() {
		next = null;
		while(delegate.hasNext()) {
			T t = delegate.next();
			if (clazz.isInstance(t)) {
				next = (U) t;
				return;
			}
		}
	}

	@Override
	public boolean hasNext() {
		return (next != null);
	}

	@Override
	public U next() {
		U result = next;
		loadNext();
		return result;
	}
	
	public static <T, U extends T> Iterable<U> iterableOf(Iterable<T> iterable, Class<U> clazz) {
		return () -> new FilteredIteratorView<T, U>(iterable.iterator(), clazz);
	}
}
