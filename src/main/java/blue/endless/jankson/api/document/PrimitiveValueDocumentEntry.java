package blue.endless.jankson.api.document;

import blue.endless.jankson.api.element.JsonElement;

public class PrimitiveValueDocumentEntry implements DocumentEntry {
	Object value;
	
	public PrimitiveValueDocumentEntry(String s) {
		value = s;
	}
	
	public PrimitiveValueDocumentEntry(long l) {
		value = l; //force it to be a long and then box it ^_^
	}
	
	public PrimitiveValueDocumentEntry(double d) {
		value = d; //force it into a double and then box it!
	}
	
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
	
	public String asString() {
		if (value==null) return "null";
		return value.toString();
	}
	
	//TODO: asEverythingElse
}
