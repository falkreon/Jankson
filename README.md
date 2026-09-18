# [Jankson](https://falkreon.github.io/Jankson)

JSONC / JSON5 / HJSON parser and preprocessor that preserves ordering and comments

Official Discord: https://discord.gg/tV6FYXE8QH

## Formats and configuration APIs

Select `JsonFormat.JSON`, `JSONC`, `JSON5` or `HJSON` for format validation.
Option-free calls retain the [legacy permissive grammar](docs/quirks.md).

For 2.x object-key behavior and escaping, see [Object key syntax](docs/object_keys.md).
Recent fixes and compatibility notes are recorded in [CHANGELOG.md](CHANGELOG.md).
For full JSON/JSONC/JSON5/HJSON profiles and file-extension-based loading, see
[Document formats](docs/formats.md).
For typed JSON/JSONC/JSON5/HJSON configuration values, `@Comment`, defaults, revision
checks and safe file replacement, see [Configuration management](docs/config_files.md).
For records, generic roots, factories and serializers, see [Object mapping](docs/object_mapping.md).

| Use case | API |
| --- | --- |
| Edit comments, key order and unknown properties | `ValueElement` with `ConfigCodecs.document()` |
| Load and save a typed current configuration | `ConfigManager<T>` |
| Explicit revision-based file operations | `ConfigFile<T>` |
| One-shot parsing, conversion or streaming | `Jankson.read/write`, `JsonReader`, `JsonWriter` |

Typed saves regenerate mapped fields and annotations; they do not retain arbitrary
source comments or unknown fields. Document output preserves supported content but
may normalize formatting. `Jankson.write(Path)` is not an atomic replacement; use
the configuration APIs when staged writes and revision checking are required.

## [Compiling](https://falkreon.github.io/Jankson/getting_started)

Jankson 2.x is currently prerelease development code, runs on Java 21 or newer,
and is not published to Maven Central. Building this checkout requires a discoverable
JDK 21 for the Gradle toolchain; a newer JDK alone does not satisfy that requirement.
Publish this checkout to your local Maven
repository before using its current coordinate:

```shell
./gradlew --no-daemon --max-workers=1 publishToMavenLocal
```

```groovy
repositories {
	mavenLocal()
	mavenCentral()
}

dependencies {
	implementation "blue.endless:jankson:2.0.0-alpha.3"
}
```

Maven Central currently provides the older stable `blue.endless:jankson:1.2.3`,
whose API differs from this branch. See [Getting started](docs/getting_started.md)
for Maven and Kotlin DSL examples.

Jankson is a general-purpose Java library rather than a Minecraft-version-specific
integration. Minecraft projects may use it when their loader and runtime support
Java 21; compatibility with a particular game version is not asserted here.

## Verification

```shell
./gradlew --no-daemon --max-workers=1 build artifactSmokeTest
```

On Windows use `.\gradlew.bat`. This checks the library and a separately compiled
consumer of a locally published Maven artifact, on both classpath and module path.
See [Release verification](docs/testing.md) for platform coverage and reports.

## Using

Jankson reads, writes and edits configuration documents with its own document and
object-mapping APIs. It can also preprocess comment-bearing input into standard JSON
for libraries such as [Gson] or [Hjson]. It does not provide their API or serializer
compatibility; select the explicit format and mapping behavior your application needs.

[Gson]:https://github.com/Google/Gson
[Hjson]:https://github.com/hjson/hjson-java

```java
try {
	// configObject will represent the document root of the config file, and contains comments and formatting
	// that can be used to recreate the file with some minor formatting and indentation cleanup.
	ValueElement configObject = Jankson.read(Path.of(configPath, "config.json5"));
	
	
	// Select JSON5 explicitly; ValueElement.toString() does not guarantee a format.
	String json5 = Jankson.toJsonString(configObject, JsonFormat.JSON5.writerOptions());
	
	
	// Asking the writer to use STRICT json allows you to use Jankson as a preprocessor for other libraries
	StringWriter stringWriter = new StringWriter();
	JsonWriter jsonWriter = new JsonWriter(stringWriter, JsonFormat.JSON.writerOptions());
	configObject.write(jsonWriter);
	stringWriter.flush();
	String strictJson = stringWriter.toString(); //strictJson is your preprocessed data
	
} catch (SyntaxError error) {
	reportSyntaxError(error);
	return;
} catch (IOException ex) {
	// JsonReader reports parser failures through IOException; preserve the useful syntax details.
	if (ex.getCause() instanceof SyntaxError error) {
		reportSyntaxError(error);
	} else {
		log.error("Couldn't read the config file", ex);
	}
	return;
}
```

## Displaying errors  

The high-level API declares both `IOException` and `SyntaxError`. A syntax failure
reported by `JsonReader` is wrapped as the cause of an `IOException`, while mapping
or structure validation may throw `SyntaxError` directly. Handle both paths as in
the example above. `SyntaxError` is
capable of producing a String which describes both the line and character that the element
started parsing at, and the line and character where the error was discovered.<br>
When presenting a SyntaxError to the user, it's strongly recommended that the stack trace is
omitted, and instead two lines are printed: the exception's `getMessage()`, followed by its
`getLineMessage()`.<br>
This will give the user the most relevant information available about how to fix the problem.
If multiple JSON files are being parsed, it may also be necessary to indicate the name and/or path to the file
so that the problem can be located.
