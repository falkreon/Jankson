# Configuration management (2.x)

The configuration API supports fixed-format JSON, JSONC, JSON5 and HJSON files.
`ConfigManager<T>` is the application-facing API for a single current configuration;
`ConfigFile<T>` is the stateless, revision-aware API for advanced workflows.

## Typed quick start

Declare an ordinary configuration class. Initializers are retained when an existing
file omits a field, and `@Comment` documents that field in comment-capable formats.

```java
import java.util.List;
import blue.endless.jankson.api.annotation.Comment;

public final class ModConfig {
    @Comment("Chance that a flip occurs")
    public float flip_chance = 0.5f;

    @Comment("Elements enabled by default")
    public List<String> someElements = List.of("123", "test");
}
```

```java
import java.nio.file.Path;
import blue.endless.jankson.api.config.ConfigManager;

ConfigManager<ModConfig> manager = ConfigManager
        .builder(Path.of("config/mod.jsonc"), ModConfig.class)
        .build();

ModConfig config = manager.loadOrCreate();
config.flip_chance = 0.75f;
config.someElements = List.of("123", "test", "extra");
manager.save();
```

The no-argument constructor supplies a complete new configuration when the file is
missing. For a record or class without one, configure a fresh supplier:

```java
record Server(int port, String host) {}

ConfigManager<Server> manager = ConfigManager
        .builder(Path.of("server.hjson"), Server.class)
        .creationDefaults(() -> new Server(25565, "localhost"))
        .build();
```

Creation defaults are only used after a confirmed `NoSuchFileException`. They do
not repair malformed files, merge defaults into existing documents, or run after
permission and decoding failures. Parent directories must already exist.

## State and saving

A manager starts unloaded. `load()` requires an existing file; `loadOrCreate()` may
create it. Repeated calls return the same managed reference without I/O. Use
`reload()` to explicitly replace it from disk.

`save()` checks the SHA-256 content revision observed by the last successful load,
creation, reload or save. External byte changes and deletion cause
`ConfigConflictException`; local edits stay in memory. Call `reload()` to discard
them or the explicitly last-writer-wins `overwrite()` to replace the file.

For immutable values, install a replacement only after it has been written:

```java
Server replacement = new Server(25566, manager.current().host());
manager.replaceAndSave(replacement);
```

`reload()` and `replaceAndSave(...)` replace the managed object. Previously returned
references then become stale and no longer affect `manager.current()`. A successful
ordinary `save()` retains object identity.

Manager methods serialize their state transitions, but a returned mutable object is
not made thread-safe. Do not mutate it concurrently with `save()`. The manager does
not watch the filesystem or retain open streams.

## Formats and comments

The final extension selects `.json`, `.jsonc`, `.json5` or `.hjson`, case-insensitively.
An explicit format overrides it and also supports custom extensions:

```java
var manager = ConfigManager.builder(Path.of("settings.conf"), ModConfig.class)
        .format(JsonFormat.JSONC)
        .build();
```

The selected format is fixed for all reads, creation and saves. There is no content
guessing, syntax-error fallback, or automatic legacy permissive parsing. Reader and
writer options must use the same explicit profile:

```java
var manager = ConfigManager.builder(Path.of("settings.jsonc"), ModConfig.class)
        .readerOptions(JsonFormat.JSONC.readerOptions().asBuilder()
                .setAllowTrailingCommas(true).build())
        .writerOptions(JsonFormat.JSONC.writerOptions())
        .build();
```

Reflective saves are canonical. `@Comment` lines are regenerated before their fields
on every save in JSONC, JSON5 and HJSON; JSON removes comments. Source comments,
unknown properties, exact whitespace, quote style and numeric spelling are not
retained by typed mapping. Use document mode when source decorations matter.

The reflective mapper excludes static, transient and synthetic fields, handles
inherited and non-public fields where reflection access permits, and rejects wire-name
collisions. Unknown input fields are ignored. Missing mutable fields retain initializer
values; missing record components fail. Enums use names (or `@SerializedName`), and
null is accepted for references but rejected for primitives. Generic inheritance,
parameterized records and arrays, and concrete collection/map subclasses retain their
resolved types. Enum map keys use the same wire names as enum values; duplicate property
or enum wire names are rejected.

Mutable classes use a declared no-argument constructor when reflection permits. Final
fields retain their initialized values unless a compatible mutator handles them.
`@MutatorFor` refers to the Java field name, not its serialized name; conventional
`setX(...)` methods are also recognized. `@Mutable`, `@Immutable` and marked
`@Deserializer` factories control strategy selection. Records require every component
exactly once, and `byte`/`short` values are range-checked instead of narrowed.

`@Comment` and `@SerializedName` also apply to record components. Multiline comments
emit each nonblank line separately. Enum wire names use `Enum.name()`, not an overridden
`toString()`.

## Validation and custom codecs

Configure a validator when values have application-level constraints:

```java
import java.io.IOException;

var manager = ConfigManager.builder(Path.of("server.json5"), Server.class)
        .creationDefaults(() -> new Server(25565, "localhost"))
        .validator(config -> {
            if (config.port() < 1 || config.port() > 65535) {
                throw new IOException("port out of range");
            }
        })
        .build();
```

Loaded values are validated after decoding. Before a write touches the target, the
source value is validated, and the value decoded from the exact generated bytes is
validated again. Validation failures report `ConfigStage.VALIDATE`.

