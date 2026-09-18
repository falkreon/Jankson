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

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.io.Serializable;
import java.io.StringReader;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;

import blue.endless.jankson.api.Jankson;
import blue.endless.jankson.api.SyntaxError;
import blue.endless.jankson.api.annotation.Deserializer;
import blue.endless.jankson.api.annotation.Immutable;
import blue.endless.jankson.api.annotation.SerializedName;
import blue.endless.jankson.api.document.PrimitiveElement;
import blue.endless.jankson.api.io.ObjectReaderFactory;
import blue.endless.jankson.api.io.ObjectWriter;
import blue.endless.jankson.api.io.StructuredDataReader;
import blue.endless.jankson.api.io.ValueElementReader;
import blue.endless.jankson.api.io.json.JsonReader;
import blue.endless.jankson.api.io.json.JsonWriterOptions;

class ReviewedMappingRegressionTests {
	private static class Types {
		SortedMap<BigDecimal, String> decimals;
		Map<BigDecimal, String> hashDecimals;
		SortedMap<String, String> strings;
		List<String> stringList;
		Box<String> box;
		BoundedBox<String> bounded;
		BoundedBox<Object> invalidBound;
		BoundedBox<NonSerializableComparable> invalidIntersection;
		Pair<String, Integer> inconsistent;
		ArrayBox<String> arrayBox;
		WrongParameterBox<String> wrongParameter;
		FixedReturnBox<String> wrongReturn;
		UnmarkedBox<String> unmarked;
	}

	private static Type type(String field) throws Exception {
		return Types.class.getDeclaredField(field).getGenericType();
	}

	private static <T> T read(String json, Type type) throws Exception {
		ObjectWriter<T> writer = new ObjectWriter<>(type);
		new JsonReader(new StringReader(json)).transferTo(writer);
		return writer.toObject();
	}

	private static <T> T update(String json, Type type, T target) throws Exception {
		ObjectWriter<T> writer = new ObjectWriter<>(type, target);
		new JsonReader(new StringReader(json)).transferTo(writer);
		return writer.toObject();
	}

	@Test
	void sortedMapInputUsesComparatorEquivalenceButHashMapsKeepEquals() throws Exception {
		IOException error = assertThrows(IOException.class,
				() -> read("{'1.0': 'first', '1.00': 'second'}", type("decimals")));
		assertInstanceOf(SyntaxError.class, error.getCause());
		assertTrue(error.getMessage().contains("1.0"));
		assertTrue(error.getMessage().contains("1.00"));
		Map<BigDecimal, String> hash = read("{'1.0': 'first', '1.00': 'second'}", type("hashDecimals"));
		assertEquals(2, hash.size());
	}

	@Test
	void existingSortedMapAllowsOneUpdateAndUsesItsCustomComparator() throws Exception {
		SortedMap<BigDecimal, String> decimals = new TreeMap<>();
		decimals.put(new BigDecimal("1.0"), "old");
		assertSame(decimals, update("{'1.00': 'new'}", type("decimals"), decimals));
		assertEquals(1, decimals.size());
		assertEquals("new", decimals.get(new BigDecimal("1")));
		assertThrows(IOException.class, () -> update("{'1.0': 'a', '1.00': 'b'}", type("decimals"), decimals));

		SortedMap<String, String> strings = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		strings.put("KEY", "old");
		assertSame(strings, update("{key: 'new'}", type("strings"), strings));
		assertEquals("new", strings.get("KEY"));
		assertThrows(IOException.class, () -> update("{key: 'a', KEY: 'b'}", type("strings"), strings));
	}

	private static class Node {}

	private static StructuredDataReader scalar(String value) {
		return ValueElementReader.of(PrimitiveElement.of(value));
	}

	@Test
	void sameFactoryCanEagerlyDelegateTheSameObjectToAnotherStrategy() throws Exception {
		ObjectReaderFactory factory = new ObjectReaderFactory();
		factory.register((Type) Node.class, value -> factory.getReader((Type) Object.class, value));
		factory.register((Type) Object.class, value -> scalar("delegated"));
		assertEquals("\"delegated\"", Jankson.writeJsonString(new Node(), factory, JsonWriterOptions.ONE_LINE));
	}

