# Release verification

Use a discoverable JDK 21 and the checked-in Gradle 8.7 wrapper:

```sh
./gradlew --no-daemon --max-workers=1 clean build artifactSmokeTest
```

On Windows:

```powershell
.\gradlew.bat --no-daemon --max-workers=1 clean build artifactSmokeTest
```

`build` runs compilation, tests, license checks and artifact assembly, including
sources and Javadoc. Generated output stays under `build/`. The first run may need
network access to obtain Gradle, plugins and test dependencies.

## Published-artifact consumer

`artifactSmokeTest` publishes only `mavenPrimary` to a freshly cleared Maven
repository at `build/artifact-smoke/repository`. It does not invoke `publish`,
`publishToMavenLocal`, or any remote publication task. The independent consumer
fixture is copied into `build/artifact-smoke/consumer` and built with a separate
offline wrapper invocation, one worker, no persistent daemon, and a five-minute
timeout. All generated fixture files stay under `build/`.

The consumer resolves `blue.endless:jankson:2.0.0-alpha.3` (or the root build's
overridden version) from that repository alone. It checks the binary's Java 21
class version and automatic module name, sources/Javadoc JAR contents, POM,
Gradle module metadata, and Maven version index. Both classpath and named-module
runs exercise the README format API, strict JSON conversion, JSONC comments,
configuration creation/save/load, and a generic `TypeRef` root. Neither run adds
jsr305 to the runtime. The module fixture opens its model package to Jankson for
reflection.

The consumer's Gradle invocation is offline; the wrapper may still need its Gradle
distribution on a fresh machine. This is an artifact-consumption check, not a remote
release. Assign a distinct version before publishing a new release and recompile
consumers after the documented binary-incompatible options changes.

## Regression coverage

| Area | Representative test classes |
| --- | --- |
| Permissions, staging integrity, symlinks and FIFO rejection | `ConfigFilesystemSafetyTests`, `ConfigFileTests` |
| Manager state, failures, depth and event budgets | `ConfigManagerTests`, `ConfigPipelineLimitsTests` |
| Decorated document transfers and one-shot buffered delivery | `DecoratedTreeTransferTests` |
| Sorted-map keys, strategy cycles, generic factories and wildcard specificity | `ReviewedMappingRegressionTests` |
| Generic capture, reflective types and mapper edge cases | `ReflectiveTypeApiTests`, `GenericResolutionRegressionTests`, `ObjectMappingHardeningTests` |
| HJSON fallback limits and bounded JSONC trivia | `HjsonBufferLimitsTests`, `TestJsonWriterComments` |
| Format conformance and streaming behavior | `TestJsonFormats`, `TestStreamingJson` |

For a focused check, for example:

```powershell
.\gradlew.bat --no-daemon --max-workers=1 test --tests blue.endless.jankson.DecoratedTreeTransferTests
```

## Cross-platform CI and reports

CI runs the full build and artifact smoke on Ubuntu and Windows with Java 21.
Ubuntu additionally checks the filesystem safety test XML: POSIX permissions,
FIFO rejection, symlink conflicts, and same-content staged-file replacement tests
must be present, with no skipped
assumptions anywhere in that test class. Reports and the local publication are
uploaded for diagnosis.

Local Gradle reports are written to `build/reports/tests/test/index.html` and
`build/test-results/test/`. Windows may skip tests whose filesystem capabilities
are unavailable; this does not replace Linux verification.

## Documentation build

In an activated Python virtual environment, install MkDocs and the Material theme,
then build with warnings treated as errors:

```sh
python -m pip install "mkdocs>=1.6,<2" "mkdocs-material>=9,<10"
python -m mkdocs build --strict --site-dir build/docs-site
```

This validates the MkDocs configuration and documentation links while rendering the
site under `build/`. It does not compile Java snippets or verify external websites.
