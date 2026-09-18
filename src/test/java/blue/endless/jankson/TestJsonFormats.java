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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import blue.endless.jankson.api.Jankson;
import blue.endless.jankson.api.SyntaxError;
import blue.endless.jankson.api.document.CommentElement;
import blue.endless.jankson.api.document.ArrayElement;
import blue.endless.jankson.api.document.CommentType;
import blue.endless.jankson.api.document.KeyValuePairElement;
import blue.endless.jankson.api.document.ObjectElement;
import blue.endless.jankson.api.document.PrimitiveElement;
import blue.endless.jankson.api.document.ValueElement;
import blue.endless.jankson.api.io.StructuredData;
import blue.endless.jankson.api.io.json.*;
import blue.endless.jankson.api.io.style.CommentStyle;
import blue.endless.jankson.api.io.style.WhitespaceStyle;

@Timeout(10)
public class TestJsonFormats {
	@TempDir Path directory;

	private static ObjectElement object(String text, JsonFormat format) throws Exception {
		return (ObjectElement) Jankson.read(text, format);
	}

	private static void rejects(String text, JsonFormat format) {
		IOException error = Assertions.assertThrows(IOException.class, () -> Jankson.read(text, format), text);
		Assertions.assertInstanceOf(SyntaxError.class, error.getCause());
	}

	private static void rejects(String text, JsonReaderOptions options) {
		IOException error = Assertions.assertThrows(IOException.class, () -> Jankson.readJson(text, options), text);
		Assertions.assertInstanceOf(SyntaxError.class, error.getCause());
	}

	@ParameterizedTest
	@EnumSource(JsonFormat.class)
	public void readsJsonSubsetAndRoundTrips(JsonFormat format) throws Exception {
		String source = "{\"ключ\": [1, true, false, null, {\"a\\\"b\":\"😀\"}], \"empty\": {}, \"array\": []}";
		ValueElement document = Jankson.read(source, format);
		String output = Jankson.toJsonString(document, format.writerOptions());
		ObjectElement again = object(output, format);
		Assertions.assertEquals(3, again.size());
		Assertions.assertEquals("😀", again.getArray("ключ").getObject(4).getPrimitive("a\"b").asString().orElseThrow());
		Assertions.assertTrue(again.getObject("empty").isEmpty());
		Assertions.assertTrue(again.getArray("array").isEmpty());
	}

	@ParameterizedTest
	@ValueSource(strings = {"", " ", "/* comment */", "// comment\n1", "# comment\n1", "{a:1}", "{'a':1}",
			"[1,]", "{\"a\":1,}", "[1 2]", "[1,,2]", "[,1]", "{\"a\":1 \"b\":2}", "{\"a\":1,,}",
			"truefalse", "true false", "{}{}", "[] null", "1 2", "NaN", "Infinity", "+1", "01", "-01", ".1", "1.", "0x10", "1e", "--1",
			"\"a\\x41\"", "\"a\\v\"", "\"a\\0\"", "\"a\\q\"", "\"a\t\"", "\"a\n\"", "\"a\\\nb\"", "\ufeff1", "\u00a01",
			"[", "{", "{\"a\":}", "{\"a\" 1}", "\"unterminated", "\"bad\\u00GG\"", "/*"})
	public void rejectsNonJsonDocuments(String source) { rejects(source, JsonFormat.JSON); }

	@ParameterizedTest
	@ValueSource(strings = {"[1 2]", "[1,,2]", "[,1]", "{a:1 b:2}", "{a:1,,}", "# comment\n1", "{port-number:1}", "{123:1}",
			"{a\\u002Db:1}", "{\\u0030abc:1}", "{a\\u005Cb:1}", "{a\\n:1}", "a:1", "truefalse", "01", "0b10", "0o10", "1_000",
			"undefined", "infinity", "nan", "\"a\\1\"", "\"a\\01\"", "\"a\r\"", "\"a\\u123\"", "/*", "[] x", "[] []", ""})
	public void rejectsNonJson5Documents(String source) { rejects(source, JsonFormat.JSON5); }

