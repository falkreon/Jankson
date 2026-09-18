# JSON, JSONC, JSON5, and HJSON documents (2.x)

`JsonFormat` selects the grammar of an entire document, including object keys.
Calls without a format retain the legacy permissive Jankson grammar.

| Feature | JSON | JSONC | JSON5 | HJSON |
| --- | --- | --- | --- | --- |
| Keys without quotes | No | No | IdentifierName | HJSON key rules |
| `//` and `/* */` comments | No | Yes | Yes | Yes |
| `#` comments | No | No | No | Yes |
| Single-quoted strings | No | No | Yes | Yes |
| Trailing commas | No | Opt-in | Yes | Yes |
| Optional commas | No | No | No | Yes |
| Brace-less root object | No | No | No | Yes |
| Quoteless string values | No | No | No | Yes |
| Triple-single-quoted multiline strings | No | No | No | Yes |
| Hexadecimal numbers, leading `+`, leading/trailing decimal point | No | No | Yes | Interpreted as quoteless text |
| `Infinity` and `NaN` | No | No | Numbers | Quoteless text |

JSON5 unquoted identifiers decode valid Unicode escapes. HJSON unquoted keys and
string values preserve backslashes literally. Quoted strings decode escapes.
See [Object keys](object_keys.md) for examples.

## Files: automatic format selection

```java
import java.nio.file.Path;
import blue.endless.jankson.api.Jankson;
import blue.endless.jankson.api.document.ValueElement;
import blue.endless.jankson.api.io.json.JsonFormat;

// Inside a method declaring IOException and SyntaxError:
ValueElement config = Jankson.read(Path.of("config.hjson"));
Jankson.write(config, Path.of("copy.hjson"));

// Explicit format overrides an unknown or misleading extension.
ValueElement custom = Jankson.read(Path.of("settings.conf"), JsonFormat.HJSON);
Jankson.write(custom, Path.of("backup.conf"), JsonFormat.HJSON);
```

Only the final extension is used, case-insensitively: `.json`, `.jsonc`, `.json5`, `.hjson`.
Unknown extensions throw `IllegalArgumentException` before opening the file.
There is no fallback to another format after a syntax error. TOML and INI retain
their separate existing APIs; they are not selected by this file API.

Files use UTF-8. Readers opened by `read(Path)` are closed automatically. File
writes serialize before opening the destination, so a serialization failure does
not truncate an existing file. The write itself is not an atomic filesystem
replacement; I/O failures can still leave a partial file.
Use [ConfigFile or ConfigManager](config_files.md) for staged replacement, validation
and revision-checked saves.

## Source text and caller-owned streams

```java
ValueElement config = Jankson.read("{port: 25565,}", JsonFormat.JSON5);
```

`String` always means source text, never a filename. `Reader` and `InputStream`
overloads also accept `JsonFormat`; caller-owned streams are not closed.
Explicit-format `InputStream` reads use UTF-8 and reject malformed input bytes.

The high-level `Jankson.read(...)` methods declare both `IOException` and
`SyntaxError`. Syntax failures originating in `JsonReader` are wrapped in an
`IOException` whose cause is `SyntaxError`; mapping or structural validation can
throw `SyntaxError` directly. Preserve the line/column details by handling both:

```java
try {
    ValueElement value = Jankson.read(Path.of("config.json5"));
} catch (SyntaxError error) {
    reportSyntaxError(error);
} catch (IOException error) {
    if (error.getCause() instanceof SyntaxError syntax) {
        reportSyntaxError(syntax);
    } else {
        throw error;
    }
}
```

An I/O or UTF-8 decoding error need not have a `SyntaxError` cause. A reader cannot
be reused after a parse failure.

## Options and typed objects

```java
import java.io.StringReader;
import blue.endless.jankson.api.io.json.JsonReaderOptions;
import blue.endless.jankson.api.io.json.JsonWriterOptions;
import blue.endless.jankson.api.io.style.WhitespaceStyle;

record Server(int port, String host) {}

Server server = Jankson.readJson(
        new StringReader("port: 25565\nhost: localhost\n"),
        JsonFormat.HJSON.readerOptions(),
        Server.class
);

var compactJson5 = JsonFormat.JSON5.writerOptions().asBuilder()
        .setWhitespace(WhitespaceStyle.COMPACT)
        .build();

var vscodeJsonc = JsonFormat.JSONC.readerOptions().asBuilder()
        .setAllowTrailingCommas(true)
        .build();
```

