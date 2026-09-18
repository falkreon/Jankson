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
import java.io.StringReader;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

import blue.endless.jankson.api.SyntaxError;
import blue.endless.jankson.api.io.StructuredData;
import blue.endless.jankson.api.io.json.JsonFormat;
import blue.endless.jankson.api.io.json.JsonReaderOptions;
import blue.endless.jankson.impl.io.LookaheadCodePointReader;

/**
 * Transactional handling of HJSON's ambiguous unbraced root. Only this case
 * buffers input/events; both attempts use the ordinary streaming contexts.
 */
final class HjsonRootContext implements ParserContext {
	private final JsonReaderOptions options;
	private Iterator<StructuredData> result;
	HjsonRootContext(JsonReaderOptions options) { this.options = options; }
	@Override public boolean isComplete(LookaheadCodePointReader r) { return result != null && !result.hasNext(); }
	@Override public void parse(LookaheadCodePointReader r, Consumer<StructuredData> out, Consumer<ParserContext> push) throws IOException, SyntaxError {
		if (result == null) {
			int line = r.getLine(), column = r.getCharacter();
			StringBuilder source = new StringBuilder();
			int bufferedCharacters = 0;
			while (r.peek() != -1) {
				if (bufferedCharacters == options.getMaxBufferedCharacters()) {
					throw new IOException("HJSON root exceeds " + options.getMaxBufferedCharacters() + " buffered characters");
				}
				source.appendCodePoint(r.read());
				bufferedCharacters++;
			}
			String text = source.toString();
			List<StructuredData> events = new ArrayList<>();
			try {
				parse(text, options, line, column, events);
			} catch (SyntaxError objectError) {
				events.clear();
				try { parse(text, options.asBuilder().setBareRootObject(false).build(), line, column, events); }
				catch (SyntaxError scalarError) { throw objectError; }
			}
			result = events.iterator();
		}
		if (result.hasNext()) out.accept(result.next());
	}
	private static void parse(String text, JsonReaderOptions options, int line, int column, List<StructuredData> events) throws IOException, SyntaxError {
		LookaheadCodePointReader r = new LookaheadCodePointReader(new StringReader(text), JsonFormat.HJSON, line, column);
		Deque<ParserContext> stack = new ArrayDeque<>();
		stack.push(new RootParserContext(options, false));
		try {
			while (!stack.isEmpty()) {
				ParserContext context = stack.peek();
				if (context.isComplete(r)) stack.pop();
				else context.parse(r, event -> {
					if (event == StructuredData.EOF) return;
					if (events.size() >= options.getMaxBufferedEvents()) throw new EventLimitException();
					events.add(event);
				}, stack::push);
			}
		} catch (EventLimitException ex) {
			throw new IOException("Parser events exceed " + options.getMaxBufferedEvents());
		}
	}

	private static final class EventLimitException extends RuntimeException {
		private static final long serialVersionUID = 1L;
	}
}
