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

import java.io.StringReader;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import blue.endless.jankson.api.SyntaxError;
import blue.endless.jankson.api.annotation.Deserializer;
import blue.endless.jankson.api.annotation.Immutable;
import blue.endless.jankson.api.annotation.Mutable;
import blue.endless.jankson.api.annotation.MutatorFor;
import blue.endless.jankson.api.annotation.SerializedName;
import blue.endless.jankson.api.io.ObjectReaderFactory;
import blue.endless.jankson.api.io.ObjectWriter;
import blue.endless.jankson.api.io.StructuredData;
import blue.endless.jankson.api.io.json.JsonReader;
import blue.endless.jankson.impl.io.objectwriter.MapDeserializer;
import blue.endless.jankson.impl.io.objectwriter.RecordDeserializer;
import blue.endless.jankson.impl.io.objectwriter.factory.ObjectWrapper;

public class MappingStrategyRegressionTests {
	private static class MutableValue {
		private String known;
		private MutableValue() {}
	}

	private static class FactoryValue {
		@SerializedName("known") private String value;
		private FactoryValue() {}

		@Deserializer
		private static FactoryValue create(@SerializedName("known") String value) {
			FactoryValue result = new FactoryValue();
			result.value = "factory:"+value;
			return result;
		}
	}

	@Mutable
	private static class ExplicitMutable {
		private String known;
		private ExplicitMutable() {}

		@Deserializer
		private static ExplicitMutable create(@SerializedName("known") String value) {
			throw new AssertionError("@Mutable must take precedence over this factory");
		}
	}

	@Mutable
	private static class MutableWithInvalidFactory {
		private String known;
		private MutableWithInvalidFactory() {}

		@Deserializer
		private void invalid() {}
	}

	@Immutable
	private static class ExplicitImmutable {
		private final String known;
		private ExplicitImmutable() { known = "no-arg"; }
		public ExplicitImmutable(@SerializedName("known") String value) { known = "constructor:"+value; }
	}

	private static class AutomaticImmutable {
		private final String known;
		public AutomaticImmutable(@SerializedName("known") String value) { known = value; }
	}

	@Immutable
	private static class ImmutableFactory {
		private String known;
		private ImmutableFactory() {}
		public ImmutableFactory(@SerializedName("known") String value) { known = "constructor:"+value; }
		@Deserializer private static ImmutableFactory create(@SerializedName("known") String value) {
			ImmutableFactory result = new ImmutableFactory();
			result.known = "factory:"+value;
			return result;
		}
	}

	private static class NonStaticFactory {
		private String known;
		private NonStaticFactory() {}
		@Deserializer private NonStaticFactory create(@SerializedName("known") String value) { return this; }
	}

	private static class WrongReturnFactory {
		private String known;
		private WrongReturnFactory() {}
		@Deserializer private static String create(@SerializedName("known") String value) { return value; }
	}

	private static class WrongNameFactory {
		private String known;
		private WrongNameFactory() {}
		public WrongNameFactory(@SerializedName("known") String value) { known = value; }
		@Deserializer private static WrongNameFactory create(@SerializedName("missing") String value) { return null; }
	}

	private static class DuplicateParameterFactory {
		private String known;
		private DuplicateParameterFactory() {}
		@Deserializer private static DuplicateParameterFactory create(
				@SerializedName("known") String first, @SerializedName("known") String second) { return null; }
	}

	private static class WrongParameterFactory {
		private String known;
		private WrongParameterFactory() {}
		@Deserializer private static WrongParameterFactory create(@SerializedName("known") int value) { return null; }
	}

	private static class WideningFactory {
		private int known;
		private WideningFactory() {}
		@Deserializer private static WideningFactory create(@SerializedName("known") long value) {
			WideningFactory result = new WideningFactory();
			result.known = (int) value + 1;
			return result;
		}
	}

	private static class BoxedFactory {
		private int known;
		private BoxedFactory() {}
		@Deserializer private static BoxedFactory create(@SerializedName("known") Integer value) {
			BoxedFactory result = new BoxedFactory();
			result.known = value + 1;
			return result;
		}
	}

	private static class MutatorParent {
		@SerializedName("wire-value") private String value;
		@SerializedName("wire-name") private String name;

		@MutatorFor("value")
		private void replace(String value) { this.value = "mutator:"+value; }
		private void setName(String name) { this.name = "setter:"+name; }
	}

