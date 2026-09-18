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

import java.io.StringReader;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import blue.endless.jankson.api.io.ObjectReaderFactory;
import blue.endless.jankson.api.io.ObjectWriter;
import blue.endless.jankson.api.io.StructuredData;
import blue.endless.jankson.api.io.StructuredDataReader;
import blue.endless.jankson.api.io.json.JsonReader;
import blue.endless.jankson.impl.io.objectwriter.ArrayDeserializer;
import blue.endless.jankson.impl.magic.ClassHierarchy;
import blue.endless.jankson.impl.magic.ReflectiveProperty;
import blue.endless.jankson.impl.magic.SyntheticType;

public class GenericResolutionRegressionTests {
	private static class Base<T> { T value; }
	private static class Child<T> extends Base<List<T>> {}
	private static class Middle<T> extends Child<Map<String, T>> {}
	private static class Leaf extends Middle<Integer> {}
	private static class ShadowChild<T> extends Base<Integer> { T other; }
	private static class ArrayBase<T> { T[] values; }
	private static class StringArrays extends ArrayBase<String> {}
	private static class ListArrays extends ArrayBase<List<String>> {}
	private static class Recursive<T extends Comparable<T>> { T value; }
	private static class Repeated<T> { Map<T, T> values; }
	private static class StringList extends ArrayList<String> {}
	private static class IntegerMap extends HashMap<String, Integer> {}
	private static class Outer<T> {
		class Inner<U> { T outerValue; U innerValue; }
		class OwnerOnly { T outerValue; }
	}
	private static class OwnerHolder<T> { Outer<T>.Inner<Integer> member; }
	private static class Types {
		List<String>[] lists;
		Map<String, Integer>[] maps;
		Outer<String>.Inner<Integer> member;
		Outer<String>.OwnerOnly ownerOnly;
		List<? extends String> wildcard;
	}
	private static class WildcardHolder<T> { List<? extends T> wildcard; }
	private static class RebasingOuter<T> {
		T inherited;
		class Inner extends RebasingOuter<Integer> { T enclosing; }
		class SymbolicInner extends RebasingOuter<List<T>> { T enclosing; }
	}
	private static class RebasingDescendant<T> extends RebasingOuter<T>.Inner {
		RebasingDescendant(RebasingOuter<T> owner) { owner.super(); }
	}
	private static class RebasingTypes<T> {
		RebasingOuter<String>.Inner exact;
		RebasingOuter<T>.SymbolicInner symbolic;
	}

	@Test
	public void rebasesAnOwnerDeclarationWhenItReappearsAsASuperclass() throws Exception {
		// Metadata only: constructing a non-static inner object is a separate concern.
		Type exact = RebasingTypes.class.getDeclaredField("exact").getGenericType();
		Assertions.assertEquals(Integer.class, property(exact, "inherited"));
		Assertions.assertEquals(String.class, property(exact, "enclosing"));
		Type descendant = new SyntheticType<>(RebasingDescendant.class, String.class);
		Assertions.assertEquals(Integer.class, property(descendant, "inherited"));
		Assertions.assertEquals(String.class, property(descendant, "enclosing"));
	}

	@Test
	public void preservesSymbolicOwnerArgumentsUntilFinalSubstitution() throws Exception {
		Type symbolic = RebasingTypes.class.getDeclaredField("symbolic").getGenericType();
		TypeVariable<?> variable = RebasingTypes.class.getTypeParameters()[0];
		TypeVariable<?> outer = RebasingOuter.class.getTypeParameters()[0];
		Assertions.assertEquals(new SyntheticType<>(List.class, variable),
				ClassHierarchy.getTypeBindings(symbolic, RebasingOuter.class).get(outer));
		Assertions.assertEquals(new SyntheticType<>(List.class, Object.class), property(symbolic, "inherited"));
		Assertions.assertEquals(Object.class, property(symbolic, "enclosing"));
		Type concrete = ClassHierarchy.substitute(symbolic, Map.of(variable, String.class));
		Assertions.assertEquals(new SyntheticType<>(List.class, String.class), property(concrete, "inherited"));
		Assertions.assertEquals(String.class, property(concrete, "enclosing"));
	}

	@Test
	public void resolvesRepeatedVariableNamesByDeclaration() throws Exception {
		Type type = new SyntheticType<>(Child.class, String.class);
		Assertions.assertEquals(new SyntheticType<>(List.class, String.class), property(type, "value"));
		Assertions.assertEquals(String.class, ClassHierarchy.getActualTypeArguments(type, Child.class).get("T"));
		Assertions.assertEquals(new SyntheticType<>(List.class, String.class),
				ClassHierarchy.getActualTypeArguments(type, Base.class).get("T"));
		Child<String> child = read("{value: ['a', 'b']}", type);
		Assertions.assertEquals(List.of("a", "b"), child.value);

		Type shadow = new SyntheticType<>(ShadowChild.class, String.class);
		Assertions.assertEquals(Integer.class, property(shadow, "value"));
		Assertions.assertEquals(String.class, property(shadow, "other"));
	}

