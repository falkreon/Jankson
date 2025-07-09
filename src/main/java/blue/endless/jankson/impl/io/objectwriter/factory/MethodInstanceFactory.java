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

import java.lang.reflect.Method;
import java.util.Map;

public class MethodInstanceFactory<T> implements InstanceFactory<T> {
	private final Method method;
	
	public MethodInstanceFactory(Method method) {
		this.method = method;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public T newInstance(Map<String, Object> arguments) throws InstantiationException {
		Object[] arrangedArguments = InstanceFactory.arrangeArguments(method, arguments);
		
		try {
			boolean access = method.canAccess(null);
			if (!access) method.setAccessible(true);
			T result = (T) method.invoke(null, arrangedArguments);
			if (!access) method.setAccessible(false);
			return result;
		} catch (Throwable t) {
			String typeName = method.getAnnotatedReturnType().getType().getTypeName();
			throw new InstantiationException("Could not create an instance of class \""+typeName+"\"");
		}
	}
}