	private static class MutatorChild extends MutatorParent {
		private MutatorChild() {}
	}

	private static class HiddenParent {
		@SerializedName("parent") String value;
	}
	private static class ChildOnlySetter extends HiddenParent {
		@SerializedName("child") String value;
		private void setValue(String value) { this.value = "child:"+value; }
	}
	private static class PrivateSetterParent {
		@SerializedName("parent") String value;
		private void setValue(String value) { this.value = "parent:"+value; }
	}
	private static class ParentOnlySetter extends PrivateSetterParent {
		@SerializedName("child") String value;
	}
	private static class SeparateSetters extends PrivateSetterParent {
		@SerializedName("child") String value;
		private void setValue(String value) { this.value = "child:"+value; }
	}
	private static class AnnotatedHiddenParent {
		@SerializedName("parent") String value;
		@MutatorFor("value") private void replace(String value) { this.value = "parent:"+value; }
	}
	private static class AnnotatedHiddenChild extends AnnotatedHiddenParent {
		@SerializedName("child") String value;
		@MutatorFor("value") private void replace(String value) { this.value = "child:"+value; }
	}
	private static class InheritedFieldSetter extends HiddenParent {
		private void setValue(String value) { this.value = "subclass:"+value; }
	}
	private static class VirtualSetterParent {
		@SerializedName("parent") String value;
		@MutatorFor("value") public void replace(String value) { this.value = "parent:"+value; }
	}
	private static class SameFieldOverride extends VirtualSetterParent {
		@Override public void replace(String value) { this.value = "override:"+value; }
	}
	private static class ConventionalParent {
		String value;
		public void setValue(String value) { this.value = "parent:"+value; }
	}
	private static class ConventionalOverride extends ConventionalParent {
		@Override public void setValue(String value) { this.value = "override:"+value; }
	}
	private static class ConventionalCrossField extends ConventionalParent {
		@SerializedName("child") String value;
		@Override public void setValue(String value) { this.value = value; }
	}
	private static class CrossFieldOverride extends VirtualSetterParent {
		@SerializedName("child") String value;
		@Override public void replace(String value) { this.value = "child:"+value; }
	}
	private static class ExcludedFieldBoundary extends HiddenParent {
		transient String value;
		private void setValue(String value) { this.value = "excluded:"+value; }
	}
	private static class ExcludedOverrideBoundary extends VirtualSetterParent {
		transient String value;
		@Override public void replace(String value) { this.value = value; }
	}
	private static class StaticFieldBoundary extends HiddenParent {
		static String value;
		private void setValue(String value) { throw new AssertionError("Static field hides inherited field"); }
	}
	private static class AnnotatedParent {
		String value;
		@MutatorFor("value") private void replace(String value) { this.value = "annotation:"+value; }
	}
	private static class ConventionalChild extends AnnotatedParent {
		private void setValue(String value) { this.value = "conventional:"+value; }
	}
	private static class AnnotatedChild extends AnnotatedParent {
		@MutatorFor("value") private void change(String value) { this.value = "derived:"+value; }
	}
	private static class AmbiguousSetters {
		final String value = "initial";
		@MutatorFor("value") private void first(String value) {}
		@MutatorFor("value") private void second(String value) {}
		private void setValue(String value) {}
	}
	private static class GenericSetterParent<T> {
		@SerializedName("parent") T value;
		@MutatorFor("value") public void replace(T value) { this.value = value; }
	}
	private static class GenericSameField extends GenericSetterParent<String> {
		@Override public void replace(String value) { this.value = "bridge:"+value; }
	}
	private static class GenericCrossField extends GenericSetterParent<String> {
		@SerializedName("child") String value;
		@Override public void replace(String value) { this.value = value; }
	}
	private static class CovariantSetterParent {
		String value;
		@MutatorFor("value") public CovariantSetterParent replace(String value) { this.value = value; return this; }
	}
	private static class CovariantSetterChild extends CovariantSetterParent {
		@Override @MutatorFor("value") public CovariantSetterChild replace(String value) {
			this.value = "covariant:"+value;
			return this;
		}
	}
	private static class ExactSetterParameter {
		String value;
		final String fixed = "initial";
		private void setValue(Object value) { throw new AssertionError("Erased parameter must match exactly"); }
	}

