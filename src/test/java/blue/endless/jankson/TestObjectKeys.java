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
import java.io.StringReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import blue.endless.jankson.api.Jankson;
import blue.endless.jankson.api.SyntaxError;
import blue.endless.jankson.api.document.ObjectElement;
import blue.endless.jankson.api.document.PrimitiveElement;
import blue.endless.jankson.api.io.json.JsonReaderOptions;
import blue.endless.jankson.api.io.json.JsonWriterOptions;
import blue.endless.jankson.api.io.json.JsonFormat;
import blue.endless.jankson.api.io.style.WhitespaceStyle;

public class TestObjectKeys {
	private static JsonReaderOptions readerOptions(JsonFormat syntax) {
		return syntax == null ? JsonReaderOptions.UNSPECIFIED : syntax.readerOptions();
	}
	private static JsonWriterOptions writerOptions(JsonFormat syntax) {
		return syntax == null ? JsonWriterOptions.DEFAULTS : syntax.writerOptions();
	}

	private static String writeKey(String key, JsonFormat syntax, boolean unquoted) throws Exception {
		ObjectElement object = new ObjectElement();
		object.put(key, PrimitiveElement.of(42));
		return Jankson.toJsonString(object, writerOptions(syntax).asBuilder()
				.setWhitespace(WhitespaceStyle.COMPACT)
				.setUnquotedKeys(unquoted && syntax != JsonFormat.JSON && syntax != JsonFormat.JSONC).build());
	}

	private static void rejects(String text, JsonReaderOptions options) {
		IOException error = Assertions.assertThrows(IOException.class, () -> Jankson.readJsonObject(text, options));
		Assertions.assertInstanceOf(SyntaxError.class, error.getCause());
	}

	static Stream<Arguments> representations() {
		return Stream.of(
				Arguments.of(JsonFormat.JSON, "host", "\"host\""),
				Arguments.of(JsonFormat.JSONC, "host", "\"host\""),
				Arguments.of(JsonFormat.JSON5, "host", "host"),
				Arguments.of(JsonFormat.JSON5, "port-number", "\"port-number\""),
				Arguments.of(JsonFormat.HJSON, "port-number", "port-number"),
				Arguments.of(null, "port-number", "port-number"),
				Arguments.of(JsonFormat.JSON5, "123", "\"123\""),
				Arguments.of(JsonFormat.HJSON, "123", "123"),
				Arguments.of(JsonFormat.JSON5, "$schema", "$schema"),
				Arguments.of(JsonFormat.JSON5, "ключ", "ключ"),
				Arguments.of(JsonFormat.HJSON, "firstli\\ne", "firstli\\ne"),
				Arguments.of(JsonFormat.HJSON, "firstli\\\\ne", "firstli\\\\ne"),
				Arguments.of(JsonFormat.HJSON, "firstli\\u006Ee", "firstli\\u006Ee"),
				Arguments.of(JsonFormat.JSON5, "firstli\\ne", "\"firstli\\\\ne\""),
				Arguments.of(JsonFormat.JSON5, "firstli\\u006Ee", "\"firstli\\\\u006Ee\""),
				Arguments.of(JsonFormat.HJSON, "user name", "\"user name\""),
				Arguments.of(JsonFormat.HJSON, "test:key", "\"test:key\""),
				Arguments.of(JsonFormat.HJSON, "", "\"\""),
				Arguments.of(JsonFormat.HJSON, "#comment", "\"#comment\""),
				Arguments.of(JsonFormat.HJSON, "//comment", "\"//comment\""),
				Arguments.of(JsonFormat.HJSON, "/*comment*/", "\"/*comment*/\""),
				Arguments.of(JsonFormat.HJSON, "a#b", "a#b"),
				Arguments.of(JsonFormat.HJSON, "a/*b*/", "a/*b*/"),
				Arguments.of(JsonFormat.HJSON, "a\"b", "a\"b"),
				Arguments.of(JsonFormat.HJSON, "\"a", "\"\\\"a\""),
				Arguments.of(null, "firstli\\ne", "\"firstli\\\\ne\""));
	}

