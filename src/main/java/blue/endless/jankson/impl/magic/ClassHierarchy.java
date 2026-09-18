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

import java.lang.reflect.AccessFlag;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ClassHierarchy {
	public record MapTypeArguments(Type keyType, Type valueType) {};
	
	
	
	/**
	 * Get the erased class corresponding to a type, or Object if there is no corresponding Class for this type.
	 * 
	 * @see <a href="https://www.artima.com/weblogs/viewpost.jsp?thread=208860">
	 *      https://www.artima.com/weblogs/viewpost.jsp?thread=208860
	 *      </a>
	 * @param type the type
	 * @return the underlying class
	 */
	public static Class<?> getErasedClass(Type type) {
		if (type instanceof SyntheticType synth) return synth.getErasure();
		if (type instanceof AnnotatedType anno) type = anno.getType();
		
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
		} else if (type instanceof WildcardType wildcard) {
			Type[] upper = wildcard.getUpperBounds();
			return upper.length == 0 ? Object.class : getErasedClass(upper[0]);
		} else if (type instanceof TypeVariable<?> variable) {
			Type[] bounds = variable.getBounds();
			return bounds.length == 0 || bounds[0] == variable ? Object.class : getErasedClass(bounds[0]);
		} else {
			return Object.class;
			//return null;
		}
	}
	
	public static Type getNextAncestor(Type child, Class<?> target) {
		Class<?> erasedChild = getErasedClass(child);
		if (erasedChild.equals(target)) return null; // We've hit the top of our range
		
		Type genericSuper = erasedChild.getGenericSuperclass();
		Class<?> erasedSuper = getErasedClass(genericSuper);
		if (genericSuper != null && target.isAssignableFrom(erasedSuper)) return genericSuper;
		
		for(Type interfaceType : erasedChild.getGenericInterfaces()) {
			Class<?> erasedInterface = getErasedClass(interfaceType);
			if (target.isAssignableFrom(erasedInterface)) return interfaceType;
		}
		
		// Couldn't find the target in this class's superclass or superinterfaces!
		// Does this class actually extend target???
		if (!target.isAssignableFrom(erasedChild)) throw new IllegalArgumentException("Target class "+target.getCanonicalName()+" is not an ancestor of type "+child.getTypeName());
		if (target == Object.class) return Object.class;
		
		return null;
	}
	
	public static Map<TypeVariable<?>, Type> getDeclaredGenerics(Type t) {
		if (t instanceof Class) return new HashMap<>();
		if (t instanceof AnnotatedType anno) {
			t = anno.getType();
			if (t instanceof Class) return new HashMap<>(); // Annotated but not generic
		}
		
		if (t instanceof TypeVariable) {
			return new HashMap<>();
		}
		
		if (t instanceof ParameterizedType pType) {
			TypeVariable<?>[] typeVars = getErasedClass(t).getTypeParameters();
			Type[] typeArgs = pType.getActualTypeArguments();
			// If we see the following, GIVE UP IMMEDIATELY. We clearly do not understand this information!
			if (typeVars.length != typeArgs.length) return new HashMap<>();
			
			Map<TypeVariable<?>, Type> result = getDeclaredGenerics(pType.getOwnerType());
			
			for(int i=0; i<typeVars.length; i++) {
				result.put(typeVars[i], typeArgs[i]);
			}
			
			return result;
		}
		
		
		return new HashMap<>();
	}
	
	
	
	public static Map<String, Type> getActualTypeArguments(Type baseType, Class<?> targetType) {
		Map<TypeVariable<?>, Type> generics = getTypeBindings(baseType, targetType);
		// Compatibility view: names are safe only within this one declaring class.
		Map<String, Type> result = new HashMap<>();
		for(TypeVariable<?> var : targetType.getTypeParameters()) {
			result.put(var.getName(), substitute(var, generics));
		}
		return result;
	}

	/**
	 * Derives declaration-keyed bindings in each ancestor's context, including its
	 * parameterized owner. Resolve each hop before rebinding: the same declaration
	 * can have different arguments as an enclosing owner and as a superclass.
	 */
	public static Map<TypeVariable<?>, Type> getTypeBindings(Type baseType, Class<?> targetType) {
		if (!targetType.isAssignableFrom(getErasedClass(baseType))) {
			throw new IllegalArgumentException(targetType.getTypeName()+" is not an ancestor of "+baseType.getTypeName());
		}
		Type current = baseType;
		while (true) {
			Map<TypeVariable<?>, Type> result = getDeclaredGenerics(current);
			if (getErasedClass(current).equals(targetType)) return result;
			Type next = getNextAncestor(current, targetType);
			if (next == null) throw new IllegalArgumentException("Cannot resolve ancestor "+targetType.getTypeName());
			current = substitute(next, result, new HashSet<>(), false);
		}
	}

	/**
	 * Resolves variables by their declaring identity, never by their name. Unbound
	 * variables and cyclic references fall back to their Java erasure. Known
	 * surrounding types survive without recursively expanding bounds.
	 */
	public static Type substitute(Type candidate, Map<TypeVariable<?>, Type> arguments) {
		return substitute(candidate, arguments, new HashSet<>(), true);
	}

	/** Unifies invariant type arguments, retaining declaration identity and repeated-variable constraints. */
	public static boolean inferTypeArguments(Type pattern, Type actual, Set<TypeVariable<?>> variables,
			Map<TypeVariable<?>, Type> bindings) {
		if (pattern instanceof TypeVariable<?> variable && variables.contains(variable)) {
			Type previous = bindings.putIfAbsent(variable, actual);
			return previous == null || previous.equals(actual);
		}
		if (pattern instanceof ParameterizedType p && actual instanceof ParameterizedType a) {
			if (!p.getRawType().equals(a.getRawType())) return false;
			if (p.getOwnerType() != null && (a.getOwnerType() == null
					|| !inferTypeArguments(p.getOwnerType(), a.getOwnerType(), variables, bindings))) return false;
			Type[] pp = p.getActualTypeArguments();
			Type[] aa = a.getActualTypeArguments();
			if (pp.length != aa.length) return false;
			for (int i = 0; i < pp.length; i++) {
				if (!inferTypeArguments(pp[i], aa[i], variables, bindings)) return false;
			}
			return true;
		}
		if (pattern instanceof GenericArrayType array) {
			Type component = actual instanceof GenericArrayType a ? a.getGenericComponentType()
					: actual instanceof Class<?> c && c.isArray() ? c.getComponentType() : null;
			return component != null && inferTypeArguments(array.getGenericComponentType(), component, variables, bindings);
		}
		return pattern.equals(actual);
	}

	/** Keeps runtime properties while recovering subclass variables from the declared ancestor context. */
	public static Type specializeRuntimeType(Type declaredType, Class<?> runtimeClass) {
		Class<?> declaredClass = getErasedClass(declaredType);
		if (declaredClass == runtimeClass) return declaredType;
		if (!(declaredType instanceof ParameterizedType) || !declaredClass.isAssignableFrom(runtimeClass)) return runtimeClass;
		Type runtimeType = SyntheticType.withOwner(runtimeClass.getDeclaringClass(), runtimeClass, runtimeClass.getTypeParameters());
		Map<TypeVariable<?>, Type> inherited = getTypeBindings(runtimeType, declaredClass);
		Map<TypeVariable<?>, Type> declared = getDeclaredGenerics(declaredType);
		Map<TypeVariable<?>, Type> inferred = new HashMap<>();
		Set<TypeVariable<?>> variables = new HashSet<>(Arrays.asList(runtimeClass.getTypeParameters()));
		for (Map.Entry<TypeVariable<?>, Type> entry : declared.entrySet()) {
			Type pattern = inherited.get(entry.getKey());
			if (pattern != null && !inferTypeArguments(pattern, entry.getValue(), variables, inferred)) {
				// A fixed runtime argument may be narrower than a declared wildcard.
				// In that case retain the runtime class's own generic hierarchy.
				return runtimeClass;
			}
		}
		return substitute(runtimeType, inferred);
	}

	private static Type substitute(Type candidate, Map<TypeVariable<?>, Type> arguments,
			Set<TypeVariable<?>> visiting, boolean finalSubstitution) {
		if (candidate instanceof AnnotatedType annotated) candidate = annotated.getType();
		if (candidate instanceof TypeVariable<?> variable) {
			if (!visiting.add(variable)) return finalSubstitution ? getErasedClass(variable) : variable;
			try {
				Type resolved = arguments.get(variable);
				return resolved == null ? (finalSubstitution ? getErasedClass(variable) : variable)
						: substitute(resolved, arguments, visiting, finalSubstitution);
			} finally {
				visiting.remove(variable);
			}
		}
		if (candidate instanceof ParameterizedType parameterized) {
			Type[] source = parameterized.getActualTypeArguments();
			Type[] resolved = new Type[source.length];
			for (int i = 0; i < source.length; i++) resolved[i] = substitute(source[i], arguments, visiting, finalSubstitution);
			Type owner = parameterized.getOwnerType();
			return SyntheticType.withOwner(owner == null ? null : substitute(owner, arguments, visiting, finalSubstitution),
					getErasedClass(parameterized), resolved);
		}
		if (candidate instanceof GenericArrayType array) {
			Type component = substitute(array.getGenericComponentType(), arguments, visiting, finalSubstitution);
			if (component instanceof Class<?> componentClass) return Array.newInstance(componentClass, 0).getClass();
			return new ResolvedGenericArrayType(component);
		}
		if (candidate instanceof WildcardType wildcard) {
			Type[] upper = wildcard.getUpperBounds();
			Type[] lower = wildcard.getLowerBounds();
			for (int i = 0; i < upper.length; i++) upper[i] = substitute(upper[i], arguments, visiting, finalSubstitution);
			for (int i = 0; i < lower.length; i++) lower[i] = substitute(lower[i], arguments, visiting, finalSubstitution);
			return new ResolvedWildcardType(upper, lower);
		}
		return candidate;
	}

	private record ResolvedGenericArrayType(Type getGenericComponentType) implements GenericArrayType {
		@Override public String getTypeName() { return getGenericComponentType.getTypeName()+"[]"; }
		@Override public boolean equals(Object other) {
			return other instanceof GenericArrayType array && getGenericComponentType.equals(array.getGenericComponentType());
		}
		@Override public int hashCode() { return getGenericComponentType.hashCode(); }
	}

	private record ResolvedWildcardType(Type[] upper, Type[] lower) implements WildcardType {
		@Override public Type[] getUpperBounds() { return upper.clone(); }
		@Override public Type[] getLowerBounds() { return lower.clone(); }
		@Override public boolean equals(Object other) {
			return other instanceof WildcardType wildcard && Arrays.equals(upper, wildcard.getUpperBounds())
					&& Arrays.equals(lower, wildcard.getLowerBounds());
		}
		@Override public int hashCode() { return Arrays.hashCode(upper) ^ Arrays.hashCode(lower); }
		@Override public String getTypeName() {
			if (lower.length > 0) return "? super "+lower[0].getTypeName();
			return upper.length == 0 || upper[0] == Object.class ? "?" : "? extends "+upper[0].getTypeName();
		}
	}
	
	/**
	 * Gets the element or member type for the provided collection type. Works for Lists, Sets,
	 * Queues, Deques, Vectors, Stacks, anything with Collection somwhere in its type
	 * hierarchy.
	 * 
	 * @param collectionType a Type representing a parameterized Collection, or a parameterized subclass of Collection.
	 * @return the type of object that can be added to a Collection of this type
	 */
	public static Type getCollectionTypeArgument(Type collectionType) {
		Map<String, Type> realTypeArguments = getActualTypeArguments(collectionType, Collection.class);
		return realTypeArguments.get("E");
	}
	
	public static MapTypeArguments getMapTypeArguments(Type mapType) {
		Map<String, Type> realTypeArguments = getActualTypeArguments(mapType, Map.class);
		return new MapTypeArguments(realTypeArguments.get("K"), realTypeArguments.get("V"));
	}
	
	/**
	 * Gets every Field in the provided type, as well as every superclass of the provided type, such
	 * that all serializeable state is represented in the returned List.
	 * @param type The type to find fields for
	 * @return a List of all serializeable fields in this Type
	 */
	public static List<Field> getAllFields(Type type) {
		List<Field> result = new ArrayList<>();
		
		Class<?> clazz = getErasedClass(type);
		if (clazz.isInterface()) return result; // Interfaces have no fields
		while (clazz != Object.class) {
			for(Field f : clazz.getDeclaredFields()) {
				Set<AccessFlag> flags = f.accessFlags();
				if (flags.contains(AccessFlag.TRANSIENT)) continue;
				if (flags.contains(AccessFlag.STATIC)) continue;
				result.add(f);
			}
			clazz = clazz.getSuperclass();
		}
		
		return result;
	}
}