	@Test
	public void hiddenFieldsHaveIndependentMutatorsInEitherInputOrder() throws Exception {
		for (String json : List.of("{parent: 'p', child: 'c'}", "{child: 'c', parent: 'p'}")) {
			ChildOnlySetter childOnly = read(json, ChildOnlySetter.class);
			assertEquals("child:c", childOnly.value);
			assertEquals("p", ((HiddenParent) childOnly).value);
			ParentOnlySetter parentOnly = read(json, ParentOnlySetter.class);
			assertEquals("c", parentOnly.value);
			assertEquals("parent:p", ((PrivateSetterParent) parentOnly).value);
			SeparateSetters separate = read(json, SeparateSetters.class);
			assertEquals("child:c", separate.value);
			assertEquals("parent:p", ((PrivateSetterParent) separate).value);
			AnnotatedHiddenChild annotated = read(json, AnnotatedHiddenChild.class);
			assertEquals("child:c", annotated.value);
			assertEquals("parent:p", ((AnnotatedHiddenParent) annotated).value);
		}
	}

	@Test
	public void preservesSameFieldDispatchAndSubclassSettersWithoutHiding() throws Exception {
		assertEquals("subclass:p", read("{parent: 'p'}", InheritedFieldSetter.class).value);
		assertEquals("override:p", read("{parent: 'p'}", SameFieldOverride.class).value);
		assertEquals("override:p", read("{value: 'p'}", ConventionalOverride.class).value);
		assertEquals("bridge:p", read("{parent: 'p'}", GenericSameField.class).value);
		assertEquals("covariant:p", read("{value: 'p'}", CovariantSetterChild.class).value);
	}

