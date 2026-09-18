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
import java.io.StringWriter;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import blue.endless.jankson.api.Jankson;
import blue.endless.jankson.api.document.ArrayElement;
import blue.endless.jankson.api.document.CommentElement;
import blue.endless.jankson.api.document.CommentType;
import blue.endless.jankson.api.document.KeyValuePairElement;
import blue.endless.jankson.api.document.ObjectElement;
import blue.endless.jankson.api.document.PrimitiveElement;
import blue.endless.jankson.api.document.ValueElement;
import blue.endless.jankson.api.io.StructuredData;
import blue.endless.jankson.api.io.json.JsonFormat;
import blue.endless.jankson.api.io.json.JsonWriter;
import blue.endless.jankson.api.io.json.JsonWriterOptions;
import blue.endless.jankson.api.io.style.CommentStyle;
import blue.endless.jankson.api.io.style.WhitespaceStyle;

public class TestJsonWriterComments {
	@Test
	public void disabledCommentsDoNotStartDeferralDuringLongStreams() throws Exception {
		StringWriter destination = new StringWriter();
		JsonWriter writer = new JsonWriter(destination, JsonFormat.JSONC.writerOptions().asBuilder()
				.setComments(CommentStyle.NONE).setMaxDeferredTriviaEvents(1)
				.setMaxDeferredTriviaCharacters(1).build());
		writer.write(StructuredData.ARRAY_START);
		writer.write(StructuredData.primitive(1));
		for (int i = 0; i < 10_000; i++) {
			writer.write(StructuredData.comment("discarded comment", CommentType.LINE_END));
			writer.write(StructuredData.NEWLINE);
			writer.write(StructuredData.whitespace("   "));
		}
		writer.write(StructuredData.primitive(2));
		writer.write(StructuredData.ARRAY_END);
		Assertions.assertFalse(destination.toString().contains("discarded"));
		Assertions.assertEquals(2, ((ArrayElement) Jankson.read(destination.toString(), JsonFormat.JSONC)).size());
	}

	@Test
	public void retainedLongRunsHaveFiniteEventAndTextBudgets() throws Exception {
		JsonWriter writer = pendingArray(new StringWriter(), JsonFormat.JSONC.writerOptions());
		for (int i = 0; i < JsonWriterOptions.DEFAULT_MAX_DEFERRED_TRIVIA_EVENTS; i++) {
			writer.write(switch (i % 3) {
				case 0 -> StructuredData.comment("", CommentType.LINE_END);
				case 1 -> StructuredData.NEWLINE;
				default -> StructuredData.whitespace("");
			});
		}
		IOException failure = Assertions.assertThrows(IOException.class,
				() -> writer.write(StructuredData.NEWLINE));
		Assertions.assertTrue(failure.getMessage().contains("4096 events"));

		JsonWriter textLimited = pendingArray(new StringWriter(), JsonFormat.JSONC.writerOptions());
		String chunk = "x".repeat(1024);
		for (int i = 0; i < 1024; i++) textLimited.write(StructuredData.comment(chunk, CommentType.LINE_END));
		Assertions.assertTrue(Assertions.assertThrows(IOException.class,
				() -> textLimited.write(StructuredData.comment("x", CommentType.LINE_END)))
				.getMessage().contains("1048576 characters"));
	}

	@ParameterizedTest
	@EnumSource(WhitespaceStyle.class)
	public void exactBudgetsPreservePlacementAndResetBetweenRuns(WhitespaceStyle whitespace) throws Exception {
		StringWriter destination = new StringWriter();
		JsonWriter writer = pendingArray(destination, JsonFormat.JSONC.writerOptions().asBuilder()
				.setWhitespace(whitespace).setMaxDeferredTriviaEvents(3).setMaxDeferredTriviaCharacters(3).build());
		writer.write(StructuredData.comment("ab", CommentType.LINE_END));
		writer.write(StructuredData.NEWLINE);
		writer.write(StructuredData.whitespace(" "));
		writer.write(StructuredData.primitive(2));
		writer.write(StructuredData.comment("end", CommentType.LINE_END));
		writer.write(StructuredData.ARRAY_END);
		String output = destination.toString();
		assertBefore(output, ",", "//ab");
		assertBefore(output, "//ab", "2");
		assertBefore(output, "2", "//end");
		Assertions.assertFalse(output.substring(output.indexOf("2")).contains(","), output);
		Assertions.assertEquals(2, ((ArrayElement) Jankson.read(output, JsonFormat.JSONC)).size());
	}

