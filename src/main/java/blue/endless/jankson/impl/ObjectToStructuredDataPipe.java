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

import java.io.IOException;
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
import blue.endless.jankson.api.document.PrimitiveElement;
import blue.endless.jankson.api.io.StructuredData;
import blue.endless.jankson.api.io.StructuredDataWriter;

/**
 * Writes arbitrary java objects as structured data to a StructuredDataWriter.
 */
public class ObjectToStructuredDataPipe {
	
	private final StructuredDataWriter underlying;
	
	/**
	 * Creates an ObjectStructuredDataWriter that will write to the specified StructuredDataWriter
	 * @param writer the writer to send strucutred data to
	 */
	public ObjectToStructuredDataPipe(StructuredDataWriter writer) {
		this.underlying = writer;
	}
	
	/**
	 * Writes an object to the underlying StructuredDataWriter
	 * @param object the object to write as structured data
	 * @throws MarshallerException if data cannot be retrieved or converted due to access or internal errors.
	 * @throws IOException if the underlying StructuredDataWriter encounters problems writing the data.
	 */
	public void write(Object object) throws MarshallerException, IOException{
		write(object, underlying);
	}
	
	/**
	 * Writes an arbitrary java object into the specified StructuredDataWriter.
	 * @param object the object to write as structured data
	 * @param writer the StructuredDataWriter to write the object to
	 * @throws MarshallerException if data cannot be retrieved or converted due to access or internal errors.
	 * @throws IOException if the StructuredDataWriter encounters problems writing the data.
	 */
	@SuppressWarnings("unchecked")
	public static void write(Object object, StructuredDataWriter writer) throws MarshallerException, IOException {
		try {
			PrimitiveElement.box(object).write(writer);
			return;
		} catch (Throwable t) {} //don't care
		
		if (object instanceof Map m) {
			packMap(m, writer);
			return;
		}
		
		if (object instanceof Collection c) {
			packCollection(c, writer);
			return;
		} else if (object.getClass().isArray()) {
			packArray(object, writer);
			return;
		}
		
		packPojo(object, writer);
	}
	
	/**
	 * Writes an arbitrary java object into the specified StructuredDataWriter.
	 * Only produces Object elements (map-like structures). If this can't be done it throws an error.
	 * @param object the object to write as structured data
	 * @param writer the StructuredDataWriter to write the object to
	 * @throws MarshallerException if data cannot be retrieved or converted due to access or internal errors.
	 * @throws IOException if the StructuredDataWriter encounters problems writing the data.
	 */
	@SuppressWarnings("unchecked")
	public static void writeObjectElement(Object object, StructuredDataWriter writer) throws MarshallerException, IOException {
		if (object instanceof Map m) {
			packMap(m, writer);
		} else {
			packPojo(object, writer);
		}
	}
	
	/**
	 * Writes the specified field into a StructuredDataWriter. Does not write any commas between map or array elements.
	 * @param f the field to write
	 * @param o the object instance that holds the field value
	 * @param writer the StructuredDataWriter to write the data to
	 * @throws MarshallerException if data cannot be retrieved or converted due to access or internal errors.
	 * @throws IOException if the StructuredDataWriter encounters problems writing the data.
	 */
	private static void writeField(Field f, Object o, StructuredDataWriter writer) throws MarshallerException, IOException {
		String fieldName = f.getName();
		
		String serializedName =
				Optional.ofNullable(f.getAnnotation(SerializedName.class))
				.map(SerializedName::value)
				.orElse(fieldName);
		
		if (f.accessFlags().contains(AccessFlag.STATIC) || f.accessFlags().contains(AccessFlag.TRANSIENT)) throw new MarshallerException("Cannot convert static or transient fields.");
		
		try {
			Object v = TypeMagic.getFieldValue(f,o);
			writer.write(StructuredData.objectKey(serializedName));
			write(v, writer);
		} catch (Throwable t) {
			if (serializedName.equals(fieldName)) {
				throw new MarshallerException("Error retrieving required field '"+serializedName+"'.", t);
			} else {
				throw new MarshallerException("Error retrieving required field '"+fieldName+"' (serializedName: '"+serializedName+"').", t);
			}
		}
	}
	
	private static void packMap(Map<Object, Object> m, StructuredDataWriter writer) throws MarshallerException, IOException {
		writer.write(StructuredData.OBJECT_START);
		//boolean first = true;
		for(Map.Entry<Object, Object> entry : m.entrySet()) {
			//if (!first) writer.nextValue();
			//first = false;
			
			writer.write(StructuredData.objectKey(entry.getKey().toString()));
			//writer.writeKeyValueDelimiter();
			write(entry.getValue(), writer); // Note: This is vulnerable to reference loops!
		}
		
		writer.write(StructuredData.OBJECT_END);
	}
	
	private static void packCollection(Collection<Object> c, StructuredDataWriter writer) throws MarshallerException, IOException {
		writer.write(StructuredData.ARRAY_START);
		//boolean first = true;
		for(Object elem : c) {
			//if (!first) writer.nextValue();
			//first = false;
			
			write(elem, writer); // Note: This is vulnerable to reference loops!
		}
		
		writer.write(StructuredData.ARRAY_END);
	}
	
	private static void packArray(Object a, StructuredDataWriter writer) throws MarshallerException, IOException {
		if (!a.getClass().isArray()) throw new MarshallerException("Passed-in object is not an array");
		
		writer.write(StructuredData.ARRAY_START);
		//boolean first = true;
		for(int i=0; i<Array.getLength(a); i++) {
			//if (!first) writer.nextValue();
			//first = false;
			
			Object elem = Array.get(a, i);
			write(elem, writer); // Note: This is vulnerable to reference loops!
		}
		
		writer.write(StructuredData.ARRAY_END);
	}
	
	private static void packPojo(Object o, StructuredDataWriter writer) throws MarshallerException, IOException {
		writer.write(StructuredData.OBJECT_START);
		
		Set<String> consumedFields = new HashSet<>();
		//boolean first = true;
		
		//Visit all fields, public and private, on the terminal class
		for(Field f : o.getClass().getDeclaredFields()) {
			// Ignore static and transient fields
			if (f.accessFlags().contains(AccessFlag.STATIC) || f.accessFlags().contains(AccessFlag.TRANSIENT)) continue;
			
			// Ignore this same field next time if we re-visit it
			consumedFields.add(f.getName());
			
			//if (!first) writer.nextValue();
			//first = false;
			
			writeField(f, o, writer);
		}
		
		//Visit public super fields
		for(Field f : o.getClass().getFields()) {
			// Ignore static and transient fields
			if (f.accessFlags().contains(AccessFlag.STATIC) || f.accessFlags().contains(AccessFlag.TRANSIENT)) continue;
			
			// Ignore this field if we're revisiting it or if it's shadowed
			if (consumedFields.contains(f.getName())) continue;
			
			//if (!first) writer.nextValue();
			//first = false;
			
			writeField(f, o, writer);
		}
		
		writer.write(StructuredData.OBJECT_END);
	}
}
