/*
 * MIT License
 *
 * Copyright (c) 2018-2023 Falkreon (Isaac Ellingson)
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

package blue.endless.jankson.api.document;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import blue.endless.jankson.api.SyntaxError;
import blue.endless.jankson.api.io.ElementType;
import blue.endless.jankson.api.io.StructuredDataReader;

public class DocumentBuilder {
	public static ValueElement build(StructuredDataReader reader) throws IOException, SyntaxError {
		List<NonValueElement> prologue = new ArrayList<>();
		List<NonValueElement> epilogue = new ArrayList<>();
		ValueElement elem = null;
		
		out:
		while(reader.hasNext()) {
			ElementType elemType = reader.next();
			
			switch(elemType) {
			case COMMENT:
				if (reader.getLatestValue() instanceof CommentElement comment) {
					if (elem == null) {
						prologue.add(comment);
					} else {
						epilogue.add(comment);
					}
				}
				break;
			
			case NEWLINE:
				prologue.add(FormattingElement.LINE_BREAK);
				break;
				
			case WHITESPACE:
				//TODO: Probably eliminate this enum entry
				break;
				
			case PRIMITIVE:
				if (elem != null) throw new IllegalStateException("Multiple values returned in a root element.");
				
				elem = PrimitiveElement.box(reader.getLatestValue());
				break;
				
			case OBJECT_START:
				if (elem != null) throw new IllegalStateException("Multiple values returned in a root element.");
				
				elem = buildObjectInternal(reader);
				break;
			
			case ARRAY_START:
				if (elem != null) throw new IllegalStateException("Multiple values returned in a root element.");
				
				elem = buildArrayInternal(reader);
				break;
				
			case OBJECT_KEY:
			case OBJECT_END:
			case ARRAY_END:
				throw new IllegalStateException("Invalid message found building a DocumentElement at the root context. ("+elemType+")");
			
			case EOF:
				break out;
			
			default:
				throw new IllegalStateException("Unknown message ("+elemType+")");
			}
		}
		
		if (elem==null) return PrimitiveElement.ofNull();
		
		for(NonValueElement e : prologue) {
			elem.getPreamble().add(e);
		}
		
		for(NonValueElement e : epilogue) {
			elem.getEpilogue().add(e);
		}
		
		return elem;
	}
	
	//Assume the START message has been consumed
	private static ObjectElement buildObjectInternal(StructuredDataReader reader) throws IOException, SyntaxError {
		List<NonValueElement> kvPrologue = new ArrayList<>();
		List<NonValueElement> kvI = new ArrayList<>();
		ObjectElement result = new ObjectElement();
		String key = null;
		
		while(true) {
			//Grab the key
			while(true) {
				ElementType elemType = reader.next();
				//System.out.println("Scanning for key: "+elemType);
				if (elemType==ElementType.EOF) throw new IOException("Encountered EOF before object ended");
				if (elemType==ElementType.COMMENT) {
					if (reader.getLatestValue() instanceof CommentElement comment) {
						if (key==null) {
							kvPrologue.add(comment);
						} else {
							kvI.add(comment);
						}
					}
				}
				if (elemType==ElementType.OBJECT_KEY) {
					key = Objects.toString(reader.getLatestValue());
					break;
				}
				
				if (elemType==ElementType.OBJECT_END) {
					return result;
				}
			}
			
			//Grab the value
			while(true) {
				ElementType elemType = reader.next();
				//System.out.println("Scanning for value: "+elemType);
				if (elemType==ElementType.OBJECT_END) throw new IOException("Encountered end of object between a key and a value.");
				if (elemType==ElementType.PRIMITIVE) {
					ValueElement elem = PrimitiveElement.box(reader.getLatestValue());
					KeyValuePairElement kv = new KeyValuePairElement(key, elem);
					key = null;
					for(NonValueElement e : kvPrologue) kv.preamble.add(e);
					for(NonValueElement e : kvI) kv.intermission.add(e);
					kvPrologue.clear();
					kvI.clear();
					result.add(kv);
					break;
				}
				if (elemType==ElementType.OBJECT_START) {
					ObjectElement elem = buildObjectInternal(reader);
					KeyValuePairElement kv = new KeyValuePairElement(key, elem);
					key = null;
					for(NonValueElement e : kvPrologue) kv.preamble.add(e);
					for(NonValueElement e : kvI) kv.intermission.add(e);
					kvPrologue.clear();
					kvI.clear();
					result.add(kv);
					break;
				}
				if (elemType==ElementType.ARRAY_START) {
					ArrayElement elem = buildArrayInternal(reader);
					KeyValuePairElement kv = new KeyValuePairElement(key, elem);
					key = null;
					for(NonValueElement e : kvPrologue) kv.preamble.add(e);
					for(NonValueElement e : kvI) kv.intermission.add(e);
					kvPrologue.clear();
					kvI.clear();
					result.add(kv);
					break;
				}
				if (elemType==ElementType.EOF) {
					throw new IOException("Encountered EOF between a key and a value.");
				}
			}
		}
	}
	
	//Assume the START message has been consumed
	private static ArrayElement buildArrayInternal(StructuredDataReader reader) throws IOException, SyntaxError {
		ArrayElement array = new ArrayElement();
		List<NonValueElement> prologue = new ArrayList<>();
		
		out:
		while(true) {
			ElementType elementType = reader.next();
			switch(elementType) {
			case ARRAY_END:
				break out;
				
			case PRIMITIVE:
				PrimitiveElement elem = PrimitiveElement.box(reader.getLatestValue());
				for(NonValueElement e : prologue) elem.preamble.add(e);
				prologue.clear();
				array.add(elem);
				break;
			
			case COMMENT:
				if (reader.getLatestValue() instanceof CommentElement comment) {
					prologue.add(comment);
				}
				break;
			
			case OBJECT_START:
				ObjectElement objectElem = buildObjectInternal(reader);
				for(NonValueElement e : prologue) objectElem.preamble.add(e);
				prologue.clear();
				array.add(objectElem);
				break;
			
			case ARRAY_START:
				ArrayElement arrayElem = buildArrayInternal(reader);
				for(NonValueElement e : prologue) arrayElem.preamble.add(e);
				prologue.clear();
				array.add(arrayElem);
				break;
			
			default:
				throw new IllegalStateException("Illegal state: "+elementType);
			}
		}
		
		if (!prologue.isEmpty()) {
			//We have comments after the last array element, but within the array. Tack them onto the array footer
			for(NonValueElement e : prologue) array.getFooter().add(e);
		}
		
		return array;
	}
}
