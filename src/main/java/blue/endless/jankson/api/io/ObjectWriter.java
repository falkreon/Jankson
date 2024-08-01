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

package blue.endless.jankson.api.io;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;

import javax.annotation.Nullable;

import blue.endless.jankson.api.SyntaxError;
import blue.endless.jankson.api.document.PrimitiveElement;
import blue.endless.jankson.api.function.CheckedFunction;
import blue.endless.jankson.impl.io.objectwriter.ArrayFunction;
import blue.endless.jankson.impl.io.objectwriter.CollectionFunction;
import blue.endless.jankson.impl.io.objectwriter.MapFunction;
import blue.endless.jankson.impl.io.objectwriter.ObjectFunction;
import blue.endless.jankson.impl.io.objectwriter.RecordFunction;
import blue.endless.jankson.impl.io.objectwriter.PrimitiveFunction;
import blue.endless.jankson.impl.magic.ClassHierarchy;

@SuppressWarnings("unchecked")
public class ObjectWriter<T> implements StructuredDataWriter {
	private final Type type;
	private T subject;
	private boolean complete = false;
	
	private StructuredDataFunction<Object> delegate = null;
	
	//private Function<Object, Optional<T>> mapper = it -> (Optional<T>) Optional.of(it);
	
	public ObjectWriter(Type subjectType, T subject) {
		this.subject = subject;
		this.type = subjectType;
	}
	
	public ObjectWriter(T subject) {
		this.subject = subject;
		this.type = subject.getClass();
	}
	
	public ObjectWriter(Type t) {
		this.type = t;
		this.subject = null;
	}
	
	// This constructor is identical to ObjectWriter(Type), but helps simplify the generics
	public ObjectWriter(Class<T> t) {
		this.type = t;
		this.subject = null;
	}
	
	/**
	 * Supplies a function to unpack structured data directly into the target type.
	 * @param type
	 * @param data
	 * @return
	 */
	public static StructuredDataFunction<?> getObjectWriter(Type type, StructuredData data, @Nullable Object subject) {
		if (!data.type().isSemantic()) throw new IllegalArgumentException("Can't get objectWriter for "+data);
		
		Class<?> targetClass = ClassHierarchy.getErasedClass(type);
		if (Collection.class.isAssignableFrom(targetClass)) {
			Type elementType = ClassHierarchy.getCollectionTypeArgument(type);
			
			if (subject instanceof Collection coll) {
				return new CollectionFunction<>(coll, elementType);
			}
			
			if (targetClass.isInterface()) {
				// Pick a "typical" implementation of popular interfaces
				Collection<?> coll = null;
				if (Set.class.isAssignableFrom(targetClass)) {
					coll = new HashSet<>();
				} else if (List.class.isAssignableFrom(targetClass)) {
					coll = new ArrayList<>();
				} else if (Queue.class.isAssignableFrom(targetClass)) { // Includes Deque
					coll = new ArrayDeque<>();
				} else if (targetClass.equals(Collection.class)) {
					// Defined too broadly but we can deal with it
					coll = new ArrayList<>();
				} else {
					// We can't create an instance of this interface, and we can't make it 
					throw new IllegalArgumentException("Can't get an implementation for unknown collection interface \""+targetClass.getCanonicalName()+"\"");
				}
				
				return new CollectionFunction<>(coll, elementType);
			} else {
				// Attempt to create an instance using the target type's no-arg constructor - just
				// about every Collection type has one. If not, give a clear indication of the problem
				
				try {
					Constructor<?> cons = targetClass.getConstructor();
					boolean access = cons.canAccess(null);
					
					if (!access) cons.setAccessible(true);
					Collection<?> coll = (Collection<?>) cons.newInstance();
					if (!access) cons.setAccessible(false);
					
					return new CollectionFunction<>(coll, elementType);
				} catch (Throwable t) {
					throw new IllegalArgumentException("Could not create an instance of collection type, \""+type.getTypeName()+"\". Is there a zero-argument constructor?", t);
				}
			}
		}
		
		if (Map.class.isAssignableFrom(targetClass)) {
			if (subject instanceof Map map) {
				// Map<Object, Object> is incorrect, but we cannot construct MapFunction<?, ?>
				return new MapFunction<Object, Object>(type, (Map<Object, Object>) subject);
			}
			
			if (targetClass.isInterface()) {
				return new MapFunction<Object, Object>(type);
			} else {
				// Attempt to create an instance using the target type's no-arg constructor - as
				// with Collection, just about every Map type has one. If not, give a clear
				// indication of the problem
				try {
					Constructor<?> cons = targetClass.getConstructor();
					boolean access = cons.canAccess(null);
					
					if (!access) cons.setAccessible(true);
					Map<Object, Object> coll = (Map<Object, Object>) cons.newInstance();
					if (!access) cons.setAccessible(false);
					
					return new MapFunction<Object, Object>(type, coll);
				} catch (Throwable t) {
					throw new IllegalArgumentException("Could not create an instance of collection type, \""+type.getTypeName()+"\". Is there a zero-argument constructor?", t);
				}
			}
			
		}
		
		if (targetClass.isRecord()) {
			return new RecordFunction<>(targetClass);
		}
		
		if (targetClass.isArray()) {
			return new ArrayFunction<>(targetClass);
		}
		
		CheckedFunction<PrimitiveElement, Object, SyntaxError> selectedMapper = primitiveMappers.get(targetClass);
		if (selectedMapper != null) {
			return new StructuredDataFunction.Mapper<>(new PrimitiveFunction(), selectedMapper);
		}
		
		return new ObjectFunction<Object>(type);
	}
	
