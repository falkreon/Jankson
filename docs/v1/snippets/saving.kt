import blue.endless.jankson.Jankson

internal class Config {
	// Variables described above...

	// Other code...

	fun saveConfig(): File? {
		val configFile = File("config.json")
		val jankson: Unit = Jankson.builder().build()
		val result: Unit = jankson
			.toJson(this)       // The first call makes a JsonObject
			.toJson(true, true) // The second turns the JsonObject into a String
		try {
			val fileIsUsable = configFile.exists() || configFile.createNewFile()
			if (!fileIsUsable) return null
			val out = FileOutputStream(configFile, false)
			out.write(result.getBytes())
			out.flush()
			out.close()
		} catch (e: IOException) {
			e.printStackTrace()
		}
		return configFile
	}
}