	@Test
	public void resolvesMultilevelNestedInheritance() throws Exception {
		Type map = new SyntheticType<>(Map.class, String.class, Integer.class);
		Assertions.assertEquals(new SyntheticType<>(List.class, map), property(Leaf.class, "value"));
		Leaf leaf = read("{value: [{count: 7}]}", Leaf.class);
		Assertions.assertEquals(List.of(Map.of("count", 7)), leaf.value);
		Assertions.assertInstanceOf(Integer.class, leaf.value.get(0).get("count"));
	}

	@Test
	public void serializesRawRuntimeChildWithoutRecursiveMetadataExpansion() {
		Assertions.assertTimeoutPreemptively(Duration.ofSeconds(5), () -> {
			Assertions.assertEquals(new SyntheticType<>(List.class, Object.class), property(Child.class, "value"));
			Child<String> child = new Child<>();
			child.value = List.of("runtime");
			StructuredDataReader reader = new ObjectReaderFactory().getReader(child);
			List<StructuredData.Type> kinds = new ArrayList<>();
			List<Object> primitives = new ArrayList<>();
			while (reader.hasNext()) {
				StructuredData event = reader.next();
				if (event.type() == StructuredData.Type.EOF) continue;
				kinds.add(event.type());
				if (event.type() == StructuredData.Type.PRIMITIVE) primitives.add(event.value());
			}
			Assertions.assertEquals(List.of(StructuredData.Type.OBJECT_START, StructuredData.Type.OBJECT_KEY,
					StructuredData.Type.ARRAY_START, StructuredData.Type.PRIMITIVE,
					StructuredData.Type.ARRAY_END, StructuredData.Type.OBJECT_END), kinds);
			Assertions.assertEquals(List.of("runtime"), primitives);
		});
	}

	@Test
	public void deserializesParameterizedListArrayComponents() throws Exception {
		Type type = declared("lists");
		List<String>[] value = read("[['a', 'b'], [], null]", type);
		Assertions.assertEquals(List[].class, value.getClass());
		Assertions.assertEquals(List.of("a", "b"), value[0]);
		Assertions.assertInstanceOf(String.class, value[0].get(0));
		Assertions.assertTrue(value[1].isEmpty());
		Assertions.assertNull(value[2]);
		Assertions.assertEquals(0, ((List<?>[]) read("[]", type)).length);
	}

	@Test
	public void deserializesParameterizedMapArrayComponents() throws Exception {
		Map<String, Integer>[] value = read("[{count: 7}, {}, null]", declared("maps"));
		Assertions.assertEquals(Map[].class, value.getClass());
		Assertions.assertEquals(Map.of("count", 7), value[0]);
		Assertions.assertInstanceOf(Integer.class, value[0].get("count"));
		Assertions.assertTrue(value[1].isEmpty());
		Assertions.assertNull(value[2]);
	}

	@Test
	public void preservesGenericArrayTypeAfterInheritedSubstitution() throws Exception {
		Type type = property(ListArrays.class, "values");
		GenericArrayType array = Assertions.assertInstanceOf(GenericArrayType.class, type);
		Assertions.assertEquals(new SyntheticType<>(List.class, String.class), array.getGenericComponentType());
		Assertions.assertEquals(declared("lists"), type);
		Assertions.assertEquals(type, declared("lists"));
		Assertions.assertEquals(declared("lists").hashCode(), type.hashCode());
		ListArrays value = read("{values: [['nested']]}", ListArrays.class);
		Assertions.assertEquals(List[].class, value.values.getClass());
		Assertions.assertEquals(List.of("nested"), value.values[0]);
	}

	@Test
	public void resolvesVariableArraysToConcreteStringArrays() throws Exception {
		Assertions.assertEquals(String[].class, property(StringArrays.class, "values"));
		StringArrays value = read("{values: ['a', 'b']}", StringArrays.class);
		Assertions.assertEquals(String[].class, value.values.getClass());
		Assertions.assertArrayEquals(new String[] {"a", "b"}, value.values);
		Assertions.assertNotNull(ArrayDeserializer.class.getConstructor(Class.class));
		Assertions.assertArrayEquals(new int[] {1, 2}, (int[]) read("[1, 2]", int[].class));
		Assertions.assertArrayEquals(new String[] {"wrapped"},
				(String[]) read("['wrapped']", new SyntheticType<>(String[].class)));
	}

