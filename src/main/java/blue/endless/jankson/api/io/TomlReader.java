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
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import blue.endless.jankson.api.SyntaxError;
import blue.endless.jankson.api.document.ArrayElement;
import blue.endless.jankson.api.document.CommentElement;
import blue.endless.jankson.api.document.ObjectElement;
import blue.endless.jankson.api.document.PrimitiveElement;
import blue.endless.jankson.api.document.ValueElement;
import blue.endless.jankson.impl.io.AbstractStructuredDataReader;
import blue.endless.jankson.impl.io.context.BooleanValueParser;
import blue.endless.jankson.impl.io.context.CommentValueParser;
import blue.endless.jankson.impl.io.context.NumberValueParser;
import blue.endless.jankson.impl.io.context.StringValueParser;
import blue.endless.jankson.impl.io.context.TomlTripleQuotedStringValueParser;

public class TomlReader extends AbstractStructuredDataReader {
	
	
	/**
	 * Unfortunately, because of the unpredictable order here, we need to crystallize the config down into an object before we can emit anything.
	 */
	private ObjectElement result = new ObjectElement();
	private List<String> bufferedKey = new ArrayList<>();
	
	private List<String> tableContext = new ArrayList<>();
	//private boolean contextIsArray = false;
	private ObjectElement contextObj = result;
	
	private boolean hasReadInData = false;
	
	/*
	 * This is a BRIEF overview of TOML's BNF.
	 * The full ABNF is available at https://github.com/toml-lang/toml/blob/1.0.0/toml.abnf
	 * 
	 * 
	 * 
	 * 
	 * A toml file is: expression *(newline expression) ; that is, we expect expressions separated by newlines.
	 * 
	 * An expression is: comment, keyval, or table
	 * 
	 *   A comment opens with an octothorpe and continues until the end of the line
	 * 
	 *   A keyval is: key = val, ignoring any whitespace around the equals
	 *     Two kinds of keys: simple or dotted
	 *     A "simple" key is a quoted or unquoted key. Keys may be unquoted if they consist only of A-Z / a-z / 0-9 / - / _ 
	 *     A "dotted" key is simple keys separated by one or more dots. Once again, we skip any whitespace around the dots.
	 *
	 *   A table is either a [key] "std-table" or a [[key]] "array-table".
	 *     whitespace is skipped inside the braces (around the key).
	 *     key is as above: either a simple or dotted key, and is used for the same purpose, to define the key of the table being defined
	 *   
	 *   A val is either: string, boolean, array, inline-table, date-time, float, or integer
	 *   
	 *   	strings can be: basic-string, basic-multiline-string, literal-string, or literal-multiline-string
	 *   		basic means they're surrounded by double-quotes and escapes are interpreted
	 *   		literal means they're surrounded by single quotes and escapes are ignored
	 *   		multiline uses three of the string delimiter (single or double quote), followed by an optional
	 *            newline, string body (possibly containing newlines), and then the closing delimiter.
	 *      
	 *      booleans are the literals true and false, as lowercase keywords
	 *      
	 *      arrays are an opening square brace, followed by one or more values separated by commas, and then a closing square brace.
	 *          newlines and comments can occur in the array
	 *          there is no limit to values nested in the array: you can nest other arrays and inline-tables just like anything else
	 *          
	 *      inline-table is basically a java object. We open with a curly brace, then have zero or more keyval entries separated by commas, then a closing brace.
	 *        whitespaces inside the braces are ignored, as are whitespaces around the commas
	 *        newlines and comments are NOT allowed inside the table, but are allowed after the closing brace.
	 *        further inline-tables and arrays may be nested, as long as they do not create a line break inside the inline-table.
	 *      
	 *      date-time is either: offset-date-time, local-date-time, local-date, or local-time
	 *        these are all standard RFC 3339 formats (2:30 AM on the release date of Disintegration by The Cure used in all cases for comparison):
	 *          1989-05-02T02:30:00Z (offset date-time)
	 *          1989-05-02 02:30:00Z
	 *          
	 *          1989-05-02T02:30:00  (local date-time (note the missing Z))
	 *          
	 *          1989-05-02           (local-date)
	 *          
	 *          02:30:00             (local-time)
	 *          
	 *        Jankson will turn all of these into strings
	 *          
	 *      floats are straightforward IEEE 754 representations
	 *      
	 *      integers have very little surprising behaviour, supporting binary, hex, underscores and, unfortunately, 0o octal notation.
	 *      
	 *    There is no representable null value.
	 *   
	 * EDGE CASE NOTES:
	 * 
	 * newline is LF or CRLF. We instead use '\n', and Reader will capture additional, non-astonishing patterns
	 * like classic mac's bare-CR newline and convert them to LF ('\n') for us.
	 * 
	 * 
	 * ws is defined as space (0x20 or ' ') and tab (0x09 or '\t'). Instead, we use the same "non-breaking whitespace" categories
	 * as we do for JSON5, on a code-point basis:
	 *  - Unicode space characters (SPACE_SEPARATOR, LINE_SEPARATOR, or PARAGRAPH_SEPARATOR) but not non-breaking space (0xA0, 0x2007, or 0x202F)
	 *  - Space or tab
	 *  - Form feed (0x0C or '\f')
	 *  - Separator control characters, 0x1C, 0x1D, 0x1E, and 0x1F
	 *  - Any additional CR (0x0D or '\r') that escapes newline translation
	 * 
	 * Or, put more simply, Character.isWhitespace(char) && char != '\n'
	 */
	
	
	
	
	
