package blue.endless.jankson.api.document;

public class CommentElement implements FormattingElement {
	protected String value;
	protected boolean lineEnd;
	
	public CommentElement(String comment) {
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
	public CommentElement asComment() {
		return this;
	}
}
