# Creating Custom (De)serializers

Suppose you have an object that's not straightforward like a POJO, and you want to serialize it in a special way.

In this example, Class A may contain information about either an array or an instance of Class B.
We only want to serialize one of them at a time. The other will always be null.

=== "Classes"
    !!! note inline "ClassA.java"
        ```java
        --8<-- "v1/snippets/non_pojo_a.java"
        ```
    !!! note "ClassB.java"
        ```java
        --8<-- "v1/snippets/non_pojo_b.java"
        ```

Attempting to serialize this as a POJO would result in a larger structure containing a null value,
a filled value representing the correct structure, and the boolean that tells us which structure to pick.

But we obviously only want it to only result in the correct structure.

=== "JSON Example"
    !!! failure inline "Incorrect"
        ```json
        --8<-- "v1/snippets/non_pojo_incorrect.json"
        ```
    !!! success "Correct"
        ```json
        --8<-- "v1/snippets/non_pojo_correct.json"
        ```

Attempting to deserialize either type would result in an error in both cases,
because the structure of Class A matches neither an array nor Class B.

To get the correct results, we have to implement a custom deserializer:

=== "Java"
    ```java
    --8<-- "v1/snippets/non_pojo_deserializer.java"
    ```
=== "Kotlin"
    ```kotlin
    --8<-- "v1/snippets/non_pojo_deserializer.kt"
    ```
=== "Groovy"
    ```groovy
    --8<-- "v1/snippets/non_pojo_deserializer.groovy"
    ```
