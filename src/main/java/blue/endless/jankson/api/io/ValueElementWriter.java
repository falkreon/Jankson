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
import java.util.ArrayDeque;
import java.util.ArrayList;

import blue.endless.jankson.api.document.ArrayElement;
import blue.endless.jankson.api.document.CommentElement;
import blue.endless.jankson.api.document.CommentType;
import blue.endless.jankson.api.document.FormattingElement;
import blue.endless.jankson.api.document.NonValueElement;
import blue.endless.jankson.api.document.ObjectElement;
import blue.endless.jankson.api.document.PrimitiveElement;
import blue.endless.jankson.api.document.ValueElement;

/**
 * StructuredDataWriter that assembles a ValueElement. Much like StringWriter, this "captures" data
 * that would normally be serialized and assembles it into intermediate state.
 */
public class ValueElementWriter implements StructuredDataWriter {
	
	private ArrayList<NonValueElement> bufferedPrologue = new ArrayList<>();
	private String bufferedKey = null;
	private ArrayList<NonValueElement> interim = new ArrayList<>();
	private ArrayDeque<ValueElement> contextStack = new ArrayDeque<>();
	private ValueElement result = null;
	
	private void assertObject() throws IllegalStateException {
		if (!(peekContext() instanceof ObjectElement)) throw new IllegalStateException("This operation requires the writer to be writing an Object.");
	}
	
	private ArrayElement assertArray() throws IllegalStateException {
		ValueElement v = peekContext();
		if (v instanceof ArrayElement arr) return arr;
		throw new IllegalStateException("This operation requires the writer to be writing an Array.");
	}
	
	private void assertObjectOrArray() throws IllegalStateException {
		ValueElement context = peekContext();
		if (context instanceof ObjectElement || context instanceof ArrayElement) return;
		throw new IllegalStateException("This operation requires the writer to be writing an Object or an Array.");
	}
	
	private ValueElement peekContext() {
		return contextStack.isEmpty() ? null : contextStack.getLast();
	}
	
	private ValueElement popContext() {
		return contextStack.isEmpty() ? null : contextStack.removeLast();
	}
	
	/**
	 * Pop and return the element. If the context stack is empty, or the top element on the stack is not the expected
	 * class, throws.
	 * @param <T>
	 * @param clazz
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private <T extends ValueElement> T popContext(Class<T> clazz) throws IllegalStateException {
		if (contextStack.isEmpty()) throw new IllegalStateException("Expected "+clazz.getSimpleName()+" but found nothing!");
		ValueElement v = contextStack.removeLast();
		if (clazz.isAssignableFrom(v.getClass())) {
			return (T) v;
		} else {
			throw new IllegalStateException("Expected '"+clazz.getSimpleName()+"' but found '"+v.getClass().getSimpleName()+"'!");
		}
	}
	
	private <T extends ValueElement> T pushContext(T elem) {
		contextStack.addLast(elem);
		return elem;
	}
	
	@Override
	public void write(StructuredData data) throws IOException {
		switch(data.type()) {
		case ARRAY_END -> {
			ArrayElement v = assertArray();
			popContext();
			
		}
			
		/*
		case ARRAY_START:
			break;
		case COMMENT:
			break;
		case EOF:
			break;
		case NEWLINE:
			break;
		case OBJECT_END:
			break;
		case OBJECT_KEY:
			break;
		case OBJECT_START:
			break;
		case PRIMITIVE:
			break;
		case WHITESPACE:
			break;
		default:
			break;
		*/
		}
		
	}
	
	public ValueElement toValueElement() {
		return PrimitiveElement.ofNull();
	}
}
