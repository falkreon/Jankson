/*
 * MIT License
 *
 * Copyright (c) 2018-2025 Falkreon (Isaac Ellingson)
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
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.regex.Pattern;


import blue.endless.jankson.api.SyntaxError;
import blue.endless.jankson.impl.io.AbstractStructuredDataReader;
import blue.endless.jankson.impl.io.LookaheadCodePointReader;
import blue.endless.jankson.impl.io.context.StringValueParser;

public class IniReader extends AbstractStructuredDataReader {

	private boolean isSection = false;
	
	public IniReader(Reader src) {
		super(src);
		enqueueOutput(StructuredData.OBJECT_START);
	}
	
	private String grabKeyOrValue() throws IOException {
		StringBuilder result = new StringBuilder();
		
		while (src.peek() != '\n' && src.peek() != '=' && src.peek() != -1) {
			result.appendCodePoint(src.read());
		}
		
		if (result.isEmpty() && src.peek() == -1) throw new IOException("EOF found in a weird place");
		
		return result.toString();
	}
	
	private String grabHeading() throws IOException {
		src.read(); // Discard '['
		
		StringBuilder result = new StringBuilder();
		while (src.peek() != '\n' && src.peek() != ']') {
			result.appendCodePoint(src.read());
		}
		
		if (src.peek() == ']') src.read(); // Discard ']'
		
		return result.toString().trim();
	}

	@Override
	protected void readNext() throws SyntaxError, IOException {
		while(Character.isWhitespace(src.peek())) src.read(); // Skip line breaks, etc.
		
		if (src.peek() == -1) {
			if (isSection) {
				readQueue.push(StructuredData.OBJECT_END);
				isSection = false;
			}
			readQueue.push(StructuredData.OBJECT_END);
			readQueue.push(StructuredData.EOF);
			return;
		}
		
		if (src.peek() == '[') {
			//Grab a section header
			String heading = grabHeading();
			
			if (isSection) {
				readQueue.push(StructuredData.OBJECT_END);
				isSection = false;
			}
			readQueue.push(StructuredData.objectKey(heading));
			readQueue.push(StructuredData.OBJECT_START);
			isSection = true;
			
			while(Character.isWhitespace(src.peek())) src.read(); // Skip line breaks, etc.
			return;
		}
		
		
		String key = grabKeyOrValue().trim();
		if (key.startsWith("[")) {
			//This is a section instead.
			if (isSection) {
				readQueue.push(StructuredData.OBJECT_END);
			}
			//TODO: Strip brackets from key
			readQueue.push(StructuredData.OBJECT_START);
		}
		
		if (src.peek() != '=') throw new IOException(new SyntaxError("Expected '=' but found "+formatCharacter(src.peek()), src.getLine(), src.getCharacter()));
		src.read(); //discard the equals
		
		readQueue.push(StructuredData.objectKey(key));
		
		skipNonBreakingWhitespace();
		
		if (src.peek() == '"') {
			try {
				String value = StringValueParser.readStatic(src);
				readQueue.push(StructuredData.primitive(value));
			} catch (SyntaxError e) {
				throw new IOException(e);
			}
		} else {
			String value = grabKeyOrValue().trim();
			readQueue.push(StructuredData.primitive(value));
		}
		
		while(Character.isWhitespace(src.peek())) src.read(); // Skip line breaks, etc.
	}
	
	private String formatCharacter(int codePoint) {
		if (codePoint == '\t') return "'\\t' (tab)";
		if (codePoint == '\r') return "'\\r' (carriage return)";
		if (codePoint == '\n') return "'\\n' (newline / form feed)";
		if (codePoint == '\'') return "'\\'' (single-quote)";
		if (codePoint>=0x21 && codePoint<=0x7E) return "'"+Character.toString(codePoint)+"'";
		
		return "'"+Character.toString(codePoint)+"' (0x" + Integer.toHexString(codePoint) + ")";
	}
	
	public static <T extends Enum<T>> Optional<T> castToEnum(String value, Class<T> enumType) {
		// Exact match?
		for(T t : enumType.getEnumConstants()) {
			if (t.name().toLowerCase(Locale.ROOT).equals(value)) {
				return Optional.of(t);
			}
		}
		
		// Case-insensitive match?
		String comparisonValue = value.toLowerCase(Locale.ROOT);
		for(T t : enumType.getEnumConstants()) {
			if (t.name().toLowerCase(Locale.ROOT).equals(comparisonValue)) {
				return Optional.of(t);
			}
		}
		
		// Nope. Try it without the underscores
		comparisonValue = comparisonValue.replaceAll(Pattern.quote("_"), "");
		for(T t : enumType.getEnumConstants()) {
			if (t.name().toLowerCase(Locale.ROOT).equals(comparisonValue)) {
				return Optional.of(t);
			}
		}
		
		return Optional.empty();
	}
	
	public static OptionalInt castToInt(String value) {
		try {
			return OptionalInt.of(Integer.parseInt(value));
		} catch (Throwable t) {
			return OptionalInt.empty();
		}
	}
	
	public static OptionalDouble castToDouble(String value) {
		try {
			return OptionalDouble.of(Double.parseDouble(value));
		} catch (Throwable t) {
			return OptionalDouble.empty();
		}
	}
	
	private static final Optional<Boolean> TRUE = Optional.of(Boolean.TRUE);
	private static final Optional<Boolean> FALSE = Optional.of(Boolean.FALSE);
	public static Optional<Boolean> castToBoolean(String value) {
		String compValue = value.toLowerCase(Locale.ROOT);
		switch(compValue) {
			case "true"  -> { return TRUE;  }
			case "false" -> { return FALSE; }
			case "yes"   -> { return TRUE;  }
			case "no"    -> { return FALSE; }
			case "on"    -> { return TRUE;  }
			case "off"   -> { return FALSE; }
			case "1"     -> { return TRUE;  }
			case "0"     -> { return FALSE; }
			default -> { return Optional.empty(); }
		}
	}
	
	public static List<String> castToList(String s) {
		List<String> result = new ArrayList<>();
		StringBuilder buf = new StringBuilder();
		
		try (LookaheadCodePointReader reader = new LookaheadCodePointReader(new StringReader(s))) {
			int ch = reader.read();
			while(ch != -1) {
				if (ch == ',') {
					result.add(buf.toString().trim());
					buf.setLength(0);
				} else {
					buf.appendCodePoint(ch);
				}
				
				ch = reader.read();
			}
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
		
		if (buf.length() > 0) {
			result.add(buf.toString().trim());
		}
		
		return result;
	}
}
