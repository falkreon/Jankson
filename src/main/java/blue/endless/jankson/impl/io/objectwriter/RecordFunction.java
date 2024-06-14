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

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Constructor;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import blue.endless.jankson.api.SyntaxError;
import blue.endless.jankson.api.annotation.SerializedName;
import blue.endless.jankson.api.io.ObjectWriter;
import blue.endless.jankson.api.io.StructuredData;

public class RecordFunction<T> extends SingleValueFunction<T> {
	private Class<T> clazz;
	private boolean foundStart = false;
	private boolean foundEnd = false;
	private T result = null;
	private Map<String, Object> values = new HashMap<>();
	private Map<String, String> serializedNameToFieldName = new HashMap<>();
	private Set<String> requiredValues = new HashSet<>();
	private String delegateKey = null;
	private StructuredDataFunction<Object> delegate = null;
	
	public RecordFunction(Class<T> clazz) {
		this.clazz = clazz;
		for (RecordComponent c : clazz.getRecordComponents()) {
			requiredValues.add(c.getName());
			
			AnnotatedType annoType = c.getAnnotatedType();
			SerializedName altName = annoType.getAnnotation(SerializedName.class);
			
			serializedNameToFieldName.put(
					(altName != null) ? altName.value() : c.getName(),
					c.getName()
				);
		}
	}
	
	@Override
	public T getResult() {
		return result;
	}
	
	/**
	 * This is a hack and a half. There is no "correct" way to access a canonical constructor. By
	 * convention, it appears last in the bytecode and reflection, but this is not guaranteed.
	 * Luckily, we ARE guaranteed that record components (inside the parentheses in the record
	 * declaration) are in the order they're declared in, which is also guaranteed to be the order
	 * they appear in, in the canonical constructor.
	 * 
	 * <p>Note that we use getType here. If we attempt to call clazz.getDeclaredConstructor with an
	 * array of AnnotatedType or GenericType, this method will fail. Generic types are erased in
	 * constructors (as with any method), even though they aren't in fields / record components.
	 * 
	 * @see <a href="https://stackoverflow.com/questions/67126109/is-there-a-way-to-recognise-a-java-16-records-canonical-constructor-via-reflect#comment118694512_67126661">
	 *      https://stackoverflow.com/questions/67126109/is-there-a-way-to-recognise-a-java-16-records-canonical-constructor-via-reflect#comment118694512_67126661</a>
	 * @return The canonical constructor for this record.
	 * @throws NoSuchMethodException if no canonical constructor can be found
	 * @throws SecurityException if we are unable to acquire the canonical constructor for security reasons
	 */
	private Constructor<T> getCanonicalConstructor() throws NoSuchMethodException, SecurityException {
		RecordComponent[] components = clazz.getRecordComponents();
		Class<?>[] componentTypes = new Class<?>[components.length];
		for(int i=0; i<components.length; i++) {
			componentTypes[i] = components[i].getType();
		}
		
		return clazz.getDeclaredConstructor(componentTypes);
	}
	
	private void checkDelegate() throws SyntaxError {
		if (delegate != null && delegate.isComplete()) {
			Object o = delegate.getResult();
			String fieldName = serializedNameToFieldName.get(delegateKey);
			if (fieldName != null) {
				values.put(fieldName, delegate.getResult());
				requiredValues.remove(fieldName);
			}
			
			delegate = null;
			delegateKey = null;
		}
		
		if (result == null && requiredValues.isEmpty()) {
			RecordComponent[] components = clazz.getRecordComponents();
			
			Class<?>[] types = new Class<?>[components.length];
			Object[] args = new Object[components.length];
			for(int i = 0; i<components.length; i++) {
				RecordComponent component = components[i];
				types[i] = component.getType();
				args[i] = values.get(component.getName());
			}
			try {
				Constructor<T> c = getCanonicalConstructor();
				boolean accessible = c.canAccess(null);
				if (!accessible) c.setAccessible(true);
				result = c.newInstance(args);
				if (!accessible) c.setAccessible(false);
			} catch (Throwable t) {
				throw new SyntaxError("Could not create record of type '"+clazz.getSimpleName()+"'.", t);
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	@Override
	protected void process(StructuredData data) throws SyntaxError {
		checkDelegate();
		if(delegate != null && !delegate.isComplete()) {
			delegate.accept(data);
			checkDelegate();
			return;
		}
		
		if (!foundStart) {
			if (data.type() == StructuredData.Type.OBJECT_START) {
				foundStart = true;
			} else {
				throw new SyntaxError("Expected object-start when unpacking a record type, found "+data.type().name());
			}
		} else if (!foundEnd) {
			//Three things are valid here: Key, Object-End, and the start of a value type.
			if (!data.type().isSemantic()) return;
			if (data.type() == StructuredData.Type.EOF) throw new SyntaxError("Missing object-end when unpacking a record type. Found EOF instead.");
			
			if (data.type() == StructuredData.Type.OBJECT_END) {
				if (delegateKey != null) throw new SyntaxError("Got a key with no value while unpacking a record type");
				foundEnd = true;
			} else if (data.type() == StructuredData.Type.OBJECT_KEY) {
				if (delegateKey != null) throw new SyntaxError("Got two keys in a row while unpacking a record type. The value is missing! (keys: "+delegateKey+", "+data.value().toString()+")");
				delegateKey = data.value().toString();
			} else {
				String fieldName = serializedNameToFieldName.get(delegateKey);
				if (fieldName == null) {
					delegate = SingleValueFunction.discard();
					delegate.accept(data);
					checkDelegate();
					return;
				}
				
				Type fieldType = null;
				for(RecordComponent comp : clazz.getRecordComponents()) {
					if (comp.getName().equals(fieldName)) {
						fieldType = comp.getGenericType();
					}
				}

				if (fieldType != null) {
					
					delegate = (StructuredDataFunction<Object>) ObjectWriter.getObjectWriter(fieldType, data, null);
					if (delegate != null) {
						delegate.accept(data);
					}
				} else {
					// This key doesn't correspond to anything recognizeable in the record.
					delegate = SingleValueFunction.discard();
					delegate.accept(data);
				}
			}
		} else {
			if (data.type() != StructuredData.Type.EOF && data.type().isSemantic()) {
				throw new SyntaxError("Found additional data past the end of an object while unpacking a record type");
			}
		}
	}
	
}