	public TomlReader(Reader src) {
		super(src);
	}

	@Override
	protected void readNext() throws IOException {
		if (hasReadInData) {
			readQueue.push(StructuredData.EOF);
			return;
		}
		// At the root level, we're looking for 'expression' (comment, keyval, table)
		
		// Comments are easy, they'll start with octothorpe
		// Table (both kinds) are easy, we'd peek an opening brace.
		// Anything else we should try to parse as a keyval.
		while(src.peek() != -1) {
			try {
				// No matter what we find, it may be prefixed with some spaces and/or blank lines
				while(Character.isWhitespace(src.peek())) src.read();
				if (src.peek() == -1) break;
				
				int ch = src.peek();
				switch(ch) {
					case '#' -> {
						CommentElement elem = CommentValueParser.readStatic(src);
						contextObj.getPrologue().add(elem);
					}
					
					case '[' -> {
						
						src.read(); // Consume the table opener
						int tablePeek = src.peek();
						if (tablePeek == '[') {
							src.read(); // Consume the rest of the table-array opener
							//Read in table-array name
							tableContext = readTomlKey();
							//contextIsArray = true;
							
							skipNonBreakingWhitespace(); //Shouldn't be needed but just in case
							if (src.read() != ']') throw new SyntaxError("Unclosed table-array name [["+formatTomlKey(tableContext)+"]]");
							if (src.read() != ']') throw new SyntaxError("Unclosed table-array name [["+formatTomlKey(tableContext)+"]]");
							
							contextObj = getNewArrayContext(tableContext);
						} else {
							tableContext = readTomlKey();
							//contextIsArray = false;
							if (src.read() != ']') throw new SyntaxError("Unclosed table name ["+formatTomlKey(tableContext)+"]");
							
							contextObj = getObjectContext(tableContext);
						}
						
					}
					
					default -> {
						bufferedKey = readTomlKey();
						if (bufferedKey.size() == 0) throw new SyntaxError("Got no key data!", src.getLine(), src.getCharacter());
						skipNonBreakingWhitespace();
						if (src.read() != '=')  throw new SyntaxError("Expected equals sign, found '"+Character.toString(src.peek())+"'", src.getLine(), src.getCharacter());
						skipNonBreakingWhitespace();
						readAndCommitValue();
					}
				}
			} catch (SyntaxError err) {
				throw new IOException(err);
			}
		}
		
		result.write(readQueue);
		hasReadInData = true;
	}

	private boolean isTomlKeyChar(int codePoint) {
		return 
				(codePoint >= 'A' && codePoint <= 'Z') ||
				(codePoint >= 'a' && codePoint <= 'z') ||
				(codePoint >= '0' && codePoint <= '9') ||
				(codePoint == '_') ||
				(codePoint == '-');
	}
	
