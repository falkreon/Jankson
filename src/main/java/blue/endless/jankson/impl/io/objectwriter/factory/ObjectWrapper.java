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

package blue.endless.jankson.impl.io.objectwriter.factory;

import java.lang.reflect.AccessFlag;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import blue.endless.jankson.api.annotation.Deserializer;
import blue.endless.jankson.api.annotation.Immutable;
import blue.endless.jankson.api.annotation.Mutable;
import blue.endless.jankson.api.annotation.MutatorFor;
import blue.endless.jankson.api.annotation.SerializedName;
import blue.endless.jankson.impl.magic.ClassHierarchy;

/**
 * Wrapper around arbitrary Java objects to allow for creation or mutation
 */
public interface ObjectWrapper<T> {
	public boolean isImmutable();
	public Type getType(String serializedName);
	public Set<String> getFieldNames();
	public void setField(String serializedName, Object value) throws ReflectiveOperationException;
	public T getResult() throws InstantiationException;
	
	
	public static <T> ObjectWrapper<T> of(Type t, @Nullable T result) {
		@SuppressWarnings("unchecked")
		Class<T> clazz = (Class<T>) ClassHierarchy.getErasedClass(t);
		// See if we can skip lengthy type analysis
		if (clazz.getAnnotation(Immutable.class) != null) {
			return new ImmutableWrapper<>(t);
		} else if (clazz.getAnnotation(Mutable.class) != null) {
			return (result != null) ? new MutableWrapper<>(result, t) : new MutableWrapper<>(t);
		}
		
		Map<String, Field> fields = getFields(t);
		// A class is mutable iff all these fields are mutable
		boolean mutable = true;
		for(Map.Entry<String, Field> entry : fields.entrySet()) {
			if (entry.getKey().startsWith("this$") && entry.getValue().accessFlags().contains(AccessFlag.SYNTHETIC)) continue;
			
			if (!entry.getValue().accessFlags().contains(AccessFlag.PUBLIC)) {
				// Non-POJO. Do we have a setter?
				Method m = getMutator(t, entry.getValue().getType(), entry.getKey());
				if (m == null) {
					mutable = false;
					break;
				}
			}
		}
		
		if (mutable) {
			return (result != null) ? new MutableWrapper<>(result, t) : new MutableWrapper<>(t);
		} else {
			return new ImmutableWrapper<>(t);
		}
	}
	
	
	
	private static Method getMutator(Type tType, Class<?> fieldType, String fieldName) {
		Class<?> clazz = ClassHierarchy.getErasedClass(tType);
		
		for(Method m : clazz.getDeclaredMethods()) {
			MutatorFor correspondingField = m.getAnnotation(MutatorFor.class);
			if (correspondingField != null && correspondingField.value().equals(fieldName)) {
				Parameter[] params = m.getParameters();
				if (params.length == 1 && params[0].getType().equals(fieldType)) {
					return m;
				}
			}
		}
		
		String setterName = "get" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
		
		for(Method m : clazz.getDeclaredMethods()) {
			if (m.getName().equals(setterName)) {
				Parameter[] params = m.getParameters();
				if (params.length == 1 && params[0].getType().equals(fieldType)) {
					return m;
				}
			}
		}
		
		return null;
	}
	
	
	private static Map<String, Field> getFields(Type tType) {
		Map<String, Field> result = new HashMap<>();
		Class<?> curLevel = ClassHierarchy.getErasedClass(tType);
		if (curLevel == null) throw new IllegalStateException("erased class was null for type "+tType.getTypeName());
		while(!curLevel.equals(Object.class)) {
			for(Field field : curLevel.getDeclaredFields()) {
				String fieldName = field.getName();
				
				SerializedName[] annos = field.getDeclaredAnnotationsByType(SerializedName.class);
				if (annos != null && annos.length > 0) {
					//System.out.println("Serialized name of field "+fieldName+" is "+serializedName.value());
					fieldName = annos[0].value();
				}
				
				if (result.containsKey(fieldName)) {
					throw new IllegalArgumentException(
							"Field \""+field.getName()+"\" ("+fieldName+") is shadowed in type "+tType.getTypeName()+".\n"+
							"""
							Cannot deserialize types having multiple fields with the same name.
							You can resolve this by annotating duplicate fields with unique '@SerializedName' values.
							""");
				}
				
				result.put(fieldName, field);
			}
			
			curLevel = curLevel.getSuperclass();
			if (curLevel == null) curLevel = Object.class;
		}
		
		return result;
	}
	