Alternatively use `builder().setFormat(JsonFormat.JSON5)`. `setFormat` resets
grammar-related switches to the format defaults while retaining presentation
settings such as whitespace. Later conflicting options are rejected at `build()`:

- JSON cannot enable unquoted keys, comments, or trailing commas.
- JSONC cannot enable unquoted keys, but may opt into trailing commas for
  VS Code-style tolerant input.
- JSON/JSONC/JSON5 cannot omit commas or root braces.
- Comma-free HJSON output requires newlines.
- Full profiles require `:`; indentation is spaces/tabs. Key rules follow the format.

Reader options may further restrict JSON5/HJSON by disabling trailing commas,
and HJSON by disabling brace-less roots or unquoted keys. The trailing-comma
option requires an explicit format; legacy parsing remains comma-agnostic.
Full profiles never silently switch into legacy parsing.

## HJSON string boundaries

A quoteless string ends at a newline, not at a comma or a comment marker:

```hjson
{
  message: hello, world # part of the string
  enabled: true # a comment after a boolean
  description:
    '''
    first line
      indented line
    '''
}
```

Consequently `{message: hello}` is not a closed one-line object: the closing brace
belongs to the quoteless string. Use `{message: "hello"}` or put the closing brace
on the next line. Multiline strings normalize line endings to LF, remove indentation
up to the opening delimiter's column, and omit the final newline before the closing
delimiter. Backslashes inside them are literal.

Quoteless values accept TAB while CR/LF end the value. Multiline strings accept
TAB and LF, and ignore CR whether standalone or part of CRLF. Both forms reject every other raw C0 control
(`U+0000` through `U+001F`) but preserve DEL and C1 characters (`U+007F` through
`U+009F`). Quoted HJSON strings follow JSON's raw-control rule and can represent
C0 characters with escapes.

## Output and preservation

The HJSON writer chooses an interoperable representation: quoted string values,
braced objects and commas by default, with HJSON-compatible unquoted keys where
possible. It need not reproduce the input's quoteless/multiline spelling to preserve
the value. Optional bare root objects and newline-separated members are available
through HJSON writer options.

JSON output removes comments. Explicit JSONC/JSON5/HJSON profiles normalize retained
comments to `//` lines, including embedded line terminators or `*/` text. Ordering
and comment content are retained where supported by the document model; exact
whitespace, original quote style, numeric spelling and comment delimiters are not.

