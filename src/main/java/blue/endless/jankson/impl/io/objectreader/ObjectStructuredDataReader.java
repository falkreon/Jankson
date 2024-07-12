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
import blue.endless.jankson.api.io.StructuredData;
import blue.endless.jankson.api.io.StructuredDataReader;
import blue.endless.jankson.impl.TypeMagic;

/**
 * StructuredDataReader which reads data directly from an arbitrary Java object.
 * 
 * <p>This class is not threadsafe! No effort is made to detect mutations during
 * object access.
 */
public class ObjectStructuredDataReader extends DelegatingStructuredDataReader {
	private final Object obj;
	private ArrayDeque<Field> pendingFields = new ArrayDeque<>();
	
	private ObjectStructuredDataReader(Object object) {
		this.obj = object;
		this.prebuffer(StructuredData.OBJECT_START);
		
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
			prebuffer(StructuredData.OBJECT_END);
			prebuffer(StructuredData.EOF);
			return;
		}
		
		Field cur = pendingFields.removeFirst();
		String fieldName = cur.getName();
		SerializedName[] serializedNames = cur.getDeclaredAnnotationsByType(SerializedName.class);
		if (serializedNames.length > 0) fieldName = serializedNames[0].value();
		prebuffer(StructuredData.objectKey(fieldName));
		try {
			Object value = TypeMagic.getFieldValue(cur, obj);
			if (value == null) {
				prebuffer(StructuredData.NULL);
			} else {
				setDelegate(ObjectStructuredDataReader.of(value));
			}
		} catch (Throwable t) {
			throw new IOException("Could not access field data for field \""+fieldName+"\" ("+cur.getName()+").", t);
		}
	}
	
	public static StructuredDataReader of(Object o) {
		if (o.getClass().isArray()) return new ArrayStructuredDataReader(o);
		if (o instanceof Collection val) return new CollectionStructuredDataReader(val);
		if (o instanceof Map val) return new MapStructuredDataReader(val);
		if (PrimitiveElement.canBox(o)) return new PrimitiveStructuredDataReader(o);
		return new ObjectStructuredDataReader(o);
	}
}