	@Test
	public void supportsJson5Grammar() throws Exception {
		String source = "\ufeff/* before */ {\n"
				+ "p\\u006Frt: +42, $schema:'ok', ключ:'так', hex:-0x10, fraction:.5, trailing:1., exponent:1.e2,\n"
				+ "infinite:+Infinity, negative:-Infinity, nan:NaN, array:[1,2,],\n"
				+ "escaped:'\\x41\\v\\0', continuation:'a\\\r\nb', identity:'\\q',\n"
				+ "} // after";
		ObjectElement obj = object(source, JsonFormat.JSON5);
		Assertions.assertEquals(42, obj.getPrimitive("port").asInt().orElseThrow());
		Assertions.assertEquals(-16, obj.getPrimitive("hex").asInt().orElseThrow());
		Assertions.assertEquals(0.5, obj.getPrimitive("fraction").asDouble().orElseThrow());
		Assertions.assertEquals(100, obj.getPrimitive("exponent").asDouble().orElseThrow());
		Assertions.assertEquals(Double.POSITIVE_INFINITY, obj.getPrimitive("infinite").asDouble().orElseThrow());
		Assertions.assertTrue(Double.isNaN(obj.getPrimitive("nan").asDouble().orElseThrow()));
		Assertions.assertEquals("A\013\0", obj.getPrimitive("escaped").asString().orElseThrow());
		Assertions.assertEquals("ab", obj.getPrimitive("continuation").asString().orElseThrow());
		Assertions.assertEquals("q", obj.getPrimitive("identity").asString().orElseThrow());
		Assertions.assertFalse(obj.getPrologue().isEmpty());
		Assertions.assertFalse(obj.getEpilogue().isEmpty());
	}

	@Test
	public void supportsJsoncCommentsWithJsonLexicalRules() throws Exception {
		String source = "/* root */ { // object\n"
				+ "\"port\" /* key */ : // value\n 42,\n"
				+ "\"items\": [1, /* between */ 2] // tail\n} // root tail";
		ObjectElement obj = object(source, JsonFormat.JSONC);
		Assertions.assertEquals(42, obj.getPrimitive("port").asInt().orElseThrow());
		Assertions.assertEquals(2, obj.getArray("items").size());
		Assertions.assertFalse(obj.getPrologue().isEmpty());
		Assertions.assertFalse(obj.getEpilogue().isEmpty());

		for (String invalid : List.of("# comment\n1", "{port:1}", "{'port':1}", "[+1]", "[.1]", "[1.]",
				"[0x10]", "[NaN]", "[Infinity]", "\"a\\x41\"", "\"a\\v\"", "\"a\\0\"",
				"\"a\\q\"", "\"a\tb\"", "\ufeff1", "\u00a01", "")) {
			rejects(invalid, JsonFormat.JSONC);
		}
	}

	@Test
	public void trailingCommasAreAnExplicitReaderOption() throws Exception {
		for (String source : List.of("[1,]", "{\"a\":1,}", "[1, /* trailing */ ]", "{\"a\":1, // trailing\n}")) {
			rejects(source, JsonFormat.JSONC);
			JsonReaderOptions tolerant = JsonFormat.JSONC.readerOptions().asBuilder()
					.setAllowTrailingCommas(true)
					.build();
			Jankson.readJson(source, tolerant);
		}

		for (JsonFormat format : List.of(JsonFormat.JSON5, JsonFormat.HJSON)) {
			Assertions.assertTrue(format.readerOptions().allowsTrailingCommas());
			rejects("[1,]", format.readerOptions().asBuilder().setAllowTrailingCommas(false).build());
		}
		Assertions.assertThrows(IllegalArgumentException.class, () -> JsonFormat.JSON.readerOptions().asBuilder()
				.setAllowTrailingCommas(true).build());
		Assertions.assertThrows(IllegalArgumentException.class, () -> JsonReaderOptions.builder()
				.setAllowTrailingCommas(false).build());
		Assertions.assertFalse(JsonFormat.JSONC.readerOptions().asBuilder().build().allowsTrailingCommas());
		Assertions.assertTrue(JsonFormat.JSONC.readerOptions().asBuilder().setAllowTrailingCommas(true)
				.build().asBuilder().build().allowsTrailingCommas());
	}