	@ParameterizedTest
	@MethodSource("representations")
	public void writesExactRepresentation(JsonFormat syntax, String key, String expectedKey) throws Exception {
		String text = writeKey(key, syntax, true);
		Assertions.assertEquals("{" + expectedKey + ":42}", text);
		ObjectElement restored = Jankson.readJsonObject(text, readerOptions(syntax));
		Assertions.assertEquals(1, restored.size());
		Assertions.assertEquals(42, restored.getPrimitive(key).asInt().orElseThrow());
	}

	@ParameterizedTest
	@EnumSource(JsonFormat.class)
	@NullSource
	public void quotesAndEscapesEvenWhenUnquotedIsSupported(JsonFormat syntax) throws Exception {
		String key = "a\"b\\c\n";
		Assertions.assertEquals("{\"a\\\"b\\\\c\\n\":42}", writeKey(key, syntax, false));
		Assertions.assertEquals("{\"\\ud83d\\ude00\":42}", writeKey("😀", syntax, false));
	}

	@ParameterizedTest
	@EnumSource(JsonFormat.class)
	@NullSource
	public void preservesAdversarialKeysAndOrder(JsonFormat syntax) throws Exception {
		List<String> keys = new ArrayList<>(List.of("", "firstline", "firstli\\ne", "firstli\\\\ne",
				"firstli\\u006Ee", "a\"b", "name\":0,\"injected", "user name", "test:key", "a=b",
				"a{b", "a}b", "a[b", "a]b", "a,b", "ключ", "😀", "\uD800", "\uDC00",
				"#key", "//key", "/*key*/", "'key", "a\u2028b", "a\u2029b", "a\u00a0b"));
		for (int i = 0; i < 32; i++) keys.add("control" + (char) i);
		ObjectElement original = new ObjectElement();
		for (int i = 0; i < keys.size(); i++) original.put(keys.get(i), PrimitiveElement.of(i));
		String text = Jankson.toJsonString(original, writerOptions(syntax));
		ObjectElement restored = Jankson.readJsonObject(text, readerOptions(syntax));
		Assertions.assertEquals(keys, new ArrayList<>(restored.keySet()));
		for (int i = 0; i < keys.size(); i++) Assertions.assertEquals(i, restored.getPrimitive(keys.get(i)).asInt().orElseThrow());
	}

	@ParameterizedTest
	@ValueSource(strings = {"firstli\\u006Ee", "\\u0066irstline"})
	public void decodesJson5IdentifierEscapes(String key) throws Exception {
		ObjectElement object = Jankson.readJsonObject("{" + key + ":42}", readerOptions(JsonFormat.JSON5));
		Assertions.assertEquals(List.of("firstline"), new ArrayList<>(object.keySet()));
	}

	@ParameterizedTest
	@ValueSource(strings = {"port-number", "123", "\\u0030abc", "a\\u002Db", "a\\u005Cb",
			"a\\u000Ab", "a\\n", "a\\\\b", "a\\u00GG", "a\\u１２３４", "a\\u12", "a\\U006E", "a\\u{006E}", "\u0301a", "a\\uD800"})
	public void rejectsInvalidJson5Identifiers(String key) {
		rejects("{" + key + ":42}", readerOptions(JsonFormat.JSON5));
	}

	@ParameterizedTest
	@ValueSource(strings = {"a\u0301", "a\u200c", "a\u200d", "$schema", "_name", "ключ", "true", "null"})
	public void acceptsJson5IdentifierNames(String key) throws Exception {
		Assertions.assertTrue(Jankson.readJsonObject("{" + key + ":42}", readerOptions(JsonFormat.JSON5)).containsKey(key));
	}

	@ParameterizedTest
	@ValueSource(strings = {"{host:42}", "{'host':42}", "{\"a\\x41\":42}", "{\"a\\v\":42}",
			"{\"a\\'\":42}", "{\"a\\q\":42}", "{\"a\t\":42}", "{\"a\\u12\":42}"})
	public void rejectsNonJsonKeySyntax(String text) {
		for (JsonFormat format : List.of(JsonFormat.JSON, JsonFormat.JSONC)) rejects(text, readerOptions(format));
	}

