/*
 * MIT License
 *
 * Copyright (c) 2018-2022 Falkreon (Isaac Ellingson)
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

import java.io.Reader;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Consumer;

import blue.endless.jankson.api.document.DocumentElement;
import blue.endless.jankson.api.document.JanksonDocument;
import blue.endless.jankson.api.document.ObjectElement;
import blue.endless.jankson.impl.context.ParserContext;

public class JsonReader implements DocumentReader {
	private final Reader source;
	private final DeserializerOptions options;
	private Deque<ParserContext<?>> contextStack = new ArrayDeque<>();
	private ObjectElement documentRoot = new ObjectElement();
	
	public JsonReader(Reader source) {
		this(source, new DeserializerOptions());
	}
	
	public JsonReader(Reader source, DeserializerOptions options) {
		this.source = source;
		this.options = options;
	}
	
	@Override
	public JanksonDocument readDocument() {
		
		//TODO: Implement
		return null;
	}
	
	private void pushContext(ParserContext<?> ctx, Consumer<DocumentElement> consumer) {
		
	}
}
