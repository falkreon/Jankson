package blue.endless.jankson.api.io.style;

/**
 * Specifies how and when to emit comments.
 */
public enum CommentStyle {
	/**
	 * Emit all comments, even quirks-mode comments that do not necessarily match the semantics of the target format.
	 */
	ALL,
	
	/**
	 * Normalizes emitted comments into a form that will be valid if read by a strict parser. For example, octothorpe
	 * ('#') line-end comments will be turned into double-slash ('//') comments in a JsonWriter.
	 */
	STRICT,
	
	/**
	 * All comments will be stripped from the output.
	 */
	NONE;
}
