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

import javax.annotation.Nullable;

import blue.endless.jankson.api.document.CommentElement;
import blue.endless.jankson.api.document.CommentType;
import blue.endless.jankson.api.document.PrimitiveElement;

/**
 * StructuredData are data particles representing a pre-order traversal of a structured data tree. If arranged in a grammatically
 * correct order, these data particles can be easily used to produce a fully-inflated ValueElement OR a flat character data
 * representation. This is the intermediate representation for all information in the system.
 * 
 * <p>IN GENERAL, when we talk about "Structured Data" in this codebase, we're talking about "tree-like" data. This data has no
 * (non-transient) reference loops, and is organized using objects (which java thinks of as Maps) and arrays (which java thinks
 * of as Collections).
 */
public record StructuredData(Type type, @Nullable Object value) {
	
	public static StructuredData primitive(PrimitiveElement value) {
		if (value.isNull()) return NULL;
		return new StructuredData(Type.PRIMITIVE, value.getValue().get());
	}
	
	public static StructuredData primitive(Object value) {
		Object sanitized = PrimitiveElement.box(value).getValue().get();
		return new StructuredData(Type.PRIMITIVE, sanitized);
	}
	
	public static StructuredData objectKey(String name) {
		return new StructuredData(Type.OBJECT_KEY, name);
	}
	
	public static StructuredData comment(String comment, CommentType value) {
		return new StructuredData(Type.COMMENT, new CommentElement(comment, value));
	}
	
	public static StructuredData whitespace(String value) {
		return new StructuredData(Type.WHITESPACE, value);
	}
	
	public static final StructuredData ARRAY_START  = new StructuredData(Type.ARRAY_START, null);
	public static final StructuredData ARRAY_END    = new StructuredData(Type.ARRAY_END, null);
	public static final StructuredData OBJECT_START = new StructuredData(Type.OBJECT_START, null);
	public static final StructuredData OBJECT_END   = new StructuredData(Type.OBJECT_END, null);
	public static final StructuredData NEWLINE      = new StructuredData(Type.NEWLINE, null);
	public static final StructuredData EOF          = new StructuredData(Type.EOF, null);
	public static final StructuredData NULL         = new StructuredData(Type.PRIMITIVE, null);
	
	
	public boolean isPrimitive() {
		return type == Type.PRIMITIVE;
	}
	
	public PrimitiveElement asPrimitive() {
		if (type != Type.PRIMITIVE) throw new IllegalStateException();
		return PrimitiveElement.box(value);
	}
	
	public boolean isComment() {
		return type == Type.COMMENT;
	}
	
	public CommentElement asComment() {
		if (type != Type.COMMENT) throw new IllegalStateException();
		return (value instanceof CommentElement comment) ? comment : new CommentElement(value.toString(), CommentType.MULTILINE);
	}
	
	
	public static enum Type {
		/**
		 * A PrimitiveElement. StructuredData of this type describes the entire value - a null value indicates the 'null' literal.
		 */
		PRIMITIVE(true, true),
		
		/**
		 * Begins an ArrayElement. Anything between this and the corresponding ARRAY_END is contained within the array.
		 */
		ARRAY_START(true, false),
		
		/**
		 * Ends an ArrayElement. Any StructuredData that follows is not contained within the array.
		 */
		ARRAY_END(true, false),
		
		/**
		 * Begins an ObjectElement. Anything between this and the corresponding OBJECT_END is contained within the object.
		 */
		OBJECT_START(true, false),
		
		/**
		 * Ends an ObjectElement. Any StructuredData that follows is not contained within the object.
		 */
		OBJECT_END(true, false),
		
		/**
		 * Within an ObjectElement, represents the key in a key-value pair. The associated value MUST be a String containing the key name.
		 */
		OBJECT_KEY(true, true),
		
		/**
		 * Represents a CommentElement. The associated value can be EITHER a String for an unspecified comment type, or an entire CommentElement.
		 */
		COMMENT(false, true),
		
		/**
		 * Represents whitespace that is NOT regular indentation. The associated value will be a String consisting of whitespace characters.
		 */
		WHITESPACE(false, true),
		
		/**
		 * Represents a FormattingElement.LINE_BREAK
		 */
		NEWLINE(false, false),
		
		/**
		 * Represents the end of the file. After writing an EOF, programs SHOULD avoid writing further data, and Writers SHOULD discard any
		 * additional data written after an EOF. After reading an EOF, programs SHOULD avoid reading further data, and Readers SHOULD continue
		 * to supply EOF for every subsequent read.
		 */
		EOF(true, false);
		
		private final boolean semantic;
		private final boolean hasValue;
		
		Type(boolean semantic, boolean hasValue) {
			this.semantic = semantic;
			this.hasValue = hasValue;
		}
		
		/**
		 * Returns true if this type is required to unpack a ValueElement. If false, StructuredData of this type can be ignored
		 * (it will be related to comments or whitespace)
		 * @return true if this StructuredData.Type is semantic data.
		 */
		public boolean isSemantic() { return this.semantic; }
		
		/**
		 * Can a meaningful value object be associated with StructuredData of this type? If this returns false, we can assume
		 * said object will be null.
		 * @return true if this StrucutredData.Type can be (and usually is) presented with an associated value object
		 */
		public boolean hasValue() { return this.hasValue; }
	}
}
