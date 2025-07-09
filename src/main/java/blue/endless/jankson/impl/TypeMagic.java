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

package blue.endless.jankson.impl;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.InaccessibleObjectException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import blue.endless.jankson.api.io.JsonIOException;

public class TypeMagic {
	private static Map<Class<?>, Class<?>> concreteClasses = new HashMap<>();
	static {
		concreteClasses.put(Map.class, HashMap.class);
		concreteClasses.put(Set.class, HashSet.class);
		concreteClasses.put(Collection.class, ArrayList.class);
		concreteClasses.put(List.class, ArrayList.class);
		concreteClasses.put(Queue.class, ArrayDeque.class);
		concreteClasses.put(Deque.class, ArrayDeque.class);
	}
	
	
	/**
	 * This is a surprisingly intractable problem in Java: "Type" pretty much represents all possible states of reified
	 * and unreified type information, and each kind of Type has different, mutually exclusive, and often unintended
	 * ways of uncovering its (un-reified) class.
	 * 
	 * <p>Generally it's much safer to use this for the type from a *field* than a blind type from an argument.
	 */
	@Nullable
	public static Class<?> classForType(Type t) {
		if (t instanceof Class) return (Class<?>) t;
		
		if (t instanceof ParameterizedType) {
			Type subtype = ((ParameterizedType)t).getRawType();
			
			/**
			 * Testing for kind of a unicorn case here. Because getRawType returns a Type, there's always the nasty
			 * possibility we get a recursively parameterized type. Now, that's not supposed to happen, but let's not
			 * rely on "supposed to".
			 */
			if (subtype instanceof Class) {
				return (Class<?>) subtype;
			} else {
				/**
				 * We're here at the unicorn case, against all odds. Let's take a lexical approach: The typeName will
				 * always start with the FQN of the class, followed by 
				 */
				
				String className = t.getTypeName();
				int typeParamStart = className.indexOf('<');
				if (typeParamStart>=0) {
					className = className.substring(0, typeParamStart);
				}
				
				try {
					return Class.forName(className);
				} catch (ClassNotFoundException ex) {
				}
			}
		}
		
		if (t instanceof WildcardType) {
			Type[] upperBounds = ((WildcardType)t).getUpperBounds();
			if (upperBounds.length==0) return Object.class; //Well, we know it's an Object class.....
			return classForType(upperBounds[0]); //I'm skeptical about multiple bounds on this one, but so far it's been okay.
		}
		
		if (t instanceof TypeVariable) {
			return Object.class;
			/*//This gets us into all kinds of trouble with multiple bounds, it turns out
			Type[] types = ((TypeVariable<?>)t).getBounds();
			if (types.length==0) return Object.class;
			return classForType(types[0]);*/
		}
		
		if (t instanceof GenericArrayType) {
			GenericArrayType arrayType = (GenericArrayType)t;
			/* ComponentClass will in practice return a TypeVariable, which will resolve to Object.
			 * This is actually okay, because *any time* you try and create a T[], you'll wind up making an Object[]
			 * instead and stuffing it into the T[]. And then it'll work.
			 * 
			 * And if Java magically improves their reflection system and/or less-partially reifies generics down the line,
			 * we can improve the TypeVariable case and wind up with more correctly-typed classes here.
			 */
			Class<?> componentClass = classForType(arrayType.getGenericComponentType());
			try {
				//We can always retrieve the class under a "dots" version of the binary name, as long as componentClass wound up resolving to a valid Object type
				Class<?> arrayClass = Class.forName("[L"+componentClass.getCanonicalName()+";");
				
				return arrayClass;
			} catch (ClassNotFoundException ex2) {
				return Object[].class; //This is probably what we're serving up anyway, so we might as well give the known-at-compile-time one out as a last resort.
			}
		}
		
		return null;
	}
	
	/**
	 * Attempts to create a new instance of type t, and (unsafely) cast it to the target type U. This might work even if
	 * the class is private or has a private constructor.
	 * @param <U> the target type.
	 * @param t the source type. The object will be created as this type.
	 * @return an object of type t, cast to type U. If any part of this process fails, this method silently returns null
	 *         instead.
	 */
	@SuppressWarnings("unchecked")
	@Nullable
	public static <U> U createAndCast(Type t) {
		try {
			return (U) createAndCast(classForType(t), false);
		} catch (Throwable ex) {
			ex.printStackTrace();
			return null;
		}
	}
	
	public static <U> U createAndCastCarefully(Type t) throws JsonIOException {
		return createAndCast(classForType(t));
	}
	
