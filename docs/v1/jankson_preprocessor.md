# Using Jankson as a Gson Preprocessor

If you're migrating from another JSON library,
and just want to use Jankson to strip comments and utilize quirks,
you can just ask Jankson to output standard JSON and keep your original workflow.

=== "Gson"
    === "Java"
        ```java
        --8<-- "v1/snippets/preprocess_gson.java"
        ```
    === "Kotlin"
        ```kotlin
        --8<-- "v1/snippets/preprocess_gson.kt"
        ```
    === "Groovy"
        ```groovy
        --8<-- "v1/snippets/preprocess_gson.groovy"
        ```
<!-- I can't be bothered to learn another library. Create these snippets and uncomment this block if you want to.
=== "Jackson"
    === "Java"
        ```java
        --8<-- "v1/snippets/preprocess_jackson.java"
        ```
    === "Kotlin"
        ```kotlin
        --8<-- "v1/snippets/preprocess_jackson.kt"
        ```
    === "Groovy"
        ```groovy
        --8<-- "v1/snippets/preprocess_jackson.groovy"
        ```
-->
<!-- Or you could just switch to Jankson. I'm not judging, who's judging? Not me. -->
