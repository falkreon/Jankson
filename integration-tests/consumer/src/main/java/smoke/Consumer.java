package smoke;

import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import blue.endless.jankson.api.Jankson;
import blue.endless.jankson.api.annotation.Comment;
import blue.endless.jankson.api.config.ConfigCodecs;
import blue.endless.jankson.api.config.ConfigManager;
import blue.endless.jankson.api.config.TypeRef;
import blue.endless.jankson.api.io.json.JsonFormat;
import blue.endless.jankson.api.io.json.JsonWriter;

/** Compiles and runs using only the public, published API. */
public final class Consumer {
	public static class Settings {
		@Comment("Release smoke comment")
		public int count = 3;
		public List<String> names = List.of("alpha", "beta");
	}

	public static void main(String[] args) throws Exception {
		boolean modular = args[1].equals("module");
		check(Consumer.class.getModule().isNamed() == modular, "Consumer launch mode");
		check(Jankson.class.getModule().isNamed() == modular, "Library launch mode");
		if (modular) {
			check(Jankson.class.getModule().getName().equals("jankson"), "Automatic module name");
			check(Jankson.class.getModule().getDescriptor().isAutomatic(), "Automatic module");
		}
		check(Jankson.class.getProtectionDomain().getCodeSource().getLocation().getPath().endsWith(".jar"),
				"Library must load from the published JAR");
		Path directory = Files.createDirectories(Path.of(args[0]));
		Path input = directory.resolve("readme.json5");
		Files.writeString(input, "{ // preserved comment\n unquoted: 'hello', count: 3, }");
		var document = Jankson.read(input);
		String json5 = Jankson.toJsonString(document, JsonFormat.JSON5.writerOptions());
		check(json5.contains("preserved comment"), "JSON5 comments");
		Jankson.read(json5, JsonFormat.JSON5);
		StringWriter output = new StringWriter();
		JsonWriter writer = new JsonWriter(output, JsonFormat.JSON.writerOptions());
		document.write(writer);
		output.flush();
		String strict = output.toString();
		check(!strict.contains("preserved comment") && strict.contains("\"unquoted\""), "Strict JSON output");
		Jankson.read(strict, JsonFormat.JSON);

		Path configPath = directory.resolve("settings.jsonc");
		var manager = ConfigManager.builder(configPath, Settings.class).build();
		Settings settings = manager.loadOrCreate();
		check(settings.count == 3, "Creation defaults");
		check(Files.readString(configPath).contains("Release smoke comment"), "Created JSONC comments");
		settings.count = 7;
		manager.save();
		var restored = ConfigManager.builder(configPath, Settings.class).build().load();
		check(restored.count == 7 && restored.names.equals(settings.names), "Config save/load");
		check(Files.readString(configPath).contains("Release smoke comment"), "Saved JSONC comments");
		Jankson.read(configPath, JsonFormat.JSONC);

		var type = new TypeRef<List<Settings>>() {};
		Path listPath = directory.resolve("root-list.jsonc");
		var listManager = ConfigManager.builder(listPath, ConfigCodecs.reflective(type))
				.creationDefaults(() -> List.of(new Settings())).build();
		List<Settings> root = listManager.loadOrCreate();
		root.get(0).count = 11;
		listManager.save();
		List<Settings> loaded = ConfigManager.builder(listPath, ConfigCodecs.reflective(type)).build().load();
		check(loaded.size() == 1 && loaded.get(0).count == 11, "TypeRef generic root");
		System.out.println("Published artifact smoke passed: " + args[1]);
	}

	private static void check(boolean condition, String message) {
		if (!condition) throw new AssertionError(message);
	}
}
