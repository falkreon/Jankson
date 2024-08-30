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

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Map;

import blue.endless.jankson.api.annotation.SerializedName;

public interface InstanceFactory<T> {
	public T newInstance(Map<String, Object> arguments) throws InstantiationException;
	
	public static Object[] arrangeArguments(Executable method, Map<String, Object> arguments) throws InstantiationException {
		Parameter[] parameters = method.getParameters();
		Object[] arrangedArguments = new Object[parameters.length];
		for(int i = 0; i < parameters.length; i++) {
			String serializedName = parameters[i].getName();
			SerializedName annotation = parameters[i].getAnnotation(SerializedName.class);
			if (annotation != null) serializedName = annotation.value();
			if (!arguments.containsKey(serializedName)) throw new InstantiationException("No value supplied for required field \""+serializedName+"\". ("+arguments+")");
			arrangedArguments[i] = arguments.get(serializedName);
		}
		return arrangedArguments;
	}
	
	public static <T> InstanceFactory<T> of(Constructor<T> constructor) {
		return new ConstructorInstanceFactory<>(constructor);
	}
	
	public static <T> InstanceFactory<T> of (Method method) {
		return new MethodInstanceFactory<>(method);
	}
}