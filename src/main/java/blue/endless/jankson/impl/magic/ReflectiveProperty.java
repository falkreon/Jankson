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

package blue.endless.jankson.impl.magic;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import blue.endless.jankson.api.annotation.Comment;
import blue.endless.jankson.api.annotation.SerializedName;
import blue.endless.jankson.impl.TypeMagic;

/** Shared reflective metadata for serialized object properties. */
public record ReflectiveProperty(String javaName, String wireName, Type type, Field field, String comment) {
	public Object get(Object instance) {
		if (field.getDeclaringClass().isRecord()) {
			for (RecordComponent component : field.getDeclaringClass().getRecordComponents()) {
				if (!component.getName().equals(javaName)) continue;
				Method accessor = component.getAccessor();
				try {
					boolean accessible = accessor.canAccess(instance);
					if (!accessible) accessor.setAccessible(true);
					try {
						return accessor.invoke(instance);
					} finally {
						if (!accessible) accessor.setAccessible(false);
					}
				} catch (ReflectiveOperationException e) {
					throw new IllegalStateException("Could not invoke record accessor "+accessor, e);
				}
			}
		}
		return TypeMagic.getFieldValue(field, instance);
	}

	public static List<ReflectiveProperty> of(Type type) {
		Class<?> clazz = ClassHierarchy.getErasedClass(type);
		if (clazz == null) throw new IllegalArgumentException("Cannot inspect properties of "+type.getTypeName());
		return clazz.isRecord() ? recordProperties(type, clazz) : fieldProperties(type, clazz);
	}

	private static List<ReflectiveProperty> fieldProperties(Type type, Class<?> clazz) {
		Map<String, ReflectiveProperty> byWireName = new LinkedHashMap<>();
		for (Class<?> level = clazz; level != null && level != Object.class; level = level.getSuperclass()) {
			Map<TypeVariable<?>, Type> bindings = ClassHierarchy.getTypeBindings(type, level);
			for (Field field : level.getDeclaredFields()) {
				int modifiers = field.getModifiers();
				if (Modifier.isStatic(modifiers) || Modifier.isTransient(modifiers) || field.isSynthetic()) continue;
				SerializedName name = field.getAnnotation(SerializedName.class);
				Comment comment = field.getAnnotation(Comment.class);
				ReflectiveProperty property = new ReflectiveProperty(field.getName(),
						name == null ? field.getName() : name.value(), ClassHierarchy.substitute(field.getGenericType(), bindings), field,
						comment == null ? "" : comment.value());
				add(type, byWireName, property);
			}
		}
		return List.copyOf(byWireName.values());
	}

	private static List<ReflectiveProperty> recordProperties(Type type, Class<?> clazz) {
		Map<String, ReflectiveProperty> byWireName = new LinkedHashMap<>();
		Map<TypeVariable<?>, Type> bindings = ClassHierarchy.getTypeBindings(type, clazz);
		for (RecordComponent component : clazz.getRecordComponents()) {
			try {
				Field field = clazz.getDeclaredField(component.getName());
				SerializedName name = component.getAnnotation(SerializedName.class);
				if (name == null) name = field.getAnnotation(SerializedName.class);
				Comment comment = component.getAnnotation(Comment.class);
				if (comment == null) comment = field.getAnnotation(Comment.class);
				ReflectiveProperty property = new ReflectiveProperty(component.getName(),
						name == null ? component.getName() : name.value(), ClassHierarchy.substitute(component.getGenericType(), bindings), field,
						comment == null ? "" : comment.value());
				add(clazz, byWireName, property);
			} catch (NoSuchFieldException e) {
				throw new IllegalStateException("Record component '"+component.getName()+"' has no backing field in "+clazz.getTypeName(), e);
			}
		}
		return List.copyOf(byWireName.values());
	}

	private static void add(Type owner, Map<String, ReflectiveProperty> byWireName, ReflectiveProperty property) {
		ReflectiveProperty previous = byWireName.putIfAbsent(property.wireName(), property);
		if (previous != null) {
			throw new IllegalArgumentException("Duplicate serialized name '"+property.wireName()+"' in "+owner.getTypeName()
					+" for fields '"+previous.field().getDeclaringClass().getTypeName()+"."+previous.javaName()
					+"' and '"+property.field().getDeclaringClass().getTypeName()+"."+property.javaName()+"'.");
		}
	}
}