	@Test
	void eagerStrategyCyclesAndFailuresCleanUpBeforeReuse() throws Exception {
		Node node = new Node();
		ObjectReaderFactory factory = new ObjectReaderFactory();
		AtomicBoolean cycle = new AtomicBoolean(true);
		factory.register((Type) Node.class, value -> factory.getReader((Type) Object.class, value));
		factory.register((Type) Object.class,
				value -> cycle.get() ? factory.getReader((Type) Node.class, value) : scalar("recovered"));
		assertThrows(IOException.class, () -> Jankson.writeJsonString(node, factory, JsonWriterOptions.ONE_LINE));
		cycle.set(false);
		assertEquals("\"recovered\"", Jankson.writeJsonString(node, factory, JsonWriterOptions.ONE_LINE));

		AtomicBoolean fail = new AtomicBoolean(true);
		factory.register((Type) Object.class, value -> {
			if (fail.getAndSet(false)) throw new IllegalStateException("factory failure");
			return scalar("retry");
		});
		assertThrows(IllegalStateException.class, () -> factory.getReader(node));
		assertEquals("\"retry\"", Jankson.writeJsonString(node, factory, JsonWriterOptions.ONE_LINE));
	}

	private static class Base<T> { T inherited; }
	private static class Child<T> extends Base<T> {
		T childGeneric;
		String extra = "subclass";
	}
	private static class Holder { Base<List<String>> value; }

	@Test
	void runtimeSubclassRetainsDeclaredAncestorBindingsAndSubclassProperties() throws Exception {
		Child<List<String>> child = new Child<>();
		child.inherited = List.of("inherited");
		child.childGeneric = List.of("child");
		Holder holder = new Holder();
		holder.value = child;
		ObjectReaderFactory factory = new ObjectReaderFactory();
		factory.setPrecise(true);
		factory.registerSerializer(type("stringList"), value -> PrimitiveElement.of("typed:"+((List<?>) value).getFirst()));
		String json = Jankson.writeJsonString(holder, factory, JsonWriterOptions.ONE_LINE);
		Map<String, Object> result = read(json, Map.class);
		assertEquals(Map.of("inherited", "typed:inherited", "childGeneric", "typed:child", "extra", "subclass"), result.get("value"));
	}

	private static class Box<T> {
		List<T> values;
		private Box() {}
		@Deserializer private static <U> Box<U> create(@SerializedName("values") List<U> values) {
			Box<U> box = new Box<>();
			box.values = values;
			return box;
		}
	}
	private static class BoundedBox<T> {
		List<T> values;
		private BoundedBox() {}
		@Deserializer private static <U extends Comparable<U> & Serializable> BoundedBox<U> create(
				@SerializedName("values") List<U> values) {
			BoundedBox<U> box = new BoundedBox<>();
			box.values = values;
			return box;
		}
	}
	private static class NonSerializableComparable implements Comparable<NonSerializableComparable> {
		@Override public int compareTo(NonSerializableComparable other) { return 0; }
	}
	private static class Pair<A, B> {
		A first;
		B second;
		private Pair() {}
		@Deserializer private static <U> Pair<U, U> create(
				@SerializedName("first") U first, @SerializedName("second") U second) {
			throw new AssertionError("Inconsistent return-type constraints must reject this method");
		}
	}
	private static class ArrayBox<T> {
		T[] values;
		private ArrayBox() {}
		@Deserializer private static <U> ArrayBox<U> create(@SerializedName("values") U[] values) {
			ArrayBox<U> box = new ArrayBox<>();
			box.values = values;
			return box;
		}
	}
	private static class WrongParameterBox<T> {
		List<T> values;
		private WrongParameterBox() {}
		@Deserializer private static <U> WrongParameterBox<U> create(@SerializedName("values") List<Integer> values) {
			throw new AssertionError("Return inference must not erase parameter constraints");
		}
	}
	private static class FixedReturnBox<T> {
		List<T> values;
		private FixedReturnBox() {}
		@Deserializer private static FixedReturnBox<Integer> create(@SerializedName("values") List<String> values) {
			throw new AssertionError("A concrete return type must agree with the requested target");
		}
	}
	@Immutable private static class UnmarkedBox<T> {
		List<T> values;
		private UnmarkedBox() {}
		private static <U> UnmarkedBox<U> create(@SerializedName("values") List<U> values) {
			UnmarkedBox<U> box = new UnmarkedBox<>();
			box.values = values;
			return box;
		}
	}

