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
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Type;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Map;

import blue.endless.jankson.api.document.CommentType;
import blue.endless.jankson.api.document.PrimitiveElement;
import blue.endless.jankson.api.io.ObjectReaderFactory;
import blue.endless.jankson.api.io.StructuredData;
import blue.endless.jankson.api.io.StructuredDataReader;
import blue.endless.jankson.impl.magic.ClassHierarchy;
import blue.endless.jankson.impl.magic.EnumNames;
import blue.endless.jankson.impl.magic.ReflectiveProperty;

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
	private ArrayDeque<ReflectiveProperty> pendingFields = new ArrayDeque<>();
	
	private ObjectStructuredDataReader(Object object, Type type, ObjectReaderFactory factory) {
		this.obj = object;
		this.buffer(StructuredData.OBJECT_START);
		this.factory = (factory == null) ? new ObjectReaderFactory() : factory;

		Type reflectiveType = ClassHierarchy.specializeRuntimeType(type, object.getClass());
		pendingFields.addAll(ReflectiveProperty.of(reflectiveType));
	}
	
	@Override
	protected void onDelegateEmpty() throws IOException {
		if (pendingFields.isEmpty()) {
			buffer(StructuredData.OBJECT_END);
			buffer(StructuredData.EOF);
			return;
		}
		
		ReflectiveProperty cur = pendingFields.removeFirst();
		String fieldName = cur.wireName();
		for (String line : cur.comment().split("\\R", -1)) {
			if (!line.isBlank()) buffer(StructuredData.comment(line, CommentType.LINE_END));
		}
		buffer(StructuredData.objectKey(fieldName));
		try {
			Object value = cur.get(obj);
			if (value == null) {
				buffer(StructuredData.NULL);
			} else {
				setDelegate(factory.getReader(cur.type(), value));
			}
		} catch (Throwable t) {
			throw new IOException("Could not access field data for field \""+fieldName+"\" ("+cur.javaName()+").", t);
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
		return of(o, o.getClass(), factory);
	}

	/**
	 * Do not use this method directly. Obtain an ObjectReaderFactory and ask it for an appropriate
	 * StructuredDataReader for the object in question.
	 * @see ObjectReaderFactory
	 */
	public static StructuredDataReader of(Object o, Type type, ObjectReaderFactory factory) {
		if (o instanceof Enum<?> value) {
			return new PrimitiveStructuredDataReader(EnumNames.wireName(value));
		}
		if (o.getClass().isArray()) {
			Type elementType = type instanceof GenericArrayType array ? array.getGenericComponentType()
					: ClassHierarchy.getErasedClass(type).isArray() ? ClassHierarchy.getErasedClass(type).getComponentType()
					: o.getClass().getComponentType();
			return new ArrayStructuredDataReader(o, elementType, factory);
		}
		if (o instanceof Collection val) {
			Type elementType = Collection.class.isAssignableFrom(ClassHierarchy.getErasedClass(type))
					? ClassHierarchy.getCollectionTypeArgument(type) : Object.class;
			return new CollectionStructuredDataReader(val, elementType, factory);
		}
		if (o instanceof Map val) {
			Type valueType = Map.class.isAssignableFrom(ClassHierarchy.getErasedClass(type))
					? ClassHierarchy.getMapTypeArguments(type).valueType() : Object.class;
			return new MapStructuredDataReader(val, valueType, factory);
		}
		if (PrimitiveElement.canBox(o)) return new PrimitiveStructuredDataReader(o);
		return new ObjectStructuredDataReader(o, type, factory);
	}
}
