package blue.endless.jankson.api.document;

import blue.endless.jankson.api.element.JsonElement;

public interface DocumentEntry {
	boolean isComment();
	CommentDocumentEntry asComment();
	boolean isJsonElement();
	JsonElement asJsonElement();
}
