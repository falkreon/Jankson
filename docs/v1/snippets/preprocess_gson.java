import blue.endless.jankson.Jankson;
import java.io.File;

class Config {
	// Other code...

	public static Config loadConfig() {
		// Create a new Jankson instance
		// (This can also be a static instance, defined outside the function)
		var jankson = Jankson.builder().build();

		// Get the config file
		var configFile = new File("config.json");
		// Parse the config file into a JsonObject
		var configJson = jankson.load(configFile);
		// Normalize the JsonObject into a String
		var normalized = configJson.toJson(false, false);

		// Create or get an instance of Gson
		return new Gson()
			// Use it to convert the string to an instance of your POJO
			.fromJson(normalized, Config.class);
	}
}
