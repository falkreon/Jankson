import blue.endless.jankson.Jankson
import java.io.File

// Other code...

fun loadConfig(): Config? {
	// Create a new Jankson instance
	// (This can also be a static instance, defined outside the function)
	val jankson: Unit = Jankson.builder().build()

	// Get the config file
	val configFile = File("config.json")
	// Parse the config file into a JsonObject
	val configJson: Unit = jankson.load(configFile)
	// Normalize the JsonObject into a String
	val normalized: Unit = configJson.toJson(false, false)

	// Create or get an instance of Gson
	return Gson()
		// Use it to convert the string to an instance of your POJO
		.fromJson(normalized, Config::class.java)
}
