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
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class JsonObject extends JsonElement {
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
	
	public String toString() {
		return toJson(true, false, 0);
	}
	
	private static final class Entry {
		protected String comment;
		protected String key;
		protected JsonElement value;
	}
}
