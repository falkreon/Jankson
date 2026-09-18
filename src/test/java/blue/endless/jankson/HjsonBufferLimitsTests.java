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

package blue.endless.jankson;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.List;

import org.junit.jupiter.api.Test;

import blue.endless.jankson.api.Jankson;
import blue.endless.jankson.api.SyntaxError;
import blue.endless.jankson.api.document.ObjectElement;
import blue.endless.jankson.api.document.PrimitiveElement;
import blue.endless.jankson.api.io.StructuredData;
import blue.endless.jankson.api.io.json.JsonFormat;
import blue.endless.jankson.api.io.json.JsonReader;
import blue.endless.jankson.api.io.json.JsonReaderOptions;

class HjsonBufferLimitsTests {
	@Test void depthLimitsCannotBecomeScalarFallbacks() throws Exception {
		var one = JsonFormat.HJSON.readerOptions().asBuilder().setMaxContainerDepth(1).build();
		var two = one.asBuilder().setMaxContainerDepth(2).build();
		for (String source : List.of("a: []", "{a: []}", "a: {}", "{a: {}}")) {
			assertTrue(Jankson.readJson(source, two) instanceof ObjectElement, source);
			IOException failure = assertThrows(IOException.class, () -> Jankson.readJson(source, one), source);
			assertTrue(failure.getCause() instanceof SyntaxError);
			assertTrue(failure.getCause().getMessage().contains("Maximum nesting depth of 1"));
			assertTrue(((SyntaxError) failure.getCause()).getLineMessage().contains("line 1"));
		}
	}

	@Test void malformedObjectStillFallsBackUnlessItExceedsAResourceLimit() throws Exception {
		var options = JsonFormat.HJSON.readerOptions().asBuilder().setMaxContainerDepth(2).build();
		for (String source : List.of("a: [", "a: {", "hello", "hello, world")) {
			assertEquals(source, ((PrimitiveElement) Jankson.readJson(source, options)).asString().orElseThrow());
		}
		IOException failure = assertThrows(IOException.class, () -> Jankson.readJson("a: [",
				options.asBuilder().setMaxContainerDepth(1).build()));
		assertTrue(failure.getCause().getMessage().contains("Maximum nesting depth"));
		assertThrows(IOException.class, () -> Jankson.readJson("a: [\n] trailing", options));
	}

	@Test void ambiguousRootDefaultsAreOperationalAndNamed() {
		var options = JsonFormat.HJSON.readerOptions();
		assertEquals(16 * 1024 * 1024, JsonReaderOptions.DEFAULT_MAX_BUFFERED_CHARACTERS);
		assertEquals(1_000_000L, JsonReaderOptions.DEFAULT_MAX_BUFFERED_EVENTS);
		assertEquals(JsonReaderOptions.DEFAULT_MAX_BUFFERED_CHARACTERS, options.getMaxBufferedCharacters());
		assertEquals(JsonReaderOptions.DEFAULT_MAX_BUFFERED_EVENTS, options.getMaxBufferedEvents());
		assertTrue(options.getMaxBufferedCharacters() < Integer.MAX_VALUE);
		assertTrue(options.getMaxBufferedEvents() < Long.MAX_VALUE);
	}

	@Test void ambiguousRootSourceLimitAcceptsExactBoundaryAndRejectsOneMore() throws Exception {
		var exact = JsonFormat.HJSON.readerOptions().asBuilder().setMaxBufferedCharacters(1).build();
		Jankson.readJson("\uD83D\uDE00", exact);

		var sourceLimited = JsonFormat.HJSON.readerOptions().asBuilder().setMaxBufferedCharacters(1).build();
		IOException sourceFailure = assertThrows(IOException.class,
				() -> Jankson.readJson("\uD83D\uDE00x", sourceLimited));
		assertTrue(sourceFailure.getMessage().contains("buffered characters"));
	}

	@Test void ambiguousRootEventLimitAcceptsExactBoundaryAndRejectsOneMore() throws Exception {
		var exact = JsonFormat.HJSON.readerOptions().asBuilder().setMaxBufferedEvents(4).build();
		Jankson.readJson("a: 1", exact);

		var eventLimited = JsonFormat.HJSON.readerOptions().asBuilder().setMaxBufferedEvents(3).build();
		IOException eventFailure = assertThrows(IOException.class,
				() -> Jankson.readJson("a: 1", eventLimited));
		assertTrue(eventFailure.getMessage().contains("events"));
	}

	@Test void ambiguousRootsReadToEofBeforeNonTriviaEventsButBracedRootsStream() throws Exception {
		TrackingReader ambiguousSource = new TrackingReader("a: 1");
		JsonReader ambiguous = new JsonReader(ambiguousSource, JsonFormat.HJSON.readerOptions());
		assertEquals(StructuredData.OBJECT_START, ambiguous.next());
		assertEquals(ambiguousSource.length(), ambiguousSource.position);

		TrackingReader bracedSource = new TrackingReader("{a: 1," + "b: 2,".repeat(10_000) + "c: 3}");
		JsonReader braced = new JsonReader(bracedSource, JsonFormat.HJSON.readerOptions().asBuilder()
				.setMaxBufferedCharacters(1).setMaxBufferedEvents(1).build());
		assertEquals(StructuredData.OBJECT_START, braced.next());
		assertTrue(bracedSource.position < bracedSource.length());

		TrackingReader withTriviaSource = new TrackingReader("\n# comment\na: 1");
		JsonReader withTrivia = new JsonReader(withTriviaSource, JsonFormat.HJSON.readerOptions());
		assertEquals(StructuredData.NEWLINE, withTrivia.next());
		assertTrue(withTriviaSource.position < withTriviaSource.length());
	}

	private static final class TrackingReader extends Reader {
		private final StringReader delegate;
		private final int length;
		private int position;

		TrackingReader(String text) {
			delegate = new StringReader(text);
			length = text.length();
		}

		int length() { return length; }

		@Override public int read(char[] buffer, int offset, int requested) throws IOException {
			int count = delegate.read(buffer, offset, Math.min(requested, 1));
			if (count > 0) position += count;
			return count;
		}

		@Override public void close() { delegate.close(); }
	}
}
