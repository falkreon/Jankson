package blue.endless.jankson.api.document;

import java.util.ArrayList;
import java.util.List;

import blue.endless.jankson.api.element.JsonElement;

public class KeyValueDocumentEntry implements DocumentEntry {
	protected CommentDocumentEntry commentBefore = null;
	protected CommentDocumentEntry commentAfter = null;
	protected List<DocumentEntry> entries = new ArrayList<>();
	protected String key;
	protected DocumentEntry valueEntry;
	
	public KeyValueDocumentEntry(String key, DocumentEntry value) {
		this.key = key;
		entries.add(value);
		valueEntry = value;
	}
	
	public KeyValueDocumentEntry(String key, DocumentEntry value, String comment) {
		commentBefore = new CommentDocumentEntry(comment);
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
		return false;
	}

	@Override
	public JsonElement asJsonElement() {
		throw new UnsupportedOperationException();
	}
	
	public String getKey() {
		return key;
	}
	
	public DocumentEntry getValue() {
		return valueEntry;
	}
	
	public DocumentEntry setValue(DocumentEntry value) {
		DocumentEntry result = valueEntry;
		for(int i=0; i<entries.size(); i++) {
			if (valueEntry==entries.get(i)) { //Because of this, keeping valueEntry and entries consistent is VERY IMPORTANT
				entries.set(i, value);
				valueEntry = value;
				return result;
			}
		}
		
		//No existing value entry?!?
		entries.add(value);
		valueEntry = value;
		return null;
	}
}