	private static Type getFieldType(Type tType, Field field) {
		Type result = field.getGenericType();
		if (result instanceof TypeVariable var) {
			Type resolved = ClassHierarchy.getActualTypeArguments(tType, field.getDeclaringClass()).get(var.getName());
			if (resolved == null) throw new IllegalStateException(
					"Could not resolve type variable "+var.getName()+" on type "+tType.getTypeName()+
					" (declared in class "+field.getDeclaringClass().getCanonicalName()+")");
			
			return resolved;
		}
		
		return field.getGenericType();
	}
	
	private static Map<String, Type> getFieldTypes(Type tType, Map<String, Field> fields) {
		Map<String, Type> result = new HashMap<>();
		for(Map.Entry<String, Field> entry : fields.entrySet()) {
			result.put(entry.getKey(), getFieldType(tType, entry.getValue()));
		}
		
		return result;
	}
	
	public static class MutableWrapper<T> implements ObjectWrapper<T> {
		private final Type tType;
		private final T result;
		private final Map<String, Field> fieldNames;
		private final Map<String, Type> fieldTypes;
		
		public MutableWrapper(Type tType) {
			this.tType = tType;
			
			try {
				Class<?> clazz = ClassHierarchy.getErasedClass(tType);
				@SuppressWarnings("unchecked")
				Constructor<T> constructor = (Constructor<T>) clazz.getConstructor();
				result = constructor.newInstance();
			} catch (Throwable t) {
				throw new IllegalArgumentException("Cannot create an object of type "+tType.getTypeName()+".\n"+
						"""
						For uninitialized fields or root values of mutable classes, Jankson needs a zero-arg constructor.
						Classes come with these by default, but declaring a constructor removes this "default" constructor.
						You can fix this by declaring a no-arg constructor, or by marking this class as @Immutable.
						""", t);
			}
			
			fieldNames = getFields(tType);
			fieldTypes = getFieldTypes(tType, fieldNames);
		}
		
		public MutableWrapper(T t, Type tType) {
			this.tType = tType;
			result = t;
			fieldNames = getFields(tType);
			fieldTypes = getFieldTypes(tType, fieldNames);
		}
		
		@Override
		public Set<String> getFieldNames() {
			return fieldNames.keySet();
		}
		
		@Override
		public void setField(String serializedName, Object value) throws ReflectiveOperationException {
			Field f = fieldNames.get(serializedName);
			if (f == null) throw new IllegalArgumentException("No field with name \""+serializedName+"\"");
			
			Method m = getMutator(tType, f.getType(), serializedName);
			if (m != null) {
				try {
					boolean access = m.canAccess(result);
					if (!access) m.setAccessible(true);
					m.invoke(result, value);
					if (!access) m.setAccessible(false);
					return;
				} catch (Throwable t) {
					throw new ReflectiveOperationException(t);
				}
			}
			
			boolean access = f.canAccess(result);
			if (!access) f.setAccessible(true);
			try {
				f.set(result, value); // TODO: Look for a setter!
			} catch (Throwable t) {
				throw new IllegalStateException("Cannot set field \""+serializedName+"\".", t);
			} finally {
				if (!access) f.setAccessible(false);
			}
		}
		
		@Override
		public boolean isImmutable() {
			return false;
		}

		@Override
		public Type getType(String serializedName) {
			return fieldTypes.get(serializedName);
		}

		@Override
		public T getResult() {
			return result;
		}
	}
	
	public static class ImmutableWrapper<T> implements ObjectWrapper<T> {
		private final Type tType;
		private final Class<T> erasedType;
		private final Map<String, Field> fieldNames;
		private final Map<String, Type> fieldTypes;
		private final Map<String, Object> fieldValues = new HashMap<>();
		
		private final InstanceFactory<T> factory;
		
		public ImmutableWrapper(Class<T> clazz) {
			this.tType = clazz;
			this.erasedType = clazz;
			fieldNames = getFields(tType);
			fieldTypes = getFieldTypes(tType, fieldNames);
			factory = getCanonicalFactory();
		}
		
