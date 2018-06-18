/*
 * MIT License
 *
 * Copyright (c) 2018 Falkreon
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

package blue.endless.jankson;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Consumer;
import java.util.function.Function;

import blue.endless.jankson.impl.Marshaller;
import blue.endless.jankson.impl.ObjectParserContext;
import blue.endless.jankson.impl.ParserContext;
import blue.endless.jankson.impl.SyntaxError;


public class Jankson {
	private Deque<ParserFrame<?>> contextStack = new ArrayDeque<>();
	private JsonObject root;
	private int line = 0;
	private int column = 0;
	private int withheldCodePoint = -1;
	private Marshaller marshaller = new Marshaller();
	
	private int retries = 0;
	private SyntaxError delayedError = null;
	
	private Jankson(Builder builder) {}
	
	public JsonObject load(String s) throws SyntaxError {
		ByteArrayInputStream in = new ByteArrayInputStream(s.getBytes(Charset.forName("UTF-8")));
		try {
			return load(in);
		} catch (IOException ex) {
			throw new RuntimeException(ex); //ByteArrayInputStream never throws
		}
	}
	
	public JsonObject load(File f) throws IOException, SyntaxError {
		try(InputStream in = new FileInputStream(f)) {
			return load(in);
		}
	}
	
	public JsonObject load(InputStream in) throws IOException, SyntaxError {
		root = null;
		
		push(new ObjectParserContext(), (it)->{
			root = it;
		});
		
		while (root==null) {
			if (delayedError!=null) {
				throw delayedError;
			}
			
			if (withheldCodePoint!=-1) {
				retries++;
				if (retries>25) throw new IOException("Parser got stuck near line "+line+" column "+column);
				processCodePoint(withheldCodePoint);
			} else {
				int codePoint = in.read();
				if (codePoint==-1) {
					//Walk up the stack sending EOF to things until either an error occurs or the stack completes
					while(!contextStack.isEmpty()) {
						ParserFrame<?> frame = contextStack.pop();
						try {
							frame.context.eof();
						} catch (SyntaxError error) {
							error.setStartParsing(frame.startLine, frame.startCol);
							error.setEndParsing(line, column);
							throw error;
						}
					}
					if (root==null) root = new JsonObject();
					return root;
				}
				processCodePoint(codePoint);
			}
		}
		
		return root;
	}
	
	/** For now, this will only work if you've defined a type adapter for the indicated class! */
	public <T> T fromJson(JsonObject obj, Class<T> clazz) {
		return marshaller.marshall(clazz, obj);
	}
	
	/** For now, this will only work if you've defined a type adapter for the indicated class! */
	public <T> T fromJson(String json, Class<T> clazz) throws SyntaxError {
		JsonObject obj = load(json);
		return fromJson(obj, clazz);
	}
	
	private void processCodePoint(int codePoint) throws SyntaxError {
		ParserFrame<?> frame = contextStack.peek();
		if (frame==null) throw new IllegalStateException("Parser problem! The ParserContext stack underflowed! (line "+line+", col "+column+")");
		
		try {
			boolean consumed = frame.context.consume(codePoint, this);
			if (frame.context.isComplete()) {
				contextStack.pop();
				frame.supply();
			}
			if (consumed) {
				withheldCodePoint = -1;
				retries=0;
			} else {
				withheldCodePoint = codePoint;
			}
			
		} catch (SyntaxError error) {
			error.setStartParsing(frame.startLine, frame.startCol);
			error.setEndParsing(line, column);
			throw error;
		}
		
		column++;
		if (codePoint=='\n') {
			line++;
			column = 0;
		}
	}
	
	
	/** Pushes a context onto the stack. MAY ONLY BE CALLED BY THE ACTIVE CONTEXT */
	public <T> void push(ParserContext<T> t, Consumer<T> consumer) {
		ParserFrame<T> frame = new ParserFrame<T>(t, consumer);
		frame.startLine = line;
		frame.startCol = column;
		contextStack.push(frame);
	}
	
	public Marshaller getMarshaller() {
		return marshaller;
	}
	
	public static Builder builder() {
		return new Builder();
	}
	
	public static class Builder {
		Marshaller marshaller = new Marshaller();
		
		/**
		 * Registers a deserializer that can transform a JsonObject into an instance of the specified class. Please note
		 * that these type adapters are unsuitable for generic types, as these types are erased during jvm execution.
		 * @param clazz    The class to register deserialization for
		 * @param adapter  A function which can produce an object representing the supplied JsonObject
		 * @return This Builder for further modification.
		 */
		public <T> Builder registerTypeAdapter(Class<T> clazz, Function<JsonObject, T> adapter) {
			marshaller.registerTypeAdapter(clazz, adapter);
			return this;
		}
		
		/**
		 * Registers a marshaller for primitive types. Most built-in json and java types are already supported, but this
		 * allows one to change the deserialization behavior of Json primitives. Please note that these adapters are not
		 * suitable for generic types, as these types are erased during jvm execution.
		 * @param clazz
		 * @param adapter
		 * @return
		 */
		public <T> Builder registerPrimitiveTypeAdapter(Class<T> clazz, Function<Object, T> adapter) {
			marshaller.register(clazz, adapter);
			return this;
		}
		
		public Jankson build() {
			Jankson result = new Jankson(this);
			result.marshaller = marshaller;
			return result;
		}
	}
	
	private static class ParserFrame<T> {
		private ParserContext<T> context;
		private Consumer<T> consumer;
		private int startLine = 0;
		private int startCol = 0;
		
		public ParserFrame(ParserContext<T> context, Consumer<T> consumer) {
			this.context = context;
			this.consumer = consumer;
		}
		
		public ParserContext<T> context() { return context; }
		public Consumer<T> consumer() { return consumer; }
		
		/** Feed the result directly from the context at this entry to its corresponding consumer */
		public void supply() throws SyntaxError {
			consumer.accept(context.getResult());
		}
	}

	public void throwDelayed(SyntaxError syntaxError) {
		syntaxError.setEndParsing(line, column);
		delayedError = syntaxError;
	}
}
