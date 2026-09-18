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

package blue.endless.jankson.api.config;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Objects;

/**
 * Captures a generic Java type while preserving its compile-time type parameter.
 * Instantiate an anonymous subclass, for example {@code new TypeRef<List<String>>() {}}.
 *
 * @param <T> the represented value type
 */
public abstract class TypeRef<T> {
	private final Type type;

	protected TypeRef() {
		Type superclass = getClass().getGenericSuperclass();
		if (!(superclass instanceof ParameterizedType parameterized)
				|| parameterized.getRawType() != TypeRef.class) {
			throw new IllegalStateException("TypeRef must be created as a direct parameterized subclass");
		}
		type = requireResolved(parameterized.getActualTypeArguments()[0]);
	}

	/** Returns the captured reflective type. */
	public final Type type() { return type; }

	static Type requireResolved(Type type) {
		Type root = Objects.requireNonNull(type, "type");
		requireResolved(root, root);
		return root;
	}

	private static void requireResolved(Type type, Type root) {
		if (type instanceof TypeVariable<?> variable) {
			throw new IllegalArgumentException("Unresolved type variable '" + variable.getName()
					+ "' in type " + root.getTypeName());
		}
		if (type instanceof ParameterizedType parameterized) {
			requireResolved(parameterized.getRawType(), root);
			Type owner = parameterized.getOwnerType();
			if (owner != null) requireResolved(owner, root);
			for (Type argument : parameterized.getActualTypeArguments()) requireResolved(argument, root);
		} else if (type instanceof GenericArrayType array) {
			requireResolved(array.getGenericComponentType(), root);
		} else if (type instanceof WildcardType wildcard) {
			for (Type bound : wildcard.getLowerBounds()) requireResolved(bound, root);
			for (Type bound : wildcard.getUpperBounds()) requireResolved(bound, root);
		}
	}
}
