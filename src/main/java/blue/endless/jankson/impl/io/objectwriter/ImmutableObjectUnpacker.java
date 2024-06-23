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

package blue.endless.jankson.impl.io.objectwriter;

import java.lang.reflect.AccessFlag;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import javax.annotation.Nullable;

import blue.endless.jankson.api.annotation.Deserializer;

/**
 * 
 */
public class ImmutableObjectUnpacker<T> {
	private Map<String, Type> fieldTypes = new HashMap<>();
	private List<Function<Object, T>> simpleFactories = new ArrayList<>();
	private List<Function<Map<String, Object>, T>> complexFactories = new ArrayList<>();
	private List<Set<String>> ambiguousConstructorSets = new ArrayList<>();
	
	public ImmutableObjectUnpacker(Class<T> clazz) {
		
		// First things first, gather a pool of annotated and non-annotated candidates
		
		List<ConversionFunction> annotated = new ArrayList<>();
		List<ConversionFunction> nonAnnotated = new ArrayList<>();
		
		for(Constructor<?> ctor : clazz.getDeclaredConstructors()) {
			ConversionFunction cur = ConversionFunction.of(ctor);
			if (cur.isAnnotated()) {
				annotated.add(cur);
			} else {
				nonAnnotated.add(cur);
			}
		}
		
		for(Method m : clazz.getDeclaredMethods()) {
			// Instance methods can't be deserializers
			if (!m.accessFlags().contains(AccessFlag.STATIC)) continue;
			// Deserializers must produce something we can put in a field of 'clazz' type
			if (!clazz.isAssignableFrom(m.getReturnType())) continue;
			
			ConversionFunction cur = ConversionFunction.of(m);
			if (cur.isAnnotated()) {
				annotated.add(cur);
			} else {
				nonAnnotated.add(cur);
			}
		}
		
		if (!annotated.isEmpty()) {
			// This is a declaration of intent. Use the annotated functions ONLY.
		} else {
			// Since we have no annotated functions, use the non-annotated functions ONLY.
		}
	}
	
	private static interface ConversionFunction {
		public int length();
		public boolean isAnnotated();
		
		/**
		 * Gets the parameterized type of the specified argument.
		 * @param argument The name of the argument to inspect
		 * @return The type of the named argument, or null if this function has no argument with that name.
		 */
		public @Nullable Type getArgumentType(String argument);
		public Map<String, Type> getTypeMap();
		public Object invoke(Map<String, Object> arguments) throws ReflectiveOperationException;
		
		public static ConversionFunction of(Method m) {
			return new FactoryConversionFunction(m);
		}
		
		public static ConversionFunction of(Constructor<?> c) {
			return new ConstructorConversionFunction(c);
		}
	}
	
	private static class FactoryConversionFunction implements ConversionFunction {
		private final Method method;
		private final Map<String, Type> typeMap;
		
		public FactoryConversionFunction(Method method) {
			if (!method.accessFlags().contains(AccessFlag.STATIC)) throw new IllegalArgumentException("Deserializer factory methods MUST be static.");
			
			this.method = method;
			
			Map<String, Type> localTypeMap = new HashMap<>();
			for(Parameter p : method.getParameters()) {
				localTypeMap.put(p.getName(), p.getParameterizedType());
			}
			
			this.typeMap = Map.copyOf(localTypeMap);
		}

		@Override
		public int length() {
			return typeMap.size();
		}

		@Override
		public boolean isAnnotated() {
			return method.getAnnotation(Deserializer.class) != null;
		}

		@Override
		public Type getArgumentType(String argument) {
			return typeMap.get(argument);
		}
		
		public Map<String, Type> getTypeMap() {
			return typeMap;
		}

		@Override
		public Object invoke(Map<String, Object> arguments) throws ReflectiveOperationException {
			Parameter[] methodParams = method.getParameters();
			Object[] methodArgs = new Object[methodParams.length];
			for(int i=0; i<methodParams.length; i++) {
				String paramName = methodParams[i].getName();
				if (!arguments.containsKey(paramName)) {
					throw new ReflectiveOperationException("Values map is missing required parameter \""+paramName+"\"");
				}
				methodArgs[i] = arguments.get(paramName);
			}
			
			return method.invoke(null, methodArgs);
		}
	}
	
	private static class ConstructorConversionFunction implements ConversionFunction {
		private final Constructor<?> constructor;
		private final Map<String, Type> typeMap;
		
		public ConstructorConversionFunction(Constructor<?> constructor) {
			this.constructor = constructor;
			
			Map<String, Type> localTypeMap = new HashMap<>();
			for(Parameter p : constructor.getParameters()) {
				localTypeMap.put(p.getName(), p.getParameterizedType());
			}
			
			this.typeMap = Map.copyOf(localTypeMap);
		}
		
		@Override
		public int length() {
			return typeMap.size();
		}

		@Override
		public boolean isAnnotated() {
			return constructor.getAnnotation(Deserializer.class) != null;
		}

		@Override
		public Type getArgumentType(String argument) {
			return typeMap.get(argument);
		}
		
		@Override
		public Map<String, Type> getTypeMap() {
			return typeMap;
		}

		@Override
		public Object invoke(Map<String, Object> arguments) throws ReflectiveOperationException {
			Parameter[] methodParams = constructor.getParameters();
			Object[] methodArgs = new Object[methodParams.length];
			for(int i=0; i<methodParams.length; i++) {
				String paramName = methodParams[i].getName();
				if (!arguments.containsKey(paramName)) {
					throw new ReflectiveOperationException("Values map is missing required parameter \""+paramName+"\"");
				}
				methodArgs[i] = arguments.get(paramName);
			}
			
			return constructor.newInstance(methodArgs);
		}
		
	}
}
