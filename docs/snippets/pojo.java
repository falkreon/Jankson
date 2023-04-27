import blue.endless.jankson.Comment;
import java.util.Map;
import java.util.Set;

public class Config {
	// Comment annotations populate serialized keys with json5 comments
	@Comment("Why orbs?")
	public int max_orbs = 10;

	// Collection and map types are properly captured in both directions.
	private List<String> some_strings = List.of("abc", "cde");

	// While it's always best to have the object initialized in the constructor or initializer,
	// as long as a full, concrete type is declared, Jankson can create the instance and fill it in.
	protected Map<String, String> uninitialized = null;
	protected Map<String, String> initialized = Map.of("a", "1", "foo", "bar");

	public Integer[] integer_array = new Integer[] { 6, 9 };
	public int[] int_array = new int[] { 4, 2, 0 };

	public NestedConfig nested = new NestedConfig();

	// Nested configurations with inner classes are also possible
	public static class NestedConfig {
		public Set<Short> nested_data = Set.of((short)1, (short)2, (short)3);
	}
}