	@Test
	void genericFactoriesInferReturnVariablesAndResolveNestedAndArrayParameters() throws Exception {
		Box<String> box = read("{values: ['a', 'b']}", type("box"));
		assertEquals(List.of("a", "b"), box.values);
		BoundedBox<String> bounded = read("{values: ['a']}", type("bounded"));
		assertEquals(List.of("a"), bounded.values);
		ArrayBox<String> array = read("{values: ['a']}", type("arrayBox"));
		assertArrayEquals(new String[] {"a"}, array.values);
		assertEquals(String[].class, array.values.getClass());
		UnmarkedBox<String> unmarked = read("{values: ['a']}", type("unmarked"));
		assertEquals(List.of("a"), unmarked.values);
	}

	@Test
	void genericFactoriesEnforceAllBoundsAndRepeatedReturnConstraints() throws Exception {
		for (String field : List.of("invalidBound", "invalidIntersection", "inconsistent", "wrongParameter", "wrongReturn")) {
			IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
					() -> read("{}", type(field)));
			assertTrue(error.getMessage().contains("@Deserializer"));
		}
	}

	@Immutable private static class LowerFirst {
		List<String> values;
		transient String selected;
		private LowerFirst() {}
		private static LowerFirst narrow(@SerializedName("values") List<? super String> values) {
			LowerFirst result = new LowerFirst(); result.selected = "lower"; return result;
		}
		private static LowerFirst broad(@SerializedName("values") List<?> values) {
			throw new AssertionError("Unbounded wildcard must not dominate lower bounded wildcard");
		}
	}
	@Immutable private static class LowerLast {
		List<String> values;
		transient String selected;
		private LowerLast() {}
		private static LowerLast broad(@SerializedName("values") List<?> values) {
			throw new AssertionError("Declaration order must not select the broad factory");
		}
		private static LowerLast narrow(@SerializedName("values") List<? super String> values) {
			LowerLast result = new LowerLast(); result.selected = "lower"; return result;
		}
	}
	@Immutable private static class LowerRange {
		List<Object> values;
		transient String selected;
		private LowerRange() {}
		private static LowerRange broad(@SerializedName("values") List<? super String> values) {
			throw new AssertionError("Lower-bound containment runs in reverse");
		}
		private static LowerRange narrow(@SerializedName("values") List<? super CharSequence> values) {
			LowerRange result = new LowerRange(); result.selected = "charSequence"; return result;
		}
	}
	@Immutable private static class UpperRange {
		List<String> values;
		transient String selected;
		private UpperRange() {}
		private static UpperRange broad(@SerializedName("values") List<?> values) {
			throw new AssertionError("Upper bound must win");
		}
		private static UpperRange narrow(@SerializedName("values") List<? extends CharSequence> values) {
			UpperRange result = new UpperRange(); result.selected = "upper"; return result;
		}
	}
	@Immutable private static class PrimitiveWidening {
		int value;
		transient String selected;
		private PrimitiveWidening() {}
		private static PrimitiveWidening narrow(@SerializedName("value") long value) {
			PrimitiveWidening result = new PrimitiveWidening(); result.selected = "long"; return result;
		}
		private static PrimitiveWidening broad(@SerializedName("value") double value) {
			throw new AssertionError("long must remain more specific than double");
		}
	}

	@Test
	void wildcardSpecificityIsAsymmetricAndIndependentOfDeclarationOrder() throws Exception {
		assertEquals("lower", ReviewedMappingRegressionTests.<LowerFirst>read("{values: ['a']}", LowerFirst.class).selected);
		assertEquals("lower", ReviewedMappingRegressionTests.<LowerLast>read("{values: ['a']}", LowerLast.class).selected);
		assertEquals("charSequence", ReviewedMappingRegressionTests.<LowerRange>read("{values: ['a']}", LowerRange.class).selected);
		assertEquals("upper", ReviewedMappingRegressionTests.<UpperRange>read("{values: ['a']}", UpperRange.class).selected);
		assertEquals("long", ReviewedMappingRegressionTests.<PrimitiveWidening>read("{value: 42}", PrimitiveWidening.class).selected);
	}
}
