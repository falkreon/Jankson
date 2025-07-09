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

package blue.endless.jankson.impl.io.context.toml;

import java.io.IOException;
import java.util.Arrays;

import blue.endless.jankson.api.SyntaxError;
import blue.endless.jankson.impl.io.Lookahead;
import blue.endless.jankson.impl.io.LookaheadCodePointReader;
import blue.endless.jankson.impl.io.context.ValueParser;

/**
 * Parses dates in RFC 3339 format.
 * 
 * <p>Note: recognizing RFC 3339 dates in bare text is expensive, and can take up to 20 characters
 * of lookahead to do properly. We only verify up to the first character that would be unambiguous
 * for TOML:
 * <ul>
 * <li>Local OffsetTime looks for {@code "##:"} (that is, two digits, followed by a colon)
 * <li>All date and date-time formats look for {@code "####-"} (two-digit years are rejected)
 * </ul>
 * <p>Once these patterns are found, all contiguous valid characters (0-9 / : / + / - / T / Z / .)
 * are scraped. If this represents a time or a datetime, we stop and return. Otherwise, we need to
 * check the following characters. If they are a space, followed by two digits and a colon, then we
 * continue to scrape the time.
 */
public class DateValueParser implements ValueParser {
	
	private static final int[] dateCharacters = { // These are sorted in ascii order
			'+', '-', '.',
			'0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
			':', 'T', 'Z', 't', 'z'
	};
	
	
	public static boolean canReadStatic(Lookahead lookahead) throws IOException {
		int c1 = lookahead.peek(1);
		int c2 = lookahead.peek(2);
		int c3 = lookahead.peek(3);
		
		if (Character.isDigit(c1) && Character.isDigit(c2) && c3 == ':') return true; //Some kind of LocalTime
		
		if (!(Character.isDigit(c1) && Character.isDigit(c2) && Character.isDigit(c3))) return false; //Definitely not a year
		
		if (Character.isDigit(lookahead.peek(4)) && lookahead.peek(5) == '-') return true; //Year
		
		return false; //Doesn't fit any pattern we understand
	}

	
	public static String readStatic(LookaheadCodePointReader reader) throws IOException, SyntaxError {
		StringBuilder result = new StringBuilder();
		while(Arrays.binarySearch(dateCharacters, reader.peek()) >= 0) {
			result.appendCodePoint(reader.read());
		}
		
		if (result.indexOf("T") < 0 && reader.peek() == ' ') {
			// This may be a space-delimited Date-Time.
			// Examine the result to make sure it started with a Date
			if (!Character.isDigit(result.charAt(0))) return result.toString();
			if (!Character.isDigit(result.charAt(1))) return result.toString();
			if (!Character.isDigit(result.charAt(2))) return result.toString();
			if (!Character.isDigit(result.charAt(3))) return result.toString();
			if (result.charAt(4) != '-') return result.toString();
			
			// Okay, it definitely started with a date. Look for the time
			if (
					reader.peek(1) == ' ' &&
					Character.isDigit(reader.peek(2)) &&
					Character.isDigit(reader.peek(3)) &&
					reader.peek(4) == ':') {
				
				result.appendCodePoint(reader.read()); // Consume the space
				
				while(Arrays.binarySearch(dateCharacters, reader.peek()) >= 0) {
					result.appendCodePoint(reader.read());
				}
			}
		}
		
		return result.toString();
	}


	@Override
	public boolean canRead(Lookahead lookahead) throws IOException {
		return canReadStatic(lookahead);
	}


	@Override
	public String read(LookaheadCodePointReader reader) throws IOException, SyntaxError {
		return readStatic(reader);
	}

}