	@Test
	public void supportsHjsonBareRootsStringsAndComments() throws Exception {
		String source = """
			# server settings
			port-number: 25565
			host: localhost
			enabled: true # typed boolean
			message: hello, world # this belongs to the string
			leading: 01
			hex: 0x10
			notfinite: Infinity
			arr: [
			  first
			  2
			  null
			]
			nested: { value: 3 }
			""";
		ObjectElement obj = object(source, JsonFormat.HJSON);
		Assertions.assertEquals(25565, obj.getPrimitive("port-number").asInt().orElseThrow());
		Assertions.assertEquals("localhost", obj.getPrimitive("host").asString().orElseThrow());
		Assertions.assertTrue(obj.getPrimitive("enabled").asBoolean().orElseThrow());
		Assertions.assertEquals("hello, world # this belongs to the string", obj.getPrimitive("message").asString().orElseThrow());
		Assertions.assertEquals("01", obj.getPrimitive("leading").asString().orElseThrow());
		Assertions.assertEquals("0x10", obj.getPrimitive("hex").asString().orElseThrow());
		Assertions.assertEquals("Infinity", obj.getPrimitive("notfinite").asString().orElseThrow());
		Assertions.assertEquals(3, obj.getArray("arr").size());
		Assertions.assertEquals(3, obj.getObject("nested").getPrimitive("value").asInt().orElseThrow());
	}

	@Test
	public void hjsonOmittedCommasRequireLineTerminators() throws Exception {
		for (String source : List.of(
				"[\"a\"\n\"b\"]", "[\"a\" // comment\n \"b\"]", "[\"a\" /* comment\n */ \"b\"]",
				"{a:\"x\"\nb:\"y\"}", "{a:{} # comment\nb:[]}", "{a:{} /* comment\n */ b:[]}")) {
			Jankson.read(source, JsonFormat.HJSON);
		}
		for (String source : List.of(
				"[\"a\" \"b\"]", "[{} []]", "[\"a\" /* comment */ \"b\"]",
				"{a:\"x\" b:\"y\"}", "{a:{} b:[]}", "{a:{} /* comment */ b:[]}")) {
			rejects(source, JsonFormat.HJSON);
		}

		Assertions.assertEquals(2, ((ArrayElement) Jankson.read("[\"a\",\"b\",]", JsonFormat.HJSON)).size());
		Assertions.assertEquals(2, object("{a:\"x\",b:\"y\",}", JsonFormat.HJSON).size());

		JsonReader stream = new JsonReader(new StringReader("[\"a\" // separator\n \"b\"]"), JsonFormat.HJSON.readerOptions());
		var events = new java.util.ArrayList<StructuredData>();
		while (stream.hasNext()) events.add(stream.next());
		Assertions.assertTrue(events.stream().anyMatch(StructuredData::isComment));
		Assertions.assertTrue(events.contains(StructuredData.NEWLINE));
	}

	@ParameterizedTest
	@ValueSource(strings = {"\n", "\r\n", ""})
	public void hjsonTextBoundariesPreserveTheWholeLine(String ending) throws Exception {
		for (String text : List.of("hello, world # text", "text ] } // /* #", "truex, false, null",
				"1e9999, still text", "https://example.com/a,b")) {
			Assertions.assertEquals(text, object("value: " + text + ending, JsonFormat.HJSON)
					.getPrimitive("value").asString().orElseThrow());
			Assertions.assertEquals(text, ((ArrayElement)
					Jankson.read("[\n" + text + "\n]" + ending, JsonFormat.HJSON))
					.getPrimitive(0).asString().orElseThrow());
		}
		ObjectElement typed = object("{a:true,b:null,c:42/*comment*/}", JsonFormat.HJSON);
		Assertions.assertTrue(typed.getPrimitive("a").asBoolean().orElseThrow());
		Assertions.assertEquals(42, typed.getPrimitive("c").asInt().orElseThrow());
		Assertions.assertEquals(3, typed.size());
	}

	@Test
	public void hjsonLongTextWithManyBoundaries() throws Exception {
		String text = "x,]}#///*".repeat(100_000);
		Assertions.assertEquals(text, object("value: " + text + "\n", JsonFormat.HJSON)
				.getPrimitive("value").asString().orElseThrow());
	}

