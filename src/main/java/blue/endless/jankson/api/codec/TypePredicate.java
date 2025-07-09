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

package blue.endless.jankson.api.codec;

import java.lang.reflect.Type;
import java.util.Objects;
import java.util.function.Predicate;

import javax.annotation.ParametersAreNonnullByDefault;

import blue.endless.jankson.impl.magic.ClassHierarchy;

/**
 * A Type Predicate that allows users to inspect the target type. This is not a "full" inspection because one might
 * 
 */
@ParametersAreNonnullByDefault
public interface TypePredicate extends Predicate<Type> {
	
	Type getTarget();
	
	public static TypePredicate exact(Type t) {
		Objects.requireNonNull(t);
		return new Exact(t);
	}
	
	public static TypePredicate ofClass(Class<?> clazz) {
		return new ClassAndSubclasses(clazz);
	}
	
	public static final class Exact implements TypePredicate {
		
		private final Type target;
		
		public Exact(Type t) {
			this.target = t;
		}
		
		@Override
		public Type getTarget() {
			return target;
		}
		
		@Override
		public boolean test(Type t) {
			Objects.requireNonNull(t);
			return target.equals(t);
		}
		
	}
	
	public static final class ClassAndSubclasses implements TypePredicate {
		
		private final Class<?> target;
		
		public ClassAndSubclasses(Class<?> clazz) {
			Objects.requireNonNull(clazz);
			this.target = clazz;
		}
		
		@Override
		public Type getTarget() {
			return target;
		}
		
		@Override
		public boolean test(Type t) {
			Objects.requireNonNull(t);
			Class<?> tClass = ClassHierarchy.getErasedClass(t);
			return target.isAssignableFrom(tClass);
		}
	}
}
