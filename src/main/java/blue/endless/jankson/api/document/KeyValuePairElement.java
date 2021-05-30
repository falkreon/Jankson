package blue.endless.jankson.api.document;

import java.util.ArrayList;
import java.util.List;

public class KeyValuePairElement implements DocumentElement {
	protected CommentElement commentBefore = null;
	protected CommentElement commentAfter = null;
	protected List<DocumentElement> entries = new ArrayList<>();
	protected String key;
	protected ValueElement valueEntry;
	
	public KeyValuePairElement(String key, ValueElement value) {
		this.key = key;
		entries.add(value);
		valueEntry = value;
	}
	
	public KeyValuePairElement(String key, DocumentElement value, String comment) {
		commentBefore = new CommentElement(comment);
	}
	
	public String getKey() {
		return key;
	}
	
	public ValueElement getValue() {
		return valueEntry;
	}
	
	public DocumentElement setValue(ValueElement value) {
		ValueElement result = valueEntry;
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