	@ParameterizedTest
	@EnumSource(JsonFormat.class)
	public void quotedEscapesAgreeAcrossKeysAndValues(JsonFormat format) throws Exception {
		var keyOptions = format.readerOptions();
		String[][] examples = {
			{"a\\n\\r\\t\\b\\f\\/\\\\\\\"", "a\n\r\t\b\f/\\\""},
			{"\\u0041\\ud83d\\ude00\\uD800", "A😀\uD800"}
		};
		for (String[] example : examples) {
			String source = "{\"" + example[0] + "\":\"" + example[0] + "\"}";
			Assertions.assertTrue(Jankson.readJsonObject(source, keyOptions).containsKey(example[1]));
			Assertions.assertEquals(example[1], object(source, format).getPrimitive(example[1]).asString().orElseThrow());
		}
		for (String escape : List.of("\\x41", "\\v", "\\0", "a\\\r\nb", "a\\\u2028b")) {
			String source = "{\"" + escape + "\":1}";
			if (format == JsonFormat.JSON5) {
				Assertions.assertEquals(Jankson.readJsonObject(source, keyOptions).keySet(), object(source, format).keySet());
			} else {
				Assertions.assertThrows(IOException.class, () -> Jankson.readJsonObject(source, keyOptions));
				rejects(source, format);
			}
		}
		for (String contents : List.of("\\u00GG", "\\u１２３４", "\\u12", "\\1", "\\01", "a\nb", "a\rb")) {
			String source = "{\"" + contents + "\":1}";
			Assertions.assertThrows(IOException.class, () -> Jankson.readJsonObject(source, keyOptions));
			rejects(source, format);
			rejects("\"" + contents + "\"", format);
		}
		String incomplete = "{\"unfinished\\";
		Assertions.assertThrows(IOException.class, () -> Jankson.readJsonObject(incomplete, keyOptions));
		rejects(incomplete, format);
		for (char ch : new char[]{0, '\t', '\013'}) {
			String key = "a" + ch + "b";
			String source = "{\"" + key + "\":1}";
			if (format != JsonFormat.JSON5) {
				Assertions.assertThrows(IOException.class, () -> Jankson.readJsonObject(source, keyOptions));
				rejects(source, format);
			} else {
				Assertions.assertTrue(Jankson.readJsonObject(source, keyOptions).containsKey(key));
				Assertions.assertTrue(object(source, format).containsKey(key));
			}
		}
	}

	@Test
	public void hjsonRejectsRawControlsInQuotedKeysAndValues() throws Exception {
		for (char control : new char[]{0, '\t', '\013', '\037'}) {
			rejects("{\"a" + control + "b\":1}", JsonFormat.HJSON);
			rejects("{key:\"a" + control + "b\"}", JsonFormat.HJSON);

			String json5 = "{\"a" + control + "b\":\"v" + control + "x\"}";
			Assertions.assertEquals("v" + control + "x", object(json5, JsonFormat.JSON5)
					.getPrimitive("a" + control + "b").asString().orElseThrow());
		}
	}

	@Test
	public void hjsonRejectsRawControlsInQuotelessAndMultilineStrings() throws Exception {
		for (char control = 0; control <= 0x1F; control++) {
			if (control == '\t' || control == '\n' || control == '\r') continue;
			rejects("{value: before" + control + "after}", JsonFormat.HJSON);
			rejects("{value: '''before" + control + "after'''}", JsonFormat.HJSON);
		}

		for (char control = 0x7F; control <= 0x9F; control++) {
			String expected = "before" + control + "after";
			Assertions.assertEquals(expected, object("{value: " + expected + "\n}", JsonFormat.HJSON)
					.getPrimitive("value").asString().orElseThrow());
			Assertions.assertEquals(expected, object("{value: '''" + expected + "'''}", JsonFormat.HJSON)
					.getPrimitive("value").asString().orElseThrow());
		}

		ObjectElement obj = object("{plain: before\tafter\nmultiline: '''before\tmiddle\r\nafter'''}", JsonFormat.HJSON);
		Assertions.assertEquals("before\tafter", obj.getPrimitive("plain").asString().orElseThrow());
		Assertions.assertEquals("before\tmiddle\nafter", obj.getPrimitive("multiline").asString().orElseThrow());
		Assertions.assertEquals("beforeafter", object("{value: '''before\rafter'''}", JsonFormat.HJSON)
				.getPrimitive("value").asString().orElseThrow());
		Assertions.assertEquals("\0\b\f\n\r\t", object("{value: \"\\u0000\\b\\f\\n\\r\\t\"}", JsonFormat.HJSON)
				.getPrimitive("value").asString().orElseThrow());
	}

	@Test
	public void hjsonRejectsLeadingBomWithoutChangingOtherFormats() throws Exception {
		rejects("\ufeff{value: 1}", JsonFormat.HJSON);
		Assertions.assertEquals(1, object("\ufeff{value: 1}", JsonFormat.JSON5)
				.getPrimitive("value").asInt().orElseThrow());
	}