	@Test
	public void preservesParameterizedOwnersAndResolvesTheirProperties() throws Exception {
		Type resolved = property(new SyntheticType<>(OwnerHolder.class, String.class), "member");
		ParameterizedType member = Assertions.assertInstanceOf(ParameterizedType.class, resolved);
		Assertions.assertEquals(new SyntheticType<>(Outer.class, String.class), member.getOwnerType());
		Assertions.assertEquals(declared("member"), resolved);
		Assertions.assertEquals(resolved, declared("member"));
		Assertions.assertEquals(declared("member").hashCode(), resolved.hashCode());
		Assertions.assertEquals(String.class, property(resolved, "outerValue"));
		Assertions.assertEquals(Integer.class, property(resolved, "innerValue"));
		Assertions.assertEquals(String.class, property(declared("ownerOnly"), "outerValue"));
	}

	@Test
	public void usesObjectFallbackForRawAndRecursivelyBoundVariables() {
		Assertions.assertTimeoutPreemptively(Duration.ofSeconds(5), () -> {
			Assertions.assertEquals(Object.class, property(Base.class, "value"));
			Assertions.assertEquals(Object.class, property(Recursive.class, "value"));
			Assertions.assertEquals(Object.class,
					ClassHierarchy.getActualTypeArguments(Recursive.class, Recursive.class).get("T"));
			Assertions.assertEquals(String.class,
					property(new SyntheticType<>(Recursive.class, String.class), "value"));
		});
	}

	@Test
	public void breaksSelfMutualAndStructuralSubstitutionCycles() {
		Assertions.assertTimeoutPreemptively(Duration.ofSeconds(5), () -> {
			TypeVariable<?> base = Base.class.getTypeParameters()[0];
			TypeVariable<?> child = Child.class.getTypeParameters()[0];
			Assertions.assertEquals(Object.class, ClassHierarchy.substitute(base, Map.of(base, base)));
			Assertions.assertEquals(Object.class, ClassHierarchy.substitute(base, Map.of(base, child, child, base)));
			Assertions.assertEquals(new SyntheticType<>(List.class, Object.class),
					ClassHierarchy.substitute(base, Map.of(base, new SyntheticType<>(List.class, base))));
			Assertions.assertEquals(new SyntheticType<>(Map.class, String.class, String.class),
					property(new SyntheticType<>(Repeated.class, String.class), "values"));
		});
	}

	@Test
	public void resolvesConcreteCollectionSubclassArguments() {
		Assertions.assertEquals(String.class, ClassHierarchy.getCollectionTypeArgument(StringList.class));
		Assertions.assertEquals(new ClassHierarchy.MapTypeArguments(String.class, Integer.class),
				ClassHierarchy.getMapTypeArguments(IntegerMap.class));
		Assertions.assertEquals(Object.class, ClassHierarchy.getCollectionTypeArgument(List.class));
		Assertions.assertEquals(new ClassHierarchy.MapTypeArguments(Object.class, Object.class),
				ClassHierarchy.getMapTypeArguments(Map.class));
	}

	@Test
	public void substitutesVariablesInsideWildcardBounds() throws Exception {
		Type resolved = property(new SyntheticType<>(WildcardHolder.class, String.class), "wildcard");
		Assertions.assertEquals(declared("wildcard"), resolved);
		Assertions.assertEquals(resolved, declared("wildcard"));
		Assertions.assertEquals(declared("wildcard").hashCode(), resolved.hashCode());
	}

	@Test
	public void syntheticTypesProtectArgumentsAndHonorReflectiveEquality() throws Exception {
		Type[] arguments = {String.class};
		SyntheticType<?> list = new SyntheticType<>(List.class, arguments);
		arguments[0] = Integer.class;
		list.getActualTypeArguments()[0] = Integer.class;
		Type reflected = ((GenericArrayType) declared("lists")).getGenericComponentType();
		Assertions.assertEquals(reflected, list);
		Assertions.assertEquals(list, reflected);
		Assertions.assertEquals(reflected.hashCode(), list.hashCode());
		Assertions.assertEquals("java.util.List<java.lang.String>", list.getTypeName());
	}

	private static Type property(Type type, String name) {
		return ReflectiveProperty.of(type).stream().filter(it -> it.javaName().equals(name))
				.findFirst().orElseThrow().type();
	}

	private static Type declared(String name) throws NoSuchFieldException {
		return Types.class.getDeclaredField(name).getGenericType();
	}

	private static <T> T read(String json, Type type) throws Exception {
		ObjectWriter<T> writer = new ObjectWriter<>(type);
		new JsonReader(new StringReader(json)).transferTo(writer);
		return writer.toObject();
	}
}
