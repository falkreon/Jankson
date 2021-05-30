package blue.endless.jankson.api.document;

public class PrimitiveValueElement implements ValueElement {
	Object value;
	
	public PrimitiveValueElement(String s) {
		value = s;
	}
	
	public PrimitiveValueElement(long l) {
		value = l; //force it to be a long and then box it ^_^
	}
	
	public PrimitiveValueElement(double d) {
		value = d; //force it into a double and then box it!
	}
	
	@Override
	public ValueElement asValueEntry() {
		//TODO: Convert this document node into a JsonPrimitive or JsonNull
		
		return null;
	}
	
	public String asString() {
		if (value==null) return "null";
		return value.toString();
	}
	
	//TODO: asEverythingElse
}
