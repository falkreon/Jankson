package blue.endless.jankson.api.io.style;

/**
 * Specifies how much whitespace and what kinds will be emitted by a StructuredDataWriter. While several writers use
 * the same enum, the exact behavior may vary depending on the semantics of the target file format.
 * 
 * <p>
 * Note that this enum does not govern tabs versus spaces. Check your writer options for an indent value.
 */
public enum WhitespaceStyle {
	/**
	 * Emit no unnecessary whitespace. The output will be minified.
	 */
	COMPACT,
	
	/**
	 * Spaces will be used to pad around key/value and make the output more readable, but newlines and indents will not
	 * be used.
	 */
	SPACES_ONLY,
	
	/**
	 * Spaces, newlines, and indents will be used to make the output as readable as possible.
	 */
	PRETTY;
}