	/*
	public void analyzeTypeAndData(StructuredData data) {
		// Ignore non-semantic data because we don't yet have enough info to define a route from data to T
		if (!data.type().isSemantic()) return;
		
		// Classes are the most common Type
		if (type instanceof Class clazz) {
			if (clazz.isRecord()) {
				if (subject != null) throw new IllegalStateException("Cannot modify an existing instance of a record type.");
				
				delegate = new RecordFunction<>(clazz);
				// Records MUST be constructed from a constructor or static factory method.
				// There are three ways to proceed, in order from best to worst:
				// - Canonical constructor
				// - @Deserializer-marked constructor
				// - @Deserializer-marked static factory method
				
				// Most people won't notice if we REQUIRE an object and only use the canonical
				// constructor, but I think that's a bad call.
			}
			
			// Check the primitive type destinations
			Function<PrimitiveElement, Optional<Object>> selectedMapper = primitiveMappers.get(clazz);
			if (selectedMapper != null) {
				var f = new StructuredDataFunction.Mapper<>(new ToPrimitiveFunction(), selectedMapper);
				delegate = (StructuredDataFunction<Object>) (Object) f;
				return;
			}
		} else if (type instanceof ParameterizedType param) {
			// Lucky find! These come from reflection on object fields. We can safely(!!!) create a
			// new object of this type with its constructor, AND for collections and maps, recursively
			// resolve the type information required to unpack their contents.
			
		}
	}*/
	
	/*
	@SuppressWarnings("unchecked")
	private Optional<Constructor<T>> findConstructorFor(Class<T> clazz, StructuredData.Type inputType) {
		Set<Constructor<T>> constructors = new HashSet<>();
		
		// Prefer current class
		for(Constructor<?> c : clazz.getDeclaredConstructors()) {
			constructors.add((Constructor<T>) c);
		}
		
		
		return Optional.empty();
	}*/
	
	public void commitResult() throws IOException {
		if (subject != null || delegate == null || !delegate.isComplete()) return;
		
		if (subject == null) {
			subject = (T) delegate.getResult();
		}
		delegate = null;
		complete = true;
	}
	
	@Override
	public void write(StructuredData data) throws IOException {
		if (data.type() == StructuredData.Type.EOF) {
			commitResult();
			return;
		}
		
		try {
			if(delegate != null) {
				delegate.write(data);
				if (delegate.isComplete()) {
					commitResult();
				}
			} else {
				// We're in "root mode". Figure out what kind of delegate we need to spin up.
				// This depends *entirely* on the destination Type, but since we may have multiple
				// deserializer candidates for the destination type, the source type may matter too.
				
				//analyzeTypeAndData(data);
				if (data.type().isSemantic()) {
					StructuredDataFunction<?> function = getObjectWriter(type, data, subject);
					if (function != null) {
						delegate = (StructuredDataFunction<Object>) function;
						delegate.write(data);
					}
				}
			}
		} catch (SyntaxError err) {
			throw new IOException(err);
		}
	}
	
	/**
	 * Returns true if this ObjectWriter has consumed an entire value from the stream
	 * @return true if this ObjectWriter is complete
	 */
	public boolean isReady() {
		return complete;
	}
	
