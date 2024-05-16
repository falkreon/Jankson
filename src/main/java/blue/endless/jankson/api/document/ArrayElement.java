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

package blue.endless.jankson.api.document;

import java.io.IOException;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;

import blue.endless.jankson.api.io.StructuredDataWriter;

public class ArrayElement extends AbstractList<ValueElement> implements ValueElement {
	protected boolean isDefault = false;
	protected List<NonValueElement> prologue = new ArrayList<>();
	protected List<ValueElement> entries = new ArrayList<>();
	protected List<NonValueElement> footer = new ArrayList<>();
	protected List<NonValueElement> epilogue = new ArrayList<>();
	
	@Override
	public List<NonValueElement> getPrologue() {
		return prologue;
	}
	
	/**
	 * Gets NonValueElements following the last ValueElement in this ObjectElement
	 */
	public List<NonValueElement> getFooter() {
		return footer;
	}
	
	@Override
	public List<NonValueElement> getEpilogue() {
		return epilogue;
	}
	
	@Override
	public ValueElement asValueElement() {
		return this;
	}
	
	public Optional<boolean[]> asBooleanArray() {
		boolean[] result = new boolean[entries.size()];
		for(int i=0; i<entries.size(); i++) {
			ValueElement elem = entries.get(i);
			if (elem instanceof PrimitiveElement prim) {
				Optional<Boolean> cur = prim.asBoolean();
				if (cur.isPresent()) {
					result[i] = cur.get();
				} else {
					return Optional.empty();
				}
			} else {
				return Optional.empty();
			}
		}
		return Optional.of(result);
	}
	
	public Optional<double[]> asDoubleArray() {
		double[] result = new double[entries.size()];
		for(int i=0; i<entries.size(); i++) {
			ValueElement elem = entries.get(i);
			if (elem instanceof PrimitiveElement prim) {
				OptionalDouble cur = prim.asDouble();
				if (cur.isPresent()) {
					result[i] = cur.getAsDouble();
				} else {
					return Optional.empty();
				}
			} else {
				return Optional.empty();
			}
		}
		return Optional.of(result);
	}
	
	public Optional<long[]> asLongArray() {
		long[] result = new long[entries.size()];
		for(int i=0; i<entries.size(); i++) {
			ValueElement elem = entries.get(i);
			if (elem instanceof PrimitiveElement prim) {
				OptionalLong cur = prim.asLong();
				if (cur.isPresent()) {
					result[i] = cur.getAsLong();
				} else {
					return Optional.empty();
				}
			} else {
				return Optional.empty();
			}
		}
		return Optional.of(result);
	}
	
	public Optional<int[]> asIntArray() {
		int[] result = new int[entries.size()];
		for(int i=0; i<entries.size(); i++) {
			ValueElement elem = entries.get(i);
			if (elem instanceof PrimitiveElement prim) {
				OptionalInt cur = prim.asInt();
				if (cur.isPresent()) {
					result[i] = cur.getAsInt();
				} else {
					return Optional.empty();
				}
			} else {
				return Optional.empty();
			}
		}
		return Optional.of(result);
	}
	
	public Optional<String[]> asStringArray() {
		String[] result = new String[entries.size()];
		for(int i=0; i<entries.size(); i++) {
			ValueElement elem = entries.get(i);
			if (elem instanceof PrimitiveElement prim) {
				Optional<String> cur = prim.asString();
				if (cur.isPresent()) {
					result[i] = cur.get();
				} else {
					return Optional.empty();
				}
			} else {
				return Optional.empty();
			}
		}
		return Optional.of(result);
	}
	
	public PrimitiveElement getPrimitive(int index) {
		if (entries.get(index) instanceof PrimitiveElement prim) {
			return prim;
		} else {
			return PrimitiveElement.ofNull();
		}
	}
	
	public ObjectElement getObject(int index) {
		if (entries.get(index) instanceof ObjectElement obj) {
			return obj;
		} else {
			return new ObjectElement();
		}
	}
	
	public ArrayElement getArray(int index) {
		if (entries.get(index) instanceof ArrayElement arr) {
			return arr;
		} else {
			return new ArrayElement();
		}
	}
	
	//extends AbstractList<ValueElement> {
	
		@Override
		public ValueElement get(int index) {
			return entries.get(index);
		}
	
		@Override
		public int size() {
			return entries.size();
		}
		
		@Override
		public ValueElement set(int index, ValueElement element) {
			return entries.set(index, element);
		}
		
		@Override
		public void add(int index, ValueElement element) {
			entries.add(index, element);
		}
		
		@Override
		public ValueElement remove(int index) {
			return entries.remove(index);
		}
	
	//}
	
	@Override
	public ValueElement stripFormatting() {
		prologue.clear();
		footer.clear();
		epilogue.clear();
		
		return this;
	}
	
	@Override
	public ValueElement stripAllFormatting() {
		prologue.clear();
		
		for(ValueElement elem : entries) {
			elem.stripAllFormatting();
		}
		
		footer.clear();
		epilogue.clear();
		
		return this;
	}
		
	public ArrayElement clone() {
		ArrayElement result = new ArrayElement();
		
		for(NonValueElement elem : prologue) {
			result.prologue.add(elem.clone());
		}
		
		for(ValueElement elem : entries) {
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
		if (obj instanceof ArrayElement elem) {
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
	
	@Override
	public void write(StructuredDataWriter writer) throws IOException {
		//TODO: Write comment preamble etc
		writer.writeArrayStart();
		for(int i=0; i<entries.size(); i++) {
			entries.get(i).write(writer);
			if (i<entries.size()-1) writer.nextValue();
		}
		
		writer.writeArrayEnd();
	}
}
