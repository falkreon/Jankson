package blue.endless.jankson.api.document;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import blue.endless.jankson.api.element.JsonArray;

public class ArrayDocumentEntry implements DocumentEntry, Iterable<DocumentEntry> {
	protected List<DocumentEntry> entries = new ArrayList<>();

	@Override
	public Iterator<DocumentEntry> iterator() {
		return entries.iterator();
	}
	
	//TODO: API
	
	public DocumentEntry getEntry(int i) {
		return entries.get(i);
	}
	
	public int size() {
		return entries.size();
	}
	
	public int elementCount() {
		int counter = 0;
		for(DocumentEntry entry : entries) {
			if (!(entry instanceof CommentDocumentEntry)) counter++;
			
			/*
			//Alternate way to do this
			if (entry instanceof ArrayDocumentEntry) counter++;
			if (entry instanceof ObjectDocumentEntry) counter++;
			if (entry instanceof PrimitiveValueDocumentEntry) counter++;
			*/
		}
		
		return counter;
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
	public JsonArray asJsonElement() {
		//TODO: Translate, and possibly cache, a deep copy of this Document node as a JsonArray
		
		return null;
	}
}
