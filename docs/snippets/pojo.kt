import blue.endless.jankson.Comment
import java.util.Map
import java.util.Set

data class Config(
	// Comment annotations populate serialized keys with json5 comments
	@Comment("Why orbs?")
	var max_orbs: Integer = 10,

	// Collection and map types are properly captured in both directions.
	private var some_strings: List<String> = listOf("abc", "cde"),

	// While it's always best to have the object initialized in the constructor or initializer,
	// as long as a full, concrete type is declared, Jankson can create the instance and fill it in.
	protected var uninitialized: Map<String, String>? = null,
	protected var initialized: Map<String, String> = mapOf("a" to "1", "foo" to "bar"),

	val integer_array: IntArray = arrayOf(6, 9),
	val int_array: IntArray = arrayOf(4, 2, 0).toIntArray(),

	val nested: NestedConfig = NestedConfig()
) {
	// Nested configurations with inner classes are also possible
	data class NestedConfig(
		val nested_data: Set<Short> = setOf(1, 2, 3)
	)
}
