# Java object mapping (2.x)

The mapper converts between Java values and structured document events. Format
selection is separate: the same model can use JSON, JSONC, JSON5 or HJSON.
For managed file operations, use [Configuration management](config_files.md).

## Typed values and generic roots

Use `Class<T>` for an ordinary root class or record. Use `TypeRef<T>` for a
parameterized root so the codec's Java type and reflective type stay linked:

```java
import java.util.List;
import blue.endless.jankson.api.Jankson;
import blue.endless.jankson.api.config.ConfigCodec;
import blue.endless.jankson.api.config.ConfigCodecs;
import blue.endless.jankson.api.config.TypeRef;
import blue.endless.jankson.api.io.json.JsonFormat;

record Server(int port, String host) {}

// Inside a method declaring IOException and SyntaxError:
TypeRef<List<Server>> servers = new TypeRef<>() {};
ConfigCodec<List<Server>> codec = ConfigCodecs.reflective(servers);
List<Server> values = codec.decode(Jankson.read(
        "[{port: 25565, host: 'localhost'}]", JsonFormat.JSON5));
String json = Jankson.toJsonString(codec.encode(values), JsonFormat.JSON.writerOptions());
```

Create a direct parameterized subclass of `TypeRef`. Capturing a method's unresolved
`T`, such as `new TypeRef<List<T>>() {}`, is rejected; it cannot recover an erased
runtime argument. Variables nested in owners, arrays and wildcard bounds are checked
too. Reflective configuration codecs also reject unresolved variables supplied through
their runtime `Type` overloads. Raw `List.class` does not carry element type information.

`ConfigCodecs.<T>reflective(Type)` remains available for dynamically obtained types,
but the caller must ensure `T` actually matches that `Type`. Prefer the `Class<T>`
or `TypeRef<T>` overload when the type is known at compile time.

Inherited fields, parameterized records, arrays and collection/map subclasses are
resolved in their declaring generic context. During serialization, a runtime subclass
retains recoverable bindings from a declared ancestor such as `Base<List<String>>`,
while still including its own serializable properties. This does not recover type
arguments absent from both the declaration and the runtime hierarchy.

## Properties, records and mutable objects

- Static, transient and synthetic fields are excluded. Inherited and non-public
  fields are included where Java reflection permits access.
- `@SerializedName` defines a property's wire name. Duplicate property names are
  rejected rather than silently selecting one field.
- `@Comment` emits nonblank lines before the property in comment-capable output.
  Both annotations support record components.
- Unknown input properties are ignored by ordinary reflective mapping. Use a custom
  codec or document validation if unknown fields should be errors.
- Mutable objects use an accessible declared no-argument constructor. Missing fields
  keep their initialized values. Initialized final fields are retained unless a
  compatible mutator handles them.
- `@MutatorFor` names the Java field, not its wire name. Conventional `setX(...)`
  methods are also considered.
- Records require each component exactly once. Missing or duplicate components fail;
  explicit `null` is a supplied value for reference components, not a missing field.
- Null is accepted for references and rejected for primitives. `byte` and `short`
  overflow is rejected rather than wrapped.

`@Mutable` and `@Immutable` override automatic strategy selection. A marked
`@Deserializer` factory takes precedence over ordinary no-argument mutation;
an unusable marked factory is an error, not permission to silently choose another
strategy. Immutable constructor/factory parameters must account for the mapped
properties exactly once, with compatible types. Use `@SerializedName` on parameters
when Java parameter names are not retained by the application's compiler.

## Generic factories and candidate selection

Generic static factories can declare their own type variables:

```java
import java.util.List;
import blue.endless.jankson.api.annotation.Deserializer;
import blue.endless.jankson.api.annotation.SerializedName;

public final class Envelope<T> {
    public final List<T> values;

    private Envelope(List<T> values) {
        this.values = values;
    }

    @Deserializer
    public static <U> Envelope<U> create(@SerializedName("values") List<U> values) {
        return new Envelope<>(values);
    }
}
```

For an `Envelope<String>` target, the mapper infers `U = String` from the generic
return type, then checks parameter compatibility and declared bounds. Repeated
return-type variables must have consistent bindings. Incompatible return types,
bounds or parameter types are rejected.

