import blue.endless.jankson.Jankson

class Config {
	// Variables described above...

	// Other code...

	File saveConfig() {
		def configFile = new File("config.json")
		def jankson = Jankson.builder().build()
		def result = jankson
				.toJson(this)       // The first call makes a JsonObject
				.toJson(true, true) // The second turns the JsonObject into a String

		try {
			def fileIsUsable = configFile.exists() || configFile.createNewFile()
			if (!fileIsUsable) return null
			def out = new FileOutputStream(configFile, false)

			out.write(result.getBytes())
			out.flush()
			out.close()
		} catch (IOException e) {
			e.printStackTrace()
		}

		return configFile
	}
}
