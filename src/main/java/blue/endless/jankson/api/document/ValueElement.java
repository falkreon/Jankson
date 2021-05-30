package blue.endless.jankson.api.document;

public interface ValueElement extends DocumentElement {
	@Override
	default boolean isValueEntry() {
		return true;
	}
}