	@Test
	public void overflowIsRejectedBeforeRetentionAndIncludesWhitespace() throws Exception {
		var options = JsonFormat.JSONC.writerOptions().asBuilder()
				.setMaxDeferredTriviaEvents(3).setMaxDeferredTriviaCharacters(2).build();
		StringWriter destination = new StringWriter();
		JsonWriter writer = pendingArray(destination, options);
		Assertions.assertThrows(IOException.class, () -> writer.write(StructuredData.comment("abc", CommentType.LINE_END)));
		// The rejected event must not be retained or consume the budget.
		writer.write(StructuredData.comment("\uD83D\uDE00", CommentType.LINE_END));
		Assertions.assertThrows(IOException.class, () -> writer.write(StructuredData.whitespace(" ")));
		writer.write(StructuredData.NEWLINE);
		writer.write(StructuredData.whitespace(""));
		Assertions.assertThrows(IOException.class, () -> writer.write(StructuredData.NEWLINE));
		writer.write(StructuredData.ARRAY_END);
		Assertions.assertFalse(destination.toString().contains("abc"));
		Assertions.assertTrue(destination.toString().contains("\uD83D\uDE00"));
		Assertions.assertEquals(1, ((ArrayElement) Jankson.read(destination.toString(), JsonFormat.JSONC)).size());
	}

	@Test
	public void deferredCommentsSnapshotMutableElements() throws Exception {
		StringWriter destination = new StringWriter();
		JsonWriter writer = pendingArray(destination, JsonFormat.JSONC.writerOptions().asBuilder()
				.setMaxDeferredTriviaCharacters(2).build());
		CommentElement comment = comment("ok");
		writer.write(new StructuredData(StructuredData.Type.COMMENT, comment));
		comment.setValue("changed and over budget");
		writer.write(StructuredData.ARRAY_END);
		Assertions.assertTrue(destination.toString().contains("//ok"));
		Assertions.assertFalse(destination.toString().contains("changed"));
	}

	@Test
	public void bufferOptionsAreImmutableCopiedAndValidated() {
		for (JsonWriterOptions preset : List.of(JsonWriterOptions.DEFAULTS, JsonWriterOptions.STRICT,
				JsonWriterOptions.ONE_LINE, JsonWriterOptions.MINIFIED, JsonWriterOptions.INI_SON)) {
			Assertions.assertEquals(4096, preset.getMaxDeferredTriviaEvents());
			Assertions.assertEquals(1024 * 1024, preset.getMaxDeferredTriviaCharacters());
		}
		for (JsonFormat format : JsonFormat.values()) {
			var builder = format.writerOptions().asBuilder().setMaxDeferredTriviaEvents(2).setMaxDeferredTriviaCharacters(3);
			var original = builder.build();
			builder.setMaxDeferredTriviaEvents(4).setMaxDeferredTriviaCharacters(5);
			var copy = original.asBuilder().setFormat(format).build();
			Assertions.assertEquals(2, copy.getMaxDeferredTriviaEvents());
			Assertions.assertEquals(3, copy.getMaxDeferredTriviaCharacters());
			Assertions.assertEquals(4, builder.build().getMaxDeferredTriviaEvents());
			Assertions.assertEquals(5, builder.build().getMaxDeferredTriviaCharacters());
		}
		for (int invalid : new int[]{0, -1}) {
			Assertions.assertThrows(IllegalArgumentException.class,
					() -> JsonWriterOptions.builder().setMaxDeferredTriviaEvents(invalid));
			Assertions.assertThrows(IllegalArgumentException.class,
					() -> JsonWriterOptions.builder().setMaxDeferredTriviaCharacters(invalid));
		}
	}

	private static JsonWriter pendingArray(StringWriter destination, JsonWriterOptions options) throws IOException {
		JsonWriter writer = new JsonWriter(destination, options);
		writer.write(StructuredData.ARRAY_START);
		writer.write(StructuredData.primitive(1));
		return writer;
	}

	@ParameterizedTest
	@EnumSource(WhitespaceStyle.class)
	public void jsoncPlacesPropertyProloguesAfterTheirSeparators(WhitespaceStyle whitespace) throws Exception {
		ObjectElement object = new ObjectElement();
		object.add(property("first", 1, "first prologue"));
		object.add(property("middle", 2, "middle prologue"));
		object.add(property("last", 3, "last prologue"));
		object.getFooter().add(comment("object footer"));

		String output = write(object, JsonFormat.JSONC, whitespace);
		assertBefore(output, "//first prologue", "\"first\"");
		assertBefore(output, ",", "//middle prologue");
		assertBefore(output, "//middle prologue", "\"middle\"");
		assertBefore(output, output.indexOf("\"middle\"") + 1, ",", "//last prologue");
		assertBefore(output, "//last prologue", "\"last\"");
		Assertions.assertFalse(output.substring(output.indexOf("\"last\"")).matches("(?s).*,\\s*//object footer.*"), output);
		Assertions.assertEquals(3, ((ObjectElement) Jankson.read(output, JsonFormat.JSONC)).size(), output);
	}

