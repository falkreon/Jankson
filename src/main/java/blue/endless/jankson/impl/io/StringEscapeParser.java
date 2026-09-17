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

package blue.endless.jankson.impl.io;

import java.util.function.Function;

import blue.endless.jankson.api.SyntaxError;
import blue.endless.jankson.api.io.json.JsonFormat;

/** Shared quoted-string rules; callers retain their own input position and string loop. */
public final class StringEscapeParser {
	private StringEscapeParser() {}

	@FunctionalInterface
	public interface Input<E extends Exception> {
		int get() throws E;
	}

	public static boolean invalidLiteral(int ch, JsonFormat syntax) {
		return ch == -1 || ch == '\n' || ch == '\r'
				|| (syntax == JsonFormat.JSON || syntax == JsonFormat.JSONC) && ch < 0x20;
	}

	/** Reads after a backslash; -1 denotes a JSON5 line continuation, not a character. */
	public static <E extends Exception> int read(JsonFormat syntax, Input<E> take, Input<E> peek,
			Function<String, SyntaxError> error) throws E, SyntaxError {
		int ch = take.get();
		if (syntax == JsonFormat.JSON5) {
			if (ch == '\n' || ch == 0x2028 || ch == 0x2029) return -1;
			if (ch == '\r') { if (peek.get() == '\n') take.get(); return -1; }
		}
		return switch (ch) {
			case '"', '\\', '/' -> ch;
			case '\'' -> {
				if (syntax == JsonFormat.JSON || syntax == JsonFormat.JSONC) throw error.apply("Invalid JSON escape.");
				yield ch;
			}
			case 'b' -> '\b';
			case 'f' -> '\f';
			case 'n' -> '\n';
			case 'r' -> '\r';
			case 't' -> '\t';
			case 'u' -> hex(4, take, error);
			default -> {
				if (syntax != JsonFormat.JSON5 || ch == -1 || ch >= '1' && ch <= '9') {
					throw error.apply("Invalid string escape.");
				}
				if (ch == 'x') yield hex(2, take, error);
				if (ch == 'v') yield 0x0B;
				if (ch == '0') {
					int next = peek.get();
					if (next >= '0' && next <= '9') throw error.apply("Digit following null escape.");
					yield 0;
				}
				yield ch;
			}
		};
	}

	public static <E extends Exception> int hex(int count, Input<E> take,
			Function<String, SyntaxError> error) throws E, SyntaxError {
		int result = 0;
		for (int i = 0; i < count; i++) {
			int ch = take.get();
			int digit = ch >= '0' && ch <= '9' ? ch - '0'
					: ch >= 'a' && ch <= 'f' ? ch - 'a' + 10
					: ch >= 'A' && ch <= 'F' ? ch - 'A' + 10 : -1;
			if (digit < 0) throw error.apply("Invalid hexadecimal escape.");
			result = result * 16 + digit;
		}
		return result;
	}
}
