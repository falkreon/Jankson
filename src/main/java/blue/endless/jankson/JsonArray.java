/*
 * MIT License
 *
 * Copyright (c) 2018 Falkreon
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

package blue.endless.jankson;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import blue.endless.jankson.impl.Marshaller;

public class JsonArray extends JsonElement implements Collection<JsonElement>, Iterable<JsonElement> {
	private List<Entry> entries = new ArrayList<>();
	protected Marshaller marshaller = Marshaller.getFallback();
	
	public JsonArray() {}
	
	public <T> JsonArray(T[] ts, Marshaller marshaller) {
		this.marshaller = marshaller;
		for(T t : ts) {
			this.add(marshaller.serialize(t));
		}
	}
	
	public JsonArray(Collection<?> ts, Marshaller marshaller) {
		this.marshaller = marshaller;
		for(Object t : ts) {
			this.add(marshaller.serialize(t));
		}
	}
	
	public JsonElement get(int i) {
		return entries.get(i).value;
	}
	
	public String getComment(int i) {
		return entries.get(i).comment;
	}
	
	@Override
	public String toJson(boolean comments, boolean newlines) {
		return toJson(comments, newlines, 0);
	}
	
	public String toJson(boolean comments, boolean newlines, int depth) {
		StringBuilder builder = new StringBuilder();
		
		builder.append("[ ");
		
		if (newlines) {
			builder.append('\n');
			for(int j=0; j<depth+1; j++) {
				builder.append("\t");
			}
		}
		
		for(int i=0; i<entries.size(); i++) {
			Entry entry = entries.get(i);
			
			if (entry.comment!=null) {
				builder.append("/* ");
				builder.append(entry.comment);
				builder.append(" */ ");
				
				if (newlines) {
					builder.append('\n');
					for(int j=0; j<depth+1; j++) {
						builder.append("\t");
					}
				}
			}
			
			builder.append(entry.value.toString());
			if (i<entries.size()-1) {
				if (newlines) {
					builder.append(",\n");
					for(int j=0; j<depth+1; j++) {
						builder.append("\t");
					}
				} else {
					builder.append(", ");
				}
			}
		}
		if (entries.size()>0) {
			if (newlines) {
				builder.append('\n');
				for(int j=0; j<depth; j++) {
					builder.append("\t");
				}
			} else {
				builder.append(' ');
			}
		}
		
		builder.append(']');
		
		return builder.toString();
	}
	
	public String toString() {
		return toJson(true, false, 0);
	}
	
	public boolean add(@Nonnull JsonElement e, String comment) {
		if (contains(e)) return false;
		
		Entry entry = new Entry();
		entry.value = e;
		entry.comment = comment;
		entries.add(entry);
		return true;
	}
	
	@Override
	public boolean equals(Object other) {
		if (other==null || !(other instanceof JsonArray)) return false;
		
		List<Entry> a = this.entries;
		List<Entry> b = ((JsonArray)other).entries;
		if (a.size()!=b.size()) return false;
		
		for(int i=0; i<a.size(); i++) {
			Entry ae = a.get(i);
			Entry be = b.get(i);
			if (!ae.value.equals(be.value)) return false;
			if (!Objects.equals(ae.comment, be.comment)) return false;
		}
		
		return true;
	}
	
	@Nullable
	public <E> E get(@Nonnull Class<E> clazz, int index) {
		JsonElement elem = get(index);
		return marshaller.marshall(clazz, elem);
	}
	
	public void setMarshaller(Marshaller marshaller) {
		this.marshaller = marshaller;
	}
	
	//IMPLEMENTATION for Cloneable
	@Override
	public JsonArray clone() {
		JsonArray result = new JsonArray();
		result.marshaller = marshaller;
		for(Entry entry : entries) {
			result.add(entry.value.clone(), entry.comment);
		}
		return result;
	}
	
	//IMPLEMENTATION for Collection<JsonElement>
	
	@Override
	public int size() {
		return entries.size();
	}
	
	@Override
	public boolean add(@Nonnull JsonElement e) {
		Entry entry = new Entry();
		entry.value = e;
		entries.add(entry);
		return true;
	}
	
	@Override
	public boolean addAll(Collection<? extends JsonElement> c) {
		boolean result = false;
		for(JsonElement elem : c) result |= add(elem);
		
		return result;
	}
	
	@Override
	public void clear() {
		entries.clear();
	}
	
	@Override
	public boolean contains(Object o) {
		if (o==null || !(o instanceof JsonElement)) return false;
		
		for(Entry entry : entries) {
			if (entry.value.equals(o)) return true;
		}
		return false;
	}
	
	@Override
	public boolean containsAll(Collection<?> c) {
		for(Object o : c) {
			if (!contains(o)) return false;
		}
		
		return true;
	}
	
	@Override
	public boolean isEmpty() {
		return entries.isEmpty();
	}
	
	@Override
	public boolean remove(Object o) {
		for(int i=0; i<entries.size(); i++) {
			Entry cur = entries.get(i);
			if (cur.value.equals(o)) {
				entries.remove(i);
				return true;
			}
		}
		return false;
	}
	
	@Override
	public boolean removeAll(Collection<?> c) {
		throw new UnsupportedOperationException("removeAll not supported");
	}
	
	@Override
	public boolean retainAll(Collection<?> c) {
		throw new UnsupportedOperationException("retainAll not supported");
	}
	
	@Override
	public JsonElement[] toArray() {
		JsonElement[] result = new JsonElement[entries.size()];
		for(int i=0; i<entries.size(); i++) {
			result[i] = entries.get(i).value;
		}
		return result;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public <T> T[] toArray(T[] a) {
		if (a.length<entries.size()) a = (T[]) new Object[entries.size()];
		for(int i=0; i<entries.size(); i++) {
			a[i] = (T)entries.get(i).value;
		}
		if (a.length>entries.size()) {
			a[entries.size()] = null; //Little-known and basically unused quirk of the toArray contract
		}
		return a;
	}
	
	
	//IMPLEMENTATION for Iterable<JsonElement>
	
	@Override
	public Iterator<JsonElement> iterator() {
		return new EntryIterator(entries);
	}
	
	
	//MISC CLASSES
	
	private static class EntryIterator implements Iterator<JsonElement> {
		private final Iterator<Entry> delegate;
		
		public EntryIterator(List<Entry> list) {
			delegate = list.iterator();
		}
		
		@Override
		public boolean hasNext() {
			return delegate.hasNext();
		}

		@Override
		public JsonElement next() {
			return delegate.next().value;
		}
		
		@Override
		public void remove() {
			delegate.remove();
		}
	}
	
	private static class Entry {
		String comment;
		JsonElement value;
	}
}
