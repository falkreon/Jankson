package blue.endless.jankson.impl;

import java.util.Locale;

import blue.endless.jankson.Jankson;
import blue.endless.jankson.JsonElement;
import blue.endless.jankson.JsonNull;
import blue.endless.jankson.JsonPrimitive;

public class ElementParserContext implements ParserContext<AnnotatedElement> {
	String comment = null;
	AnnotatedElement result = null;
	boolean childActive = false;
	
	@Override
	public boolean consume(int codePoint, Jankson loader) throws SyntaxError {
		//Figure out element type and dispatch down to 
		
		if (Character.isWhitespace(codePoint)) return true; //Whitespace
		switch(codePoint) {
		case '/':
		case '#': //Comment
			
			loader.push(new CommentParserContext(codePoint), (it)->comment=it);
			return true;
		case '\'':
		case '"': //String
			loader.push(new StringParserContext(codePoint), this::setResult);
			childActive = true;
			return true;
		case '{': //Object
			loader.push(new ObjectParserContext(), this::setResult);
			childActive = true;
			return false; //Give the opening brace to the object context
		case '[': //Array
			loader.push(new ArrayParserContext(), this::setResult);
			childActive = true;
			return true;
			
		case '}':
		case ']':
			System.out.println("Curious! Bailing from our intermediate context.");
			//loader.throwDelayed(new SyntaxError("Found '}' while parsing an element - this shouldn't happen!"));
			return false;
		default:
			if (Character.isDigit(codePoint) || codePoint=='-' || codePoint=='+') {
				loader.push(new NumberParserContext(codePoint), this::setResult);
				childActive = true;
				return true;
			}
			
			loader.push(new TokenParserContext(codePoint), (it)->{
				if (it.asString().toLowerCase(Locale.ROOT).equals("null")) {
					setResult(JsonNull.INSTANCE);
				} else if (it.asString().toLowerCase(Locale.ROOT).equals("true")) {
					setResult(JsonPrimitive.TRUE);
				} else if (it.asString().toLowerCase(Locale.ROOT).equals("false")) {
					setResult(JsonPrimitive.FALSE);
				} else {
					loader.throwDelayed(new SyntaxError("Found unrecognized token '"+it.asString()+"' while looking for a json element of any type."));
				}
			});
			childActive = true;
			
			return true;
		}
		
	}
	
	public void setResult(JsonElement elem) {
		result = new AnnotatedElement(elem, comment);
	}
	
	@Override
	public void eof() throws SyntaxError {
		//We should be fine as long as any child parser has been initiated.
		if (!childActive) throw new SyntaxError("Unexpected end-of-file while looking for a json element!");
	}
	
	@Override
	public boolean isComplete() {
		return result!=null;
	}
	
	@Override
	public AnnotatedElement getResult() throws SyntaxError {
		return result;
	}

}
