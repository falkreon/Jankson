# Changelog

## [Unreleased]

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
- Quoted keys and values share escape and hexadecimal rules. HJSON quoted keys consistently accept raw control characters other than CR/LF, matching the reference parser.

### Grammar validation and parser limits

**Problem and explanation**

Permissive parsing could accept syntax that is invalid for a selected format, and deeply nested or ambiguous documents had no explicit resource boundary. Reader options were also dropped by typed `readJson` overloads.

**Solution**

- Explicit profiles validate commas, comments, literals, strings, keys, trailing input, and format-specific whitespace, and report syntax locations. `JsonReaderOptions.setAllowTrailingCommas` controls trailing commas for explicit profiles without changing legacy comma handling.
- Typed `Jankson.readJson` overloads now pass reader options through for nested and mapped objects.
- Explicit profiles reject conflicting grammar options instead of silently changing dialects.
- Explicit profiles use the context-based streaming pipeline and enforce a 256-container nesting limit. Only ambiguous unbraced HJSON roots buffer source and speculative events to preserve object-to-scalar fallback; the separate full-document parser was removed.
- Direct event consumers may observe valid prefix events before a later syntax error. Individual strings/comments and trees returned by `Jankson.read` still require proportional memory.
- Numbers continue to use Jankson's `long`/`double` model. JSON rejects overflow to infinity, JSON5 supports its non-finite values, and HJSON treats non-finite numeric text as a string.

### Writer correctness and comments

**Problem and explanation**

Comment styles were not fully enforced, nested objects could lose braces in bare-root mode, non-finite values could produce invalid JSON/HJSON, and writer location counters ignored text written in string chunks.

**Solution**

- `CommentStyle.NONE` now strips comments for both legacy and explicit-profile writers.
- Explicit JSONC/JSON5/HJSON writers using `CommentStyle.STRICT` normalize comments to safe `//` lines. `CommentStyle.ALL` preserves original delimiters without guaranteeing valid output for the selected grammar. Legacy `STRICT` retains its existing formatting behavior apart from normalizing `#` comments to `//`.
- JSONC comments no longer force a trailing comma before an object or array closing delimiter; default JSONC writer output round-trips through the default JSONC reader.
- Explicit JSON, JSONC, and HJSON writers reject non-finite numbers; JSON5 writes `Infinity`, `-Infinity`, and `NaN`.
- Bare-root output now removes braces only from the root object and retains braces around nested objects.
- `JsonWriter.getLine()` and `getColumn()` now account for text written through string chunks.

### Scalar comment preservation

**Problem and explanation**

Comments around scalar document roots could be attached incorrectly: leading comments could be lost and trailing comments could be duplicated.

**Solution**

- Leading scalar comments are preserved in the value prologue, and trailing scalar comments are emitted exactly once in the epilogue.

### Options initialization and compatibility

**Problem and explanation**

Static defaults in `JsonReaderOptions` and `JsonWriterOptions` could participate in a superclass/subclass initialization cycle and deadlock when initialized concurrently.

**Solution**

- Converted both options types to immutable final classes with independent builders. `build()` returns the options directly; nested `Access` types were removed.
- Format presets are created by `JsonFormat` factories rather than static profile fields on options. Format initialization does not initialize options, and builders do not inherit from options.
- This is an intentional source/binary-incompatible API simplification. Replace `JsonReaderOptions.Access`/`JsonWriterOptions.Access` with their enclosing options types, replace format constants with `JsonFormat` factories, and recompile consumers.

### Documentation and regression coverage

**Problem and explanation**

The distinctions between legacy parsing and full document profiles were not documented, and the corrected edge cases lacked focused regression coverage.

**Solution**

- Added documentation for object-key rules, document formats, extension-based loading, stream ownership, parser limits, and API migration, and linked it from the README and MkDocs navigation.
- Added regression tests for grammar acceptance and rejection, key quoting and escaping, HJSON strings, comments, numeric limits, UTF-8 handling, typed reads, option conflicts, file selection, writer safety, nesting limits, and round trips.
- Added gated-input streaming checks, late-error/reader-failure tests, ambiguous HJSON fallback cases, Unicode/location coverage, and concurrent cold-initialization checks in fresh JVMs.
- Corrected the TOML-to-JSON test fixture so `CommentStyle.NONE` is tested according to its documented behavior.
