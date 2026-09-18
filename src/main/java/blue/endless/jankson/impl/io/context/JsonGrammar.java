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
import java.math.BigInteger;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import blue.endless.jankson.api.SyntaxError;
import blue.endless.jankson.api.document.CommentType;
import blue.endless.jankson.api.io.StructuredData;
import blue.endless.jankson.api.io.json.JsonFormat;
import blue.endless.jankson.api.io.json.JsonReaderOptions;
import blue.endless.jankson.impl.io.LookaheadCodePointReader;

/** Lexical rules shared by the streaming document contexts. */
final class JsonGrammar {
	private JsonGrammar() {}
	private static final Pattern JSON_NUMBER = Pattern.compile("-?(?:0|[1-9][0-9]*)(?:\\.[0-9]+)?(?:[eE][+-]?[0-9]+)?");
	private static final Pattern JSON5_NUMBER = Pattern.compile("[+-]?(?:(?:0|[1-9][0-9]*)(?:\\.[0-9]*)?|\\.[0-9]+)(?:[eE][+-]?[0-9]+)?");
	private static final Pattern HEX = Pattern.compile("[+-]?0[xX][0-9a-fA-F]+");

	static SyntaxError error(LookaheadCodePointReader r, String message) {
		return new SyntaxError(message, r.getLine(), r.getCharacter());
	}
	/** A parser budget failure must never trigger an alternative grammar interpretation. */
	static final class ResourceLimitError extends SyntaxError {
		private static final long serialVersionUID = 1L;
		ResourceLimitError(LookaheadCodePointReader r, String message) {
			super(message, r.getLine(), r.getCharacter());
		}
	}
	static boolean whitespace(int ch, JsonFormat format) {
		return ch == ' ' || ch == '\t' || ch == '\r' || ch == '\n'
				|| format == JsonFormat.JSON5 && (ch == 0x0B || ch == 0x0C || ch == 0xFEFF
				|| ch == 0x2028 || ch == 0x2029 || Character.getType(ch) == Character.SPACE_SEPARATOR);
	}
	static boolean starts(LookaheadCodePointReader r, String s) throws IOException { return r.peekString(s.length()).equals(s); }
	static boolean comment(LookaheadCodePointReader r) throws IOException {
		return r.peek() == '#' || starts(r, "//") || starts(r, "/*");
	}
	/** Emits at most one event per call, keeping the output queue bounded. */
	static boolean trivia(LookaheadCodePointReader r, JsonFormat format, Consumer<StructuredData> out) throws IOException, SyntaxError {
		return trivia(r, format, out, () -> {});
	}
	/** Reports line terminators even when they occur inside an emitted block comment. */
	static boolean trivia(LookaheadCodePointReader r, JsonFormat format, Consumer<StructuredData> out,
			Runnable lineTerminator) throws IOException, SyntaxError {
		while (whitespace(r.peek(), format)) {
			int ch = r.read();
			if (ch == '\r') { if (r.peek() == '\n') r.read(); lineTerminator.run(); out.accept(StructuredData.NEWLINE); return true; }
			if (ch == '\n' || format == JsonFormat.JSON5 && (ch == 0x2028 || ch == 0x2029)) {
				lineTerminator.run(); out.accept(StructuredData.NEWLINE); return true;
			}
		}
		if (!comment(r)) return false;
		boolean hash = r.peek() == '#', block = starts(r, "/*");
		if (format == JsonFormat.JSON || hash && format != JsonFormat.HJSON) throw error(r, "Comment is not allowed in " + format);
		r.read(); if (!hash) r.read();
		StringBuilder text = new StringBuilder();
		if (block) {
			while (!starts(r, "*/")) {
				if (r.peek() == -1) throw error(r, "Unclosed block comment.");
				int ch = r.read();
				if (ch == '\r' || ch == '\n' || format == JsonFormat.JSON5 && (ch == 0x2028 || ch == 0x2029)) {
					lineTerminator.run();
				}
				text.appendCodePoint(ch);
			}
			r.read(); r.read();
		} else {
			while (r.peek() != -1 && r.peek() != '\n' && r.peek() != '\r'
					&& !(format == JsonFormat.JSON5 && (r.peek() == 0x2028 || r.peek() == 0x2029))) text.appendCodePoint(r.read());
		}
		out.accept(StructuredData.comment(text.toString(), block ? CommentType.MULTILINE : hash ? CommentType.OCTOTHORPE : CommentType.LINE_END));
		return true;
	}
	static void value(LookaheadCodePointReader r, JsonReaderOptions opts, int depth,
			Consumer<StructuredData> out, Consumer<ParserContext> push) throws IOException, SyntaxError {
		JsonFormat format = opts.getFormat();
		int ch = r.peek();
		if (ch == '{' || ch == '[') {
			if (depth >= opts.getMaxContainerDepth()) {
				throw new ResourceLimitError(r, "Maximum nesting depth of " + opts.getMaxContainerDepth() + " exceeded.");
			}
			push.accept(ch == '{' ? new ObjectParserContext(opts, true, depth + 1) : new ArrayParserContext(opts, depth + 1));
		} else if (ch == '"' || ch == '\'') {
			if ((format == JsonFormat.JSON || format == JsonFormat.JSONC) && ch == '\'') {
				throw error(r, "Single-quoted strings are not allowed in " + format + ".");
			}
			out.accept(StructuredData.primitive(format == JsonFormat.HJSON && starts(r, "'''") ? multiline(r) : KeyRules.readQuoted(r, format)));
		} else if (ch == -1) {
			throw error(r, "Expected a value, found end of input.");
		} else if (format == JsonFormat.HJSON) {
			hjsonValue(r, out);
		} else {
			StringBuilder token = new StringBuilder();
			while (r.peek() != -1 && !whitespace(r.peek(), format) && ",]}".indexOf(r.peek()) < 0 && !comment(r)) token.appendCodePoint(r.read());
			StructuredData value = primitive(token.toString(), format, r);
			if (value == null) throw error(r, "Invalid " + format + " value: " + token);
			out.accept(value);
		}
	}
	private static void hjsonValue(LookaheadCodePointReader r, Consumer<StructuredData> out) throws IOException, SyntaxError {
		if ("{}[],:".indexOf(r.peek()) >= 0) throw error(r, "Expected an HJSON value.");
		StringBuilder text = new StringBuilder();
		while (true) {
			int ch = r.peek();
			boolean end = ch == -1 || ch == '\r' || ch == '\n';
			if (end || ",]}".indexOf(ch) >= 0 || comment(r)) {
				StructuredData value = primitive(text.toString().trim(), JsonFormat.HJSON, r);
				if (value != null) { out.accept(value); return; }
				while (r.peek() != -1 && r.peek() != '\r' && r.peek() != '\n') appendHjsonStringCharacter(r, text);
				String result = text.toString().trim();
				if (result.isEmpty()) throw error(r, "Expected an HJSON value.");
				out.accept(StructuredData.primitive(result)); return;
			}
			appendHjsonStringCharacter(r, text);
		}
	}
	private static String multiline(LookaheadCodePointReader r) throws IOException, SyntaxError {
		int indent = r.getCharacter();
		r.read(); r.read(); r.read();
		while (r.peek() == ' ' || r.peek() == '\t' || r.peek() == '\r') r.read();
		if (r.peek() == '\n') { r.read(); skipIndent(r, indent); }
		StringBuilder text = new StringBuilder();
		while (!starts(r, "'''")) {
			int ch = r.read();
			if (ch == -1) throw error(r, "Unclosed multiline HJSON string.");
			if (ch == '\r') continue;
			if (invalidHjsonStringCharacter(ch)) throw error(r, "Unescaped control character in HJSON string.");
			text.appendCodePoint(ch);
			if (ch == '\n') skipIndent(r, indent);
		}
		r.read(); r.read(); r.read();
		if (!text.isEmpty() && text.charAt(text.length() - 1) == '\n') text.setLength(text.length() - 1);
		return text.toString();
	}
	private static void appendHjsonStringCharacter(LookaheadCodePointReader r, StringBuilder text) throws IOException, SyntaxError {
		int ch = r.read();
		if (invalidHjsonStringCharacter(ch)) throw error(r, "Unescaped control character in HJSON string.");
		text.appendCodePoint(ch);
	}
	private static boolean invalidHjsonStringCharacter(int ch) {
		return ch >= 0 && ch <= 0x1F && ch != '\t' && ch != '\n' && ch != '\r';
	}
	private static void skipIndent(LookaheadCodePointReader r, int count) throws IOException {
		while (count-- > 0 && (r.peek() == ' ' || r.peek() == '\t' || r.peek() == '\r')) r.read();
	}
	private static StructuredData primitive(String token, JsonFormat format, LookaheadCodePointReader r) throws SyntaxError {
		if (token.equals("true")) return StructuredData.primitive(true);
		if (token.equals("false")) return StructuredData.primitive(false);
		if (token.equals("null")) return StructuredData.NULL;
		if (format == JsonFormat.JSON5) {
			if (token.equals("NaN") || token.equals("+NaN") || token.equals("-NaN")) return StructuredData.primitive(Double.NaN);
			if (token.equals("Infinity") || token.equals("+Infinity")) return StructuredData.primitive(Double.POSITIVE_INFINITY);
			if (token.equals("-Infinity")) return StructuredData.primitive(Double.NEGATIVE_INFINITY);
			if (HEX.matcher(token).matches()) {
				boolean negative = token.startsWith("-");
				BigInteger n = new BigInteger(token.substring(token.startsWith("+") || negative ? 3 : 2), 16);
				if (negative) n = n.negate();
				if (negative && n.signum() == 0) return StructuredData.primitive(-0.0);
				return n.bitLength() < 64 ? StructuredData.primitive(n.longValue()) : StructuredData.primitive(n.doubleValue());
			}
		}
		if (!(format == JsonFormat.JSON5 ? JSON5_NUMBER : JSON_NUMBER).matcher(token).matches()) return null;
		if (!token.contains(".") && !token.contains("e") && !token.contains("E") && !token.equals("-0")) {
			try { return StructuredData.primitive(Long.parseLong(token)); }
			catch (NumberFormatException ignored) { /* Fall back to the model's double representation. */ }
		}
		double n = Double.parseDouble(token);
		if (!Double.isFinite(n) && format != JsonFormat.JSON5) {
			if (format == JsonFormat.HJSON) return null;
			throw error(r, "Number is outside the supported finite double range.");
		}
		return StructuredData.primitive(n);
	}
}
