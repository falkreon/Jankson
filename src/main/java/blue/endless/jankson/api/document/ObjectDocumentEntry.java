package blue.endless.jankson.api.document;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.annotation.Nullable;

import blue.endless.jankson.api.element.JsonObject;

public class ObjectDocumentEntry implements DocumentEntry, Iterable<DocumentEntry> {
	protected CommentDocumentEntry commentBefore = null;
	protected CommentDocumentEntry commentAfter = null;
	protected List<DocumentEntry> entries = new ArrayList<>();
	
	@Override
	public Iterator<DocumentEntry> iterator() {
		return entries.iterator();
	}
	
	@Nullable
	public DocumentEntry get(String key) {
		for(DocumentEntry entry : entries) {
			if (entry instanceof KeyValueDocumentEntry) {
				if (((KeyValueDocumentEntry) entry).getKey().equals(key)) {
					return ((KeyValueDocumentEntry) entry).getValue();
				}
			}
		}
		
		return null;
	}
	
	public DocumentEntry put(String key, DocumentEntry value) {
		for(DocumentEntry entry : entries) {
			if (entry instanceof KeyValueDocumentEntry) {
				KeyValueDocumentEntry pair = (KeyValueDocumentEntry) entry;
				
				if (pair.getKey().equals(key)) {
					return pair.setValue(value);
				}
			}
		}
		
		//No matching KeyValueDocumentEntry. Add one at the end of the object's sub-document
		entries.add(new KeyValueDocumentEntry(key, value));
		return null;
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
	public JsonObject asJsonElement() {
		//TODO: Deep copy (and possibly cache) this object into a JsonObject
		
		return null;
	}
}
