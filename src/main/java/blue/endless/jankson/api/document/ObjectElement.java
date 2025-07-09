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
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nullable;

import blue.endless.jankson.api.SyntaxError;
import blue.endless.jankson.api.io.JsonWriter;
import blue.endless.jankson.api.io.StructuredData;
import blue.endless.jankson.api.io.StructuredDataWriter;

public class ObjectElement implements ValueElement, Map<String, ValueElement>, Iterable<KeyValuePairElement> {
	protected boolean isDefault = false;
	protected List<NonValueElement> prologue = new ArrayList<>();
	protected List<KeyValuePairElement> entries = new ArrayList<>();
	protected List<NonValueElement> footer = new ArrayList<>();
	protected List<NonValueElement> epilogue = new ArrayList<>();
	
	@Override
	public List<NonValueElement> getPrologue() {
		return prologue;
	}
	
	/**
	 * Gets NonValueElements following the last key-value pair in this ObjectElement
	 */
	public List<NonValueElement> getFooter() {
		return footer;
	}
	
	@Override
	public List<NonValueElement> getEpilogue() {
		return epilogue;
	}
	
	public void add(KeyValuePairElement entry) {
		entries.add(entry);
	}
	
	@Override
	public ValueElement stripFormatting() {
		prologue.clear();
		footer.clear();
		epilogue.clear();
		
		return this;
	}
	
	@Override
	public ObjectElement stripAllFormatting() {
		prologue.clear();
		
		for(KeyValuePairElement elem : entries) {
			elem.stripAllFormatting();
		}
		
		footer.clear();
		epilogue.clear();
		
		return this;
	}
	
