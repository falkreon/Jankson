package blue.endless.jankson.api.document;

import blue.endless.jankson.api.element.JsonElement;

public class CommentDocumentEntry implements DocumentEntry {
	protected String value;
	protected boolean lineEnd;
	
	public CommentDocumentEntry(String comment) {
		value = comment;
	}
	
	public String getValue() { return value; }
	
	public String setValue(String value) {
		String result = this.value;
		this.value = value;
		return result;
	}
	
	public boolean isLineEnd() {
		return lineEnd;
	}
	
	public void setLineEnd(boolean lineEnd) {
		this.lineEnd = lineEnd;
	}

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
