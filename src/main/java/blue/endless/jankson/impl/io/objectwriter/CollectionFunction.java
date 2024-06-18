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
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;

import blue.endless.jankson.api.SyntaxError;
import blue.endless.jankson.api.io.StructuredData;

public class CollectionFunction<V, T extends Collection<V>> extends SingleValueFunction<Collection<V>>{
	
	private Collection<V> result;
	
	public CollectionFunction(Collection<V> result) {
		this.result = result;
	}
	
	public CollectionFunction(Type resultType) {
		if (resultType instanceof AnnotatedType anno) {
			resultType = anno.getType(); // Discard the annotations and extract the class or generic type
		}
		
		if (resultType instanceof ParameterizedType generic) {
			Type[] typeArgs = generic.getActualTypeArguments();
			Type genericClass = generic.getRawType();
			if (genericClass instanceof Class clazz) {
				
			}
		}
		//if (resultType instanceof GenericClass generic) {
			
		//}
		
	}
	
	@Override
	public Collection<V> getResult() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected void process(StructuredData data) throws SyntaxError {
		// TODO Auto-generated method stub
		
	}

}