	public ObjectElement clone() {
		ObjectElement result = new ObjectElement();
		for(NonValueElement elem : prologue) {
			result.prologue.add(elem.clone());
		}
		
		for(KeyValuePairElement elem : entries) {
			result.entries.add(elem.clone());
		}
		
		for(NonValueElement elem : footer) {
			result.footer.add(elem.clone());
		}
		
		for(NonValueElement elem : epilogue) {
			result.epilogue.add(elem.clone());
		}
		
		result.isDefault = isDefault;
		
		return result;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof ObjectElement elem) {
			if (!prologue.equals(elem.prologue)) return false;
			if (!footer.equals(elem.footer)) return false;
			if (!epilogue.equals(elem.epilogue)) return false;
			if (!entries.equals(elem.entries)) return false;
			
			return true;
		} else {
			return false;
		}
	}
	
	@Override
	public boolean isDefault() {
		return isDefault;
	}
	
	@Override
	public void setDefault(boolean isDefault) {
		this.isDefault = isDefault;
	}
	
	public void write(StructuredDataWriter writer) throws SyntaxError, IOException {
		for(NonValueElement elem : prologue) elem.write(writer);
		
		writer.write(StructuredData.OBJECT_START);
		
		for(KeyValuePairElement elem : entries) elem.write(writer);
		
		for(NonValueElement elem : footer) elem.write(writer);
		
		writer.write(StructuredData.OBJECT_END);
		
		for(NonValueElement elem : epilogue) elem.write(writer);
	}
	
	public String toString() {
		StringWriter w = new StringWriter();
		JsonWriter v = new JsonWriter(w);
		
		try {
			this.write(v);
		} catch (SyntaxError | IOException e) {
			throw new RuntimeException(e);
		}
		
		w.flush();
		return w.toString();
	}

	/**
	 * Gets an Optional KeyValuePairElement matching the provided key.
	 * If no such element exist an empty Optional is returned.
	 * @param key the key whose corresponding entry should be returned
	 * @return Optional with the entry if it is present.
	 */
	public Optional<KeyValuePairElement> getKeyValuePair(String key) {
		for (KeyValuePairElement element : entries) {
			if (Objects.equals(element.key, key)) {
				return Optional.of(element);
			}
		}
		return Optional.empty();
	}

	/**
	 * Gets a primtive element if it exists in this object. This method cannot return null values, only
	 * PrimitiveElements representing the null value. If no element exists with the specified key, or the element
	 * at the specified key is not a primitive, then a synthetic null literal element will be returned.
	 * @param key the key whose corresponding value should be returned
	 * @return The value if it is present and primitive, otherwise a synthetic element representing null.
	 */
	public PrimitiveElement getPrimitive(String key) {
		for(KeyValuePairElement entry : entries) {
			if (entry.getKey().equals(key) && entry.getValue() instanceof PrimitiveElement prim) {
				return prim;
			}
		}
		
		return PrimitiveElement.ofNull();
	}
	
	/**
	 * Tries to get a PrimitiveElement value for the given key. If no element exists with the specified
	 * key, or the element at the specified key is not a primitive, then an empty optional will be returned.
	 * @param key the key whose corresponding value should be returned
	 * @return An optional containing the PrimitiveElement if it exists, otherwise empty.
	 */
	public Optional<PrimitiveElement> tryGetPrimitive(String key) {
		for(KeyValuePairElement entry : entries) {
			if (entry.getKey().equals(key) && entry.getValue() instanceof PrimitiveElement prim) {
				return Optional.of(prim);
			}
		}
		
		return Optional.empty();
	}
	
	/**
	 * Gets an array element if it exists in this object. If no element is mapped to the key, or if the value mapped to
	 * the key is not an array, an empty array will be returned.
	 * @param key the key whose corresponding value should be returned
	 * @return the value if it is present and an array, otherwise a synthetic empty array representing the missing element.
	 */
	public ArrayElement getArray(String key) {
		for(KeyValuePairElement entry : entries) {
			if (entry.getKey().equals(key) && entry.getValue() instanceof ArrayElement arr) {
				return arr;
			}
		}
		
		return new ArrayElement();
	}
	
	/**
	 * Tries to get an ArrayElement with the specified key. If no element exists with the specified key,
	 * or that element is not an ArrayElement, then empty is returned.
	 * @param key the key whose corresponding value should be returned
	 * @return An optional containing the ArrayElement if it exists, otherwise empty.
	 */
	public Optional<ArrayElement> tryGetArray(String key) {
		for(KeyValuePairElement entry : entries) {
			if (entry.getKey().equals(key) && entry.getValue() instanceof ArrayElement arr) {
				return Optional.of(arr);
			}
		}
		
		return Optional.empty();
	}
	
	/**
	 * Gets an object element if it exists in this object. If no element is mapped to the key, or if the value mapped to
	 * the key is not an object, an empty object will be returned.
	 * @param key the key whose corresponding value should be returned
	 * @return the value if it is present and an object, otherwise a synthetic empty object representing the missing element.
	 */
	public ObjectElement getObject(String key) {
		for(KeyValuePairElement entry : entries) {
			if (entry.getKey().equals(key) && entry.getValue() instanceof ObjectElement obj) {
				return obj;
			}
		}
		
		return new ObjectElement();
	}
	
	/**
	 * Tries to get an ObjectElement with the specified key. If no element exists with the specified key,
	 * or that element is not an ObjectElement, then empty is returned.
	 * @param key the key whose corresponding value should be returned
	 * @return an Optional containing the requested ObjectElement if it exists, otherwise empty.
	 */
	public Optional<ObjectElement> tryGetObject(String key) {
		for(KeyValuePairElement entry : entries) {
			if (entry.getKey().equals(key) && entry.getValue() instanceof ObjectElement obj) {
				return Optional.of(obj);
			}
		}
		
		return Optional.empty();
	}

	@Override
	public Iterator<KeyValuePairElement> iterator() {
		return this.entries.iterator();
	}
	
	//implements Map {
		@Override
		public int size() {
			return entries.size();
		}
		
		@Override
		public boolean isEmpty() {
			return entries.isEmpty();
		}
		
		@Override
		public boolean containsKey(Object key) {
			for(KeyValuePairElement elem : entries) {
				if (elem.getKey().equals(key)) return true;
			}
			
			return false;
		}
		
		@Override
		public boolean containsValue(Object value) {
			for(KeyValuePairElement elem : entries) {
				if (Objects.equals(elem.getValue(), value)) return true;
			}
			
			return false;
		}
		
		@Nullable
		@Override
		public ValueElement get(Object key) {
			for(KeyValuePairElement entry : entries) {
				if (entry.getKey().equals(key)) {
					return entry.getValue();
				}
			}
			
			return null;
		}
		
		@Nullable
		@Override
		public ValueElement put(String key, ValueElement value) {
			//Validate
			if (
					value instanceof KeyValuePairElement ||
					value instanceof CommentElement) throw new IllegalArgumentException();
			
			for(DocumentElement entry : entries) {
				if (entry instanceof KeyValuePairElement pair) {
					
					if (pair.getKey().equals(key)) {
						return pair.setValue(value);
					}
				}
			}
			
			//No matching KeyValueDocumentEntry. Add one at the end of the object's sub-document
			entries.add(new KeyValuePairElement(key, value));
			return null;
		}
		
		@Override
		public ValueElement remove(Object key) {
			KeyValuePairElement found = null;
			for(KeyValuePairElement entry : entries) {
				if (entry.getKey().equals(key)) {
					found = entry;
					break;
				}
			}
			
			if (found!=null) {
				entries.remove(found);
				return found.getValue();
			} else {
				return null;
			}
		}
		
		@Override
		public void putAll(Map<? extends String, ? extends ValueElement> map) {
			for(Map.Entry<? extends String, ? extends ValueElement> entry : map.entrySet()) {
				this.put(entry.getKey(), entry.getValue());
			}
		}
		
		@Override
		public Collection<ValueElement> values() {
			//TODO: This isn't quite right; it's supposed to be a collection *view* into this Map.
			ArrayList<ValueElement> result = new ArrayList<>();
			for(KeyValuePairElement entry : entries) {
				result.add(entry.getValue());
			}
			
			return result;
		}
		
		@Override
		public Set<Entry<String, ValueElement>> entrySet() {
			//TODO: This isn't quite right; it's supposed to be a collection *view* into this Map.
			//We use a LinkedHashSet here to preserve ordering even though the API discourages this.
			LinkedHashSet<Entry<String, ValueElement>> result = new LinkedHashSet<>();
			for(KeyValuePairElement entry : entries) {
				result.add(entry);
			}
			
			return result;
		}
		
		@Override
		public void clear() {
			entries.clear();
		}
		
		@Override
		public Set<String> keySet() {
			//TODO: This isn't quite right; it's supposed to be a collection *view* into this Map.
			//We use a LinkedHashSet here to preserve ordering even though the API discourages this.
			LinkedHashSet<String> result = new LinkedHashSet<>();
			for(KeyValuePairElement entry : entries) {
				result.add(entry.getKey());
			}
			
			return result;
		}
	//}
}
