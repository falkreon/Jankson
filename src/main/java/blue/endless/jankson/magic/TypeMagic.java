/*
 * MIT License
 *
 * Copyright (c) 2018 Falkreon
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

package blue.endless.jankson.magic;

import java.lang.reflect.Constructor;
import java.lang.reflect.Type;

import javax.annotation.Nullable;

public class TypeMagic {
	@Nullable
	public static Class<?> classForType(Type t) {
		String className = t.getTypeName();
		int typeParamStart = className.indexOf('<');
		if (typeParamStart>=0) {
			className = className.substring(0, typeParamStart);
		}
		
		try {
			return Class.forName(className);
		} catch (ClassNotFoundException ex) {
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
			return (U) createAndCast(classForType(t));
		} catch (Throwable ex) {
			return null;
		}
	}
	
	/**
	 * Attempts to create a new instance of the specified class using its no-arg constructor, if it has one. This might
	 * work even if the class is private or the constructor is private/hidden!
	 * @param <U> the target type.
	 * @param t the source type. The object will be created as this type.
	 * @return a new object of type U. If any part of this process fails, this method silently returns null instead.
	 */
	@Nullable
	public static <U> U createAndCast(Class<U> t) {
		try {
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
					return null;
				}
			}
			boolean available = noArg.isAccessible();
			if (!available) noArg.setAccessible(true);
			U u = noArg.newInstance();
			if (!available) noArg.setAccessible(false); //restore accessibility
			return u;
		} catch (Throwable ex) {
			ex.printStackTrace();
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
}
