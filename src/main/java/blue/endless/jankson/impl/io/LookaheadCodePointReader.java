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

package blue.endless.jankson.impl.io;

import java.io.IOException;
import java.io.Reader;

public class LookaheadCodePointReader implements CodePointReader, Lookahead {
	private static final int REPLACEMENT_CHARACTER = 0xFFFD;
	
	private final Reader in;
	
	private int charLookahead = -1;
	
	private final int[] lookahead;
	private int len = 0;
	private int ofs = 0;
	
	public LookaheadCodePointReader(Reader in) {
		this(in, 16);
	}
	
	public LookaheadCodePointReader(Reader in, int lookahead) {
		this.in = in;
		this.lookahead = new int[lookahead];
	}
	
	@Override
	public String peekString(int length) throws IOException {
		
		if (length > len) {
			int lookaheadNeeded = length - len;
			for(int i=0; i<lookaheadNeeded; i++) lookahead();
		}
		
		int resultLength = Math.min(length, len); //We might have hit EOF before filling our quota
		StringBuilder result = new StringBuilder();
		for(int i=0; i<resultLength; i++) {
			int ptOfs = (ofs + i) % lookahead.length;
			int point = lookahead[ptOfs];
			result.append(Character.toString(point));
		}
		
		return result.toString();
	}

	/**
	 * Tries to add a character to the lookahead buffer
	 */
	private void lookahead() throws IOException {
		if (len >= lookahead.length) return; //Refuse to clobber the start of the ring buffer
		int newIndex = (ofs + len) % lookahead.length;
		int nextChar = readInternal();
		if (nextChar==-1) return;
		
		lookahead[newIndex] = nextChar;
		len++;
	}
	
	private int readInternal() throws IOException {
		int high = (charLookahead==-1) ? in.read() : charLookahead;
		if (high==-1) return -1;
		
		if (!Character.isSurrogate((char) high)) {
			charLookahead = -1;
			return high;
		}
		
		if (Character.isLowSurrogate((char) high)) {
			charLookahead = -1;
			return REPLACEMENT_CHARACTER;
		}
		
		if (Character.isHighSurrogate((char) high)) {
			int low = in.read();
			if (low==-1) {
				//High surrogate followed by EOF, report this as an error
				charLookahead = -1;
				return REPLACEMENT_CHARACTER;
			}
			if (Character.isLowSurrogate((char) low)) {
				charLookahead = -1;
				return Character.toCodePoint((char) high, (char) low);
			} else {
				//High surrogate followed by anything that isn't a low surrogate. Stash the new character as a lookahead
				charLookahead = low;
				return REPLACEMENT_CHARACTER;
			}
		}
		
		//We're not sure what this character is, it's some kind of surrogate but not high or low. Report the error.
		charLookahead = -1;
		return REPLACEMENT_CHARACTER;
	}
	
	@Override
	public int read() throws IOException {
		if (len>0) {
			int result = lookahead[ofs];
			ofs = (ofs + 1) % lookahead.length;
			len--;
			
			return result;
		} else {
			return readInternal();
		}
	}
	
	@Override
	public int peek() throws IOException {
		return peek(1);
	}
	
	@Override
	public int peek(int distanceAhead) throws IOException {
		if (distanceAhead < 1) throw new IllegalArgumentException("Cannot peak fewer than one character ahead");
		if (distanceAhead > lookahead.length) throw new IllegalArgumentException("Cannot lookahead "+distanceAhead+" characters. The lookahead buffer length is "+lookahead.length+".");
		
		if (len < distanceAhead) {
			int charsNeeded = distanceAhead - len;
			for(int i=0; i<charsNeeded; i++) lookahead();
		}
		
		int index = (ofs + (distanceAhead - 1)) % lookahead.length;
		return lookahead[index];
	}

	@Override
	public void close() throws IOException {
		in.close();
	}

}