	/**
	 * Attempts to create a new instance of the specified class using its no-arg constructor, if it has one. This might
	 * work even if the class is private or the constructor is private/hidden!
	 * @param <U> the target type.
	 * @param t the source type. The object will be created as this type.
	 * @return a new object of type U. If any part of this process fails, this method silently returns null instead.
	 */
	@SuppressWarnings("unchecked")
	@Nullable
	public static <U> U createAndCast(Class<U> t, boolean failFast) throws JsonIOException {
		if (t.isInterface()) {
			Class<?> substitute = concreteClasses.get(t);
			if (substitute!=null) try {
				return (U) createAndCast(substitute);
			} catch (Throwable ex) {
				return null;
			}
		}
		
		/* Using getConstructor instead of class::newInstance takes some errors we can't otherwise detect, and
		 * instead wraps them in InvocationTargetExceptions which we *can* catch.
		 */
		Constructor<U> noArg = null;
		try {
			noArg = t.getConstructor();
		} catch (Throwable ex2) {
			try {
				noArg = t.getDeclaredConstructor();
			} catch (Throwable ex3) {
				if (failFast) {
					throw new JsonIOException("Class "+t.getCanonicalName()+" doesn't have a no-arg constructor, so an instance can't be created.");
				}
				return null;
			}
		}
		
		try {
			boolean available = noArg.canAccess(null);
			//boolean available = noArg.isAccessible();
			if (!available) noArg.setAccessible(true);
			U u = noArg.newInstance();
			if (!available) noArg.setAccessible(false); //restore accessibility
			return u;
		} catch (Throwable ex) {
			if (failFast) {
				throw new JsonIOException("An error occurred while creating an object.", ex);
			}
			return null;
		}
	}
	
	/**
	 * Extremely unsafely casts an object into another type. It's possible to mangle a List&lt;Integer&gt; into a
	 * List&lt;String&gt; this way, and the JVM might not throw an error until the program attempts to insert a String!
	 * So use this method with extreme caution as a last resort.
	 * @param <T> the destination type
	 * @param o the source object, of any type
	 * @return the source object cast to T.
	 */
	@SuppressWarnings("unchecked")
	public static <T> T shoehorn(Object o) {
		return (T) o;
	}
	
	
	
	public static Optional<Field> getField(Type type, String fieldName) {
		try {
			return Optional.of(type.getClass().getDeclaredField(fieldName));
		} catch (NoSuchFieldException | SecurityException e) {
			// 👻
		}
		
		try {
			return Optional.of(type.getClass().getField(fieldName));
		} catch (NoSuchFieldException | SecurityException e) {
			// 👻
		}
		
		return Optional.empty();
	}
	
	/**
	 * Reflectively gets the value of a Field, setting and then reverting the accessible flag on the object if needed.
	 * @param field     The field to retrieve
	 * @param instance  The object instance that "owns" the field
	 * @return          The value of that field, promoted to an Object by reflection if needed.
	 * @throws InaccessibleObjectException  if the value can't be obtained at all. In particular, we can expect this
	 *    to happen for private members of another module.
	 */
	public static Object getFieldValue(Field field, Object instance) throws InaccessibleObjectException {
		if (field.canAccess(instance)) {
			try {
				return field.get(instance);
			} catch (Throwable t) {
				throw new InaccessibleObjectException();
			}
		} else {
			try {
				field.setAccessible(true);
				Object result = field.get(instance);
				field.setAccessible(false);
				
				return result;
			} catch (Throwable t) {
				throw new InaccessibleObjectException();
			}
		}
	}
	
	public static boolean setFieldValue(Field field, Object instance, Object value) {
		if (field.canAccess(instance)) {
			try {
				field.set(instance, value);
				return true;
			} catch (Throwable t) {
				return false;
			}
		} else {
			try {
				field.setAccessible(true);
				field.set(instance, value);
				field.setAccessible(false);
				
				return true;
			} catch (Throwable t) {
				return false;
			}
		}
	}
	
	public static enum Nullity {
		NULLABLE,
		NON_NULLABLE,
		UNKNOWN;
		
		public boolean isNullable() {
			return this==NULLABLE || this==UNKNOWN;
		}
	}
	
	private static final String[] NONNULL_ANNOTATIONS = {
		"NonNull",
		"Nonnull",
		"NotNull",
	};
	
	private static final String[] NULLABLE_ANNOTATIONS = {
		"Nullable",
		"CheckForNull",
	};
	
