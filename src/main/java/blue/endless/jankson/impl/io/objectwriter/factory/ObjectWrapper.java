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

package blue.endless.jankson.impl.io.objectwriter.factory;

import java.lang.invoke.MethodType;
import java.lang.reflect.AccessFlag;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.Arrays;
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
import blue.endless.jankson.impl.magic.ReflectiveProperty;

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
		
		// Explicit factories take precedence over the automatic no-arg strategy.
		// Let ImmutableWrapper validate marked factories, including malformed ones.
		for (Method method : clazz.getDeclaredMethods()) {
			if (method.isAnnotationPresent(Deserializer.class)) return new ImmutableWrapper<>(t);
		}

		boolean mutable;
		try {
			clazz.getDeclaredConstructor();
			mutable = true;
		} catch (NoSuchMethodException e) {
			mutable = false;
		}
		
		if (mutable) {
			return (result != null) ? new MutableWrapper<>(result, t) : new MutableWrapper<>(t);
		} else {
			return new ImmutableWrapper<>(t);
		}
	}
	
	
	
	private static Method getMutator(Type tType, Field field) {
		Class<?> clazz = ClassHierarchy.getErasedClass(tType);
		String fieldName = field.getName();
		String setterName = "set" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
		// Annotation priority is global; declaration depth breaks ties within it.
		for (boolean annotated : new boolean[] {true, false}) {
			for (Class<?> level = clazz; level != null && level != Object.class; level = level.getSuperclass()) {
				Method selected = null;
				for (Method method : level.getDeclaredMethods()) {
					if (Modifier.isStatic(method.getModifiers()) || method.isBridge() || method.isSynthetic()) continue;
					if (method.getParameterCount() != 1 || method.getParameterTypes()[0] != field.getType()) continue;
					MutatorFor annotation = method.getAnnotation(MutatorFor.class);
					if (annotated ? annotation == null || !annotation.value().equals(fieldName)
							: annotation != null || !method.getName().equals(setterName)) continue;
					if (!field.equals(nearestField(level, fieldName))) continue;
					if (selected != null) throw new IllegalArgumentException("Ambiguous mutators for field "+field
							+": "+selected+" and "+method);
					selected = method;
				}
				if (selected != null) {
					validateDispatch(clazz, selected, field);
					return selected;
				}
			}
		}
		return null;
	}

	private static Field nearestField(Class<?> declaringClass, String name) {
		for (Class<?> level = declaringClass; level != null; level = level.getSuperclass()) {
			try {
				// Even excluded fields form a Java field-hiding boundary.
				return level.getDeclaredField(name);
			} catch (NoSuchFieldException ignored) {
				// Continue toward the nearest declaration.
			}
		}
		return null;
	}

	private static void validateDispatch(Class<?> runtimeClass, Method selected, Field field) {
		if (Modifier.isPrivate(selected.getModifiers()) || Modifier.isFinal(selected.getModifiers())) return;
		// Walk base-to-derived so package-private overrides that widen access are
		// respected. Bridges are not candidates, but they DO participate in dispatch.
		List<Class<?>> descendants = new ArrayList<>();
		for (Class<?> level = runtimeClass; level != selected.getDeclaringClass(); level = level.getSuperclass()) {
			descendants.add(level);
		}
		Method dispatched = selected;
		for (int i = descendants.size() - 1; i >= 0; i--) {
			Class<?> level = descendants.get(i);
			int modifiers = dispatched.getModifiers();
			if (!Modifier.isPublic(modifiers) && !Modifier.isProtected(modifiers)
					&& !dispatched.getDeclaringClass().getPackageName().equals(level.getPackageName())) continue;
			for (Method method : level.getDeclaredMethods()) {
				if (Modifier.isPrivate(method.getModifiers()) || Modifier.isStatic(method.getModifiers())) continue;
				if (!method.getName().equals(selected.getName())
						|| !Arrays.equals(method.getParameterTypes(), selected.getParameterTypes())) continue;
				MutatorFor annotation = method.getAnnotation(MutatorFor.class);
				String name = annotation == null ? field.getName() : annotation.value();
				if (!field.equals(nearestField(level, name))) {
					throw new IllegalArgumentException("Mutator override crosses field boundary for "+field
							+": "+selected+" dispatches to "+method);
				}
				dispatched = method;
			}
		}
	}
	
	
	private static Map<String, ReflectiveProperty> getFields(Type tType) {
		Map<String, ReflectiveProperty> result = new HashMap<>();
		for (ReflectiveProperty property : ReflectiveProperty.of(tType)) result.put(property.wireName(), property);
		return result;
	}
	
	public static class MutableWrapper<T> implements ObjectWrapper<T> {
		private final T result;
		private final Map<String, ReflectiveProperty> fieldNames;
		private final Map<String, Method> mutators;
		
		public MutableWrapper(Type tType) {
			fieldNames = getFields(tType);
			mutators = getMutators(tType, fieldNames);
			
			try {
				Class<?> clazz = ClassHierarchy.getErasedClass(tType);
				
				if (clazz.getEnclosingClass() != null) {
					boolean isStatic = Modifier.isStatic(clazz.getModifiers());
					if (!isStatic) {
						throw new IllegalArgumentException("Cannot create an object of type "+tType.getTypeName()+".\n"+
							"""
							You may have intended to make this class a static inner class, and instead made it a
							non-static inner class. This means there can never be a no-arg constructor; there is an
							implicit argument of an instance of the enclosing class. If you did not intend this, you
							can fix this by declaring this class as "static". If not, you'll have to supply a deserializer
							some other way.
							""");
					}
				}
				@SuppressWarnings("unchecked")
				Constructor<T> constructor = (Constructor<T>) clazz.getDeclaredConstructor();
				boolean accessible = constructor.canAccess(null);
				if (!accessible) constructor.setAccessible(true);
				try {
					result = constructor.newInstance();
				} finally {
					if (!accessible) constructor.setAccessible(false);
				}
			} catch (NoSuchMethodException t) {
				throw new IllegalArgumentException("Cannot create an object of type "+tType.getTypeName()+".\n"+
						"""
						For uninitialized fields or root values of mutable classes, Jankson needs a zero-arg constructor.
						Classes come with these by default, but declaring a constructor removes this "default" constructor.
						You can fix this by declaring a no-arg constructor, or by marking this class as @Immutable.
						""", t);
			} catch (InvocationTargetException | IllegalAccessException | InstantiationException t) {
				throw new RuntimeException("An unexpected error occurred creating this object.", t);
			}
			
		}
		
		public MutableWrapper(T t, Type tType) {
			result = t;
			fieldNames = getFields(tType);
			mutators = getMutators(tType, fieldNames);
			for (Map.Entry<String, Method> entry : mutators.entrySet()) {
				validateDispatch(t.getClass(), entry.getValue(), fieldNames.get(entry.getKey()).field());
			}
		}

		private static Map<String, Method> getMutators(Type type, Map<String, ReflectiveProperty> properties) {
			Map<String, Method> result = new HashMap<>();
			for (ReflectiveProperty property : properties.values()) {
				Method mutator = getMutator(type, property.field());
				if (mutator != null) result.put(property.wireName(), mutator);
			}
			return result;
		}
		
		@Override
		public Set<String> getFieldNames() {
			return fieldNames.keySet();
		}
		
		@Override
		public void setField(String serializedName, Object value) throws ReflectiveOperationException {
			ReflectiveProperty property = fieldNames.get(serializedName);
			if (property == null) throw new IllegalArgumentException("No field with name \""+serializedName+"\"");
			Field f = property.field();
			
			Method m = mutators.get(serializedName);
			if (m != null) {
				boolean access = m.canAccess(result);
				try {
					if (!access) m.setAccessible(true);
					m.invoke(result, value);
					return;
				} catch (InvocationTargetException e) {
					throw new ReflectiveOperationException(e.getCause() == null ? e : e.getCause());
				} catch (ReflectiveOperationException | RuntimeException e) {
					throw new ReflectiveOperationException(e);
				} finally {
					if (!access) m.setAccessible(false);
				}
			}
			if (Modifier.isFinal(f.getModifiers())) return;
			
			boolean access = f.canAccess(result);
			if (!access) f.setAccessible(true);
			try {
				f.set(result, value);
			} catch (IllegalAccessException | IllegalArgumentException e) {
				throw new IllegalStateException("Cannot set field \""+serializedName+"\".", e);
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
			ReflectiveProperty property = fieldNames.get(serializedName);
			return property == null ? null : property.type();
		}

		@Override
		public T getResult() {
			return result;
		}
	}
	
	public static class ImmutableWrapper<T> implements ObjectWrapper<T> {
		private final Type tType;
		private final Class<T> erasedType;
		private final Map<String, ReflectiveProperty> fieldNames;
		private final Map<String, Object> fieldValues = new HashMap<>();
		
		private final InstanceFactory<T> factory;
		
		public ImmutableWrapper(Class<T> clazz) {
			this((Type) clazz);
		}
		
		@SuppressWarnings("unchecked")
		public ImmutableWrapper(Type tType) {
			this.tType = tType;
			this.erasedType = (Class<T>) ClassHierarchy.getErasedClass(tType);
			fieldNames = getFields(tType);
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

		private boolean matchesFields(Executable executable) {
			Set<String> names = getParameters(executable);
			// Each property must occur exactly once, not just once after deduplication.
			if (executable.getParameterCount() != names.size() || !names.equals(fieldNames.keySet())) return false;
			Map<TypeVariable<?>, Type> bindings = factoryBindings(executable);
			if (bindings == null) return false;
			for (Parameter parameter : executable.getParameters()) {
				SerializedName annotation = parameter.getAnnotation(SerializedName.class);
				String name = annotation == null ? parameter.getName() : annotation.value();
				Type parameterType = ClassHierarchy.substitute(parameter.getParameterizedType(), bindings);
				if (!acceptsParameter(parameterType, fieldNames.get(name).type())) return false;
			}
			return true;
		}

		private Map<TypeVariable<?>, Type> factoryBindings(Executable executable) {
			Map<TypeVariable<?>, Type> bindings = ClassHierarchy.getTypeBindings(tType, executable.getDeclaringClass());
			if (executable instanceof Method method && tType instanceof ParameterizedType
					&& method.getGenericReturnType() instanceof ParameterizedType) {
				Set<TypeVariable<?>> variables = new HashSet<>(Arrays.asList(method.getTypeParameters()));
				if (!ClassHierarchy.inferTypeArguments(method.getGenericReturnType(), tType, variables, bindings)) return null;
				for (TypeVariable<?> variable : method.getTypeParameters()) {
					Type inferred = bindings.get(variable);
					if (inferred == null) continue;
					for (Type bound : variable.getBounds()) {
						if (!acceptsParameter(ClassHierarchy.substitute(bound, bindings), inferred)) return null;
					}
				}
			}
			return bindings;
		}

		private static boolean acceptsParameter(Type parameterType, Type propertyType) {
			Class<?> parameter = ClassHierarchy.getErasedClass(parameterType);
			Class<?> property = ClassHierarchy.getErasedClass(propertyType);
			Class<?> boxedProperty = MethodType.methodType(property).wrap().returnType();
			if (!parameter.isPrimitive()) {
				if (!parameter.isAssignableFrom(boxedProperty)) return false;
				Type referencePropertyType = property.isPrimitive() ? boxedProperty : propertyType;
				if (parameterType instanceof GenericArrayType parameterArray) {
					Type propertyComponent = referencePropertyType instanceof GenericArrayType propertyArray
							? propertyArray.getGenericComponentType() : property.getComponentType();
					return propertyComponent != null
							&& acceptsParameter(parameterArray.getGenericComponentType(), propertyComponent);
				}
				if (!(parameterType instanceof ParameterizedType)) return true;

				Map<TypeVariable<?>, Type> parameterBindings = ClassHierarchy.getTypeBindings(parameterType, parameter);
				Map<TypeVariable<?>, Type> propertyBindings = ClassHierarchy.getTypeBindings(referencePropertyType, parameter);
				for (TypeVariable<?> variable : parameter.getTypeParameters()) {
					Type required = ClassHierarchy.substitute(variable, parameterBindings);
					Type supplied = ClassHierarchy.substitute(variable, propertyBindings);
					if (!acceptsTypeArgument(required, supplied)) return false;
				}
				return true;
			}
			Class<?> unboxedProperty = MethodType.methodType(boxedProperty).unwrap().returnType();
			if (parameter == unboxedProperty) return true;
			// Reflection permits unboxing followed by primitive widening, but never narrowing.
			return switch (unboxedProperty.getName()) {
				case "byte" -> parameter == short.class || parameter == int.class || parameter == long.class
						|| parameter == float.class || parameter == double.class;
				case "short", "char" -> parameter == int.class || parameter == long.class
						|| parameter == float.class || parameter == double.class;
				case "int" -> parameter == long.class || parameter == float.class || parameter == double.class;
				case "long" -> parameter == float.class || parameter == double.class;
				case "float" -> parameter == double.class;
				default -> false;
			};
		}

		private static boolean acceptsTypeArgument(Type required, Type supplied) {
			if (required.equals(supplied)) return true;
			if (!(required instanceof WildcardType wildcard)) return false;
			// Containment compares the entire range, not the erasure of a wildcard.
			if (supplied instanceof WildcardType suppliedWildcard) {
				for (Type upper : wildcard.getUpperBounds()) {
					boolean contained = false;
					for (Type suppliedUpper : suppliedWildcard.getUpperBounds()) {
						contained |= acceptsParameter(upper, suppliedUpper);
					}
					if (!contained) return false;
				}
				for (Type lower : wildcard.getLowerBounds()) {
					boolean contained = false;
					for (Type suppliedLower : suppliedWildcard.getLowerBounds()) {
						contained |= acceptsParameter(suppliedLower, lower);
					}
					if (!contained) return false;
				}
				return true;
			}
			for (Type upper : wildcard.getUpperBounds()) {
				if (!acceptsParameter(upper, supplied)) return false;
			}
			for (Type lower : wildcard.getLowerBounds()) {
				if (!acceptsParameter(supplied, lower)) return false;
			}
			return true;
		}

		private Map<String, Type> parameterTypes(Executable executable) {
			Map<String, Type> result = new HashMap<>();
			Map<TypeVariable<?>, Type> bindings = factoryBindings(executable);
			for (Parameter parameter : executable.getParameters()) {
				SerializedName annotation = parameter.getAnnotation(SerializedName.class);
				result.put(annotation == null ? parameter.getName() : annotation.value(),
						ClassHierarchy.substitute(parameter.getParameterizedType(), bindings));
			}
			return result;
		}

		private boolean moreSpecific(Executable candidate, Executable other) {
			Map<String, Type> candidateTypes = parameterTypes(candidate);
			Map<String, Type> otherTypes = parameterTypes(other);
			boolean strict = false;
			for (Map.Entry<String, Type> entry : candidateTypes.entrySet()) {
				Type candidateType = entry.getValue();
				Type otherType = otherTypes.get(entry.getKey());
				if (otherType == null || !acceptsParameter(otherType, candidateType)) return false;
				strict |= !acceptsParameter(candidateType, otherType);
			}
			return strict;
		}
		
		@SuppressWarnings("unchecked")
		private InstanceFactory<T> getCanonicalFactory() {
			boolean annotationFound = false;
			List<Executable> markedFactories = new ArrayList<>();
			List<Executable> otherFactories = new ArrayList<>();
			for(Constructor<?> cons : erasedType.getDeclaredConstructors()) {
				if (!matchesFields(cons)) continue;
				otherFactories.add(cons);
			}
			
			for(Method m : erasedType.getDeclaredMethods()) {
				if (m.getAnnotation(Deserializer.class) != null) annotationFound = true;

				// Quickly reject nonstatic methods, or methods which do not return the target type.
				if (!m.accessFlags().contains(AccessFlag.STATIC)) continue;
				if (!m.getReturnType().equals(erasedType)) continue;
				
				if (!matchesFields(m)) continue;
				
				if (m.getAnnotation(Deserializer.class) != null) {
					markedFactories.add(m);
				} else {
					otherFactories.add(m);
				}
			}

			if (markedFactories.size() == 1) return (InstanceFactory<T>) InstanceFactory.of((Method) markedFactories.getFirst());
			if (markedFactories.size() > 1) throw new IllegalArgumentException("Ambiguous @Deserializer factories for type "
					+tType.getTypeName()+": "+markedFactories);
			
			if (annotationFound) throw new IllegalArgumentException(
					"One or more @Deserializer annotations exist for type "+tType.getTypeName()+", but none of the marked methods can be used.\n"+
					"""
					Since this class has been judged to be immutable, it can only be created with a constructor
					or factory method. Constructors and factory methods must account for each field in their
					parameter lists exactly once, including superclass fields, with compatible parameter types.
					Factory methods must be static and return the type that declares them.
					
					At least one method on this class was marked for this purpose, but did not meet these
					requirements.
					""");
			
			List<Executable> mostSpecific = otherFactories.stream()
					.filter(candidate -> otherFactories.stream().noneMatch(other -> candidate != other
							&& moreSpecific(other, candidate)))
					.toList();
			if (mostSpecific.size() == 1) {
				Executable selected = mostSpecific.getFirst();
				return selected instanceof Constructor<?> constructor
						? (InstanceFactory<T>) InstanceFactory.of(constructor)
						: (InstanceFactory<T>) InstanceFactory.of((Method) selected);
			}
			if (mostSpecific.size() > 1) throw new IllegalArgumentException("Ambiguous unmarked deserializers for type "
					+tType.getTypeName()+": "+mostSpecific+". Mark the intended factory with @Deserializer.");
			
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
			ReflectiveProperty property = fieldNames.get(serializedName);
			return property == null ? null : property.type();
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
