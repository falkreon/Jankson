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

package blue.endless.jankson.api.io.json;

import java.nio.file.Path;
import java.util.Locale;

/** Selects the grammar of an entire document, including object keys. */
public enum JsonFormat {
	JSON, JSON5, HJSON, JSONC;

	public JsonReaderOptions readerOptions() { return JsonReaderOptions.builder().setFormat(this).build(); }
	public JsonWriterOptions writerOptions() { return JsonWriterOptions.builder().setFormat(this).build(); }

	/** Uses only the final filename extension; never guesses from content. */
	public static JsonFormat fromPath(Path path) {
		String name = path.getFileName() == null ? "" : path.getFileName().toString();
		int dot = name.lastIndexOf('.');
		String extension = dot < 0 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
		return switch (extension) {
			case "json" -> JSON;
			case "json5" -> JSON5;
			case "hjson" -> HJSON;
			case "jsonc" -> JSONC;
			default -> throw new IllegalArgumentException("Unknown configuration extension: " + path + "; specify a JsonFormat explicitly.");
		};
	}
}
