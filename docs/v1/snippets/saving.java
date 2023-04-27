import blue.endless.jankson.Jankson;
import java.io.File;

class Config {
	// Variables described above...

	// Other code...

	public File saveConfig() {
		var configFile = new File("config.json");
		var jankson = Jankson.builder().build();
		var result = jankson
				.toJson(this)        // The first call makes a JsonObject
				.toJson(true, true); // The second turns the JsonObject into a String

		try {
			var fileIsUsable = configFile.exists() || configFile.createNewFile();
			if (!fileIsUsable) return null;
			var out = new FileOutputStream(configFile, false);

			out.write(result.getBytes());
			out.flush();
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return configFile;
	}
}