		@SuppressWarnings("unchecked")
		public ImmutableWrapper(Type tType) {
			this.tType = tType;
			this.erasedType = (Class<T>) ClassHierarchy.getErasedClass(tType);
			fieldNames = getFields(tType);
			fieldTypes = getFieldTypes(tType, fieldNames);
			factory = getCanonicalFactory();
		}
		
		private static Set<String> getParameters(Executable exec) {
			Set<String> paramNames = new HashSet<>();
			for(Parameter p : exec.getParameters()) {
				String serializedName = p.getName();
				SerializedName annotation = p.getAnnotation(SerializedName.class);
				if (annotation != null) serializedName = annotation.value();
				paramNames.add(serializedName);
			}
			return paramNames;
		}
		
		@SuppressWarnings("unchecked")
		private InstanceFactory<T> getCanonicalFactory() {
			boolean annotationFound = false;
			List<InstanceFactory<T>> otherFactories = new ArrayList<>();
			for(Constructor<?> cons : erasedType.getConstructors()) {
				if (cons.getAnnotation(Deserializer.class) != null) annotationFound = true;
				
				Set<String> paramNames = getParameters(cons);
				if (paramNames.size() != fieldNames.size()) continue; // Fast-reject for the cases below.
				// Accept the constructor only if it has a parameter for every field
				if (!paramNames.containsAll(fieldNames.keySet())) continue;
				// Reject the constructor if it has any parameter that is not a field
				if (!fieldNames.keySet().containsAll(paramNames)) continue;
				
				if (cons.getAnnotation(Deserializer.class) != null) {
					// We can short-circuit if we have a matching Deserializer-annotated constructor
					return (InstanceFactory<T>) InstanceFactory.of(cons);
					//annotatedFactories.add((InstanceFactory<T>) InstanceFactory.of(cons));
				} else {
					otherFactories.add((InstanceFactory<T>) InstanceFactory.of(cons));
				}
			}
			
			for(Method m : erasedType.getDeclaredMethods()) {
				// Quickly reject nonstatic methods, or methods which do not return the target type.
				if (!m.accessFlags().contains(AccessFlag.STATIC)) continue;
				if (!m.getReturnType().equals(erasedType)) continue;
				
				if (m.getAnnotation(Deserializer.class) != null) annotationFound = true;
				
				Set<String> paramNames = getParameters(m);
				if (paramNames.size() != fieldNames.size()) continue;
				if (!paramNames.containsAll(fieldNames.keySet())) continue;
				if (!fieldNames.keySet().containsAll(paramNames)) continue;
				
				if (m.getAnnotation(Deserializer.class) != null) {
					return (InstanceFactory<T>) InstanceFactory.of(m);
				} else {
					otherFactories.add((InstanceFactory<T>) InstanceFactory.of(m));
				}
			}
			
			if (annotationFound) throw new IllegalArgumentException(
					"One or more @Deserializer annotations exist for type "+tType.getTypeName()+", but none of the marked methods can be used.\n"+
					"""
					Since this class has been judged to be immutable, it can only be created with a constructor
					or factory method. Constructors and factory methods must account for each field in their
					parameter lists, including superclass fields.
					Factory methods must be static and return the type that declares them.
					
					At least one method on this class was marked for this purpose, but did not meet these
					requirements.
					""");
			
			if (otherFactories.size() > 0) return otherFactories.getFirst();
			
			throw new IllegalArgumentException(
					"No candidate deserializers exist for type "+tType.getTypeName()+".\n"+
					"""
					Since this class has been judged to be immutable, it can only be created with a constructor
					or factory method. Constructors and factory methods must account for each field in their
					parameter lists, including superclass fields.
					Factory methods must be static and return the type that declares them.
					
					No constructors or methods declared by this class met these requirements.
					""");
		}
		
		@Override
		public boolean isImmutable() {
			return true;
		}
		
		@Override
		public Set<String> getFieldNames() {
			return fieldNames.keySet();
		}
		
		@Override
		public Type getType(String serializedName) {
			return fieldTypes.get(serializedName);
		}
		
		@Override
		public void setField(String serializedName, Object value) {
			fieldValues.put(serializedName, value);
		}
		
		@Override
		public T getResult() throws InstantiationException {
			return factory.newInstance(fieldValues);
		}
		
	}
}
