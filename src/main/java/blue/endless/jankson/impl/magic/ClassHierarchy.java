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

package blue.endless.jankson.impl.magic;

import java.lang.reflect.AccessFlag;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
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
		if (target.isAssignableFrom(erasedSuper)) return genericSuper;
		
		for(Type interfaceType : erasedChild.getGenericInterfaces()) {
			Class<?> erasedInterface = getErasedClass(interfaceType);
			if (target.isAssignableFrom(erasedInterface)) return interfaceType;
		}
		
		// Couldn't find the target in this class's superclass or superinterfaces!
		// Does this class actually extend target???
		if (!target.isAssignableFrom(erasedChild)) throw new IllegalArgumentException("Target class "+target.getCanonicalName()+" is not an ancestor of type "+child.getTypeName());
		
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
			
			Map<TypeVariable<?>, Type> result = new HashMap<>();
			
			for(int i=0; i<typeVars.length; i++) {
				result.put(typeVars[i], typeArgs[i]);
			}
			
			return result;
		}
		
		
		return new HashMap<>();
	}
	
	
	
	public static Map<String, Type> getActualTypeArguments(Type baseType, Class<?> targetType) {
		Map<TypeVariable<?>, Type> generics = getDeclaredGenerics(baseType);
		
		Type cur = baseType;
		do {
			if (!getErasedClass(cur).equals(targetType)) { // In case we were originally given the target type
				cur = getNextAncestor(cur, targetType);
			}
			
			for(Map.Entry<TypeVariable<?>, Type> entry : getDeclaredGenerics(cur).entrySet()) {
				if (entry.getValue() instanceof TypeVariable<?> var) {
					if (generics.containsKey(var)) {
						generics.put(entry.getKey(), generics.get(var));
					}
				} else {
					generics.put(entry.getKey(), entry.getValue());
				}
			}
			
		} while (!getErasedClass(cur).equals(targetType));
		
		// We've reified all the type arguments, now build the result
		Map<String, Type> result = new HashMap<>();
		
		for(TypeVariable<?> var : targetType.getTypeParameters()) {
			if (generics.containsKey(var)) {
				result.put(var.getName(), generics.get(var));
			} else {
				result.put(var.getName(), Object.class);
			}
		}
		
		return result;
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
		if (collectionType instanceof Class) return Object.class;
		
		Map<String, Type> realTypeArguments = getActualTypeArguments(collectionType, Collection.class);
		return realTypeArguments.get("E");
	}
	
	public static MapTypeArguments getMapTypeArguments(Type mapType) {
		if (mapType instanceof Class) return new MapTypeArguments(Object.class, Object.class);
		
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
