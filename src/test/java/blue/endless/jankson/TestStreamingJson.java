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

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import blue.endless.jankson.api.Jankson;
import blue.endless.jankson.api.SyntaxError;
import blue.endless.jankson.api.document.ObjectElement;
import blue.endless.jankson.api.document.PrimitiveElement;
import blue.endless.jankson.api.io.StructuredData;
import blue.endless.jankson.api.io.json.JsonFormat;
import blue.endless.jankson.api.io.json.JsonReader;
import blue.endless.jankson.api.io.json.JsonReaderOptions;

@Timeout(30)
public class TestStreamingJson {
	/** Fails instead of letting an eager parser silently read the rest of the document. */
	private static final class GatedReader extends Reader {
		private final String text;
		private int position;
		private int available = 16;
		GatedReader(String text) { this.text = text; }
		@Override public int read(char[] buffer, int offset, int length) throws IOException {
			if (length == 0) return 0;
			if (position == text.length()) return -1;
			if (position >= available) throw new IOException("Read beyond available prefix");
			buffer[offset] = text.charAt(position++);
			return 1;
		}
		@Override public void close() { }
	}

	@ParameterizedTest
	@EnumSource(JsonFormat.class)
	public void emitsContainerBeforeReadingRemainder(JsonFormat format) throws Exception {
		GatedReader source = new GatedReader("[0," + "1,".repeat(10_000) + "2]");
		JsonReader reader = new JsonReader(source, format.readerOptions());
		Assertions.assertEquals(StructuredData.ARRAY_START, reader.next());
		Assertions.assertTrue(source.position <= 16);
		source.available = source.text.length();
		int primitives = 0;
		while (reader.hasNext()) if (reader.next().type() == StructuredData.Type.PRIMITIVE) primitives++;
		Assertions.assertEquals(10_002, primitives);
		Assertions.assertEquals(StructuredData.EOF, reader.next());
	}

	@ParameterizedTest
	@EnumSource(JsonFormat.class)
	public void doesNotAccumulateLeadingCommentOrNewlineEvents(JsonFormat format) throws Exception {
		String trivia = format == JsonFormat.JSON ? "\n" : "//x\n";
		GatedReader source = new GatedReader(trivia.repeat(10_000) + "[]");
		JsonReader reader = new JsonReader(source, format.readerOptions());
		Assertions.assertEquals(format == JsonFormat.JSON ? StructuredData.Type.NEWLINE : StructuredData.Type.COMMENT, reader.next().type());
		Assertions.assertTrue(source.position <= 16);
	}

	@ParameterizedTest
	@EnumSource(JsonFormat.class)
	public void lateFailurePoisonsReader(JsonFormat format) throws Exception {
		JsonReader reader = new JsonReader(new StringReader("[42, {"), format.readerOptions());
		Assertions.assertEquals(StructuredData.ARRAY_START, reader.next());
		Assertions.assertEquals(StructuredData.Type.PRIMITIVE, reader.next().type());
		Assertions.assertEquals(StructuredData.OBJECT_START, reader.next());
		IOException error = Assertions.assertThrows(IOException.class, () -> { while (reader.hasNext()) reader.next(); });
		Assertions.assertInstanceOf(SyntaxError.class, error.getCause());
		Assertions.assertThrows(IOException.class, reader::next);
		Assertions.assertThrows(IOException.class, () -> Jankson.read("[42, {", format));
	}

	@ParameterizedTest
	@ValueSource(strings = {"a: [", "a: true, b:", "a: {unfinished"})
	public void preservesAmbiguousHjsonScalarFallback(String text) throws Exception {
		PrimitiveElement result = (PrimitiveElement) Jankson.read(text, JsonFormat.HJSON);
		Assertions.assertEquals(text, result.asString().orElseThrow());
	}

	@ParameterizedTest
	@ValueSource(strings = {"\n", "\r", "\r\n", "\u2028", "\u2029"})
	public void json5LocationsSurviveLineBoundaries(String newline) {
		IOException failure = Assertions.assertThrows(IOException.class, () -> Jankson.read("[1," + newline + "?]", JsonFormat.JSON5));
		Assertions.assertTrue(((SyntaxError) failure.getCause()).getLineMessage().contains("line 2"));
	}

	@ParameterizedTest
	@EnumSource(JsonFormat.class)
	public void preservesUtf16AndSupplementaryCharactersInReaderInput(JsonFormat format) throws Exception {
		String value = "😀\uD800x\uDC00";
		ObjectElement result = (ObjectElement) Jankson.read(new StringReader("{\"" + value + "\":\"" + value + "\"}"), format);
		Assertions.assertEquals(value, result.getPrimitive(value).asString().orElseThrow());
	}

	@ParameterizedTest
	@EnumSource(JsonFormat.class)
	public void acceptsMaximumDepth(JsonFormat format) throws Exception {
		Jankson.read("[".repeat(256) + "0" + "]".repeat(256), format);
	}

	@Test
	public void builderChangesDoNotMutateBuiltOptions() {
		var builder = JsonFormat.JSON5.readerOptions().asBuilder();
		var before = builder.build();
		var after = builder.setUnquotedKeys(false).build();
		Assertions.assertTrue(before.isUnquotedKeys());
		Assertions.assertFalse(after.isUnquotedKeys());
		Assertions.assertEquals(JsonFormat.JSON5, after.getFormat());
	}

	@ParameterizedTest
	@ValueSource(ints = {0, 1, 2, 3})
	public void concurrentColdInitializationFinishes(int order) throws Exception {
		String javaExecutable = Path.of(System.getProperty("java.home"), "bin", "java").toString();
		String separator = System.getProperty("path.separator");
		String classpath = Path.of(JsonReaderOptions.class.getProtectionDomain().getCodeSource().getLocation().toURI())
				+ separator + Path.of(OptionsInitializationProbe.class.getProtectionDomain().getCodeSource().getLocation().toURI());
		Process process = new ProcessBuilder(javaExecutable, "-cp", classpath, OptionsInitializationProbe.class.getName(), Integer.toString(order))
				.redirectErrorStream(true).start();
		try {
			Assertions.assertTrue(process.waitFor(15, TimeUnit.SECONDS), "Options initialization timed out");
			Assertions.assertEquals(0, process.exitValue(), new String(process.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8));
		} finally { process.destroyForcibly(); }
	}
}
