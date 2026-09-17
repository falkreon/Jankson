# Object key syntax (2.x)

Use the same `JsonFormat` for reading and writing to choose a document grammar,
including its object-key rules. See [document formats](formats.md) for file APIs.
Without a format, the legacy Jankson rules apply. There is no separate public
key-policy setting.

## Rules by format

| Format | Unquoted keys | Escapes in unquoted keys |
| --- | --- | --- |
| `JANKSON` (default) | Nonempty ASCII letters, digits, underscores, and hyphens | None |
| `JSON` | Never allowed; double-quoted keys use JSON string escapes | Not applicable |
| `JSONC` | Never allowed; double-quoted keys use JSON string escapes | Not applicable |
| `JSON5` | ECMAScript IdentifierName character categories, including `$`, `_`, and Unicode letters | `\uXXXX`, only for characters valid in that identifier position |
| `HJSON` | Broader keys, excluding whitespace, control characters, and `{}[],:` | None; backslashes are literal |

The JSON5 character classification uses the running JDK's Unicode categories,
not Java identifier rules. Digits, combining marks, and join controls may occur
in subsequent positions but not as the first character. Escapes cannot bypass
these restrictions. Reserved words such as `true` may be keys.

The writer also quotes keys beginning with quotes or comment introducers (`#`,
`//`, `/*`), keys containing the configured separator, and keys containing lone
UTF-16 surrogates. It may quote additional keys conservatively. Quoted output uses
double quotes and JSON-compatible escapes, including UTF-16 escapes for Unicode.

`setUnquotedKeys(true)` permits/preferentially emits unquoted keys **only when
the selected syntax allows them**. Setting it to `false` rejects unquoted input
keys or forces quoted output. It does not change how quoted keys are decoded.
The `JSON` and `JSONC` policies always require double quotes, regardless of this flag.

## Example

```java
import blue.endless.jankson.api.Jankson;
import blue.endless.jankson.api.document.ObjectElement;
import blue.endless.jankson.api.io.json.JsonReaderOptions;
import blue.endless.jankson.api.io.json.JsonWriterOptions;
import blue.endless.jankson.api.io.json.JsonFormat;

// In a method declaring IOException and SyntaxError:
var reading = JsonFormat.JSON5.readerOptions();
var writing = JsonFormat.JSON5.writerOptions().asBuilder()
        .setUnquotedKeys(true)
        .build();

ObjectElement document = Jankson.readJsonObject(
        "{host: \"localhost\", \"port-number\": 25565}", reading);
String result = Jankson.toJsonString(document, writing);
```

The output leaves `host` unquoted and quotes `port-number`. With `JsonFormat.HJSON`,
both names can be emitted unquoted. The selected format validates the entire document.

## Backslashes: file text versus key value

The following are literal spellings **in a configuration file**, not Java string
literals:

| File spelling | JSON5 key value | HJSON key value |
| --- | --- | --- |
| `firstli\u006Ee` | `firstline` | Literal `firstli\u006Ee` |
| `firstli\ne` | Invalid identifier | One literal backslash followed by `n` and `e` |
| `firstli\\ne` | Invalid identifier | Two literal backslashes followed by `n` and `e` |
| `"firstli\\ne"` | One literal backslash followed by `n` and `e` | Same |
| `"firstli\ne"` | `firstli`, a newline, then `e` | Same |

Keys passed to `ObjectElement.put` are already **values**, not encoded source.
The writer never interprets an apparent escape in such a key. For example, the
Java string `"firstli\\u006Ee"` must not silently become `firstline`.

The writer emits literal unquoted identifiers when possible; it does not generate
Unicode escapes in unquoted JSON5 identifiers. The reader accepts valid escaped
identifiers as well as their literal equivalents.

## Compatibility and limitations

- Existing option-free calls retain the permissive Jankson document parser. The
  default key policy adds hyphenated key reading to match existing writer output.
- Unsafe default output keys are now quoted instead of being written verbatim.
- `JsonReaderOptions.isUnquotedKeys()` now defaults to `true`, matching previous
  actual parsing and documentation. Explicit `false` now has an effect.
- Typed `readJson` overloads honor the supplied options, including key syntax.
- A custom key/value separator is honored inside objects and cannot occur in a
  literal unquoted output key. This does not complete bare-root/INI_SON reading.
- JSON5/HJSON quoted keys are read with their respective string escape rules;
  legacy Jankson retains the existing permissive string-key parser.
- Full profiles reject options that contradict the selected grammar.

Reference rules: [JSON5](https://spec.json5.org/#objects),
[ECMAScript IdentifierName](https://262.ecma-international.org/5.1/#sec-7.6), and
[HJSON keys](https://hjson.github.io/syntax.html#keys).
