# Getting Started with Jankson

The current 2.x branch is prerelease development code and requires Java 21 or newer.
It is not available from Maven Central. Maven Central currently contains Jankson
`1.2.3`, whose API differs from the 2.x documentation on this branch.

Jankson itself is not tied to a Minecraft version. A Minecraft project can use it
when its loader and runtime support Java 21; compatibility with individual game
versions depends on that surrounding environment.

## Using the 2.x development build

Building this checkout requires an installed JDK 21 discoverable by Gradle's Java
toolchain detection (for example, through `JAVA_HOME`). The build does not provision
that JDK automatically. A newer runtime can run the library's Java 21 bytecode, but
a newer JDK alone does not satisfy the build's exact Java 21 toolchain request.

From a Jankson checkout, publish `2.0.0-alpha.3` to your local Maven repository:

```shell
./gradlew --no-daemon --max-workers=1 publishToMavenLocal
```

Then add the local repository and dependency to the consuming project. `mavenLocal()`
is intentional here; the 2.x coordinate below is not on Maven Central.

=== "Groovy DSL"
    This goes in `build.gradle`:
    ```groovy
    repositories {
        mavenLocal()
        mavenCentral()
    }

    dependencies {
        implementation "blue.endless:jankson:2.0.0-alpha.3"
    }
    ```
=== "Kotlin DSL"
    This goes in `build.gradle.kts`:
    ```kotlin
    repositories {
        mavenLocal()
        mavenCentral()
    }

    dependencies {
        implementation("blue.endless:jankson:2.0.0-alpha.3")
    }
    ```
=== "Apache Maven"
    Maven checks its local repository automatically. This goes in your project's
    `pom.xml` file:
    ```xml
    <dependencies>
        <dependency>
            <groupId>blue.endless</groupId>
            <artifactId>jankson</artifactId>
            <version>2.0.0-alpha.3</version>
        </dependency>
    </dependencies>
    ```

The alpha number only needs to change when a distinct artifact is going to be
published. Source or documentation changes alone do not make the existing local
coordinate resolve to a new artifact; republish the checkout when testing them.
Before publishing a distinct release, assign a new version and recompile consumers:
the current options API changes are source- and binary-incompatible with previous
2.x development builds. See [Testing and release verification](testing.md) for an
isolated artifact-consumer check that does not publish to a remote repository.

## Stable Maven Central release

For the older 1.x API, use Maven Central and `blue.endless:jankson:1.2.3`.
Do not use that coordinate with the 2.x examples in these pages.

## All done

Continue with the documentation matching the version you selected.

=== "Jankson 2.x"
    - [Document formats](formats.md)
    - [Configuration management](config_files.md)
    - [Object mapping, records and generics](object_mapping.md)
    - [Object key syntax](object_keys.md)
    - [Release verification](testing.md)

=== "Jankson 1.2.x"
    - [Loading and Saving POJOs](v1/loading_pojos.md)
