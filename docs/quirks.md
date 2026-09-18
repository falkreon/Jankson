# Legacy JSON quirks

This page describes the **legacy permissive parser** used by option-free calls.
For explicit JSON, JSONC, JSON5,
and HJSON document profiles (including HJSON quoteless and multiline values), see
[Document formats](formats.md). Their accepted syntax is intentionally different.

Jankson produces reliable behavior when encountering many quirks
which are normal for configuration files:

* Comments<br>
  Normally disallowed in JSON, but completely legal, inspectable,
  and retained in the document model. Comment-capable document output preserves
  supported comments; typed mapping does not retain arbitrary source comments.

* Missing or extra commas<br>
  These are completely ignored, allowing smaller config file
  diffs when object or array elements are added or removed.
  This also protects end-users from some hard-to-notice syntax errors,
  and eliminates the need for lengthy restarts because the user intent was clear.

* Unquoted object keys<br>
  This is a very common quirk, and as usual, the user's intent is very clear in these cases.

Jankson will also reliably produce descriptive errors for certain other quirks:

* Unmatched quotes are completely ambiguous.
  The amount of text captured may have greatly exceeded the size of the intended quotation,
  possibly even running into the end of the stream.
  This constitutes a macro-structural ambiguity and must be addressed by the user.

* Unmatched braces are direct structural ambiguities.
  One might be able to recover the user's intent from indentation,
  but for unknown input where the indentation may have been clobbered or minified,
  we can't assume good faith and must ask the user to clarify.


???+ info "JSON5"
    The legacy grammar accepts JSON5-like extensions, but is not the JSON5 validation
    profile. Select `JsonFormat.JSON5` for its identifier, comma and comment rules.

    === "Supported"
        - Unquoted keys
        - `'` Single-quotes (apostrophes) around values
        - `\` Line-breaks
        - `.` Leading / trailing decimal points in values
        - `+` Positive signs before values
        - `0x` Hexadecimal values
        - `//` and `/* ... */` comments (`#` is an additional legacy extension, not JSON5)

    ??? info "Example"
        ```
        {
          // comments
          unquotedKey: 'and you can quote me on that',
          singleQuotes: 'I can use "double quotes" here',
          lineBreaks: "Look, Mom! \
        No \\n's!",
          hexadecimal: 0xdecaf,
          leadingDecimalPoint: .8675309, andTrailing: 8675309.,
          positiveSign: +1,
          trailingComma: 'in objects', andIn: ['arrays',],
          "backwardsCompatible": "with JSON",
        }
        ```

???+ info "HJSON"
    A selection of HJSON-like extensions is supported by the legacy grammar.
    Explicit `JsonFormat.HJSON` additionally supports brace-less roots, quoteless
    values and triple-single-quoted multiline strings.

    === "Supported"
        - Unquoted keys
        - Omitted commas at the end of lines
        - `,` Trailing commas
        - `#` `//` `/**/` Comments

        ??? info "Example"
            ```
            {
              # omit quotes for keys
              key: 1,
              // omit commas at the end of a line
              cool: {
              foo: 1
              bar: 2
            }
              /* allow trailing commas */
              list: [
                1,
                2,
              ]
            }
            ```
    === "Use the explicit HJSON profile"
        - Unquoted string values
        - Multi-line block strings

        Both forms below are supported by `JsonFormat.HJSON`, not by the legacy
        parser. See [HJSON string boundaries](formats.md#hjson-string-boundaries).
        
        ??? info "Example"
            ```
            {
              unquotedValue: this value has no quotes
              multiLineStrings:
                  '''
                  My half empty glass,
                  I will fill your empty half.
                  Now you are half full.
                  '''
            }
            ```

???+ info "ONLY in Jankson"
    Some quirks are unique to Jankson.

    - Omitting commas is fine *anywhere*!

    ??? info "Example"
        ```
        {
            key1: 1 key2: 2 key3: 3
            items: [4 3 2 1 6 2 { foo: 'cool' } false]
        }
        ```
