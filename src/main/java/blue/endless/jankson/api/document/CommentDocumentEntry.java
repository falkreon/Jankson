package blue.endless.jankson.api.document;

import blue.endless.jankson.api.element.JsonElement;

public class CommentDocumentEntry implements DocumentEntry {

	@Override
	public boolean isComment() {
		return true;
	}

	@Override
	public CommentDocumentEntry asComment() {
		return this;
	}

	@Override
	public boolean isJsonElement() {
		return false;
	}

	@Override
	public JsonElement asJsonElement() {
		throw new UnsupportedOperationException();
	}
	
}
