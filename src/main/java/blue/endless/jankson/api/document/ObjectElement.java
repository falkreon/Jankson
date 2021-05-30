package blue.endless.jankson.api.document;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

public class ObjectElement implements ValueElement {
	protected CommentElement commentBefore = null;
	protected CommentElement commentAfter = null;
	protected List<DocumentElement> entries = new ArrayList<>();
	
	@Nullable
	public ValueElement get(String key) {
		for(DocumentElement entry : entries) {
			if (entry instanceof KeyValuePairElement) {
				if (((KeyValuePairElement) entry).getKey().equals(key)) {
					return ((KeyValuePairElement) entry).getValue();
				}
			}
		}
		
		return null;
	}
	
	public DocumentElement put(String key, ValueElement value) {
		//Validate
		if (
				value instanceof KeyValuePairElement ||
				value instanceof CommentElement) throw new IllegalArgumentException();
		
		for(DocumentElement entry : entries) {
			if (entry instanceof KeyValuePairElement) {
				KeyValuePairElement pair = (KeyValuePairElement) entry;
				
				if (pair.getKey().equals(key)) {
					return pair.setValue(value);
				}
			}
		}
		
		//No matching KeyValueDocumentEntry. Add one at the end of the object's sub-document
		entries.add(new KeyValuePairElement(key, value));
		return null;
	}
	
	public CommentElement commentBefore() {
		return commentBefore;
	}
	
	public CommentElement commentAfter() {
		return commentAfter;
	}
	
	public void setCommentBefore(String comment) {
		commentBefore = new CommentElement(comment);
	}
	
	public void setCommentBefore(CommentElement comment) {
		commentBefore = comment;
	}
	
	public void setCommentAfter(String comment) {
		commentAfter = new CommentElement(comment);
		commentAfter.setLineEnd(true);
	}
	
	public void setCommentAfter(CommentElement comment) {
		commentAfter = comment;
		//Possibly force comment as a lineEndComment but for now assume user knows what they're doing
	}

	@Override
	public boolean isComment() {
		return false;
	}

	@Override
	public CommentElement asComment() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isValueEntry() {
		return true;
	}

	@Override
	public ValueElement asValueEntry() {
		return this;
	}
}
