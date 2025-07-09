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