	/**
	 * Examines the annotations on a method or field for clues about its nullity.
	 * @param o The method or field to inspect
	 * @return NULLABLE if the value can definitely be null. NON_NULLABLE if null is definitely not allowed.
	 *         UNKNOWN if there do not appear to be any explicit rules defined.
	 */
	public static Nullity getNullity(AccessibleObject o) {
		//See https://github.com/google/guava/issues/2960 for rationale for using SimpleNames instead of a longer list of FQNs.
		
		for(Annotation a : o.getAnnotations()) {
			//Is it a known nullable annotation?
			String simpleName = a.getClass().getSimpleName();
			for(String s : NONNULL_ANNOTATIONS) {
				if (s.equals(simpleName)) return Nullity.NON_NULLABLE;
			}
			
			//Is it a known nonnull annotation?
			for(String s : NULLABLE_ANNOTATIONS) {
				if (s.equals(simpleName)) return Nullity.NULLABLE;
			}
		}
		
		//Indirect annotations are more or less a bust.
		//Not directly annotated. Is it indirectly annotated?
		/*
		Class<?> enclosingType = f.getDeclaringClass();
		for(Annotation a : enclosingType.getAnnotations()) {
			String simpleName = a.getClass().getSimpleName();
			for(String s : INDIRECT_NONNULL_ANNOTATIONS) {
				if (s.equals(simpleName)) return Nullity.NON_NULLABLE;
			}
		}*/
		
		return Nullity.UNKNOWN;
	}
	
	
	
	
	/**
	 * Get the erased class corresponding to a type, or null if there is no corresponding Class for this type.
	 * 
	 * @see <a href="https://www.artima.com/weblogs/viewpost.jsp?thread=208860">
	 *      https://www.artima.com/weblogs/viewpost.jsp?thread=208860
	 *      </a>
	 * @param type the type
	 * @return the underlying class
	 */
	public static Class<?> getErasedClass(Type type) {
		// Original impl returned the member type of generic arrays as 
		if (type instanceof Class clazz) {
			return clazz;
		} else if (type instanceof ParameterizedType pt) {
			// We just want to erase the type parameters. But while erasing the type parameters
			// will usually give us a Class, it could also give us something else we need to unpack,
			// which then needs to be unpacked, so just feed it back through.
			return getErasedClass(pt.getRawType());
		} else if (type instanceof GenericArrayType gen) {
			// For arrays, there's no way around getting the member type, making a new instance,
			// and getting the class of that instance, unfortunately.
			Type memberType = gen.getGenericComponentType();
			Class<?> erasedMemberType = getErasedClass(memberType);
			if (erasedMemberType == null) return null;
			return Array.newInstance(erasedMemberType, 0).getClass();
		} else {
			return null;
		}
	}
	
	/**
	 * Get the actual type arguments a child class has used to extend a generic base class.
	 *
	 * @see <a href="https://www.artima.com/weblogs/viewpost.jsp?thread=208860">
	 *      https://www.artima.com/weblogs/viewpost.jsp?thread=208860
	 *      </a>
	 * @param baseClass the base class
	 * @param childClass the child class
	 * @return a list of the raw classes for the actual type arguments.
	 */
	/*
	public static <T> List<Type> getTypeArguments(Type baseType, Class<?> interfaceType) {
		Map<Type, Type> resolvedTypes = new HashMap<Type, Type>();
		// start walking up the inheritance hierarchy until we hit baseClass
		Type type = baseType;
		while (! type.equals(interfaceType)) {
			if (type == Object.class) {
				return null;
			}
			
			if (type instanceof Class) {
				// there is no useful information for us in raw types, so just keep going.
				type = ((Class<?>) type).getGenericSuperclass();
			} else {
				ParameterizedType parameterizedType = (ParameterizedType) type;
				Class<?> rawType = (Class<?>) parameterizedType.getRawType();

				Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
				TypeVariable<?>[] typeParameters = rawType.getTypeParameters();
				for (int i = 0; i < actualTypeArguments.length; i++) {
					resolvedTypes.put(typeParameters[i], actualTypeArguments[i]);
				}

				if (!rawType.equals(baseClass)) {
					type = rawType.getGenericSuperclass();
				}
			}
		}

		// finally, for each actual type argument provided to baseClass, determine (if possible)
		// the raw class for that type argument.
		Type[] actualTypeArguments;
		if (type instanceof Class) {
			actualTypeArguments = ((Class) type).getTypeParameters();
		}
		else {
			actualTypeArguments = ((ParameterizedType) type).getActualTypeArguments();
		}
		List<Class<?>> typeArgumentsAsClasses = new ArrayList<Class<?>>();
		// resolve types by chasing down type variables.
		for (Type baseType: actualTypeArguments) {
			while (resolvedTypes.containsKey(baseType)) {
				baseType = resolvedTypes.get(baseType);
			}
			typeArgumentsAsClasses.add(getClass(baseType));
		}
		return typeArgumentsAsClasses;
	}*/
}
