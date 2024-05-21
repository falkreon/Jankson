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

package blue.endless.jankson.impl;

import java.lang.reflect.AccessFlag;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import blue.endless.jankson.api.MarshallerException;
import blue.endless.jankson.api.annotation.SerializedName;
import blue.endless.jankson.api.document.ArrayElement;
import blue.endless.jankson.api.document.ObjectElement;
import blue.endless.jankson.api.document.PrimitiveElement;
import blue.endless.jankson.api.document.ValueElement;

/**
 * Turns an arbitrary java object into its corresponding tree-like "structured data" representation.
 */
@FunctionalInterface
public interface ToStructuredDataFunction {
	
	public static final ToStructuredDataFunction INSTANCE = ToStructuredDataFunction::toStructuredData;
	
	public ValueElement apply(Object object) throws MarshallerException;
	
	/**
	 * Turns an arbitrary java object into its corresponding tree-like "structured data" representation.
	 * @param object the object to turn into structured data
	 * @return a ValueElement containing the serializable parts of the source object.
	 * @throws MarshallerException if data cannot be retrieved or converted due to access or internal errors.
	 */
	@SuppressWarnings("unchecked")
	public static ValueElement toStructuredData(Object object) throws MarshallerException {
		try {
			return PrimitiveElement.box(object);
		} catch (Throwable t) {} //don't care
		
		if (object instanceof Map m) {
			return packMap(m);
		}
		
		if (object instanceof Collection c) {
			return packCollection(c);
		} else if (object.getClass().isArray()) {
			return packArray(object);
		}
		
		return packPojo(object);
	}
	
	/**
	 * Turns an arbitrary Java object into its corresponding tree-like "structured data" representation.
	 * Only produces Object elements (map-like structures). If this can't be done it throws an error.
	 * @param object the object to turn into an ObjectElement
	 * @return an ObjectElement containing the serializable parts of the source object.
	 */
	@SuppressWarnings("unchecked")
	public static ObjectElement toObjectElement(Object object) throws MarshallerException {
		if (object instanceof Map m) {
			return packMap(m);
		} else {
			return packPojo(object);
		}
	}
	
	private static void packField(Field f, Object o, ObjectElement destination) throws MarshallerException{
		String fieldName = f.getName();
		
		String serializedName =
				Optional.ofNullable(f.getAnnotation(SerializedName.class))
				.map(SerializedName::value)
				.orElse(fieldName);
		
		if (f.accessFlags().contains(AccessFlag.STATIC) || f.accessFlags().contains(AccessFlag.TRANSIENT)) throw new MarshallerException("Cannot convert static or transient fields.");
		
		try {
			Object v = TypeMagic.getFieldValue(f,o);
			destination.put(serializedName, toStructuredData(v));
		} catch (Throwable t) {
			if (serializedName.equals(fieldName)) {
				throw new MarshallerException("Error retrieving required field '"+serializedName+"'.", t);
			} else {
				throw new MarshallerException("Error retrieving required field '"+fieldName+"' (serializedName: '"+serializedName+"').", t);
			}
		}
	}
	
	private static ObjectElement packMap(Map<Object, Object> m) throws MarshallerException {
		ObjectElement result = new ObjectElement();
		for(var entry : m.entrySet()) {
			result.put(entry.getKey().toString(), toStructuredData(entry.getValue())); //Note: This is vulnerable to reference loops!
		}
		
		return result;
	}
	
	private static ArrayElement packCollection(Collection<Object> c) throws MarshallerException {
		ArrayElement result = new ArrayElement();
		for(Object elem : c) {
			result.add(toStructuredData(elem)); //Note: This is vulnerable to reference loops!
		}
		
		return result;
	}
	
	private static ArrayElement packArray(Object a) throws MarshallerException {
		if (!a.getClass().isArray()) throw new MarshallerException("Passed-in object is not an array");
		ArrayElement result = new ArrayElement();
		for(int i=0; i<Array.getLength(a); i++) {
			Object elem = Array.get(a, i);
			result.add(toStructuredData(elem)); //Note: This is vulnerable to reference loops!
		}
		
		return result;
	}
	
	private static ObjectElement packPojo(Object o) throws MarshallerException {
		//Unpack the object's fields into an ObjectElement
		ObjectElement result = new ObjectElement();
		Set<String> consumedFields = new HashSet<>();
		//Visit all fields, public and private, on the terminal class
		for(Field f : o.getClass().getDeclaredFields()) {
			// Ignore static and transient fields
			if (f.accessFlags().contains(AccessFlag.STATIC) || f.accessFlags().contains(AccessFlag.TRANSIENT)) continue;
			
			// Ignore this same field next time if we re-visit it
			consumedFields.add(f.getName());
			
			packField(f, o, result);
		}
		
		//Visit public super fields
		for(Field f : o.getClass().getFields()) {
			// Ignore static and transient fields
			if (f.accessFlags().contains(AccessFlag.STATIC) || f.accessFlags().contains(AccessFlag.TRANSIENT)) continue;
			
			// Ignore this field if we're revisiting it or if it's shadowed
			if (consumedFields.contains(f.getName())) continue;
			
			packField(f, o, result);
		}
		
		return result;
	}
}
