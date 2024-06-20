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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import blue.endless.jankson.api.SyntaxError;
import blue.endless.jankson.api.io.ObjectWriter;
import blue.endless.jankson.api.io.StructuredData;
import blue.endless.jankson.impl.magic.ClassHierarchy;

public class MapFunction<K, V> extends SingleValueFunction<Map<K, V>> {
	
	private final Type keyType;
	private final Type valueType;
	private final Function<String, K> toKFunction;
	private final Map<K, V> result;
	private K bufferedKey = null;
	
	private boolean startFound = false;
	private boolean endFound = false;
	
	private StructuredDataFunction<V> delegate = null;
	
	public MapFunction(Type keyType, Type valueType) {
		this.keyType = keyType;
		this.valueType = valueType;
		this.result = new HashMap<K, V>();
		this.toKFunction = getKeyFunction(keyType);
		
	}
	
	public MapFunction(Type keyType, Type valueType, Map<K, V> map) {
		this.keyType = keyType;
		this.valueType = valueType;
		this.result = map;
		this.toKFunction = getKeyFunction(keyType);
	}
	
	public MapFunction(Type mapType) {
		this.result = new HashMap<>();
		var mapTypes = ClassHierarchy.getMapTypeArguments(mapType);
		this.keyType = mapTypes.keyType();
		this.valueType = mapTypes.valueType();
		this.toKFunction = getKeyFunction(keyType);
	}
	
	public MapFunction(Type mapType, Map<K, V> map) {
		this.result = map;
		var mapTypes = ClassHierarchy.getMapTypeArguments(mapType);
		this.keyType = mapTypes.keyType();
		this.valueType = mapTypes.valueType();
		this.toKFunction = getKeyFunction(keyType);
	}
	
	@SuppressWarnings("unchecked")
	private static <K> Function<String, K> getKeyFunction(Type keyType) throws IllegalArgumentException {
		if (keyType.equals(String.class)) return (it) -> (K) it;
		
		Class<K> keyClass = (Class<K>) ClassHierarchy.getErasedClass(keyType);
		try {
			Constructor<K> cons = keyClass.getConstructor(String.class);
			return (it) -> {
				try {
					return cons.newInstance(it);
				} catch (Throwable t) {
					throw new RuntimeException(t);
				}
			};
		} catch (Throwable t) {
			throw new IllegalArgumentException("Could not get an appropriate (String) constructor for objects of type " + keyType.getTypeName(), t);
		}
	}
	
	@Override
	public Map<K, V> getResult() {
		return result;
	}
	
	private void checkDelegate() {
		if (delegate == null) return;
		if (delegate.isComplete()) {
			result.put(bufferedKey, delegate.getResult());
			delegate = null;
			bufferedKey = null;
		}
	}
	
	@SuppressWarnings("unchecked")
	@Override
	protected void process(StructuredData data) throws SyntaxError {
		if (!startFound) {
			if (data.type() == StructuredData.Type.OBJECT_START) {
				startFound = true;
			} else {
				if (data.type().isSemantic()) throw new SyntaxError("Expected object-start, found "+data.type());
			}
		} else if (!endFound) {
			if (bufferedKey == null) {
				switch(data.type()) {
					case OBJECT_KEY -> {
						bufferedKey = toKFunction.apply(data.value().toString());
					}
					
					case OBJECT_END -> {
						endFound = true;
					}
					
					default -> {
						if (data.type().isSemantic()) {
							throw new SyntaxError("Expected object key or end of object, found "+data.type()+" instead.");
						}
					}
				}
			} else {
				switch(data.type()) {
					case EOF -> {
						throw new SyntaxError("Expected a value for key \"" + bufferedKey + "\". Found EOF instead!");
					}
					
					default -> {
						delegate = (StructuredDataFunction<V>) ObjectWriter.getObjectWriter(valueType, data, null);
						delegate.accept(data);
						checkDelegate();
					}
				}
			}
		} else {
			if (data.type().isSemantic() && data.type() != StructuredData.Type.EOF) {
				throw new SyntaxError("Data found past end of map.");
			}
		}
	}
	
}
