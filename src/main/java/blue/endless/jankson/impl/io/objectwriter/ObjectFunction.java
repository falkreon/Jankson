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

import java.io.IOException;
import java.lang.reflect.Type;

import blue.endless.jankson.api.SyntaxError;
import blue.endless.jankson.api.io.ObjectWriter;
import blue.endless.jankson.api.io.StructuredData;
import blue.endless.jankson.api.io.StructuredDataFunction;
import blue.endless.jankson.impl.io.objectwriter.factory.ObjectWrapper;

public class ObjectFunction<T> extends SingleValueFunction<T>{
	
	private final Type tType;
	private boolean foundStart = false;
	private boolean foundEnd = false;
	private String delegateKey = null;
	private StructuredDataFunction<Object> delegate = null;
	
	private ObjectWrapper<T> wrapper;
	
	public ObjectFunction(Type t) {
		tType = t;
		wrapper = ObjectWrapper.of(t, null);
	}
	
	public ObjectFunction(Class<T> clazz) {
		tType = clazz;
		wrapper = ObjectWrapper.of(clazz, null);
	}
	
	public ObjectFunction(Type t, T result) {
		tType = t;
		wrapper = ObjectWrapper.of(t, result);
	}
	
	private void checkDelegate() throws SyntaxError {
		if (delegate != null && delegate.isComplete() && delegateKey != null) {
				try {
					Object o = delegate.getResult();
					wrapper.setField(delegateKey, o);
				} catch (ReflectiveOperationException e) {
					throw new SyntaxError("Could not write to field \""+delegateKey+"\"", e);
				} finally {
					delegateKey = null;
					delegate = null;
				}
		}
	}
	
	@Override
	public T getResult() {
		try {
			return wrapper.getResult();
		} catch (InstantiationException ex) {
			throw new RuntimeException(ex);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void process(StructuredData data) throws SyntaxError, IOException {
		
		if (delegate != null) {
			delegate.write(data);
			checkDelegate();
			return;
		}
		
		if (!foundStart) {
			if (data.type() == StructuredData.Type.OBJECT_START) {
				foundStart = true;
			} else {
				if (data.type().isSemantic()) throw new SyntaxError("Expected object-start when unpacking an object of type "+tType.getTypeName()+", found "+data.type().name());
			}
		} else if (!foundEnd) {
			
			switch(data.type()) {
				case EOF -> {
					if (delegateKey == null) {
						throw new SyntaxError("Found end of file while looking for the next key or the end of an object.");
					} else {
						throw new SyntaxError("Missing value for key \""+delegateKey+"\" (found EOF)");
					}
				}
				
				case OBJECT_KEY -> {
					if (delegateKey != null) throw new SyntaxError(
							"Missing value for key \""+delegateKey+
							"\" - we seem to have skipped ahead to \""+data.value().toString()+"\".");
					
					delegateKey = data.value().toString();
				}
				
				case OBJECT_END -> {
					foundEnd = true;
					if (delegateKey != null) throw new SyntaxError("Missing value for key \""+delegateKey+"\" (found end of object)");
				}
				
				default -> {
					if (!data.type().isSemantic()) return;
					
					Type fieldType = wrapper.getType(delegateKey);
					if (fieldType == null) {
						delegate = SingleValueFunction.discard();
						delegate.write(data);
						checkDelegate();
						return;
					}
					
					// TODO: Hand over the instance to start with
					delegate = (StructuredDataFunction<Object>) ObjectWriter.getObjectWriter(fieldType, data, null);
					if (delegate != null) {
						delegate.write(data);
						checkDelegate();
					} else {
						throw new IllegalStateException("Oops");
					}
				}
			}
			
		} else {
			if (data.type() != StructuredData.Type.EOF && data.type().isSemantic()) {
				throw new SyntaxError("Found additional data past the end of an object while unpacking an object type");
			}
		}
	}

}
