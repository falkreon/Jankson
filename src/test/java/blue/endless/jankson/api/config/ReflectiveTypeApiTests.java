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

package blue.endless.jankson.api.config;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Type;
import java.util.List;

import org.junit.jupiter.api.Test;

import blue.endless.jankson.api.document.ValueElement;
import blue.endless.jankson.api.io.ObjectReaderFactory;

class ReflectiveTypeApiTests {
	public record RuntimeValue(int number, String name) {}
	public List<RuntimeValue> runtimeValues;

	private static class GenericFields<T> {
		public List<T> values;
	}
	private static class GenericOwner<T> {
		class Member {}
	}

	@Test void rejectsUnresolvedGenericHelperCaptures() {
		IllegalArgumentException direct = assertThrows(IllegalArgumentException.class,
				() -> captureUnresolved());
		assertTrue(direct.getMessage().contains("Unresolved type variable 'T'"));

		IllegalArgumentException nested = assertThrows(IllegalArgumentException.class,
				() -> captureNestedUnresolved());
		assertTrue(nested.getMessage().contains("java.util.List<T>"));

		assertThrows(IllegalArgumentException.class, () -> captureUnresolvedArray());
		assertThrows(IllegalArgumentException.class, () -> captureUnresolvedUpperWildcard());
		assertThrows(IllegalArgumentException.class, () -> captureUnresolvedLowerWildcard());
		assertThrows(IllegalArgumentException.class, () -> captureUnresolvedOwner());
	}

	@Test void acceptsConcreteWildcardAndGenericArrayCaptures() {
		assertEquals(String.class, new TypeRef<String>() {}.type());
		assertEquals("java.util.List<? extends java.lang.Number>",
				new TypeRef<List<? extends Number>>() {}.type().getTypeName());
		assertEquals("java.util.List<java.lang.String>[]",
				new TypeRef<List<String>[]>() {}.type().getTypeName());
	}

	@Test void runtimeTypeOverloadsRejectUnresolvedVariables() throws Exception {
		Type unresolved = GenericFields.class.getDeclaredField("values").getGenericType();
		assertThrows(IllegalArgumentException.class, () -> ConfigCodecs.reflective(unresolved));
		assertThrows(IllegalArgumentException.class,
				() -> ConfigCodecs.reflective(unresolved, new ObjectReaderFactory()));
		assertThrows(IllegalArgumentException.class,
				() -> ConfigCodecs.reflective(unresolved, new ObjectReaderFactory(), 100));
	}

	@Test void runtimeFieldTypeCodecRoundTrips() throws Exception {
		Type type = ReflectiveTypeApiTests.class.getField("runtimeValues").getGenericType();
		ConfigCodec<List<RuntimeValue>> codec = ConfigCodecs.reflective(type);
		assertNotNull(ConfigCodecs.reflective(type, new ObjectReaderFactory()));
		assertNotNull(ConfigCodecs.reflective(type, new ObjectReaderFactory(), 100));

		List<RuntimeValue> expected = List.of(new RuntimeValue(1, "one"), new RuntimeValue(2, "two"));
		ValueElement encoded = codec.encode(expected);
		assertEquals(expected, codec.decode(encoded));
	}

	private static <T> TypeRef<T> captureUnresolved() {
		return new TypeRef<T>() {};
	}

	private static <T> TypeRef<List<T>> captureNestedUnresolved() {
		return new TypeRef<List<T>>() {};
	}

	private static <T> TypeRef<T[]> captureUnresolvedArray() {
		return new TypeRef<T[]>() {};
	}

	private static <T> TypeRef<List<? extends T>> captureUnresolvedUpperWildcard() {
		return new TypeRef<List<? extends T>>() {};
	}

	private static <T> TypeRef<List<? super T>> captureUnresolvedLowerWildcard() {
		return new TypeRef<List<? super T>>() {};
	}

	private static <T> TypeRef<GenericOwner<T>.Member> captureUnresolvedOwner() {
		return new TypeRef<GenericOwner<T>.Member>() {};
	}

}
