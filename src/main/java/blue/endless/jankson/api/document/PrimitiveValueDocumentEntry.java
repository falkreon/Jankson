package blue.endless.jankson.api.document;

import blue.endless.jankson.api.element.JsonElement;

public class PrimitiveValueDocumentEntry implements DocumentEntry {

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
		return true;
	}

	@Override
	public JsonElement asJsonElement() {
		//TODO: Convert this document node into a JsonPrimitive or JsonNull
		
		return null;
	}

}
