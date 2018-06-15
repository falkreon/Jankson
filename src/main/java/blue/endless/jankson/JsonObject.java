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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class JsonObject extends JsonElement implements Map<String, JsonElement> {
	private List<Entry> entries = new ArrayList<>();
	
	/**
	 * If there is an entry at this key, and that entry is a json object, return it. Otherwise returns null.
	 */
	@Nullable
	public JsonObject getObject(@Nonnull String name) {
		for(Entry entry : entries) {
			if (entry.key.equalsIgnoreCase(name)) {
				if (entry.value instanceof JsonObject) {
					return (JsonObject)entry.value;
				} else {
					return null;
				}
			}
		}
		
		return null;
	}
	
	
	
	/**
	 * Replaces a key-value mapping in this object if it exists, or adds the mapping to the end of the object if it
	 * doesn't. Returns the old value mapped to this key if there was one.
	 */
	public JsonElement put(@Nonnull String key, @Nonnull JsonElement elem, @Nullable String comment) {
		for(Entry entry : entries) {
			if (entry.key.equalsIgnoreCase(key)) {
				JsonElement result = entry.value;
				entry.value = elem;
				entry.comment = comment;
				return result;
			}
		}
		
		//If we reached here, there's no existing mapping, so make one.
		Entry entry = new Entry();
		entry.key = key;
		entry.value = elem;
		entry.comment = comment;
		entries.add(entry);
		return null;
	}
	
	public void putDefault(@Nonnull String key, @Nonnull JsonElement elem, String comment) {
		for(Entry entry : entries) {
			if (entry.key.equalsIgnoreCase(key)) {
				return;
			}
		}
		
		//If we reached here, there's no existing mapping, so make one.
		Entry entry = new Entry();
		entry.key = key;
		entry.value = elem;
		entry.comment = comment;
		entries.add(entry);
	}
	
	/**
	 * Returns the comment "attached to" a given key-value mapping, which is to say, the comment appearing immediately
	 * before it or the single-line comment to the right of it.
	 */
	@Nullable
	public String getComment(@Nonnull String name) {
		for(Entry entry : entries) {
			if (entry.key.equalsIgnoreCase(name)) {
				return entry.comment;
			}
		}
		
		return null;
	}
	
	public String toJson(boolean comments, boolean newlines) {
		return toJson(comments, newlines, 0);
	}
	
	public String toJson(boolean comments, boolean newlines, int depth) {
		StringBuilder builder = new StringBuilder();
		builder.append("{ ");
		if (newlines && entries.size()>0) builder.append('\n');
		
		for(int i=0; i<entries.size(); i++) {
			Entry entry = entries.get(i);
			
			if (newlines) {
				for(int j=0; j<depth+1; j++) {
					builder.append("\t");
				}
			}
			
			if (comments && entry.comment!=null) {
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
			
			builder.append("\"");
			builder.append(entry.key);
			builder.append("\": ");
			
			if (entry.value instanceof JsonObject) {
				builder.append(((JsonObject)entry.value).toJson(comments, newlines, depth+1));
			} else if (entry.value instanceof JsonArray) {
				builder.append(((JsonArray)entry.value).toJson(comments, newlines, depth+1));
			} else {
				builder.append(entry.value.toString());
			}
			
			if (i<entries.size()-1) {
				if (newlines) {
					builder.append(",\n");
				} else {
					builder.append(", ");
				}
			}
		}
		
		if (entries.size()>0) {
			if (newlines) {
				builder.append('\n');
				if (depth>0) for(int j=0; j<depth; j++) {
					builder.append("\t");
				}
			} else {
				builder.append(' ');
			}
		}
		
		builder.append("}");
		
		return builder.toString();
	}
	
	@Override
	public String toString() {
		return toJson(true, false, 0);
	}
	
	@Override
	public boolean equals(Object other) {
		if (other==null || !(other instanceof JsonObject)) return false;
		JsonObject otherObject = (JsonObject)other;
		if (entries.size()!=otherObject.entries.size()) return false;
		
		//Lists are identical sizes, but if the contents, comments, or ordering are at all different, fail them
		for(int i=0; i<entries.size(); i++) {
			Entry a = entries.get(i);
			Entry b = otherObject.entries.get(i);
			
			if (!a.equals(b)) return false;
		}
		
		return true;
	}
	
	@Override
	public int hashCode() {
		return entries.hashCode();
	}
	
	/**
	 * Gets a (potentially nested) element from this object if it exists.
	 * @param clazz The expected class of the element
	 * @param key   The keys of the nested elements, separated by periods, such as "foo.bar.baz"
	 * @return The element at that location, if it exists and is of the proper type, otherwise null.
	 */
	@SuppressWarnings("unchecked")
	@Nullable
	public <E extends JsonElement> E recursiveGet(@Nonnull Class<E> clazz, @Nonnull String key) {
		if (key.isEmpty()) throw new IllegalArgumentException("Cannot get from empty key");
		String[] parts = key.split("\\.");
		JsonObject cur = this;
		for(int i=0; i<parts.length; i++) {
			String s = parts[i];
			if (s.isEmpty()) throw new IllegalArgumentException("Cannot get from broken key '"+key+"'");
			JsonElement elem = cur.get(s);
			if (i<parts.length-1) {
				//elem must be a JsonObject or we're sunk
				if (elem instanceof JsonObject) {
					cur = (JsonObject) elem;
					continue;
				} else {
					return null;
				}
			} else {
				if (clazz.isAssignableFrom(elem.getClass())) {
					return (E) elem;
				} else {
					return null;
				}
			}
		}
		throw new IllegalArgumentException("Cannot get from broken key '"+key+"'");
	}
	
	/**
	 * Gets a (potentially nested) element from this object if it exists, or creates it and any intermediate objects
	 * needed to put it at the indicated location in the hierarchy.
	 * @param clazz The expected class of the element
	 * @param key   The keys of the nested elements, separated by periods, such as "foo.bar.baz"
	 * @return The element at that location if it exists, or the newly-created element if it did not previously exist.
	 */
	@SuppressWarnings("unchecked")
	public <E extends JsonElement> E recursiveGetOrCreate(@Nonnull Class<E> clazz, @Nonnull String key, @Nonnull E fallback, @Nullable String comment) {
		if (key.isEmpty()) throw new IllegalArgumentException("Cannot get from empty key");
		String[] parts = key.split("\\.");
		JsonObject cur = this;
		for(int i=0; i<parts.length; i++) {
			String s = parts[i];
			if (s.isEmpty()) throw new IllegalArgumentException("Cannot get from broken key '"+key+"'");
			JsonElement elem = cur.get(s);
			if (i<parts.length-1) {
				//elem must be a JsonObject or we're sunk
				if (elem instanceof JsonObject) {
					cur = (JsonObject) elem;
					continue;
				} else {
					JsonObject replacement = new JsonObject();
					cur.put(s, replacement);
					cur = replacement;
					continue;
				}
			} else {
				if (clazz.isAssignableFrom(elem.getClass())) {
					return (E) elem;
				} else {
					E result = (E) fallback.clone();
					cur.put(key, result, comment);
					return result;
				}
			}
		}
		
		throw new IllegalArgumentException("Cannot get from broken key '"+key+"'");
	}
	
	
	private static final class Entry {
		protected String comment;
		protected String key;
		protected JsonElement value;
		
		@Override
		public boolean equals(Object other) {
			if (other==null || !(other instanceof Entry)) return false;
			Entry o = (Entry)other;
			if (!comment.equals(o.comment)) return false;
			if (!key.equals(o.key)) return false;
			if (!value.equals(o.value)) return false;
			
			return true;
		}
		
		@Override
		public int hashCode() {
			return Objects.hash(comment, key, value);
		}
	}

	//IMPLEMENTATION for Cloneable
	
	@Override
	public JsonObject clone() {
		JsonObject result = new JsonObject();
		for(Entry entry : entries) {
			result.put(entry.key, entry.value.clone(), entry.comment);
		}
		return result;
	}
	
	//IMPLEMENTATION for Map<JsonElement>
	
	/**
	 * Replaces a key-value mapping in this object if it exists, or adds the mapping to the end of the object if it
	 * doesn't. Returns the old value mapped to this key if there was one.
	 */
	@Override
	@Nullable
	public JsonElement put(@Nonnull String key, @Nonnull JsonElement elem) {
		for(Entry entry : entries) {
			if (entry.key.equalsIgnoreCase(key)) {
				JsonElement result = entry.value;
				entry.value = elem;
				return result;
			}
		}
		
		//If we reached here, there's no existing mapping, so make one.
		Entry entry = new Entry();
		entry.key = key;
		entry.value = elem;
		entries.add(entry);
		return null;
	}
	
	@Override
	public void clear() {
		entries.clear();
	}

	@Override
	public boolean containsKey(@Nullable Object key) {
		if (key==null) return false;
		if (!(key instanceof String)) return false;
		
		for(Entry entry : entries) {
			if (entry.key.equalsIgnoreCase((String)key)) {
				return true;
			}
		}
		
		return false;
	}

	@Override
	public boolean containsValue(@Nullable Object val) {
		if (val==null) return false;
		if (!(val instanceof JsonElement)) return false;
		
		for(Entry entry : entries) {
			if (entry.value.equals(val)) return true;
		}
		
		return false;
	}
	
	/**
	 * Creates a semi-live shallow copy instead of a live view
	 */
	@Override
	public Set<Map.Entry<String, JsonElement>> entrySet() {
		Set<Map.Entry<String, JsonElement>> result = new HashSet<>();
		for(Entry entry : entries) {
			result.add(new Map.Entry<String, JsonElement>(){
				@Override
				public String getKey() {
					return entry.key;
				}

				@Override
				public JsonElement getValue() {
					return entry.value;
				}

				@Override
				public JsonElement setValue(JsonElement value) {
					JsonElement oldValue = entry.value;
					entry.value = value;
					return oldValue;
				}
				
			});
		}
		
		return result;
	}

	@Override
	@Nullable
	public JsonElement get(@Nullable Object key) {
		if (key==null || !(key instanceof String)) return null;
		
		for(Entry entry : entries) {
			if (entry.key.equalsIgnoreCase((String)key)) {
				return entry.value;
			}
		}
		return null;
	}

	@Override
	public boolean isEmpty() {
		return entries.isEmpty();
	}

	/** Returns a defensive copy instead of a live view */
	@Override
	@Nonnull
	public Set<String> keySet() {
		Set<String> keys = new HashSet<>();
		for(Entry entry : entries) {
			keys.add(entry.key);
		}
		return keys;
	}

	@Override
	public void putAll(Map<? extends String, ? extends JsonElement> map) {
		for(Map.Entry<? extends String, ? extends JsonElement> entry : map.entrySet()) {
			put(entry.getKey(), entry.getValue());
		}
	}

	@Override
	@Nullable
	public JsonElement remove(@Nullable Object key) {
		if (key==null || !(key instanceof String)) return null;
		
		for(int i=0; i<entries.size(); i++) {
			Entry entry = entries.get(i);
			if (entry.key.equalsIgnoreCase((String)key)) {
				return entries.remove(i).value;
			}
		}
		return null;
	}

	@Override
	public int size() {
		return entries.size();
	}

	@Override
	public Collection<JsonElement> values() {
		List<JsonElement> values = new ArrayList<>();
		for(Entry entry : entries) {
			values.add(entry.value);
		}
		return values;
	}
}