	@Test
	public void hjsonMultilineDedentsAndKeepsBackslashesLiteral() throws Exception {
		String source = "text:\n  '''\n  first\\n\n    second\n  '''\nfirstli\\ne: 1\nfirstli\\u006Ee: 2\n";
		ObjectElement obj = object(source, JsonFormat.HJSON);
		Assertions.assertEquals("first\\n\n  second", obj.getPrimitive("text").asString().orElseThrow());
		Assertions.assertTrue(obj.containsKey("firstli\\ne"));
		Assertions.assertTrue(obj.containsKey("firstli\\u006Ee"));
		Assertions.assertEquals("hello", ((PrimitiveElement) Jankson.read("'''hello'''", JsonFormat.HJSON)).asString().orElseThrow());
	}

	@ParameterizedTest
	@ValueSource(strings = {"{a: text}", "{a:}", "[1,,2]", "{a b:1}", "{a:[1,2}", "{a:'''never closed}", "/*", "{a:1} trailing"})
	public void rejectsInvalidHjson(String source) { rejects(source, JsonFormat.HJSON); }

	@Test
	public void hjsonRootAmbiguityIsWithinTheSelectedGrammar() throws Exception {
		Assertions.assertTrue(object("# only a comment", JsonFormat.HJSON).isEmpty());
		Assertions.assertTrue(object("", JsonFormat.HJSON).isEmpty());
		Assertions.assertEquals("hello", ((PrimitiveElement) Jankson.read("hello", JsonFormat.HJSON)).asString().orElseThrow());
		Assertions.assertEquals(42, ((PrimitiveElement) Jankson.read("42", JsonFormat.HJSON)).asInt().orElseThrow());
		Assertions.assertEquals("v", object("\"key\": v", JsonFormat.HJSON).getPrimitive("key").asString().orElseThrow());
		rejects("key: value", JsonFormat.JSON5);
	}

	@ParameterizedTest
	@EnumSource(JsonFormat.class)
	public void preservesNegativeZeroAndBoundsNesting(JsonFormat format) throws Exception {
		PrimitiveElement value = (PrimitiveElement) Jankson.read("-0", format);
		Assertions.assertEquals(Double.doubleToRawLongBits(-0.0), Double.doubleToRawLongBits(value.asDouble().orElseThrow()));
		rejects("[".repeat(257) + "0" + "]".repeat(257), format);
	}

	@Test
	public void numbersRespectRepresentationLimits() throws Exception {
		rejects("1e9999", JsonFormat.JSON);
		rejects("1e9999", JsonFormat.JSONC);
		Assertions.assertEquals("1e9999", ((PrimitiveElement) Jankson.read("1e9999", JsonFormat.HJSON)).asString().orElseThrow());
		Assertions.assertEquals(Double.POSITIVE_INFINITY, ((PrimitiveElement) Jankson.read("1e9999", JsonFormat.JSON5)).asDouble().orElseThrow());
		Assertions.assertEquals(Long.MAX_VALUE, ((PrimitiveElement) Jankson.read(Long.toString(Long.MAX_VALUE), JsonFormat.JSON)).asLong().orElseThrow());
	}

	@Test
	public void commentsSurviveScalarRootsExactlyOnce() throws Exception {
		ValueElement value = Jankson.read("/*before*/ 42 /*after*/", JsonFormat.JSON5);
		Assertions.assertEquals(1, value.getPrologue().size());
		Assertions.assertEquals(1, value.getEpilogue().size());
		String output = Jankson.toJsonString(value, JsonFormat.JSON.writerOptions());
		Assertions.assertEquals("42", output);
	}

	@Test
	public void commentStylesControlEmissionWithoutBreakingWriterState() throws Exception {
		ObjectElement obj = new ObjectElement();
		obj.getPrologue().add(new CommentElement("root-prologue", CommentType.LINE_END));
		KeyValuePairElement first = new KeyValuePairElement("first", PrimitiveElement.of(1));
		first.getPrologue().add(new CommentElement("entry-prologue", CommentType.OCTOTHORPE));
		first.getValue().getEpilogue().add(new CommentElement("value-epilogue", CommentType.MULTILINE));
		obj.add(first);
		ArrayElement array = new ArrayElement();
		PrimitiveElement item = PrimitiveElement.of(2);
		item.getPrologue().add(new CommentElement("item-prologue", CommentType.DOC));
		array.add(item);
		array.getFooter().add(new CommentElement("array-footer", CommentType.LINE_END));
		obj.put("array", array);
		obj.getFooter().add(new CommentElement("object-footer", CommentType.OCTOTHORPE));
		obj.getEpilogue().add(new CommentElement("root-epilogue", CommentType.MULTILINE));

		String output = Jankson.toJsonString(obj, JsonWriterOptions.STRICT);
		for (String marker : List.of("root-prologue", "entry-prologue", "value-epilogue", "item-prologue",
				"array-footer", "object-footer", "root-epilogue")) {
			Assertions.assertFalse(output.contains(marker), output);
		}
		ObjectElement parsed = object(output, JsonFormat.JSON);
		Assertions.assertEquals(1, parsed.getPrimitive("first").asInt().orElseThrow());
		Assertions.assertEquals(2, parsed.getArray("array").getPrimitive(0).asInt().orElseThrow());
	}