	private List<String> readTomlKey() throws IOException, SyntaxError {
		StringBuilder token = new StringBuilder();
		
		List<String> list = new ArrayList<>();
		boolean bufferedSpace = false;
		
		while(true) {
			int peek = src.peek();
			if (peek == -1) {
				throw new SyntaxError("Encountered EOF while reading in a TOML key. All keys need values!", src.getLine(), src.getCharacter());
			} else if (peek == '\n') {
				throw new SyntaxError("Encountered a newline while reading in a TOML key. All keys need values!", src.getLine(), src.getCharacter());
			} else if (peek == '.') {
				src.read(); //Consume the dot
				//When we encounter a bare dot (not part of a quoted string), add whatever we've scraped so far as a path element and reset the scraper
				list.add(token.toString());
				token = new StringBuilder();
			} else if (isTomlKeyChar(peek)) {
				if (bufferedSpace) {
					bufferedSpace = false;
					token.append(' ');
				}
				//Identifier part, let's go
				token.appendCodePoint(src.read());
			} else if (Character.isWhitespace(peek)) {
				// Skip it / buffer a single space
				src.read();
				bufferedSpace = true;
			} else if (peek == '"' || peek == '\'') {
				// Pull in a quoted string
				token.append(StringValueParser.readStatic(src));
			} else {
				// Terminate the key. If the next character is anomlaous, we'll know from context.
				if (!token.isEmpty()) list.add(token.toString());
				return list;
			}
		}
	}
	
	private String formatTomlKey(List<String> key) {
		if (key.isEmpty()) return "(empty)";
		StringBuilder result = new StringBuilder();
		boolean first = true;
		for(String s : key) {
			if (first) {
				first = false;
			} else {
				result.append('.');
			}
			
			if (s.isBlank() || s.contains(".") || s.contains(" ")) {
				result.append('"');
				result.append(s);
				result.append('"');
			} else {
				result.append(s);
			}
		}
		return result.toString();
	}
	
	private String formatCharacter(int codePoint) {
		if (codePoint == '\t') return "'\\t' (tab)";
		if (codePoint == '\r') return "'\\r' (carriage return)";
		if (codePoint == '\n') return "'\\n' (newline / form feed)";
		if (codePoint == '\'') return "'\\'' (single-quote)";
		if (codePoint>=0x21 && codePoint<=0x7E) return "'"+Character.toString(codePoint)+"'";
		
		return "'"+Character.toString(codePoint)+"' (0x" + Integer.toHexString(codePoint) + ")";
	}
	
	private ValueElement readValue() throws IOException, SyntaxError {
		if (TomlTripleQuotedStringValueParser.canReadStatic(src)) {
			return PrimitiveElement.of(TomlTripleQuotedStringValueParser.readStatic(src));
		//TODO: PARSE "LITERAL" (single-quoted) STRINGS
		} else if (StringValueParser.canReadStatic(src)) {
			return PrimitiveElement.of(StringValueParser.readStatic(src));
		} else if (BooleanValueParser.canReadStatic(src)) {
			return PrimitiveElement.of(BooleanValueParser.readStatic(src));
		//} else if ( TODO: CAN WE PARSE DATES ) {
			// TODO: Parse dates
			// Do this at a higher priority than numbers because we might get false positives
		} else if (NumberValueParser.canReadStatic(src)) {
			return PrimitiveElement.box(NumberValueParser.readStatic(src));
		} else if (src.peek() == '[') {
			return readInlineArray();
		//TODO: Parse inline tables
		} else if (src.peek() == '{') {
			return readInlineTable();
		} else {
			throw new SyntaxError("Unknown value type", src.getLine(), src.getCharacter());
		}
	}
	
	private void readAndCommitValue() throws IOException, SyntaxError {
		ValueElement val = readValue();
		commitKvPair(bufferedKey, val);
		bufferedKey.clear();
	}
	/*	
	private void readAndCommitValue() throws IOException, SyntaxError {
		if (TomlTripleQuotedStringValueParser.canReadStatic(src)) {
			String val = TomlTripleQuotedStringValueParser.readStatic(src);
			commitKvPair(bufferedKey, PrimitiveElement.of(val));
			bufferedKey.clear();
		//TODO: PARSE "LITERAL" (single-quoted) STRINGS
		} else if (StringValueParser.canReadStatic(src)) {
			String val = StringValueParser.readStatic(src);
			commitKvPair(bufferedKey, PrimitiveElement.of(val));
			bufferedKey.clear();
		} else if (BooleanValueParser.canReadStatic(src)) {
			boolean val = BooleanValueParser.readStatic(src);
			commitKvPair(bufferedKey, PrimitiveElement.of(val));
			bufferedKey.clear();
			
		//} else if ( TODO: CAN WE PARSE DATES ) {
			// TODO: Parse dates
			// Do this at a higher priority than numbers because we might get false positives
		} else if (NumberValueParser.canReadStatic(src)) {
			Number val = NumberValueParser.readStatic(src);
			commitKvPair(bufferedKey, PrimitiveElement.box(val));
			bufferedKey.clear();
			
		//TODO: Parse arrays and inline-tables
		} else if (src.peek() == '[') {
			commitKvPair(bufferedKey, readInlineArray());
			bufferedKey.clear();
		} else {
			throw new SyntaxError("Unknown value type", src.getLine(), src.getCharacter());
		}
	}*/
	
