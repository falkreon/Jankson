/*
 * MIT License
 *
 * Copyright (c) 2018-2019 Falkreon (Isaac Ellingson)
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
import java.util.Collection;
import java.util.Map;

import javax.annotation.Nullable;

import blue.endless.jankson.JsonArray;
import blue.endless.jankson.JsonElement;
import blue.endless.jankson.JsonNull;
import blue.endless.jankson.JsonObject;
import blue.endless.jankson.JsonPrimitive;
import blue.endless.jankson.magic.TypeMagic;

public class POJODeserializer {
	public static void unpackObject(Object target, JsonObject source) throws DeserializationException {
		//if (o.getClass().getTypeParameters().length>0) throw new DeserializationException("Can't safely deserialize generic types!");
		//well, let's try anyway and see if we run into problems.
		
		//Create a copy we can redact keys from
		JsonObject work = source.clone();
		
		//Fill public and private fields declared in the target object's immediate class
		for(Field f : target.getClass().getDeclaredFields()) {
			//System.out.println("Unpacking "+f.getName());
			unpackField(target, f, work);
		}
		
		//Attempt to fill public, accessible fields declared in the target object's superclass.
		for(Field f : target.getClass().getFields()) {
			//System.out.println("Unpacking "+f.getName());
			unpackField(target, f, work);
		}
		
		if (!work.isEmpty()) {
			//System.out.println("Unable to deserialize: "+work.toJson(true, true));
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
					//System.out.println("Unable to set field "+f.getName()+" of class "+parent.getClass().getCanonicalName()+" to null.");
				}
			} else {
				try {
					//System.out.println(""+elem+" --> "+f.getName());
					unpackFieldData(parent, f, elem, source.getMarshaller());
					//System.out.println(f.getName()+" == "+f.get(parent));
				} catch (Throwable t) {
					//System.out.println("Unable to unpack field "+f.getName()+" of class "+parent.getClass().getCanonicalName()+" using data "+elem);
				}
			}
		}
	}
	
	
	/** NOT WORKING YET, HIGHLY EXPERIMENTAL */
	@SuppressWarnings("unchecked")
	@Nullable
	public static Object Unpack(Type t, JsonElement elem, Marshaller marshaller) {
		Class<?> rawClass = TypeMagic.classForType(t);
		if (rawClass.isPrimitive()) return null; //We can't unpack a primitive into an object of primitive type. Maybe in the future we can return a boxed type?
		
		if (elem==null) return null;
		/*
		if (type instanceof Class) {
			try {
				return marshaller.marshall((Class<?>) type, elem);
			} catch (ClassCastException t) {
				return null;
			}
		}
		
		if (type instanceof ParameterizedType) {
			try {
				Class<?> clazz = (Class<?>) TypeMagic.classForType(type);
				
				if (List.class.isAssignableFrom(clazz)) {
					Object result = TypeMagic.createAndCast(type);
					
					try {
						unpackList((List<Object>) result, type, elem, marshaller);
						return result;
					} catch (DeserializationException e) {
						e.printStackTrace();
						return result;
					}
				}
				
				return null;
			} catch (ClassCastException t) {
				return null;
			}
		}*/
		
		return null;
	}
	
	@SuppressWarnings("unchecked")
	public static boolean unpackFieldData(Object parent, Field field, JsonElement elem, Marshaller marshaller) throws Throwable {
		if (elem==null) return true;
		try {
			field.setAccessible(true);
		} catch (Throwable t) {
			//System.out.println("Can't set field accessible");
			return false; //skip this field probably.
		}
		
		if (elem==JsonNull.INSTANCE) {
			field.set(parent, null);
			return true;
		}
		
		Class<?> fieldClass = field.getType();
		
		if (!isCollections(fieldClass)) {
			//Try to directly marshall
			Object result = marshaller.marshall(fieldClass, elem);
			field.set(parent, result);
			return true;
		}
		
		
		if (field.get(parent)==null) {
			Object fieldValue = TypeMagic.createAndCast(field.getGenericType());
			
			if (fieldValue==null) {
				return false; //Can't deserialize this somehow
			} else {
				field.set(parent, fieldValue);
			}
		}
			
		/*
		//Is List?
		if (List.class.isAssignableFrom(fieldClass)) {
			Type listElementType = ((ParameterizedType)field.getGenericType()).getActualTypeArguments()[0];
			
			unpackList((List<Object>)field.get(parent), listElementType, elem, marshaller);
			
			return true;
		}*/
		
		//Is Map?
		if (Map.class.isAssignableFrom(fieldClass)) {
			Type[] parameters = ((ParameterizedType)field.getGenericType()).getActualTypeArguments();
			
			unpackMap((Map<Object, Object>) field.get(parent), parameters[0], parameters[1], elem, marshaller);
			
			return true;
		}
		
		if (Collection.class.isAssignableFrom(fieldClass)) {
			Type elementType = ((ParameterizedType)field.getGenericType()).getActualTypeArguments()[0];
			
			unpackCollection((Collection<Object>)field.get(parent), elementType, elem, marshaller);
			
			return true;
		}
		
		return false;
	}
	
	private static boolean isCollections(Class<?> clazz) {
		return
				Map.class.isAssignableFrom(clazz) ||
				Collection.class.isAssignableFrom(clazz);
	}
	
	/*
	public static void unpackList(List<Object> list, Type elementType, JsonElement elem, Marshaller marshaller) throws DeserializationException {
		if (!(elem instanceof JsonArray)) throw new DeserializationException("Cannot deserialize a "+elem.getClass().getSimpleName()+" into a List - expected a JsonArray!");
		
		Class<?> elementClass = TypeMagic.classForType(elementType); //TODO: Marshall to Type instead
		
		JsonArray array = (JsonArray)elem;
		for(JsonElement arrayElem : array) {
			Object o = marshaller.marshall(elementClass, arrayElem);
			if (o!=null) list.add(o);
		}
	}*/
	
	public static void unpackMap(Map<Object, Object> map, Type keyType, Type valueType, JsonElement elem, Marshaller marshaller) throws DeserializationException {
		if (!(elem instanceof JsonObject)) throw new DeserializationException("Cannot deserialize a "+elem.getClass().getSimpleName()+" into a Map - expected a JsonObject!");
		
		Class<?> keyClass = TypeMagic.classForType(keyType);
		Class<?> valueClass = TypeMagic.classForType(valueType);
		JsonObject object = (JsonObject)elem;
		for(Map.Entry<String, JsonElement> entry : object.entrySet()) {
			try {
				Object k = marshaller.marshall(keyClass, new JsonPrimitive(entry.getKey())); //TODO: Type marshall instead
				Object v = marshaller.marshall(valueClass, entry.getValue());                //TODO: "" ""
				if (k!=null && v!=null) map.put(k, v);
			} catch (Throwable t) {}
		}
	}
	
	public static void unpackCollection(Collection<Object> collection, Type elementType, JsonElement elem, Marshaller marshaller) throws DeserializationException {
		if (!(elem instanceof JsonArray)) throw new DeserializationException("Cannot deserialize a "+elem.getClass().getSimpleName()+" into a Set - expected a JsonArray!");
		
		Class<?> elementClass = TypeMagic.classForType(elementType); //TODO: Marshall to Type instead
		
		JsonArray array = (JsonArray)elem;
		for(JsonElement arrayElem : array) {
			Object o = marshaller.marshall(elementClass, arrayElem);
			if (o!=null) collection.add(o);
		}
	}
}