	@Test
	public void strictAndAllCommentStylesRemainDistinct() throws Exception {
		ObjectElement obj = new ObjectElement();
		obj.getPrologue().add(new CommentElement("line comment", CommentType.LINE_END));
		obj.getPrologue().add(new CommentElement("hash comment", CommentType.OCTOTHORPE));
		obj.getPrologue().add(new CommentElement("block comment", CommentType.MULTILINE));
		obj.getPrologue().add(new CommentElement("doc comment", CommentType.DOC));
		obj.put("value", PrimitiveElement.of(1));

		JsonWriterOptions strict = JsonFormat.JSON5.writerOptions().asBuilder()
				.setComments(CommentStyle.STRICT)
				.setWhitespace(WhitespaceStyle.COMPACT)
				.build();
		String strictOutput = Jankson.toJsonString(obj, strict);
		Assertions.assertTrue(strictOutput.startsWith("//line comment\n//hash comment\n"
				+ "//block comment\n//doc comment\n"), strictOutput);
		Assertions.assertEquals(1, object(strictOutput, JsonFormat.JSON5).getPrimitive("value").asInt().orElseThrow());

		JsonWriterOptions all = strict.asBuilder().setComments(CommentStyle.ALL).build();
		String allOutput = Jankson.toJsonString(obj, all);
		Assertions.assertTrue(allOutput.startsWith("//line comment\n#hash comment\n"
				+ "/*block comment*/ /**doc comment*/ "), allOutput);
	}

	@ParameterizedTest
	@EnumSource(JsonFormat.class)
	public void writerNormalizesCommentsAndRespectsFormat(JsonFormat format) throws Exception {
		ObjectElement obj = new ObjectElement();
		obj.put("port-number", PrimitiveElement.of("yes"));
		obj.getPrologue().add(new CommentElement("text */ {bad}\nmore\u2028last", CommentType.MULTILINE));
		obj.getFooter().add(new CommentElement("# trailing", CommentType.OCTOTHORPE));
		String output = Jankson.toJsonString(obj, format.writerOptions().asBuilder().setWhitespace(WhitespaceStyle.COMPACT).build());
		Assertions.assertEquals("yes", object(output, format).getPrimitive("port-number").asString().orElseThrow());
		if (format == JsonFormat.JSON) Assertions.assertFalse(output.contains("//"));
		else Assertions.assertTrue(output.contains("//"));
	}

	@Test
	public void jsoncWriterDoesNotTurnFooterCommentsIntoTrailingCommas() throws Exception {
		ObjectElement obj = new ObjectElement();
		obj.put("first", PrimitiveElement.of(1));
		KeyValuePairElement second = new KeyValuePairElement("second", PrimitiveElement.of(2));
		second.getPrologue().add(new CommentElement("before second", CommentType.LINE_END));
		obj.add(second);
		ArrayElement array = new ArrayElement();
		array.add(PrimitiveElement.of(3));
		array.getFooter().add(new CommentElement("array footer", CommentType.LINE_END));
		obj.put("array", array);
		obj.getFooter().add(new CommentElement("object footer", CommentType.LINE_END));

		for (WhitespaceStyle whitespace : List.of(WhitespaceStyle.PRETTY, WhitespaceStyle.COMPACT)) {
			JsonWriterOptions options = JsonFormat.JSONC.writerOptions().asBuilder().setWhitespace(whitespace).build();
			String output = Jankson.toJsonString(obj, options);
			ObjectElement restored = object(output, JsonFormat.JSONC);
			Assertions.assertEquals(2, restored.getPrimitive("second").asInt().orElseThrow(), output);
			Assertions.assertEquals(3, restored.getArray("array").getPrimitive(0).asInt().orElseThrow(), output);
		}

		String json = Jankson.toJsonString(obj, JsonFormat.JSONC.writerOptions().asBuilder()
				.setComments(CommentStyle.NONE).build());
		Assertions.assertEquals(3, object(json, JsonFormat.JSON).size());
		Assertions.assertDoesNotThrow(() -> JsonFormat.JSONC.writerOptions().asBuilder()
				.setComments(CommentStyle.ALL).build());
	}