	private ArrayElement readInlineArray() throws IOException, SyntaxError {
		ArrayElement result = new ArrayElement();
		
		int openBracket = src.read();
		if (openBracket != '[') throw new SyntaxError("Expected open-bracket ('['), got "+formatCharacter(openBracket), src.getLine(), src.getCharacter());
		
		while(Character.isWhitespace(src.peek())) src.read();
		
		while(src.peek() != ']') {
			result.add(readValue());
			
			while(Character.isWhitespace(src.peek())) src.read();
			if (src.peek() == ',') {
				src.read();
				while(Character.isWhitespace(src.peek())) src.read();
			}
		}
		
		src.read(); // consume closing bracket
		
		return result;
	}
	
	private ObjectElement readInlineTable() throws IOException, SyntaxError {
		ObjectElement result = new ObjectElement();
		
		int openBrace = src.read();
		if (openBrace != '{') throw new SyntaxError("Expected open-brace ('{'), got "+formatCharacter(openBrace), src.getLine(), src.getCharacter());
		
		while(Character.isWhitespace(src.peek())) src.read();
		
		while(src.peek() != '}') {
			List<String> key = readTomlKey();
			if (key.isEmpty()) throw new SyntaxError("Expected a key, got "+formatCharacter(src.peek()), src.getLine(), src.getCharacter());
			
			skipNonBreakingWhitespace();
			
			if (src.peek() != '=') throw new SyntaxError("Expected equals sign ('='), got "+formatCharacter(src.peek()), src.getLine(), src.getCharacter());
			src.read(); //Consume the equals sign
			
			skipNonBreakingWhitespace();
			
			ValueElement val = readValue();
			
			ObjectElement subject = result;
			
			if (key.size() > 1) for (int i = 0; i<key.size()-1; i++) {
				subject = getObjectContext(subject, key.get(i));
			}
			
			String k = key.getLast();
			
			subject.put(k, val);
			
			while(Character.isWhitespace(src.peek())) src.read();
			if (src.peek() == ',') {
				src.read();
				while(Character.isWhitespace(src.peek())) src.read();
			}
		}
		
		src.read(); // consume closing bracket
		
		return result;
	}
	
	private ObjectElement getObjectContext(ObjectElement subject, String key) throws SyntaxError {
		Optional<ObjectElement> maybeNewSubject = subject.tryGetObject(key);
		if (maybeNewSubject.isPresent()) {
			return maybeNewSubject.get();
		} else {
			Optional<ArrayElement> maybeArraySubject = subject.tryGetArray(key);
			if (maybeArraySubject.isPresent()) {
				ArrayElement arr = maybeArraySubject.get();
				if (arr.size() == 0) {
					ObjectElement newSubject = new ObjectElement();
					arr.add(newSubject);
					return newSubject;
				} else {
					ValueElement newSubject = arr.getLast();
					if (newSubject instanceof ObjectElement obj) {
						return obj;
					} else {
						throw new SyntaxError("Expected Object or Array, got "+subject.get(key).getClass().getSimpleName(), src.getLine(), src.getCharacter());
					}
				}
			} else {
				if (subject.containsKey(key)) throw new SyntaxError("Expected ObjectElement for key '"+key+"', got "+subject.get(key).getClass().getSimpleName(), src.getLine(), src.getCharacter());
				ObjectElement newSubject = new ObjectElement();
				subject.put(key, newSubject);
				return newSubject;
			}
		}
	}
	