	@Test
	public void distinguishesLiteralHjsonKeysFromQuotedEscapes() throws Exception {
		ObjectElement object = Jankson.readJsonObject("{firstli\\ne:1,\"firstli\\ne\":2,firstli\\\\ne:3}", readerOptions(JsonFormat.HJSON));
		Assertions.assertEquals(3, object.size());
		Assertions.assertEquals(1, object.getPrimitive("firstli\\ne").asInt().orElseThrow());
		Assertions.assertEquals(2, object.getPrimitive("firstli\ne").asInt().orElseThrow());
		Assertions.assertEquals(3, object.getPrimitive("firstli\\\\ne").asInt().orElseThrow());
		ObjectElement quoted = Jankson.readJsonObject("{\"firstli\\\\ne\":4}", readerOptions(JsonFormat.HJSON));
		Assertions.assertEquals(4, quoted.getPrimitive("firstli\\ne").asInt().orElseThrow());
	}

	@ParameterizedTest
	@ValueSource(strings = {"{a b:42}", "{a[b:42}", "{a,b:42}", "{\"a\\x41\":42}", "{\"a\\q\":42}"})
	public void rejectsInvalidHjsonKeys(String text) {
		rejects(text, readerOptions(JsonFormat.HJSON));
	}

	@Test
	public void readsQuotedJson5Keys() throws Exception {
		ObjectElement object = Jankson.readJsonObject("{'a\\x62':1, 'c\\\r\nd':2, 'e\\v':3}", readerOptions(JsonFormat.JSON5));
		Assertions.assertTrue(object.containsKey("ab"));
		Assertions.assertTrue(object.containsKey("cd"));
		Assertions.assertTrue(object.containsKey("e\u000b"));
	}

	@ParameterizedTest
	@EnumSource(JsonFormat.class)
	@NullSource
	public void honorsDisablingUnquotedKeys(JsonFormat syntax) throws Exception {
		var options = readerOptions(syntax).asBuilder().setUnquotedKeys(false).build();
		rejects("{host:42}", options);
		Assertions.assertTrue(Jankson.readJsonObject("{\"host\":42}", options).containsKey("host"));
	}

	@Test
	public void honorsOptionsInBothTypedOverloadsAndNestedObjects() throws Exception {
		record Config(int port) {}
		var options = readerOptions(JsonFormat.JSON5);
		String text = "{p\\u006Frt:42}";
		Assertions.assertEquals(new Config(42), Jankson.readJson(new StringReader(text), options, Config.class));
		Assertions.assertEquals(new Config(42), Jankson.<Config>readJson(new StringReader(text), options, (Type) Config.class));
		Assertions.assertTrue(Jankson.readJsonObject("{outer:[{p\\u006Frt:42}]}", options)
				.getArray("outer").getObject(0).containsKey("port"));
		var quoted = options.asBuilder().setUnquotedKeys(false).build();
		Assertions.assertThrows(IOException.class, () -> Jankson.readJson(new StringReader("{port:42}"), quoted, Config.class));
		Assertions.assertThrows(IOException.class, () -> Jankson.readJson(new StringReader("{port:42}"), quoted, (Type) Config.class));
	}

	@Test
	public void respectsCustomSeparatorAndOptionsCopies() throws Exception {
		var reader = JsonReaderOptions.builder().setKeyValueSeparator('=').build();
		var writer = JsonWriterOptions.MINIFIED.asBuilder()
				.setUnquotedKeys(true).setKeyValueSeparator('=').build();
		ObjectElement object = new ObjectElement();
		object.put("a=b", PrimitiveElement.of(42));
		String text = Jankson.toJsonString(object, writer.asBuilder().build());
		Assertions.assertEquals("{\"a=b\"=42}", text);
		Assertions.assertEquals(42, Jankson.readJsonObject(text, reader.asBuilder().build()).getPrimitive("a=b").asInt().orElseThrow());
		Assertions.assertNull(reader.asBuilder().build().getFormat());
		Assertions.assertNull(writer.asBuilder().build().getFormat());
		Assertions.assertTrue(Jankson.readJsonObject("{port-number=42}", reader).containsKey("port-number"));
		String ini = Jankson.toJsonString(object, JsonWriterOptions.INI_SON);
		Assertions.assertTrue(ini.contains("\"a=b\"= 42"), ini);
	}
}
