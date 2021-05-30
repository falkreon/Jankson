package blue.endless.jankson.impl.context.toml;

import blue.endless.jankson.api.Jankson;
import blue.endless.jankson.api.SyntaxError;
import blue.endless.jankson.api.element.JsonObject;
import blue.endless.jankson.impl.context.ParserContext;

public class ObjectParserContext implements ParserContext<JsonObject> {

	@Override
	public boolean consume(int codePoint, Jankson loader) throws SyntaxError {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void eof() throws SyntaxError {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean isComplete() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public JsonObject getResult() throws SyntaxError {
		// TODO Auto-generated method stub
		return null;
	}
	
}
