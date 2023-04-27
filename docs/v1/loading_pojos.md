# Loading and Saving Plain Old Java Objects (POJOs)

A POJO is a straightforward type with no references to any particular frameworks.
No special functions or inheritances are required.

## Introduction

As an example, here is what your config class might look like.

=== "Java"
    ```java
    --8<-- "snippets/pojo.java"
    ```
=== "Kotlin"
    ```kotlin
    --8<-- "snippets/pojo.kt"
    ```
=== "Groovy"
    ```groovy
    --8<-- "snippets/pojo.groovy"
    ```

When we're finished here, this POJO will be able to be serialized into and deserialized out of this JSON5 format:

=== "JSON5"
    ```json
    --8<-- "v1/snippets/example.json5"
    ```

## Loading from a file

=== "Java"
    ```java
    --8<-- "v1/snippets/loading.java"
    ```
=== "Kotlin"
    ```kotlin
    --8<-- "v1/snippets/loading.kt"
    ```
=== "Groovy"
    ```groovy
    --8<-- "v1/snippets/loading.groovy"
    ```

## Saving to a file

=== "Java"
    ```java
    --8<-- "v1/snippets/saving.java"
    ```
=== "Kotlin"
    ```kotlin
    --8<-- "v1/snippets/saving.kt"
    ```
=== "Groovy"
    ```groovy
    --8<-- "v1/snippets/saving.groovy"
    ```

## Generics

But these functions can be genericized to be used with *any* POJO.
Just make the functions static and take a type argument.

Or in Kotlin, you can use a reified type argument.

=== "Kotlin"
    ```kotlin
    --8<-- "v1/snippets/generic_save_load.kt"
    ```