	public T toObject() {
		try {
			commitResult();
		} catch (IOException ex) {
			throw new RuntimeException("Couldn't commit object before EOF, and errored after", ex);
		}
		
		return subject;
	}
	
	//private static Map<Class<?>, Supplier<SingleValueFunction<?>>> classConfigs = new HashMap<>();
	private static Map<Class<?>, CheckedFunction<PrimitiveElement, Object, SyntaxError>> primitiveMappers = new HashMap<>();
	
	static {
		primitiveMappers.put(String.class, (prim) -> prim.asString().orElseThrow());
		
		primitiveMappers.put(Integer.class, (prim) -> prim.mapAsInt((it)-> it).orElseThrow(()->new SyntaxError("Required: Int")));
		primitiveMappers.put(Long.class,    (prim) -> prim.mapAsLong((it)-> it).orElseThrow(()->new SyntaxError("Required: Long")));
		primitiveMappers.put(Short.class,   (prim) -> prim.mapAsInt((it) -> (short) it).orElseThrow(()->new SyntaxError("Required: Short")));
		primitiveMappers.put(Byte.class,    (prim) -> prim.mapAsInt((it) -> (byte) it).orElseThrow(()->new SyntaxError("Required: Byte")));
		primitiveMappers.put(Double.class,  (prim) -> prim.mapAsDouble((it)-> it).orElseThrow(()->new SyntaxError("Required: Double")));
		primitiveMappers.put(Float.class,   (prim) -> prim.mapAsDouble((it) -> (float) it).orElseThrow(()->new SyntaxError("Required: Float")));
		primitiveMappers.put(Boolean.class, (prim) -> prim.mapAsBoolean((it) -> it).orElseThrow(()->new SyntaxError("Required: Boolean")));
		primitiveMappers.put(Integer.TYPE,  (prim) -> prim.mapAsInt((it)-> it).orElseThrow(()->new SyntaxError("Required: Int")));
		primitiveMappers.put(Long.TYPE,     (prim) -> prim.mapAsLong((it)-> it).orElseThrow(()->new SyntaxError("Required: Long")));
		primitiveMappers.put(Double.TYPE,   (prim) -> prim.mapAsDouble((it) -> it).orElseThrow(()->new SyntaxError("Required: Double")));
		primitiveMappers.put(Short.TYPE,    (prim) -> prim.mapAsInt((it) -> (short) it).orElseThrow(()->new SyntaxError("Required: Short")));
		primitiveMappers.put(Float.TYPE,    (prim) -> prim.mapAsDouble((it) -> (float) it).orElseThrow(()->new SyntaxError("Required: Float")));
		primitiveMappers.put(Boolean.TYPE,  (prim) -> prim.mapAsBoolean((it) -> it).orElseThrow(()->new SyntaxError("Required: Boolean")));
		
		// This one's complex because no good canonical serialization makes sense
		primitiveMappers.put(Character.class, (prim) -> {
			Optional<Object> value = prim.getValue();
			if (value.isEmpty()) return null;
			if (value.get() instanceof String s) {
				if (s.length() != 1) return null;
				return Character.valueOf(s.charAt(0));
			} else {
				return prim.mapAsInt((it) -> (char) it).orElseThrow(()->new SyntaxError("Required: Character"));
			}
		});
		
		// PrimitiveElement has convenience methods for these two, so let's set consistent expectations
		// It's truly unfortunate that Java can't tell that e.g. Optional<BigInteger> is castable to Optional<Object>
		primitiveMappers.put(BigInteger.class, (prim) -> (Optional<Object>) (Object) prim.asBigInteger().orElseThrow(()->new SyntaxError("Required: BigInteger")));
		primitiveMappers.put(BigDecimal.class, (prim) -> (Optional<Object>) (Object) prim.asBigDecimal().orElseThrow(()->new SyntaxError("Required: BigDecimal")));
		
		primitiveMappers.put(LocalDate.class, (prim) -> prim.mapAsString(LocalDate::parse).orElseThrow(() -> new SyntaxError("Required: LocalDate")));
		primitiveMappers.put(LocalTime.class, (prim) -> prim.mapAsString(LocalTime::parse).orElseThrow(() -> new SyntaxError("Required: LocalTime")));
		primitiveMappers.put(LocalDateTime.class, (prim) -> prim.mapAsString(LocalDateTime::parse).orElseThrow(() -> new SyntaxError("Required: LocalDateTime")));
	}
}
