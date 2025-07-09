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

package blue.endless.jankson.impl.io.objectreader;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import blue.endless.jankson.api.annotation.SerializedName;
import blue.endless.jankson.api.document.PrimitiveElement;
import blue.endless.jankson.api.io.ObjectReaderFactory;
import blue.endless.jankson.api.io.StructuredData;
import blue.endless.jankson.api.io.StructuredDataReader;
import blue.endless.jankson.impl.TypeMagic;

/**
 * StructuredDataReader which reads data directly from an arbitrary Java object.
 * 
 * <p>Instances of this object can be created indirectly through ObjectReaderFactory.
 * 
 * <p>This class is not threadsafe! No effort is made to detect mutations during
 * object access.
 */
public class ObjectStructuredDataReader extends DelegatingStructuredDataReader {
	private final Object obj;
	private final ObjectReaderFactory factory;
	private ArrayDeque<Field> pendingFields = new ArrayDeque<>();
	
	private ObjectStructuredDataReader(Object object, ObjectReaderFactory factory) {
		this.obj = object;
		this.buffer(StructuredData.OBJECT_START);
		this.factory = (factory == null) ? new ObjectReaderFactory() : factory;
		
		Set<String> alreadyTaken = new HashSet<>();
		for(Field f : obj.getClass().getDeclaredFields()) {
			if (alreadyTaken.contains(f.getName())) continue;
			alreadyTaken.add(f.getName());
			pendingFields.addLast(f);
		}
		for(Field f : obj.getClass().getFields()) {
			if (alreadyTaken.contains(f.getName())) continue;
			alreadyTaken.add(f.getName());
			pendingFields.addLast(f);
		}
	}
	
	@Override
	protected void onDelegateEmpty() throws IOException {
		if (pendingFields.isEmpty()) {
			buffer(StructuredData.OBJECT_END);
			buffer(StructuredData.EOF);
			return;
		}
		
		Field cur = pendingFields.removeFirst();
		String fieldName = cur.getName();
		SerializedName[] serializedNames = cur.getDeclaredAnnotationsByType(SerializedName.class);
		if (serializedNames.length > 0) fieldName = serializedNames[0].value();
		buffer(StructuredData.objectKey(fieldName));
		try {
			Object value = TypeMagic.getFieldValue(cur, obj);
			if (value == null) {
				buffer(StructuredData.NULL);
			} else {
				setDelegate(factory.getReader(value));
			}
		} catch (Throwable t) {
			throw new IOException("Could not access field data for field \""+fieldName+"\" ("+cur.getName()+").", t);
		}
	}
	
	/*
	 * Control flow note:
	 * This method *is* the fallback behavior of ObjectReaderFactory.
	 * 
	 * The contents of this method MUST NOT delegate directly to the provided ORF, because ORF
	 * delegates directly to this method to provide readers for non-overridden types.
	 */
	
	/**
	 * Do not use this method directly. Obtain an ObjectReaderFactory and ask it for an appropriate
	 * StructuredDataReader for the object in question.
	 * @see ObjectReaderFactory
	 */
	public static StructuredDataReader of(Object o, ObjectReaderFactory factory) {
		if (o.getClass().isArray()) return new ArrayStructuredDataReader(o, factory);
		if (o instanceof Collection val) return new CollectionStructuredDataReader(val, factory);
		if (o instanceof Map val) return new MapStructuredDataReader(val, factory);
		if (PrimitiveElement.canBox(o)) return new PrimitiveStructuredDataReader(o);
		return new ObjectStructuredDataReader(o, factory);
	}
}