Unmarked compatible candidates are compared by specificity. Wildcard containment
uses both upper and lower bounds: `List<? super String>` can be more specific than
`List<?>`, without both candidates eliminating each other. Equivalent or incomparable
candidates remain ambiguous; mark the intended factory with `@Deserializer`.
The selected method must be static and return the class that declares it.

## Map keys and enums

Enums use `Enum.name()` or `@SerializedName`, never an overridden `toString()`.
Enum map keys use the same wire names. Duplicate enum wire names and unknown input
enum names are rejected. Boolean keys accept only `true` and `false`; other supported
key classes are decoded through a public `String` constructor.

Repeated decoded input keys are checked using sorted-map comparator equivalence,
identity for `IdentityHashMap`, or ordinary equality otherwise. For example:

```json
{"1.0": "first", "1.00": "second"}
```

As `SortedMap<BigDecimal, String>`, this is rejected because natural ordering equates
the two keys. As `HashMap<BigDecimal, String>`, they are distinct because
`BigDecimal.equals` includes scale. A case-insensitive `TreeMap` similarly rejects
`key` and `KEY` in the same input. Updating an existing map with one incoming key is
allowed; existing entries do not count as duplicates in that input.

Low-level mapping into an existing mutable object/map is not transactional: earlier
fields or entries may already be changed when a later input fails. Managed reloads
assign a newly decoded value only after successful validation.

See [Object keys](object_keys.md) for the separate lexical quoting and escaping rules.

## Custom serializers and codecs

`ObjectReaderFactory` reads Java objects into document events: its registrations
customize **serialization**, despite the word "reader". `ObjectWriter<T>` consumes
events to construct or update Java objects.

Register `registerSerializer(Class<T>, Function<T, ValueElement>)` for a tree-producing
serializer, or `register(...)` for a streaming reader. `Type` registration overloads
support exact parameterized types. Null bypasses registrations and emits JSON null.

Default dispatch prefers an exact declared type, then an exact runtime type, then
the most specific assignable registration. Loose matching uses **erased classes**,
not generic-argument containment. If registrations distinguish `List<String>` from
`List<Integer>`, use `setPrecise(true)` and supply the declared `Type` to `getReader`.
Precise mode selects exact registrations only; unmatched types still use the built-in
reflective reader. Ambiguous loose registrations fail explicitly.

Cycle detection follows the active object/strategy path. Finite delegation of the
same object between distinct strategies is allowed; revisiting the same pair on
that path fails. Shared acyclic values are allowed and serialized at each occurrence.
Custom callbacks remain responsible for their own recursion and allocations.

`ConfigCodecs.reflective(typeRef, factory)` copies the factory's registrations when
the codec is created. It customizes encoding only; decoding remains reflective.
The generated shape must therefore remain acceptable to that decoder. For arbitrary
shape-changing behavior in both directions, implement `ConfigCodec<T>` instead.

## Comments and document preservation

Typed saves regenerate fields and annotations. They do not retain arbitrary source
comments, unknown properties or original formatting. Choose `ConfigCodecs.document()`
when users edit those details directly.

`ValueElementReader` traverses values, property prologues, epilogues and container
footers iteratively, including through registered serializers. It preserves supported
decorations in the event stream and rejects active-path document cycles. After partial
consumption, `transferTo` transfers the remaining events rather than replaying the root.

The built-in buffered writer base, including `BufferedStructuredDataWriter.of(...)`,
delivers once after reader exhaustion when used through `reader.transferTo(writer)`.
That callback sees trailing comments. Direct event writes deliver eagerly once the
root completes; later trivia does not trigger another callback. Writers implementing
the interface independently control their own buffering behavior.

The selected output format still governs preservation: JSON drops comments, and
comment-capable profiles may normalize delimiters, whitespace and quote style.

## Java modules and resource limits

On the module path, the library's automatic module name is `jankson`. Reflective
models in named modules may need their package opened to it:

```java
module example.application {
    requires jankson;
    opens example.application.config to jankson;
}
```

Reflection does not bypass module access rules. Parser limits, configuration depth
and event budgets, and JSONC writer buffering limits protect different stages; see
[format limits](formats.md#limits-and-compatibility) and
[configuration limits](config_files.md#limits). Mapping a number to `BigInteger` or
`BigDecimal` does not recover precision already lost by the document's `long`/`double`
number representation.
