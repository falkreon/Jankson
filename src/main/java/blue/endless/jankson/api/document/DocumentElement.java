package blue.endless.jankson.api.document;

public interface DocumentElement {
	default boolean isComment() { return false; }
	default CommentElement asComment() { throw new UnsupportedOperationException(); }
	default boolean isValueEntry() { return false; }
	default ValueElement asValueEntry() { throw new UnsupportedOperationException(); }
}
