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

package blue.endless.jankson.impl.io.context;

import java.io.IOException;
import java.util.function.Function;

import blue.endless.jankson.api.SyntaxError;
import blue.endless.jankson.api.io.json.JsonReaderOptions;
import blue.endless.jankson.api.io.json.JsonFormat;
import blue.endless.jankson.impl.io.LookaheadCodePointReader;
import blue.endless.jankson.impl.io.StringEscapeParser;

/** Shared lexical rules for reading keys and choosing their output representation. */
public final class KeyRules {
	private KeyRules() {}

	public static boolean canWriteUnquoted(String key, JsonFormat syntax, char separator) {
		if (key.isEmpty() || syntax == JsonFormat.JSON || syntax == JsonFormat.JSONC) return false;
		// At the beginning of a token these would be interpreted as strings or comments.
		if (key.startsWith("\"") || key.startsWith("'") || key.startsWith("#")
				|| key.startsWith("//") || key.startsWith("/*")) return false;
		for (int i = 0; i < key.length();) {
			int ch = key.codePointAt(i);
			if (ch == separator || !isLiteralCharacter(ch, i == 0, syntax)) return false;
			i += Character.charCount(ch);
		}
		return true;
	}

	private static boolean isLiteralCharacter(int ch, boolean first, JsonFormat syntax) {
		return switch (syntax) {
			case JSON, JSONC -> false;
			case null -> ch >= 'a' && ch <= 'z' || ch >= 'A' && ch <= 'Z'
					|| ch >= '0' && ch <= '9' || ch == '_' || ch == '-';
			case JSON5 -> isIdentifierCharacter(ch, first);
			case HJSON -> ch >= 0 && !Character.isISOControl(ch)
					&& !Character.isWhitespace(ch) && !Character.isSpaceChar(ch)
					&& ch != 0xFEFF && Character.getType(ch) != Character.SURROGATE
					&& "{}[],:".indexOf(ch) < 0;
		};
	}

	public static boolean isIdentifierCharacter(int ch, boolean first) {
		if (ch == '$' || ch == '_') return true;
		return switch (Character.getType(ch)) {
			case Character.UPPERCASE_LETTER, Character.LOWERCASE_LETTER,
					Character.TITLECASE_LETTER, Character.MODIFIER_LETTER,
					Character.OTHER_LETTER, Character.LETTER_NUMBER -> true;
			case Character.NON_SPACING_MARK, Character.COMBINING_SPACING_MARK,
					Character.DECIMAL_DIGIT_NUMBER, Character.CONNECTOR_PUNCTUATION -> !first;
			default -> !first && (ch == 0x200C || ch == 0x200D);
		};
	}

	public static String read(LookaheadCodePointReader reader, JsonReaderOptions options) throws IOException, SyntaxError {
		int ch = reader.peek();
		JsonFormat syntax = options.getFormat();
		if (syntax == JsonFormat.JSON || syntax == JsonFormat.JSONC) {
			if (ch != '"') throw error(reader, "Expected a double-quoted " + syntax + " key.");
			return readQuoted(reader, syntax);
		}
		if (ch == '"' || ch == '\'') {
			return syntax == null ? StringValueParser.readStatic(reader) : readQuoted(reader, syntax);
		}
		if (!options.isUnquotedKeys()) throw error(reader, "Unquoted keys are disabled.");

		StringBuilder result = new StringBuilder();
		while ((ch = reader.peek()) != -1 && ch != options.getKeyValueSeparator()) {
			if (syntax == JsonFormat.JSON5 && ch == '\\') {
				reader.read();
				if (reader.read() != 'u') throw error(reader, "Only Unicode escapes are allowed in JSON5 identifiers.");
				int decoded = readHex(reader, 4);
				if (!isIdentifierCharacter(decoded, result.isEmpty())) {
					throw error(reader, "Escaped character is not valid in this JSON5 identifier position.");
				}
				result.appendCodePoint(decoded);
			} else {
				if (!isLiteralCharacter(ch, result.isEmpty(), syntax)) break;
				result.appendCodePoint(reader.read());
			}
		}
		if (result.isEmpty()) throw error(reader, "Expected an unquoted key.");
		return result.toString();
	}

	private static int readHex(LookaheadCodePointReader reader, int count) throws IOException, SyntaxError {
		return StringEscapeParser.hex(count, reader::read, message -> error(reader, message));
	}

	public static String readQuoted(LookaheadCodePointReader reader, JsonFormat syntax) throws IOException, SyntaxError {
		int quote = reader.read();
		StringBuilder result = new StringBuilder();
		StringEscapeParser.Input<IOException> read = reader::read, peek = reader::peek;
		Function<String, SyntaxError> failure = message -> error(reader, message);
		while (true) {
			int ch = reader.read();
			if (ch == quote) return result.toString();
			if (StringEscapeParser.invalidLiteral(ch, syntax)) {
				throw error(reader, "Unterminated key or unescaped control character.");
			}
			if (ch == '\\') {
				ch = StringEscapeParser.read(syntax, read, peek, failure);
				if (ch == -1) continue;
			}
			result.appendCodePoint(ch);
		}
	}

	private static SyntaxError error(LookaheadCodePointReader reader, String message) {
		return new SyntaxError(message, reader.getLine(), reader.getCharacter());
	}
}
