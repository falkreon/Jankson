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

package blue.endless.jankson.impl;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import blue.endless.jankson.JsonArray;
import blue.endless.jankson.JsonElement;
import blue.endless.jankson.JsonObject;
import blue.endless.jankson.magic.TypeMagic;

public class POJODeserializer {
	public static void unpackObject(Object target, JsonObject source) throws DeserializationException {
		//if (o.getClass().getTypeParameters().length>0) throw new DeserializationException("Can't safely deserialize generic types!");
		//well, let's try anyway and see if we run into problems.
		
		//Create a copy we can redact keys from
		JsonObject work = source.clone();
		
		//Fill public and private fields declared in the target object's immediate class
		for(Field f : target.getClass().getDeclaredFields()) {
			System.out.println("Unpacking "+f.getName());
			unpackField(target, f, work);
		}
		
		//Attempt to fill public, accessible fields declared in the target object's superclass.
		for(Field f : target.getClass().getFields()) {
			unpackField(target, f, work);
		}
		
		if (!work.isEmpty()) {
			System.out.println("Unable to deserialize: "+work.toJson(true, true));
		}
	}
	
	public static void unpackField(Object parent, Field f, JsonObject source) {
		if (source.containsKey(f.getName())) {
			JsonElement elem = source.get(f.getName());
			source.remove(f.getName()); //Prevent it from getting re-unpacked
			if (elem==null) {
				try {
					f.set(parent, null);
				} catch (IllegalArgumentException | IllegalAccessException e) {
					System.out.println("Unable to set field "+f.getName()+" of class "+parent.getClass().getCanonicalName()+" to null.");
				}
			} else {
				try {
					unpackFieldData(parent, f, elem, source.getMarshaller());
				} catch (Throwable t) {
					System.out.println("Unable to unpack field "+f.getName()+" of class "+parent.getClass().getCanonicalName()+" using data "+elem);
				}
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	public static boolean unpackFieldData(Object parent, Field field, JsonElement elem, Marshaller marshaller) throws Throwable {
		if (elem==null) return true;
		try {
			field.setAccessible(true);
		} catch (Throwable t) {
			return false; //skip this field probably.
		}
		Class<?> fieldClass = field.getType();
		if (field.get(parent)==null) {
			Object fieldValue = TypeMagic.createAndCast(field.getGenericType());
			
			if (fieldValue==null) {
				return false; //Can't deserialize this somehow
			} else {
				field.set(parent, fieldValue);
			}
		}
		
		//Is List?
		if (List.class.isAssignableFrom(fieldClass)) {
			//System.out.println("C");
			//System.out.println("Unpacking List - "+fieldClass);
			//System.out.println("Existing/new field value: "+field.get(parent));
			//System.out.println("Annotations: "+field.getAnnotations());
			//System.out.println("Generic Type: "+field.getGenericType().getTypeName());
			Type listElementType = ((ParameterizedType)field.getGenericType()).getActualTypeArguments()[0];
			//Class<?> listElementClass = TypeMagic.classForType(listElementType);
			
			unpackList((List<Object>)field.get(parent), listElementType, elem, marshaller);
			
			return true;
		}
		System.out.println("D");
		//Is Map?
		if (Map.class.isAssignableFrom(fieldClass)) {
			System.out.println("Unpacking List - "+fieldClass);
			return true;
		}
		
		//Is Set?
		if (Set.class.isAssignableFrom(fieldClass)) {
			System.out.println("Unpacking Set - "+fieldClass);
			return true;
		}
		
		if (Queue.class.isAssignableFrom(fieldClass)) {
			System.out.println("Unpacking Queue - "+fieldClass);
			return true;
		}
		
		return false;
	}
	
	
	public static void unpackList(List<Object> list, Type elementType, JsonElement elem, Marshaller marshaller) throws DeserializationException {
		if (!(elem instanceof JsonArray)) throw new DeserializationException("Cannot deserialize a "+elem.getClass().getSimpleName()+" into a List - expected a JsonArray!");
		
		Class<?> elementClass = TypeMagic.classForType(elementType); //TODO: Marshall to Type instead
		
		JsonArray array = (JsonArray)elem;
		for(JsonElement arrayElem : array) {
			Object o = marshaller.marshall(elementClass, arrayElem);
			if (o!=null) list.add(o);
		}
		
	}
}
