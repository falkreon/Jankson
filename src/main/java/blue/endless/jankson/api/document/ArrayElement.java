package blue.endless.jankson.api.document;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ArrayElement implements ValueElement {
	protected CommentElement commentBefore;
	protected CommentElement commentAfter;
	protected List<DocumentElement> entries = new ArrayList<>();
	
	public DocumentElement get(int index) {
		int cur = 0;
		for(DocumentElement entry : entries) {
			if (entry.isValueEntry()) {
				if (index==cur) return entry;
				cur++;
			}
		}
		
		throw new IndexOutOfBoundsException("Index: "+index+", Size: "+cur);
	}
	
	public int size() {
		int result = 0;
		for(DocumentElement entry : entries) {
			if (entry.isValueEntry()) result++;
		}
		
		return result;
	}
	
	/*
	public DocumentEntry iterator() {
		//TODO: Implement
	}*/
	
	public void add(DocumentElement entry) {
		this.entries.add(entry);
	}
	
	public int entrySize() {
		return entries.size();
	}
	
	public DocumentElement getEntry(int i) {
		return entries.get(i);
	}
	
	public Iterator<DocumentElement> entryIterator() {
		return entries.iterator();
	}
	
	public int elementCount() {
		int counter = 0;
		for(DocumentElement entry : entries) {
			if (!(entry instanceof CommentElement)) counter++;
			
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
	public ValueElement asValueEntry() {
		return this;
	}
}
