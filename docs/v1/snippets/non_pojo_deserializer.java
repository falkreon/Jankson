import blue.endless.jankson.Jankson;
import blue.endless.jankson.annotation.Deserializer;
import blue.endless.jankson.annotation.Serializer;
import blue.endless.jankson.api.SyntaxError;

public class ClassA {
	private static final Jankson JANKSON = Jankson.builder().build();

	// Variables described above

	// Use the Serializer annotation to create a serializer
	@Serializer
	public JsonElement toJson() {
		// The boolean isClassB is a stand-in for your logic that decides what type to parse
		// You might use an enum if you have more than two types of data
		if (isClassB && classB != null) return JANKSON.toJson(classB);
		// If we can't do that, it might be the other type.
		else if (!isClassB && arr != null) return JANKSON.toJson(arr);
		// This object is invalid. Return an empty JSON object.
		else return JANKSON.toJson(new Object());
	}

	// Use the Deserializer annotation to create a deserializer
	@Deserializer
	// Take a JsonArray as an argument when you want to parse an array
	public static ClassA fromArray(JsonArray array) throws SyntaxError {
		var value = new ClassA();
		// Convert from a JsonArray to an int array
		value.arr = JANKSON.fromJson(array.toJson(), int[].class);
		// Set the boolean so we can re-serialize the data later
		value.isClassB = false;
		return value;
	}

	@Deserializer
	// Take a JsonObject when you want to parse a POJO
	public static ClassA fromObject(JsonObject object) throws SyntaxError {
		var value = new ClassA();
		// Convert from an object to your POJO instance
		value.classB = JANKSON.fromJson(object.toJson(), ClassB.class);
		// Set the boolean so we can re-serialize the data later
		value.isClassB = true;
		return value;
	}
}
