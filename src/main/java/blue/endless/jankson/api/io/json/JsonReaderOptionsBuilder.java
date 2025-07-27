package blue.endless.jankson.api.io.json;

public class JsonReaderOptionsBuilder {
	protected boolean bareRootObject = false;
	protected boolean unquotedKeys = false;
	protected char keyValueSeparator = ':';
	
	public boolean isBareRootObject() { return bareRootObject; }
	public void setBareRootObject(boolean value) { bareRootObject = value; }
	
	public boolean isUnquotedKeys() { return unquotedKeys; }
	public void setUnquotedKeys(boolean value) { unquotedKeys = value; }
	
	public char getKeyValueSeparator() { return keyValueSeparator; }
	public void setKeyValueSeparator(char ch) {
		if (Character.isJavaIdentifierPart(ch)) {
			throw new IllegalArgumentException("Java identifier characters are not allowed as key-value separators.");
		}
		this.keyValueSeparator = ch;
	}
	
	public JsonReaderOptions build() {
		return new JsonReaderOptions(this);
	}
}