These guarantees apply to document operations, including `ValueElementReader`
event transfers and tree-producing registered serializers. Ordinary typed mapping
regenerates fields and `@Comment` annotations instead of retaining the source tree;
see [Comments and document preservation](object_mapping.md#comments-and-document-preservation).

The JSONC writer never intentionally emits trailing commas, even when its output
will be read with tolerant options. A separator is emitted before the next entry or
item's prologue comments, while footer comments remain before the closing delimiter
without forcing a trailing separator. This also applies across nested containers.
`CommentStyle.ALL` remains an explicitly format-unsafe escape hatch and may produce
text that the selected profile cannot read; use `STRICT` for the profile validity
guarantee or `NONE` for comment-free JSON-compatible output.

Non-finite Java doubles are rejected by JSON/JSONC/HJSON writers rather than silently
converted to strings or null. JSON5 represents them as `Infinity`, `-Infinity`, or
`NaN`. JSON5 and HJSON data cannot always be converted to JSON without application
decisions about such values.

## Limits and compatibility

- Existing `readJson(...)` calls without a format keep the permissive legacy parser.
  Use the explicit profiles when format validation is required.
- JSON, JSONC, JSON5, and braced HJSON objects/arrays use the same streaming context
  pipeline as the legacy reader, with format-specific grammar rules. Individual
  strings and comments are materialized as complete values, not streamed in chunks.
- Ambiguous unbraced HJSON roots eagerly read to EOF before emitting a non-trivia event and
  buffer input and speculative events. Both the object attempt and scalar fallback
  use that same context pipeline. This
  preserves reference behavior, including `a: [` being a scalar string after the
  object attempt fails. No fallback to a different format occurs. The operational
  defaults are `JsonReaderOptions.DEFAULT_MAX_BUFFERED_CHARACTERS` (16,777,216
  Unicode code points) and `DEFAULT_MAX_BUFFERED_EVENTS` (1,000,000 non-EOF events).
  Override them with `JsonReaderOptions.Builder.setMaxBufferedCharacters(...)` and
  `setMaxBufferedEvents(...)`; the exact limit is accepted and both setters reject
  non-positive values. Braced HJSON roots stream and do not use these two limits.
  Resource-limit failures (including container depth) are always propagated and never
  trigger scalar fallback, even if the same text could be interpreted as a string.
- JSONC output buffers comments and subsequent whitespace/newline events after a value
  until the next structural event determines separator placement. Each pending run is
  limited to `JsonWriterOptions.DEFAULT_MAX_DEFERRED_TRIVIA_EVENTS` (4,096 events) and
  `DEFAULT_MAX_DEFERRED_TRIVIA_CHARACTERS` (1,048,576 UTF-16 code units of comment and
  whitespace text, excluding generated delimiters/indentation). Newline events consume
  one event but no text budget. Configure these with `setMaxDeferredTriviaEvents(...)`
  and `setMaxDeferredTriviaCharacters(...)` on the writer builder; both require positive
  values and are preserved by `asBuilder()` and `setFormat(...)`. Exact limits are
  accepted; overflow throws `IOException` before retaining the offending event, including
  an oversized single comment. Budgets reset when the pending run is written.
  `CommentStyle.NONE` discards comments before deferral, so disabled comments do not
  consume either budget. These limits bound pending trivia, not total document size or
  individual values outside that buffer; streaming output may already contain a prefix
  when a limit fails.
- A direct `JsonReader` consumer may receive valid prefix events before a later
  syntax error. `Jankson.read(...)` consumes and validates the whole document before
  returning its tree; it still needs memory proportional to the resulting tree.
- The explicit-profile parser defaults to a limit of 256 simultaneously open containers.
  Configure it with `JsonReaderOptions.Builder.setMaxContainerDepth(...)`; exceeding it
  produces a syntax error. Increasing this limit does not make downstream tree builders
  or consumers iterative. The [configuration API](config_files.md) applies stronger
  structural checks and its own value-depth limit of 256, with the root at depth 0, and
  overrides this parser option accordingly.
- Numbers use Jankson's `long`/`double` model, not arbitrary precision. Large integers
  may be rounded as doubles. JSON/JSONC numbers overflowing finite double range are rejected;
  JSON5 permits infinity; HJSON treats such numeric text as a quoteless string.
- Duplicate object keys are not rejected by the parser; the existing document model
  and Java-object mapping determine their handling. The configuration pipeline rejects
  duplicate document keys before decoding. Java map decoding also rejects collisions
  after key conversion according to the target map's supported equivalence rules.
  See [Map keys and enums](object_mapping.md#map-keys-and-enums).
- `CommentStyle.NONE` now works even for legacy writers. A former TOML-to-STRICT test
  incorrectly expected a hash comment in JSON and has been corrected.

## Options API migration

`JsonReaderOptions` and `JsonWriterOptions` are immutable final classes. Their
independent `Builder` classes return options directly from `build()`; the nested
`Access` types and the class/interface inheritance hierarchy have been removed.
Because 2.x is still prerelease, this is an intentional source and binary incompatible
change between development versions: update explicit `Access` types and recompile
consumers. Existing legacy presets remain available. For release-version guidance,
see [Getting started](getting_started.md#using-the-2x-development-build).

Use `JsonFormat.JSON.readerOptions()` and corresponding factories instead of
format constants on the options classes. Factories return fresh immutable options;
the format enum does not initialize options during its own static initialization.
Object-key syntax cannot be selected independently of the document format.

## Validation

Current build commands, cross-platform regression coverage and artifact-consumer
checks are described in [Release verification](testing.md).

Repository tests exercise positive and negative grammar cases, JSONC comment placement
and trailing-comma modes, comments, HJSON strings,
numeric limits, UTF-8, file selection, ownership, option conflicts, and round trips.
Earlier checks of the buffered implementation additionally compared parsing and generated output against:

- `jsonc-parser`, `json5` 2.2.3, and `hjson` 3.2.2;
- [json5-tests](https://github.com/json5/json5-tests) at `ceb24d4`;
- [JSONTestSuite](https://github.com/nst/JSONTestSuite) at `1ef36fa`;
- [hjson-js test assets](https://github.com/hjson/hjson-js/tree/master/test/assets) at `5734a70`.

Implementation-defined JSONTestSuite cases are reported separately rather than counted
as mandatory acceptance/rejection checks. External reference packages are not runtime
or Gradle dependencies of Jankson.