	@Test
	public void writerNonFiniteNumbersAndHjsonBareRoot() throws Exception {
		for (double value : new double[]{Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY}) {
			for (JsonFormat format : List.of(JsonFormat.JSON, JsonFormat.HJSON, JsonFormat.JSONC)) {
				Assertions.assertThrows(IOException.class, () -> Jankson.toJsonString(PrimitiveElement.of(value), format.writerOptions()));
			}
			String text = Jankson.toJsonString(PrimitiveElement.of(value), JsonFormat.JSON5.writerOptions());
			Assertions.assertEquals(value, ((PrimitiveElement) Jankson.read(text, JsonFormat.JSON5)).asDouble().orElseThrow());
		}
		ObjectElement obj = object("{nested:{x:1},arr:[{x:2},3]}", JsonFormat.JSON5);
		String text = Jankson.toJsonString(obj, JsonFormat.HJSON.writerOptions().asBuilder().setBareRootObject(true).setOmmitCommas(true).build());
		ObjectElement again = object(text, JsonFormat.HJSON);
		Assertions.assertEquals(1, again.getObject("nested").getPrimitive("x").asInt().orElseThrow());
		Assertions.assertEquals(2, again.getArray("arr").getObject(0).getPrimitive("x").asInt().orElseThrow());
	}

	@Test
	public void disallowsConflictingOptionsAndCopiesFormat() {
		Assertions.assertThrows(IllegalArgumentException.class, () -> JsonFormat.JSON.readerOptions().asBuilder().setUnquotedKeys(true).build());
		Assertions.assertThrows(IllegalArgumentException.class, () -> JsonFormat.JSONC.readerOptions().asBuilder().setUnquotedKeys(true).build());
		Assertions.assertThrows(IllegalArgumentException.class, () -> JsonFormat.JSON5.readerOptions().asBuilder().setBareRootObject(true).build());
		Assertions.assertThrows(IllegalArgumentException.class, () -> JsonFormat.JSON.writerOptions().asBuilder().setComments(CommentStyle.ALL).build());
		Assertions.assertThrows(IllegalArgumentException.class, () -> JsonFormat.JSON5.writerOptions().asBuilder().setOmmitCommas(true).build());
		Assertions.assertThrows(IllegalArgumentException.class, () -> JsonFormat.HJSON.writerOptions().asBuilder().setOmmitCommas(true).setWhitespace(WhitespaceStyle.COMPACT).build());
		Assertions.assertThrows(IllegalArgumentException.class, () -> JsonFormat.JSON.writerOptions().asBuilder().setIndentValue("xxx").build());
		Assertions.assertThrows(IllegalArgumentException.class, () -> JsonFormat.JSON.readerOptions().asBuilder().setKeyValueSeparator('=').build());
		Assertions.assertNull(JsonReaderOptions.UNSPECIFIED.getFormat());
		for (JsonFormat format : JsonFormat.values()) {
			Assertions.assertEquals(format, format.readerOptions().asBuilder().build().getFormat());
			Assertions.assertEquals(format, format.writerOptions().asBuilder().build().getFormat());
		}
	}

	@Test
	public void typedReadsHonorEntireGrammar() throws Exception {
		record Config(int port) {}
		Assertions.assertEquals(new Config(42), Jankson.readJson(new StringReader("port: 42"), JsonFormat.HJSON.readerOptions(), Config.class));
		Assertions.assertEquals(new Config(42), Jankson.<Config>readJson(new StringReader("port: 42"), JsonFormat.HJSON.readerOptions(), (Type) Config.class));
		Assertions.assertThrows(IOException.class, () -> Jankson.readJson(new StringReader("{port:42}"), JsonFormat.JSON.readerOptions(), Config.class));
	}