`ConfigManager.builder(path, codec)` and `ConfigFile.builder(path, codec)` accept a
custom `ConfigCodec<T>`. Codecs map `ValueElement` independently of the selected file
format and must support concurrent calls. Custom manager codecs need
`creationDefaults(...)` before `loadOrCreate()` can create a missing file. A manager
rejects a null root, including a custom codec that decodes generated output to null.

Custom callbacks must bound recursion and allocation inside their own `encode`, `decode`
and streaming callbacks. Documents and event streams returned to the configuration
pipeline are still checked by its limits.

## Creation and replacement policies

Missing-file publication defaults to `ConfigCreationPolicy.PORTABLE_BEST_EFFORT`.
It performs a portable, non-replacing move of an already complete sibling temporary
file. This is best-effort no-clobber publication: Java does not guarantee that the
check and move are atomic on every filesystem provider. If another creator wins and
publication reports `FileAlreadyExistsException`, that winner is loaded instead.

For atomic no-clobber namespace publication, require hard-link support:

```java
.creationPolicy(ConfigCreationPolicy.REQUIRE_ATOMIC_NO_CLOBBER)
```

Replacement of an existing file is a separate policy. It requires `ATOMIC_MOVE` by
default. An explicit weaker fallback is available:

```java
.atomicWrites(AtomicWritePolicy.ALLOW_NON_ATOMIC_FALLBACK)
```

The fallback is used only when the provider throws `AtomicMoveNotSupportedException`;
other atomic-move failures are propagated.

Before touching the target, writes validate the source value, encode it, serialize
it, parse the exact generated bytes, decode them again and validate the decoded
result. They then stage a unique sibling temporary file. Validation, mapping and
staging failures preserve the previous target. Cleanup is attempted on every path;
after a successful commit, cleanup failure schedules the temporary path for deletion
at JVM exit rather than turning a committed save into an ambiguous failed operation.

Target symlinks, including dangling ones, are rejected. Parent-directory aliases
are not a security boundary. Replacement does not preserve existing ACLs, owner or
permissions and is not a crash-durability/fsync guarantee.

## Document mode

For editable comments, ordering and unknown keys, use a document manager:

```java
ConfigManager<ValueElement> manager = ConfigManager
        .builder(Path.of("settings.jsonc"), ConfigCodecs.document())
        .creationDefaults(ObjectElement::new)
        .build();

ValueElement document = manager.loadOrCreate();
manager.save();
```

Document mode retains supported comments and ordering. Serialization may still
normalize presentation, and JSON necessarily removes comments.

## Low-level ConfigFile

`ConfigFile<T>` performs independent stateless operations and exposes revisions:

```java
ConfigFile<Server> file = ConfigFile
        .builder(Path.of("server.json5"), ConfigCodecs.reflective(Server.class))
        .build();

ConfigSnapshot<Server> loaded = file.load();
ConfigSnapshot<Server> saved = file.save(
        new Server(25566, loaded.value().host()), loaded.revision());
```

Snapshots are revision-bearing observations, not deep copies; their value may be
mutable. Revisions are bound to an absolute normalized lexical path. Same-byte
replacement is not a conflict, and checking remains advisory against external
processes rather than a filesystem compare-and-swap.

`ConfigSnapshot.origin()` is `FILE` for a disk load, `CREATED` for creation, and
`SAVED` for conflict-checked or overwrite saves. A successful write snapshot retains
the exact supplied value by identity. The separately decoded generated value verifies
the output but does not replace the snapshot value. Do not mutate a value concurrently
with a `ConfigFile` save.

## Limits

The default input/output limit is 16 MiB of encoded UTF-8 bytes. `maxBytes(...)` accepts
a positive value smaller than `Integer.MAX_VALUE` and applies to both loaded input and
generated output.

Configuration values are limited to a depth of 256 with the root at depth 0; this fixed
policy overrides `JsonReaderOptions.maxContainerDepth`. Empty containers at depth 256
are accepted, while a value at depth 257 is rejected. Duplicate object keys, null
document nodes and cyclic document graphs are rejected recursively. Shared acyclic
document nodes are allowed.

Reflective encoding additionally defaults to
`ConfigCodecs.DEFAULT_MAX_ENCODE_EVENTS`, or 1,000,000 expanded non-EOF events per
encode. Keys, comments, formatting, primitives, container boundaries and every use of
a shared reference count toward the budget. Use
`ConfigCodecs.reflective(type, factory, maxEncodeEvents)` to select another positive
budget. Exceeding it reports `ConfigStage.ENCODE`.

## Errors

`ConfigFileException` exposes `path()`, `format()` and `stage()`. Original causes,
including `SyntaxError`, remain available. The stages are `READ`, `PARSE`, `DECODE`,
`VALIDATE`, `DEFAULTS`, `ENCODE`, `SERIALIZE`, `WRITE_TEMPORARY`, `CHECK_CONFLICT`,
`CREATE` and `REPLACE`. Runtime failures from defaults, codecs, validators and
filesystem operations receive the same context. Cleanup failures are attached to an
earlier error. Manager access, including `reload()`, before a successful initial load
throws `IllegalStateException`.

Existing `Jankson.read/write` and low-level reader/writer APIs remain available for
one-shot I/O. TOML and INI are not enrolled in configuration management yet.
