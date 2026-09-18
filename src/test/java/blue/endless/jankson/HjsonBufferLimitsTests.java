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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import blue.endless.jankson.api.Jankson;
import blue.endless.jankson.api.io.json.JsonFormat;

class HjsonBufferLimitsTests {
	@Test void ambiguousRootSourceAndEventsUseReaderOptionLimits() {
		var sourceLimited = JsonFormat.HJSON.readerOptions().asBuilder().setMaxBufferedCharacters(4).build();
		IOException sourceFailure = assertThrows(IOException.class,
				() -> Jankson.readJson("value", sourceLimited));
		assertTrue(sourceFailure.getMessage().contains("buffered characters"));

		var eventLimited = JsonFormat.HJSON.readerOptions().asBuilder().setMaxBufferedEvents(3).build();
		IOException eventFailure = assertThrows(IOException.class,
				() -> Jankson.readJson("a: 1", eventLimited));
		assertTrue(eventFailure.getMessage().contains("events"));
	}
}