	@Test
	public void parsesUtf8StreamsWithoutClosingCallerResources() throws Exception {
		class Input extends ByteArrayInputStream {
			boolean closed;
			Input() { super("{\"ключ\":\"так\"}".getBytes(StandardCharsets.UTF_8)); }
			@Override public void close() { closed = true; }
		}
		Input input = new Input();
		Assertions.assertEquals("так", ((ObjectElement) Jankson.read(input, JsonFormat.JSON)).getPrimitive("ключ").asString().orElseThrow());
		Assertions.assertFalse(input.closed);
		StringReader reader = new StringReader("42");
		Jankson.read(reader, JsonFormat.JSON);
		Assertions.assertEquals(-1, reader.read());
		JsonReader stream = new JsonReader(new StringReader("42"), JsonFormat.JSON.readerOptions());
		while (stream.hasNext()) stream.next();
		Assertions.assertEquals(StructuredData.EOF, stream.next());
		Assertions.assertEquals(StructuredData.EOF, stream.next());
	}

	@Test
	public void selectsFileFormatAndAllowsExplicitOverride() throws Exception {
		Path json5 = directory.resolve("config.JSON5");
		Files.writeString(json5, "{port:42,}");
		Assertions.assertEquals(42, ((ObjectElement) Jankson.read(json5)).getPrimitive("port").asInt().orElseThrow());
		Path json = directory.resolve("config.json");
		Files.writeString(json, "{port:42,}");
		Assertions.assertThrows(IOException.class, () -> Jankson.read(json));
		Assertions.assertEquals(42, ((ObjectElement) Jankson.read(json, JsonFormat.JSON5)).getPrimitive("port").asInt().orElseThrow());
		Path hjson = directory.resolve("config.hjson");
		Files.writeString(hjson, "port-number: 42\n");
		Assertions.assertTrue(((ObjectElement) Jankson.read(hjson)).containsKey("port-number"));
		Path jsonc = directory.resolve("config.JSONC");
		Files.writeString(jsonc, "{/*comment*/\"port\":42}");
		Assertions.assertEquals(42, ((ObjectElement) Jankson.read(jsonc)).getPrimitive("port").asInt().orElseThrow());
		Path unknown = directory.resolve("config.conf");
		Files.writeString(unknown, "port: 42");
		Assertions.assertThrows(IllegalArgumentException.class, () -> Jankson.read(unknown));
		Assertions.assertTrue(((ObjectElement) Jankson.read(unknown, JsonFormat.HJSON)).containsKey("port"));
		Assertions.assertThrows(IllegalArgumentException.class, () -> JsonFormat.fromPath(Path.of("config.json.backup")));
		Assertions.assertThrows(IllegalArgumentException.class, () -> JsonFormat.fromPath(Path.of("config")));
	}

	@ParameterizedTest
	@EnumSource(JsonFormat.class)
	public void writesUtf8FilesAndProtectsExistingContentsOnSerializationErrors(JsonFormat format) throws Exception {
		Path file = directory.resolve("config." + format.name().toLowerCase(java.util.Locale.ROOT));
		ObjectElement original = new ObjectElement();
		original.put("ключ", PrimitiveElement.of("😀"));
		Jankson.write(original, file);
		Assertions.assertEquals("😀", ((ObjectElement) Jankson.read(file)).getPrimitive("ключ").asString().orElseThrow());
		if (format != JsonFormat.JSON5) {
			String before = Files.readString(file);
			Assertions.assertThrows(IOException.class, () -> Jankson.write(PrimitiveElement.of(Double.NaN), file));
			Assertions.assertEquals(before, Files.readString(file));
		}
		Path explicit = directory.resolve("settings.conf");
		Jankson.write(original, explicit, format);
		Assertions.assertTrue(((ObjectElement) Jankson.read(explicit, format)).containsKey("ключ"));
	}

	@Test
	public void errorReportsLineAndColumn() {
		IOException error = Assertions.assertThrows(IOException.class, () -> Jankson.read("{\r\n\"a\":1,\r\n}", JsonFormat.JSON));
		SyntaxError syntax = (SyntaxError) error.getCause();
		Assertions.assertTrue(syntax.getLineMessage().contains("line 3"), syntax.getLineMessage());
	}

	@ParameterizedTest
	@EnumSource(JsonFormat.class)
	public void rejectsMalformedUtf8AndDoesNotGuessAnotherFormat(JsonFormat format) throws Exception {
		byte[] invalid = new byte[]{'"', (byte) 0xC0, (byte) 0xAF, '"'};
		Assertions.assertThrows(IOException.class, () -> Jankson.read(new ByteArrayInputStream(invalid), format));
		Path file = directory.resolve("bad." + format.name().toLowerCase(java.util.Locale.ROOT));
		Files.write(file, invalid);
		Assertions.assertThrows(IOException.class, () -> Jankson.read(file));
	}
}
