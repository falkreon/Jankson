/*
 * MIT License
 *
 * Copyright (c) 2018 Falkreon
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package blue.endless.jankson.impl;

import java.util.Locale;

import blue.endless.jankson.JsonLoader;
import blue.endless.jankson.JsonNull;
import blue.endless.jankson.JsonObject;
import blue.endless.jankson.JsonPrimitive;

public class ObjectParserContext implements ParserContext<JsonObject> {
	private JsonObject result = new JsonObject();
	
	private String comment;
	private boolean openBraceFound = false;
	private String key;
	private boolean colonFound = false;
	private boolean closeBraceFound = false;
	
	public ObjectParserContext() {}
	
	
	@Override
	public boolean consume(int codePoint, JsonLoader loader) throws SyntaxError {
		if (!openBraceFound) {
			if (Character.isWhitespace(codePoint)) return true; //We're fine, this is just whitespace
			if (codePoint=='{') { //We're good. We can start parsing the object now
				openBraceFound = true;
				return true;
			}
			
			throw new SyntaxError("Found character '"+(char)codePoint+"' instead of '{' while looking for the start of an object");
		} else {
			if (closeBraceFound) return false; //Shouldn't happen!
			
			if (key==null) {
				if (Character.isWhitespace(codePoint)) return true;
				
				//Expecting: Key or End
				switch(codePoint) {
				case '}':
					closeBraceFound = true;
					return true;
				case ',':
					return true; //commas are basically whitespace to us
				case '\'':
				case '"':
					loader.push(new StringParserContext(codePoint), (it)->key=it.asString());
					return true;
				case '/':
					loader.push(new CommentParserContext(codePoint), (it)->comment=it);
					return true;
				default:
					loader.push(new TokenParserContext(codePoint), (it)->key=it.asString());
					return true;
				}
				
			} else if (colonFound) {
				//Expecting: Value
				
				if (Character.isWhitespace(codePoint)) return true;
				
				switch(codePoint) {
				case '}':
					throw new SyntaxError("Found end of object ('}') when a value for key '"+key+"' was expected");
				case '\'':
				case '"':
					loader.push(new StringParserContext(codePoint), (it)->{
						result.put(key, it, comment);
						key = null;
						colonFound = false;
						comment = null;
					});
					return true;
				case '{':
					loader.push(new ObjectParserContext(), (it)->{
						result.put(key, it, comment);
						key = null;
						colonFound = false;
						comment = null;
					});
					return false;
				case '[':
					loader.push(new ArrayParserContext(), (it)->{
						result.put(key, it, comment);
						key = null;
						colonFound = false;
						comment = null;
					});
					return true;
				default:
					if (Character.isDigit(codePoint)) {
						loader.push(new NumberParserContext(codePoint), (it)->{
							result.put(key, it, comment);
							key = null;
							colonFound = false;
							comment = null;
						});
						return true;
					}
					
					loader.push(new TokenParserContext(codePoint), (it)->{
						if (it.asString().toLowerCase(Locale.ROOT).equals("null")) {
							result.put(key, JsonNull.INSTANCE, comment);
							key = null;
							colonFound = false;
							comment = null;
						} else if (it.asString().toLowerCase(Locale.ROOT).equals("true")) {
							result.put(key, new JsonPrimitive(Boolean.TRUE), comment);
							key = null;
							colonFound = false;
							comment = null;
						}  else if (it.asString().toLowerCase(Locale.ROOT).equals("false")) {
							result.put(key, new JsonPrimitive(Boolean.FALSE), comment);
							key = null;
							colonFound = false;
							comment = null;
						} else {
							loader.throwDelayed(new SyntaxError("Found unrecognized token '"+it.asString()+"' while parsing the value of key '"+key+"'"));
						}
					});
					
					return true;
				}
			} else {
				if (Character.isWhitespace(codePoint)) return true; //Don't care about whitespace
				if (codePoint==':') {
					colonFound = true;
					return true;
				}
				
				throw new SyntaxError("Found unexpected character '"+(char)codePoint+"' while looking for the colon (':') between a key and a value in an object");
			}
		}
	}

	@Override
	public boolean isComplete() {
		return closeBraceFound;
	}

	@Override
	public JsonObject getResult() {
		return result;
	}


	@Override
	public void eof() throws SyntaxError {
		if (closeBraceFound) return;
		throw new SyntaxError("Expected to find '}' to end an object, found EOF instead.");
	}
	
	
}
