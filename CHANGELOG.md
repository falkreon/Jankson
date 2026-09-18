# Changelog

## [Unreleased]

These changes describe the current 2.x development branch, not a new published
release. The checkout still builds as `2.0.0-alpha.3` by default and requires a
Java 21 toolchain. See [Getting started](docs/getting_started.md) for version and
dependency guidance, and [Release verification](docs/testing.md) for checks.

### Configuration management

**Problem and explanation**

The existing one-shot APIs did not provide a managed current value, revision-based
conflict detection, validated creation defaults, or staged replacement suitable for
application configuration files. Reflective serialization and deserialization also
used inconsistent property metadata and did not reliably handle generic types,
records, enums, nulls, or comments.

**Solution**

- Added stateful `api.config.ConfigManager<T>` for managing one current configuration
  value, plus stateless `ConfigFile<T>` for revision-explicit workflows.
- Added fixed JSON/JSONC/JSON5/HJSON selection, format-specific options, document and
  reflective codecs, custom codecs, validation, snapshots, and configuration origins.
- Added SHA-256 content revisions, conflict-checked saves, explicit last-writer-wins
  overwrite, immutable replacement, and reload without partial state changes on failure.
- Writes validate the source value, encode and serialize it, parse the exact generated
  bytes, decode and validate them again, then publish a sibling temporary file.
- Added atomic replacement by default with an opt-in non-atomic fallback, portable
  best-effort no-clobber creation by default, and strict atomic hard-link publication
  as an opt-in. Parent directories must already exist.
