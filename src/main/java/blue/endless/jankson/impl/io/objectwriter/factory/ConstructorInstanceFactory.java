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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;

public class ConstructorInstanceFactory<T> implements InstanceFactory<T> {
	private final Constructor<T> constructor;
	
	public ConstructorInstanceFactory(Constructor<T> constructor) {
		this.constructor = constructor;
	}
	
	@Override
	public T newInstance(Map<String, Object> arguments) throws InstantiationException {
		Object[] arrangedArguments = InstanceFactory.arrangeArguments(constructor, arguments);
		boolean access = constructor.canAccess(null);
		try {
			if (!access) constructor.setAccessible(true);
			return constructor.newInstance(arrangedArguments);
		} catch (InvocationTargetException e) {
			throw failure(e.getCause() == null ? e : e.getCause());
		} catch (ReflectiveOperationException | RuntimeException e) {
			throw failure(e);
		} finally {
			if (!access) constructor.setAccessible(false);
		}
	}

	private InstantiationException failure(Throwable cause) {
		String typeName = constructor.getAnnotatedReturnType().getType().getTypeName();
		InstantiationException result = new InstantiationException("Could not create an instance of class \""+typeName+"\"");
		result.initCause(cause);
		return result;
	}
}
