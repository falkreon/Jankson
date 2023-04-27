import blue.endless.jankson.Jankson

class Config(
	// Variables described above
) {
	// Other code...

	fun loadConfig(): Config? {
		// Create a new Jankson instance
		// (This can also be a static instance, defined outside the function)
		val jankson = Jankson.builder()
			// You can register adapters here to customize deserialization
			//.registerTypeAdapter(...)
			// Likewise, you can customize serializer behavior
			//.registerSerializer(...)
			// In most cases, the default Jankson is all you need.
			.build();
		// Parse the config file into a JSON Object
		val configFile = File("config.json")
		val configJson: JsonObject = jankson.load(configFile)
		// Convert the raw object into your POJO type.
		return jankson.fromJson(configJson, ConfigObject::class.java)
	}
}
