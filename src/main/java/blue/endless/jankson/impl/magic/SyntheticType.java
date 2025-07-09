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

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Arrays;
import java.util.Objects;

public class SyntheticType<T> implements ParameterizedType {
	
	private final Class<T> erasure;
	private final Type[] typeArguments;
	private final Type ownerType;
	
	public SyntheticType(Class<T> clazz, Type... typeArgs) {
		this.erasure = clazz;
		this.typeArguments = typeArgs;
		this.ownerType = clazz.getEnclosingClass();
		if (clazz.getTypeParameters().length != typeArgs.length) {
			String typeArgList = Arrays.stream(clazz.getTypeParameters())
					.map(it -> it.getName())
					.reduce((a, b) -> a + ", " + b)
					.orElse("(none)");
			throw new IllegalArgumentException(
					"This class needs the following type parameters: " + typeArgList +
					" (" + clazz.getTypeParameters().length + " type params) but " +
					typeArgs.length + " parameters were supplied."
					);
		}
	}
	
	@SuppressWarnings("unchecked")
	public SyntheticType(Type reified) {
		//Throw away annotations
		if (reified instanceof AnnotatedType anno) reified = anno.getType();
		if (reified instanceof TypeVariable) throw new IllegalArgumentException("Cannot create SyntheticType for non-concrete type TypeVariable");
		
		if (reified instanceof SyntheticType synthetic) {
			this.erasure = synthetic.erasure;
			this.typeArguments = synthetic.typeArguments;
			this.ownerType = synthetic.ownerType;
		} else if (reified instanceof Class clazz) {
			this.erasure = clazz;
			this.typeArguments = new Type[clazz.getTypeParameters().length];
			Arrays.fill(this.typeArguments, Object.class);
			this.ownerType = clazz.getEnclosingClass();
		} else if (reified instanceof ParameterizedType param) {
			this.erasure = (Class<T>) ClassHierarchy.getErasedClass(reified);
			this.typeArguments = param.getActualTypeArguments();
			this.ownerType = param.getOwnerType();
		} else {
			throw new IllegalArgumentException("Unknown class for type \""+reified.getTypeName()+"\": "+reified.getClass().getCanonicalName());
		}
	}
	
	@Override
	public Type[] getActualTypeArguments() {
		return typeArguments;
	}

	@Override
	public Type getOwnerType() {
		return ownerType;
	}

	@Override
	public Type getRawType() {
		return erasure;
	}
	
	public Class<T> getErasure() {
		return erasure;
	}
	
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof ParameterizedType param) {
			return param.getRawType().equals(erasure) &&
					Arrays.equals(param.getActualTypeArguments(), typeArguments) &&
					Objects.equals(param.getOwnerType(), ownerType);
		} else {
			return false;
		}
	}
	
	public static SyntheticType<?> of(Field f) {
		return new SyntheticType<>(f.getGenericType());
	}
}