	@Test
	public void rejectsCrossFieldOverridesIncludingBridgesAndExcludedFields() {
		for (Class<?> type : List.of(CrossFieldOverride.class, ConventionalCrossField.class,
				GenericCrossField.class, ExcludedOverrideBoundary.class)) {
			IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> ObjectWrapper.of(type, null));
			assertTrue(error.getMessage().contains("crosses field boundary"));
			assertTrue(error.getMessage().contains("value"));
		}
		assertThrows(IllegalArgumentException.class,
				() -> ObjectWrapper.of(VirtualSetterParent.class, new CrossFieldOverride()));
	}

	@Test
	public void excludedFieldsStillHideAndInexactSettersDoNotQualify() throws Exception {
		ExcludedFieldBoundary excluded = read("{parent: 'p'}", ExcludedFieldBoundary.class);
		assertEquals("p", ((HiddenParent) excluded).value);
		assertNull(excluded.value);
		assertEquals("p", ((HiddenParent) read("{parent: 'p'}", StaticFieldBoundary.class)).value);
		ExactSetterParameter exact = read("{value: 'p', fixed: 'replacement'}", ExactSetterParameter.class);
		assertEquals("p", exact.value);
		assertEquals("initial", exact.fixed);
	}

	@Test
	public void annotationPriorityThenDeclarationDepthSelectMutatorsAndTiesFail() throws Exception {
		assertEquals("annotation:p", read("{value: 'p'}", ConventionalChild.class).value);
		assertEquals("derived:p", read("{value: 'p'}", AnnotatedChild.class).value);
		IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
				() -> ObjectWrapper.of(AmbiguousSetters.class, null));
		assertTrue(error.getMessage().contains("Ambiguous mutators"));
		assertTrue(error.getMessage().contains("first"));
		assertTrue(error.getMessage().contains("second"));
	}

	private record CountedRecord(@SerializedName("wire-value") String value) {
		private static int constructions;
		private CountedRecord { constructions++; }
	}

	private record EmptyRecord() {
		private static int constructions;
		private EmptyRecord { constructions++; }
	}

	private enum Mode {
		NORMAL, @SerializedName("slow-mode") SLOW
	}

	private enum Collision {
		@SerializedName("same") FIRST, @SerializedName("same") SECOND
	}

	private enum RenamedCollision {
		@SerializedName("SECOND") FIRST, SECOND
	}

	@Test
	public void skipsUnknownValuesAndResumesAtKnownProperty() throws Exception {
		for (String unknown : List.of("42", "null", "{nested: [1, {deeper: true}]}", "[1, {nested: [2]}, []]")) {
			String json = "{unknown: "+unknown+", known: 'ok'}";
			assertEquals("ok", read(json, MutableValue.class).known);
			assertEquals("factory:ok", read(json, FactoryValue.class).value);
		}
		assertEquals("ok", read("{a: 1, b: {}, c: [], known: 'ok'}", MutableValue.class).known);
	}

	@Test
	public void explicitFactoryWinsOverPrivateNoArgAndExistingInstance() throws Exception {
		assertTrue(ObjectWrapper.of(FactoryValue.class, null).isImmutable());
		assertEquals("factory:ok", read("{known: 'ok'}", FactoryValue.class).value);
		FactoryValue existing = new FactoryValue();
		ObjectWrapper<FactoryValue> wrapper = ObjectWrapper.of(FactoryValue.class, existing);
		wrapper.setField("known", "replacement");
		assertNotSame(existing, wrapper.getResult());
		assertEquals("factory:replacement", wrapper.getResult().value);
		assertNull(existing.value);
	}

	@Test
	public void explicitTypeAnnotationsPrecedeFactoriesAndAutomaticFallback() throws Exception {
		assertFalse(ObjectWrapper.of(ExplicitMutable.class, null).isImmutable());
		assertEquals("ok", read("{known: 'ok'}", ExplicitMutable.class).known);
		assertEquals("ok", read("{known: 'ok'}", MutableWithInvalidFactory.class).known);
		assertTrue(ObjectWrapper.of(ExplicitImmutable.class, null).isImmutable());
		assertEquals("constructor:ok", read("{known: 'ok'}", ExplicitImmutable.class).known);
		assertEquals("factory:ok", read("{known: 'ok'}", ImmutableFactory.class).known);
		assertEquals("ok", read("{known: 'ok'}", AutomaticImmutable.class).known);
		assertFalse(ObjectWrapper.of(MutableValue.class, null).isImmutable());
	}

	@Test
	public void invalidMarkedFactoriesFailRatherThanUsingNoArgOrUnmarkedConstructor() {
		for (Class<?> type : List.of(NonStaticFactory.class, WrongReturnFactory.class, WrongNameFactory.class,
				DuplicateParameterFactory.class, WrongParameterFactory.class)) {
			IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
					() -> ObjectWrapper.of(type, null), type.getSimpleName());
			assertTrue(error.getMessage().contains("@Deserializer"));
			assertTrue(error.getMessage().contains(type.getTypeName()));
		}
	}

	@Test
	public void factoryParametersSupportReflectiveBoxingAndWidening() throws Exception {
		assertEquals(43, read("{known: 42}", WideningFactory.class).known);
		assertEquals(43, read("{known: 42}", BoxedFactory.class).known);
	}

	@Test
	public void inheritedMutatorsUseJavaNamesForRenamedProperties() throws Exception {
		MutatorParent result = read("{'wire-value': 'a', 'wire-name': 'b', 'wire-value': 'c'}", MutatorChild.class);
		assertEquals("mutator:c", result.value);
		assertEquals("setter:b", result.name);
	}

	@Test
	public void recordConstructionWaitsForValidatedObjectEnd() throws Exception {
		CountedRecord.constructions = 0;
		RecordDeserializer<CountedRecord> reader = recordWithValue(StructuredData.primitive("ok"));
		assertNull(reader.getResult());
		assertEquals(0, CountedRecord.constructions);
		reader.write(StructuredData.OBJECT_END);
		assertEquals("ok", reader.getResult().value());
		assertEquals(1, CountedRecord.constructions);
		assertTrue(reader.isComplete());
		reader.getResult();
		assertEquals(1, CountedRecord.constructions);
	}

	@Test
	public void lateDuplicateIncludingNullNeverRunsRecordConstructor() throws Exception {
		for (StructuredData value : List.of(StructuredData.primitive("first"), StructuredData.NULL)) {
			CountedRecord.constructions = 0;
			RecordDeserializer<CountedRecord> reader = recordWithValue(value);
			reader.write(StructuredData.objectKey("wire-value"));
			SyntaxError error = assertThrows(SyntaxError.class, () -> reader.write(StructuredData.NULL));
			assertTrue(error.getMessage().contains("Duplicate record component 'wire-value'"));
			assertEquals(0, CountedRecord.constructions);
			assertNull(reader.getResult());
		}
	}

	@Test
	public void missingAndUnterminatedRecordsNeverRunConstructor() throws Exception {
		CountedRecord.constructions = 0;
		RecordDeserializer<CountedRecord> missing = new RecordDeserializer<>(CountedRecord.class);
		missing.write(StructuredData.OBJECT_START);
		assertThrows(SyntaxError.class, () -> missing.write(StructuredData.OBJECT_END));
		RecordDeserializer<CountedRecord> unterminated = recordWithValue(StructuredData.NULL);
		assertThrows(SyntaxError.class, () -> unterminated.write(StructuredData.EOF));
		RecordDeserializer<CountedRecord> missingValue = recordWithValue(StructuredData.NULL);
		missingValue.write(StructuredData.objectKey("unknown"));
		assertThrows(SyntaxError.class, () -> missingValue.write(StructuredData.OBJECT_END));
		assertEquals(0, CountedRecord.constructions);
	}

	@Test
	public void emptyRecordsAlsoWaitForObjectEnd() throws Exception {
		EmptyRecord.constructions = 0;
		RecordDeserializer<EmptyRecord> reader = new RecordDeserializer<>(EmptyRecord.class);
		reader.write(StructuredData.OBJECT_START);
		assertEquals(0, EmptyRecord.constructions);
		reader.write(StructuredData.OBJECT_END);
		assertNotNull(reader.getResult());
		assertEquals(1, EmptyRecord.constructions);
	}

	@Test
	public void recordsDiscardNestedUnknownFieldsAndAcceptExplicitNull() throws Exception {
		CountedRecord.constructions = 0;
		assertNull(read("{a: 1, b: {nested: []}, c: [{}], 'wire-value': null}", CountedRecord.class).value());
		assertEquals(1, CountedRecord.constructions);
	}

	@Test
	public void enumMapKeysAndValuesUseSameSyntaxError() throws Exception {
		MapDeserializer<Mode, String> map = new MapDeserializer<>(Mode.class, String.class);
		map.write(StructuredData.OBJECT_START);
		SyntaxError keyError = assertThrows(SyntaxError.class,
				() -> map.write(StructuredData.objectKey("missing")));
		SyntaxError valueError = assertThrows(SyntaxError.class,
				() -> ObjectWriter.getObjectWriter(Mode.class, StructuredData.primitive("missing"), null)
						.write(StructuredData.primitive("missing")));
		assertEquals(valueError.getMessage(), keyError.getMessage());
		assertTrue(keyError.getMessage().contains("Unknown "+Mode.class.getTypeName()+" value 'missing'"));
		assertEquals(Mode.SLOW, read("'slow-mode'", Mode.class));
		MapDeserializer<Mode, String> valid = new MapDeserializer<>(Mode.class, String.class);
		valid.write(StructuredData.OBJECT_START);
		valid.write(StructuredData.objectKey("slow-mode"));
		valid.write(StructuredData.primitive("ok"));
		valid.write(StructuredData.OBJECT_END);
		assertEquals(Map.of(Mode.SLOW, "ok"), valid.getResult());
	}

	@Test
	public void enumNameCollisionsFailForKeysValuesAndSerialization() {
		for (Class<?> type : List.of(Collision.class, RenamedCollision.class)) {
			IllegalArgumentException keyError = assertThrows(IllegalArgumentException.class,
					() -> new MapDeserializer<>(type, String.class));
			IllegalArgumentException valueError = assertThrows(IllegalArgumentException.class,
					() -> ObjectWriter.getObjectWriter(type, StructuredData.primitive("same"), null)
							.write(StructuredData.primitive("same")));
			assertEquals(valueError.getMessage(), keyError.getMessage());
			assertTrue(keyError.getMessage().contains("Duplicate serialized enum name"));
			assertTrue(keyError.getMessage().contains("FIRST"));
			assertTrue(keyError.getMessage().contains("SECOND"));
			assertThrows(IllegalArgumentException.class,
					() -> new ObjectReaderFactory().getReader(type.getEnumConstants()[0]).next());
		}
	}

	private static RecordDeserializer<CountedRecord> recordWithValue(StructuredData value) throws Exception {
		RecordDeserializer<CountedRecord> reader = new RecordDeserializer<>(CountedRecord.class);
		reader.write(StructuredData.OBJECT_START);
		reader.write(StructuredData.objectKey("wire-value"));
		reader.write(value);
		return reader;
	}

	private static <T> T read(String json, Class<T> type) throws Exception {
		ObjectWriter<T> writer = new ObjectWriter<>(type);
		new JsonReader(new StringReader(json)).transferTo(writer);
		return writer.toObject();
	}
}
