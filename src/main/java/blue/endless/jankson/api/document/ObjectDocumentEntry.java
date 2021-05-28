package blue.endless.jankson.api.document;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ObjectDocumentEntry implements DocumentEntry, Iterable<DocumentEntry> {
	protected List<DocumentEntry> entries = new ArrayList<>();

	@Override
	public Iterator<DocumentEntry> iterator() {
		return entries.iterator();
	}
	
	//TODO: API
	
	public DocumentEntry getValue(String key) {
		boolean foundKey = false;
		for(DocumentEntry entry : entries) {
			if (foundKey) {
				if (entry instanceof KeyDocumentEntry) return null;
				if (entry instanceof CommentDocumentEntry) continue;
				return entry;
			} else {
				if (entry instanceof KeyDocumentEntry) {
					/* // How universal are String values? Is this something specific to keys or something we could find for all entries?
					if (entry.getValue().equals(key)) {
						foundKey = true;
					}
					*/
				}
				
			}
		}
		
		return null;
	}
}
