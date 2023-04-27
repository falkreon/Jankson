import blue.endless.jankson.Jankson;
import java.io.File;

// Take a generic type argument to represent any loadable POJO
inline fun <reified T> loadConfig(fileName: String): T {
	val jankson = Jankson.builder().build()
	val configFile = File(fileName)
	val configJson = jankson.load(configFile)
	return jankson.fromJson(configJson, T::class.java)
}

// Saving is more of the same.
// We just bake the config down to a String and write it out.
inline fun <reified T> saveConfig(pojo: T, fileName: String): File {
	val configFile = configDirectory.resolve(fileName)
	val jankson = Jankson.builder().build()
	val result = jankson
		.toJson(pojo)
		.toJson(true, true)

	try {
		if (!configFile.exists()) configFile.createNewFile()
		val out = FileOutputStream(configFile, false)

		out.write(result.toByteArray())
		out.flush()
		out.close()
	} catch (e: IOException) {
		e.printStackTrace()
	}
}
