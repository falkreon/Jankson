package blue.endless.jankson.impl.io.context;

import java.io.IOException;
import java.util.Arrays;
import java.util.Locale;

import blue.endless.jankson.api.SyntaxError;
import blue.endless.jankson.impl.io.Lookahead;
import blue.endless.jankson.impl.io.LookaheadCodePointReader;

public class NumberValueParser implements ValueParser {
	private static final int[] numberValueStart = createSortedLookup("-+.0123456789");
	private static final int[] numberValueChars = createSortedLookup("-+.0123456789ABCDEFabcdefINintxy");
	
	@Override
	public boolean canRead(Lookahead lookahead) throws IOException {
		String infTest = lookahead.peekString(8);
		if (infTest.equals("Infinity") || infTest.equals("infinity")) return true;
		if (infTest.toLowerCase(Locale.ROOT).startsWith("nan")) return true;
		
		int ch = lookahead.peek();
		return Arrays.binarySearch(numberValueStart, ch) >= 0;
	}

	@Override
	public Object read(LookaheadCodePointReader reader) throws IOException, SyntaxError {
		int startLine = reader.getLine();
		int startChar = reader.getCharacter();
		
		StringBuilder sb = new StringBuilder();
		int ch = reader.read();
		sb.appendCodePoint(ch);
		
		ch = reader.peek();
		while(Arrays.binarySearch(numberValueChars, ch) >= 0) {
			ch = reader.read();
			sb.appendCodePoint(ch);
			ch = reader.peek();
		}
		
		if (sb.charAt(0)=='.') sb.insert(0, '0');
		
		String result = sb.toString();
		if (
			result.equals("Infinity") ||
			result.equals("infinity") ||
			result.equals("+Infinity") ||
			result.equals("+infinity")
			) return Double.POSITIVE_INFINITY;
		
		if (
			result.equals("-Infinity") ||
			result.equals("-infinity")
			) return Double.NEGATIVE_INFINITY;
		
		if (result.toLowerCase().equals("nan")) return Double.NaN;
		
		if (result.startsWith("0x")) {
			try {
				result = result.substring(2);
				return Long.parseLong(result, 16);
			} catch (NumberFormatException ex) {
				SyntaxError err = new SyntaxError("Invalid number format for '"+result+"'.", ex);
				err.setStartParsing(startLine, startChar);
				err.setEndParsing(reader.getLine(), reader.getCharacter());
				throw err;
			}
		}
		
		if (result.startsWith("-0x")) {
			try {
				result = result.substring(3);
				return -Long.parseLong(result, 16);
			} catch (NumberFormatException ex) {
				SyntaxError err = new SyntaxError("Invalid number format for '"+result+"'.", ex);
				err.setStartParsing(startLine, startChar);
				err.setEndParsing(reader.getLine(), reader.getCharacter());
				throw err;
			}
		}
		
		try {
			if (result.indexOf(".")>=0 || result.indexOf("e")>=0) {
				return Double.parseDouble(result);
			} else {
				return Long.parseLong(sb.toString());
			}
		} catch (NumberFormatException ex) {
			SyntaxError err = new SyntaxError("Invalid number format for '"+result+"'.", ex);
			err.setStartParsing(startLine, startChar);
			err.setEndParsing(reader.getLine(), reader.getCharacter());
			throw err;
		}
	}
	
	private static int[] createSortedLookup(String s) {
		int[] arr = s.chars().toArray();
		Arrays.sort(arr);
		return arr;
	}
}
