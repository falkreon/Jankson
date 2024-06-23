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

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import blue.endless.jankson.api.SyntaxError;
import blue.endless.jankson.api.io.StructuredData;

public class ObjectFunction<T> extends SingleValueFunction<T>{
	
	private Class<T> clazz;
	private boolean foundStart = false;
	private boolean foundEnd = false;
	private T result = null;
	private Map<String, Object> values = new HashMap<>();
	private Map<String, String> serializedNameToFieldName = new HashMap<>();
	private Set<String> requiredValues = new HashSet<>();
	private String delegateKey = null;
	private StructuredDataFunction<Object> delegate = null;
	
	public ObjectFunction(Type t) {
		
	}
	
	public ObjectFunction(Class<T> clazz) {
		this.clazz = clazz;
		configureForType();
	}
	
	private void configureForType() {
		
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
			// Create a new object
			/*
			HashSet<Field> fields = new HashSet<>();
			fields.addAll(fields)
			
			
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
			*/
		}
	}
	
	@Override
	public T getResult() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected void process(StructuredData data) throws SyntaxError {
		// TODO Auto-generated method stub
		
	}

}