	@ParameterizedTest
	@EnumSource(WhitespaceStyle.class)
	public void jsoncPlacesArrayProloguesAndFooterWithoutTrailingComma(WhitespaceStyle whitespace) throws Exception {
		ArrayElement array = new ArrayElement();
		PrimitiveElement first = PrimitiveElement.of(1);
		first.getPrologue().add(comment("first item"));
		array.add(first);
		PrimitiveElement second = PrimitiveElement.of(2);
		second.getPrologue().add(comment("second item"));
		array.add(second);
		array.getFooter().add(comment("array footer"));

		String output = write(array, JsonFormat.JSONC, whitespace);
		assertBefore(output, "//first item", "1");
		assertBefore(output, ",", "//second item");
		assertBefore(output, "//second item", "2");
		Assertions.assertFalse(output.substring(output.indexOf("2")).matches("(?s).*,\\s*//array footer.*"), output);
		Assertions.assertEquals(2, ((ArrayElement) Jankson.read(output, JsonFormat.JSONC)).size(), output);
	}

	@Test
	public void jsoncNestedFooterDoesNotConsumeParentSeparator() throws Exception {
		ObjectElement nested = new ObjectElement();
		nested.put("value", PrimitiveElement.of(1));
		nested.getFooter().add(comment("nested footer"));
		ObjectElement root = new ObjectElement();
		root.put("nested", nested);
		root.add(property("next", 2, "next prologue"));

		String output = write(root, JsonFormat.JSONC, WhitespaceStyle.PRETTY);
		assertBefore(output, "//nested footer", "}");
		assertBefore(output, output.indexOf("//nested footer") + 1, ",", "//next prologue");
		ObjectElement restored = (ObjectElement) Jankson.read(output, JsonFormat.JSONC);
		Assertions.assertEquals(1, restored.getObject("nested").getPrimitive("value").asInt().orElseThrow(), output);
		Assertions.assertEquals(2, restored.getPrimitive("next").asInt().orElseThrow(), output);
	}

	@ParameterizedTest
	@EnumSource(JsonFormat.class)
	public void commentedOutputReparsesInSelectedProfile(JsonFormat format) throws Exception {
		ObjectElement object = new ObjectElement();
		object.add(property("first", 1, "first"));
		object.add(property("second", 2, "second"));
		object.getFooter().add(comment("footer"));

		for (WhitespaceStyle whitespace : List.of(WhitespaceStyle.PRETTY, WhitespaceStyle.COMPACT)) {
			String output = write(object, format, whitespace);
			Assertions.assertEquals(2, ((ObjectElement) Jankson.read(output, format)).size(), output);
		}
	}

	@Test
	public void deferredCommentsKeepWriterLocationAccurate() throws Exception {
		ObjectElement object = new ObjectElement();
		object.put("first", PrimitiveElement.of(1));
		object.add(property("second", 2, "second"));
		StringWriter destination = new StringWriter();
		JsonWriter writer = new JsonWriter(destination, options(JsonFormat.JSONC, WhitespaceStyle.PRETTY));
		object.write(writer);

		String output = destination.toString();
		int lastNewline = output.lastIndexOf('\n');
		Assertions.assertEquals(output.lines().count() - 1, writer.getLine());
		Assertions.assertEquals(output.length() - lastNewline - 1, writer.getColumn());
	}

	private static KeyValuePairElement property(String key, long value, String prologue) {
		KeyValuePairElement result = new KeyValuePairElement(key, PrimitiveElement.of(value));
		result.getPrologue().add(comment(prologue));
		return result;
	}

	private static CommentElement comment(String value) {
		return new CommentElement(value, CommentType.LINE_END);
	}

	private static String write(ValueElement value, JsonFormat format, WhitespaceStyle whitespace) throws Exception {
		return Jankson.toJsonString(value, options(format, whitespace));
	}

	private static JsonWriterOptions options(JsonFormat format, WhitespaceStyle whitespace) {
		return format.writerOptions().asBuilder().setWhitespace(whitespace).build();
	}

	private static void assertBefore(String output, String first, String second) {
		assertBefore(output, 0, first, second);
	}

	private static void assertBefore(String output, int start, String first, String second) {
		int firstIndex = output.indexOf(first, start);
		int secondIndex = output.indexOf(second, firstIndex + first.length());
		Assertions.assertTrue(firstIndex >= start && secondIndex > firstIndex, output);
	}
}