- Staging uses exclusive single-channel creation and owner-only POSIX permissions.
  Prepublication checks validate staged identity when available and compare bytes with
  a bounded no-follow read. Revision checks run before staging and before publication;
  observed target symlinks are classified as conflicts. See [filesystem guarantees](docs/config_files.md#creation-and-replacement-policies).
- Managed files reject target symlinks, duplicate keys, null document nodes, cyclic
  documents, and values deeper than 256 levels from the root. Input and output default
  to a 16 MiB UTF-8 byte limit.
- Reflective configuration encoding has a configurable expanded-event budget, defaulting
  to 1,000,000 non-EOF events per encode. Custom callbacks remain responsible for
  bounding recursion and allocation inside their own code.
- Failures retain path, format and exact pipeline stage through `ConfigFileException`;
  parser and callback errors remain available in the cause chain.
- Typed saves are canonical and regenerate annotations; document mode retains supported
  comments, ordering and unknown properties. Existing one-shot and low-level APIs remain
  available, while the mapping improvements below also apply to them.

### Object mapping, records and generics

- Unified reflective property metadata across serialization and deserialization.
  Mapping now includes inherited and non-public instance fields where reflection permits,
  excludes static, transient and synthetic fields, and rejects duplicate wire names.
- `@SerializedName` and `@Comment` now explicitly support record components. Comment
  annotations emit each nonblank line immediately before its property in comment-capable
  formats.
- Generic types are resolved by declaration across multilevel inheritance, nested and
  owner types, wildcards, generic arrays, and concrete collection/map subclasses.
  Low-level unresolved or cyclic variables fall back to Java erasure (often `Object`).
- Generic static factories infer method variables from the requested return type;
  runtime subclasses retain declared ancestor bindings; wildcard factory specificity
  uses asymmetric containment instead of erasure alone.
- Map duplicate detection respects sorted-map comparators while allowing a single
  update to an existing entry. Serializer cycle detection distinguishes finite strategy
  delegation from a repeated object/strategy pair.
- Added `api.config.TypeRef<T>` and corresponding reflective codec overloads for
  compile-time-linked generic root types. Captures must be direct parameterized
  subclasses with no unresolved type variables. Runtime `Type` overloads remain
  available with caller-checked `T`; reflective config codecs reject unresolved variables.
- Enums and enum map keys now use `Enum.name()` or `@SerializedName` instead of
  `toString()`. Unknown names produce `SyntaxError`, and duplicate enum wire names are
  rejected consistently.
- JSON null maps to reference targets and is rejected for primitives. Primitive `byte`
  and `char` mapping was completed, `byte`/`short` overflow is rejected instead of
  wrapping, and `BigInteger`/`BigDecimal` no longer fail through invalid casts.
- Record construction waits for a validated object end. Missing or duplicate components
  fail before construction, explicit null counts as present for references, and unknown
  nested properties are discarded safely.
- Mutable mapping supports private no-argument constructors and reflective field access,
  preserves initialized final fields unless a mutator handles them, and deterministically
  selects `@MutatorFor` or conventional `setX` methods.
- Explicit `@Mutable`/`@Immutable` annotations take precedence over automatic strategy
  selection. Marked `@Deserializer` factories take precedence over no-argument mutation;
  malformed marked factories fail rather than silently falling back.
- Runtime-subtype serializers are now honored when the declared property type is broader.
- Exact parameterized serializer registrations can be selected with `setPrecise(true)`.
  Default loose registration matching still uses erased assignability; it is not
  generic-argument-aware dispatch. See [Object mapping](docs/object_mapping.md).
- Existing class-based `ArrayDeserializer` and `RecordDeserializer` constructors remain
  available alongside new `Type` overloads.

### Document formats and file APIs

**Problem and explanation**

The existing JSON reader is intentionally permissive, so selecting reader options did not validate a complete JSON, JSON5, or HJSON document grammar. There was also no unified UTF-8 file API that selected a format from a filename.

**Solution**

- Added full document profiles through `JsonFormat.JSON`, `JSONC`, `JSON5`, and `HJSON`, with `readerOptions()` and `writerOptions()` factories. JSONC uses JSON lexical rules with JavaScript line/block comments; trailing commas are an explicit reader opt-in. Option-free calls continue to use the legacy permissive grammar.
- Added `Jankson.read(Path)` and `write(ValueElement, Path)` APIs that select `.json`, `.jsonc`, `.json5`, or `.hjson` from the final extension, case-insensitively. Explicit format overloads override the extension, while unknown extensions require an explicit format.
- Added explicit-format overloads for source text, `Reader`, and `InputStream`. Caller-owned streams are not closed.
- File and `InputStream` reads use UTF-8. This also changes legacy option-free `InputStream` reads, which previously used the platform-default charset. Explicit-format stream reads reject malformed UTF-8.
- File writes serialize before opening the destination, so a representation error does not truncate an existing file. The filesystem write itself is not atomic.
- Format detection never guesses from content and never falls back to another format after a syntax error.

### HJSON support

**Problem and explanation**

The legacy parser supported selected HJSON-like quirks but could not parse complete HJSON documents, including their ambiguous brace-less roots and string forms.

**Solution**

- Added HJSON brace-less root objects, optional commas, quoteless string values, and triple-single-quoted multiline strings with indentation handling.
- Added HJSON unquoted keys whose backslashes remain literal.
- HJSON quoteless strings with many delimiters are scanned in linear time instead of repeatedly copying growing prefixes.
- Non-finite numeric text that cannot be represented as a finite HJSON number is retained as a string.
- HJSON quoteless and multiline strings reject disallowed C0 controls while accepting
  DEL and C1 characters; quoted strings retain their JSON-compatible control rules.

### Object key syntax and escaping

**Problem and explanation**

Object-key rules were implicit and inconsistent between readers and writers. Writers could emit invalid or altered keys, quoted keys were not escaped, explicit disabling of unquoted keys was ineffective, and the default reader rejected hyphenated keys produced by the default writer.

**Solution**

- Centralized internal key rules for legacy Jankson and JSON/JSONC/JSON5/HJSON. Public key-only policies were removed; `JsonFormat` selects the complete document grammar.
- Added JSON5 identifier parsing with validated Unicode escapes and HJSON-style unquoted keys with literal backslashes.
- Quoted keys now escape quotes, backslashes, control characters, and other values that cannot be emitted safely.
- Writers quote empty keys, comment prefixes, configured separators, and any key that is invalid under the selected policy.
- Default `JANKSON` unquoted keys are limited to ASCII letters, digits, underscores, and hyphens. Previously the writer emitted keys without validating them.
- `JsonReaderOptions` now defaults `unquotedKeys` to `true`, matching its documented behavior, while explicit `setUnquotedKeys(false)` is enforced.
- Default readers now accept hyphenated keys, and object parsing honors the configured key/value separator.
- Quoted keys and values share escape and hexadecimal rules. HJSON quoted strings
  reject raw C0 controls and represent them through JSON-compatible escapes.

### Grammar validation and parser limits

**Problem and explanation**

Permissive parsing could accept syntax that is invalid for a selected format, and deeply nested or ambiguous documents had no explicit resource boundary. Reader options were also dropped by typed `readJson` overloads.

**Solution**

- Explicit profiles validate commas, comments, literals, strings, keys, trailing input, and format-specific whitespace, and report syntax locations. `JsonReaderOptions.setAllowTrailingCommas` controls trailing commas for explicit profiles without changing legacy comma handling.
- Typed `Jankson.readJson` overloads now pass reader options through for nested and mapped objects.
- Explicit profiles reject conflicting grammar options instead of silently changing dialects.
- Explicit profiles use the context-based streaming pipeline and default to a 256-container nesting limit, configurable through `JsonReaderOptions.Builder.setMaxContainerDepth(...)`. Only ambiguous unbraced HJSON roots buffer source and speculative events to preserve object-to-scalar fallback; the separate full-document parser was removed.
- Ambiguous unbraced HJSON roots eagerly read to EOF before emitting a non-trivia event and now default to operational
  limits of 16,777,216 buffered Unicode code points and 1,000,000 buffered non-EOF
  events. Named constants expose both defaults, and builder overrides remain available.
- HJSON resource-limit failures, including nesting depth, no longer trigger scalar fallback.
- Direct event consumers may observe valid prefix events before a later syntax error. Individual strings/comments and trees returned by `Jankson.read` still require proportional memory.
- Numbers continue to use Jankson's `long`/`double` model. JSON rejects overflow to infinity, JSON5 supports its non-finite values, and HJSON treats non-finite numeric text as a string.

### Writer correctness and comments

**Problem and explanation**

Comment styles were not fully enforced, nested objects could lose braces in bare-root mode, non-finite values could produce invalid JSON/HJSON, and writer location counters ignored text written in string chunks.

**Solution**

- `CommentStyle.NONE` now strips comments for both legacy and explicit-profile writers.
- Explicit JSONC/JSON5/HJSON writers using `CommentStyle.STRICT` normalize comments to safe `//` lines. `CommentStyle.ALL` preserves original delimiters without guaranteeing valid output for the selected grammar. Legacy `STRICT` retains its existing formatting behavior apart from normalizing `#` comments to `//`.
- JSONC separators are emitted before the following entry or item prologue comments,
  while footer comments no longer force a trailing comma before a closing delimiter.
  Nested footers and deferred-comment writer locations are handled correctly, and default
  JSONC writer output round-trips through the default JSONC reader.
- JSONC deferred trivia has configurable event and text budgets; overflow fails with
  `IOException`. Disabled comments are discarded before buffering. See [writer limits](docs/formats.md#limits-and-compatibility).
- Explicit JSON, JSONC, and HJSON writers reject non-finite numbers; JSON5 writes `Infinity`, `-Infinity`, and `NaN`.
- Bare-root output now removes braces only from the root object and retains braces around nested objects.
- `JsonWriter.getLine()` and `getColumn()` now account for text written through string chunks.

### Scalar comment preservation

**Problem and explanation**

Comments around scalar document roots could be attached incorrectly: leading comments could be lost and trailing comments could be duplicated.

**Solution**

- Leading scalar comments are preserved in the value prologue, and trailing scalar comments are emitted exactly once in the epilogue.
- `ValueElementReader` emits value/property prologues, value epilogues and container
  footers iteratively. Reader-to-buffered-writer transfers include trailing comments
  before delivering once; direct event writes remain eager and do not repeat delivery
  for later trivia. Partially consumed readers transfer their remaining events.

### Options initialization and compatibility

**Problem and explanation**

Static defaults in `JsonReaderOptions` and `JsonWriterOptions` could participate in a superclass/subclass initialization cycle and deadlock when initialized concurrently.

**Solution**

- Converted both options types to immutable final classes with independent builders. `build()` returns the options directly; nested `Access` types were removed.
- Format presets are created by `JsonFormat` factories rather than static profile fields on options. Format initialization does not initialize options, and builders do not inherit from options.
- Because 2.x remains prerelease, removal of `JsonReaderOptions.Access` and
  `JsonWriterOptions.Access` is an intentional source/binary-incompatible change
  between development versions. Replace them with their enclosing options types,
  replace format constants with `JsonFormat` factories, and recompile consumers.
  Increment the alpha version only when publishing a distinct artifact; this change
  does not by itself require changing the version in the build.

### Documentation and regression coverage

**Problem and explanation**

The distinctions between legacy parsing and full document profiles were not documented, and the corrected edge cases lacked focused regression coverage.

**Solution**

- Added documentation for object-key rules, document formats, extension-based loading, stream ownership, parser limits, and API migration, and linked it from the README and MkDocs navigation.
- Added 2.x object-mapping guidance, including generic factories, map-key equivalence,
  custom serializer dispatch, document preservation and JPMS reflective access.
- Updated the legacy quirks page to distinguish it from explicit JSONC/JSON5/HJSON
  profiles, and marked the `docs/v1` guides as 1.x-only.
- Added regression tests for grammar acceptance and rejection, key quoting and escaping, HJSON strings, comments, numeric limits, UTF-8 handling, typed reads, option conflicts, file selection, writer safety, nesting limits, and round trips.
- Added gated-input streaming checks, late-error/reader-failure tests, ambiguous HJSON fallback cases, Unicode/location coverage, and concurrent cold-initialization checks in fresh JVMs.
- Added Windows/Linux CI and an isolated published-artifact consumer on classpath and
  module path. Linux filesystem safety tests must execute without skipped assumptions,
  including same-content staged-file replacement. See [Release verification](docs/testing.md).
- Corrected the TOML-to-JSON test fixture so `CommentStyle.NONE` is tested according to its documented behavior.
