import blue.endless.jankson.Jankson;
import blue.endless.jankson.JsonObject;
import java.io.File;

class Config {
	// Variables described above...

	// Other code...

	public static Config loadConfig() {
		// Create a new Jankson instance
		// (This can also be a static instance, defined outside the function)
		var jankson = Jankson.builder()
			// You can register adapters here to customize deserialization
			//.registerTypeAdapter(...)
			// Likewise, you can customize serializer behavior
			//.registerSerializer(...)
			// In most cases, the default Jankson is all you need.
			.build();
		// Parse the config file into a JSON Object
		try {
			File configFile = new File("config.json");
			JsonObject configJson = jankson.load(configFile);
			// Convert the raw object into your POJO type
			return jankson.fromJson(configJson, Config.class);
		} catch (IOException | SyntaxError e) {
			e.printStackTrace();
			return new Config(); // You could also throw a RuntimeException instead
		}
	}
}
