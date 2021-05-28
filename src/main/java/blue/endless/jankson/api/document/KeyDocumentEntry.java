package blue.endless.jankson.api.document;

import blue.endless.jankson.api.element.JsonElement;

public class KeyDocumentEntry implements DocumentEntry {

	@Override
	public boolean isComment() {
		return false;
	}

	@Override
	public CommentDocumentEntry asComment() {
		throw new UnsupportedOperationException();
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
