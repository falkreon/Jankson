import blue.endless.jankson.Jankson
import blue.endless.jankson.annotation.Deserializer
import blue.endless.jankson.annotation.Serializer
import blue.endless.jankson.api.SyntaxError as JanksonSyntaxError

class ClassA {
	// Variables described above

	// Use the Serializer annotation to create a serializer
	@Serializer
	fun toJson(): JsonElement {
		// The boolean isClassB is a stand-in for your logic that decides what type to parse
		// You might use an enum if you have more than two types of data
		return if (isClassB && classB != null) JANKSON.toJson(classB)
		// If we can't do that, it might be the other type.
		else if (!isClassB && arr != null) JANKSON.toJson(arr)
		// This object is invalid. Return an empty JSON object.
		else JANKSON.toJson(Any())
	}

	companion object {
		private val JANKSON: Jankson = Jankson.builder().build()

		// Use the Deserializer annotation to create a deserializer
		@Deserializer // Take a JsonArray as an argument when you want to parse an array
		@Throws(JanksonSyntaxError::class)
		fun fromArray(array: JsonArray): ClassA {
			val value = ClassA()
			// Convert from a JsonArray to an int array
			value.arr = JANKSON.fromJson(array.toJson(), IntArray::class.java)
			// Set the boolean so we can re-serialize the data later
			value.isClassB = false
			return value
		}

		@Deserializer // Take a JsonObject when you want to parse a POJO
		@Throws(JanksonSyntaxError::class)
		fun fromObject(`object`: JsonObject): ClassA {
			val value = ClassA()
			// Convert from an object to your POJO instance
			value.classB = JANKSON.fromJson(`object`.toJson(), ClassB::class.java)
			// Set the boolean so we can re-serialize the data later
			value.isClassB = true
			return value
		}
	}
}
