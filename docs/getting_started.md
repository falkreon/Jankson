# Getting Started with Jankson

Adding Jankson to your project with Maven is simple.

## Repositories

First, make sure Maven Central is in your repositories list.

Select your chosen build tool below to see the relevant code.

=== "Groovy DSL"
    This goes in `build.gradle`:
    ```groovy
    repositories {
        mavenCentral()
        // Other repositories...
    }
    ```
=== "Kotlin DSL"
    This goes in `build.gradle.kts`:
    ```kotlin
    repositories {
        mavenCentral()
        // Other repositories...
    }
    ```
=== "Apache Maven"
    Nothing needs to be added. Maven Central is included by default.

## Dependencies

Then you can add the dependency itself.

=== "Groovy DSL"
    This goes in your project's `build.gradle` file:
    ```groovy
    dependencies {
        // Other dependencies
        compile "blue.endless:jankson:x.y.z"
    }
    ```
=== "Kotlin DSL"
    This goes in your project's `build.gradle.kts` file:
    ```groovy
    dependencies {
        // Other dependencies
        implementation("blue.endless:jankson:x.y.z")
    }
    ```
=== "Apache Maven"
    This goes in your project's `pom.xml` file:
    ```xml
    <dependencies>
        <!-- Other dependencies -->
        <dependency>
            <groupId>blue.endless</groupId>
            <artifactId>jankson</artifactId>
            <version>x.y.z</version>
        </dependency>
    </dependencies>
    ```
You can replace `x.y.z` in any of these with the latest version number.

The latest version of Jankson is
<img alt="Maven Central" src="https://img.shields.io/maven-central/v/blue.endless/jankson?label=%20&style=flat-square">

## All done

Wasn't that painless? Now you can move on to the next step.

=== "Jankson 1.2.x"
    - [Loading and Saving POJOs](v1/loading_pojos.md)
