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

package blue.endless.jankson.impl.io.objectwriter;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import blue.endless.jankson.api.SyntaxError;
import blue.endless.jankson.api.io.ObjectWriter;
import blue.endless.jankson.api.io.AbstractDeserializer;
import blue.endless.jankson.api.io.StructuredData;
import blue.endless.jankson.api.io.Deserializer;
import blue.endless.jankson.impl.magic.ClassHierarchy;
import blue.endless.jankson.impl.magic.ReflectiveProperty;

public class RecordDeserializer<T> extends AbstractDeserializer<T> {
	private final Class<T> clazz;
	private boolean foundStart = false;
	private boolean foundEnd = false;
	private T result = null;
	private final Map<String, Object> values = new HashMap<>();
	private final Map<String, ReflectiveProperty> properties = new HashMap<>();
	private final Set<String> requiredValues = new LinkedHashSet<>();
	private String delegateKey = null;
	private Deserializer<Object> delegate = null;
	
	@SuppressWarnings("unchecked")
	public RecordDeserializer(Type type) {
		this.clazz = (Class<T>) ClassHierarchy.getErasedClass(type);
		List<ReflectiveProperty> metadata = ReflectiveProperty.of(type);
		for (ReflectiveProperty property : metadata) {
			requiredValues.add(property.javaName());
			properties.put(property.wireName(), property);
		}
	}

	public RecordDeserializer(Class<T> type) {
		this((Type) type);
	}
	
	@Override
	public T getResult() {
		return result;
	}
	
	/**
	 * Finds the canonical constructor using the record components in declaration order.
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
			ReflectiveProperty property = properties.get(delegateKey);
			if (property != null) {
				if (values.containsKey(property.javaName())) {
					throw new SyntaxError("Duplicate record component '"+property.wireName()+"'");
				}
				values.put(property.javaName(), delegate.getResult());
				requiredValues.remove(property.javaName());
			}
			
			delegate = null;
			delegateKey = null;
		}
	}

	private void instantiate() throws SyntaxError {
		RecordComponent[] components = clazz.getRecordComponents();
		Object[] args = new Object[components.length];
		for(int i = 0; i<components.length; i++) {
			args[i] = values.get(components[i].getName());
		}
		try {
			Constructor<T> c = getCanonicalConstructor();
			boolean accessible = c.canAccess(null);
			if (!accessible) c.setAccessible(true);
			try {
				result = c.newInstance(args);
			} finally {
				if (!accessible) c.setAccessible(false);
			}
		} catch (ReflectiveOperationException | IllegalArgumentException t) {
			throw new SyntaxError("Could not create record of type '"+clazz.getSimpleName()+"'.", t);
		}
	}
	
	@SuppressWarnings("unchecked")
	@Override
	protected void process(StructuredData data) throws SyntaxError, IOException {
		checkDelegate();
		if(delegate != null && !delegate.isComplete()) {
			delegate.write(data);
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
				if (!requiredValues.isEmpty()) throw new SyntaxError("Missing required record component(s) for "
						+clazz.getTypeName()+": "+String.join(", ", requiredValues));
				instantiate();
				foundEnd = true;
			} else if (data.type() == StructuredData.Type.OBJECT_KEY) {
				if (delegateKey != null) throw new SyntaxError("Got two keys in a row while unpacking a record type. The value is missing! (keys: "+delegateKey+", "+data.value().toString()+")");
				delegateKey = data.value().toString();
			} else {
				ReflectiveProperty property = properties.get(delegateKey);
				if (property == null) {
					delegate = AbstractDeserializer.discard();
					delegate.write(data);
					checkDelegate();
					return;
				}
				
				delegate = (Deserializer<Object>) ObjectWriter.getObjectWriter(property.type(), data, null);
				delegate.write(data);
				checkDelegate();
			}
		} else {
			if (data.type() != StructuredData.Type.EOF && data.type().isSemantic()) {
				throw new SyntaxError("Found additional data past the end of an object while unpacking a record type");
			}
		}
	}
	
}