	private ObjectElement getObjectContext(List<String> key) throws SyntaxError {
		if (key.isEmpty()) throw new IllegalArgumentException("Cannot get a context object with no key");
		
		ObjectElement subject = result;
		for(String k : key) {
			subject = getObjectContext(subject, k);
			/*
			Optional<ObjectElement> maybeNewSubject = subject.tryGetObject(k);
			if (maybeNewSubject.isPresent()) {
				subject = maybeNewSubject.get();
			} else {
				Optional<ArrayElement> maybeArraySubject = subject.tryGetArray(k);
				if (maybeArraySubject.isPresent()) {
					ArrayElement arr = maybeArraySubject.get();
					if (arr.size() == 0) {
						ObjectElement newSubject = new ObjectElement();
						arr.add(newSubject);
						subject = newSubject;
					} else {
						ValueElement newSubject = arr.getLast();
						if (newSubject instanceof ObjectElement obj) {
							subject = obj;
						} else {
							throw new SyntaxError("Expected ObjectElement, got "+subject.get(k).getClass().getSimpleName(), src.getLine(), src.getCharacter());
						}
					}
				} else {
					if (subject.containsKey(k)) throw new SyntaxError("Expected ObjectElement for key '"+k+"', got "+subject.get(k).getClass().getSimpleName(), src.getLine(), src.getCharacter());
					ObjectElement newSubject = new ObjectElement();
					subject.put(k, newSubject);
					subject = newSubject;
				}
			}*/
		}
		
		return subject;
	}
	
	private ObjectElement getNewArrayContext(List<String> key) throws SyntaxError {
		if (key.isEmpty()) throw new SyntaxError("Cannot get a context object with no key", src.getLine(), src.getCharacter());
		
		ObjectElement subject = result;
		if (key.size() > 1) for(int i=0; i<key.size()-1; i++) {
			subject = getObjectContext(subject, key.get(i));
			/*
			Optional<ObjectElement> maybeNewSubject = subject.tryGetObject(key.get(i));
			if (maybeNewSubject.isPresent()) {
				subject = maybeNewSubject.get();
			} else {
				Optional<ArrayElement> maybeArraySubject = subject.tryGetArray(key.get(i));
				if (maybeArraySubject.isPresent()) {
					ArrayElement arr = maybeArraySubject.get();
					if (arr.size() == 0) {
						ObjectElement newSubject = new ObjectElement();
						arr.add(newSubject);
						subject = newSubject;
					} else {
						ValueElement newSubject = arr.getLast();
						if (newSubject instanceof ObjectElement obj) {
							subject = obj;
						} else {
							throw new SyntaxError("Expected ObjectElement, got "+subject.get(key.get(i)).getClass().getSimpleName(), src.getLine(), src.getCharacter());
						}
					}
				} else {
					if (subject.containsKey(key.get(i))) throw new SyntaxError("Expected Object or Aray, got "+subject.get(key.get(i)).getClass().getSimpleName(), src.getLine(), src.getCharacter());
					
					ObjectElement newSubject = new ObjectElement();
					subject.put(key.get(i), newSubject);
					subject = newSubject;
				}
			}*/
		}
		
		String k = key.getLast();
		Optional<ArrayElement> maybeArray = subject.tryGetArray(k);
		if (maybeArray.isPresent()) {
			subject = new ObjectElement();
			maybeArray.get().add(subject);
		} else {
			if (subject.containsKey(k)) throw new SyntaxError("Expected ArrayElement, got "+subject.get(k).getClass().getSimpleName(), src.getLine(), src.getCharacter());
			ArrayElement elem = new ArrayElement();
			subject.put(k, elem);
			subject = new ObjectElement();
			elem.add(subject);
		}
		
		return subject;
	}
	
	private void commitKvPair(List<String> key, ValueElement value) throws SyntaxError {
		if (key.isEmpty()) throw new IllegalArgumentException("Cannot set a value with no key");
		
		ObjectElement subject = contextObj;
		if (key.size() > 1) for(int i=0; i<key.size()-1; i++) {
			Optional<ObjectElement> maybeNewSubject = subject.tryGetObject(key.get(i));
			if (maybeNewSubject.isPresent()) {
				subject = maybeNewSubject.get();
			} else {
				if (subject.containsKey(key.get(i))) throw new SyntaxError("Expected ObjectElement, got "+subject.get(key.get(i)).getClass().getSimpleName(), src.getLine(), src.getCharacter());
				ObjectElement newSubject = new ObjectElement();
				subject.put(key.get(i), newSubject);
				subject = newSubject;
			}
		}
		
		subject.put(key.getLast(), value);
	}
	
}
