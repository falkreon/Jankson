# Jankson

Documentation for the current **2.x prerelease branch**. The stable Maven Central
release is **1.2.3** and uses a different API; its guides are under the **1.2.x** tab.

## What is Jankson?

Jankson is a Java library for reading, writing and editing JSON, JSONC, [JSON5] and
[HJSON] configuration documents. It provides an editable document model, reflective
Java-object mapping, and revision-aware configuration file management.

Document mode preserves supported comments, key order and unknown properties.
Typed mode produces canonical output from the mapped fields and annotations.
Neither mode promises byte-for-byte preservation of the original formatting.

## Where do I get it?

**Users:** Jankson should have been included with your program.
Release history is available in [releases on GitHub]; those artifacts do not
necessarily match this development branch.

**Developers:** To get started using Jankson, head over to the [Getting Started] page.
The 2.x checkout requires a discoverable JDK 21 to build and Java 21 or newer to run.

## Choose a guide

- [Document formats](formats.md): format profiles, file APIs, streaming and limits.
- [Configuration management](config_files.md): defaults, validation, revisions and staged writes.
- [Object mapping](object_mapping.md): records, generic types, factories and serializers.
- [Object keys](object_keys.md): quoting, escaping and map-key collisions.
- [Release verification](testing.md): regression coverage and published-artifact checks.

## How is it different from other JSON libraries?

Explicit profiles validate the selected grammar rather than accepting every extension
at once. The option-free legacy parser supports permissive "[quirks]", such as omitted
commas and unquoted keys. Use an explicit profile when the input must conform to a
specific format. The configuration APIs always use an explicit profile.

[releases on GitHub]:https://github.com/falkreon/Jankson/releases
[Getting Started]:getting_started.md
[JSON5]:https://json5.org/
[HJSON]:https://hjson.github.io/#intro
[quirks]:./quirks.md
