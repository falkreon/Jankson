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

import blue.endless.jankson.JsonArray;
import blue.endless.jankson.JsonLoader;
import blue.endless.jankson.JsonNull;
import blue.endless.jankson.JsonPrimitive;

public class ArrayParserContext implements ParserContext<JsonArray> {
	private JsonArray result = new JsonArray();
	private boolean foundClosingBrace = false;
	private String comment = null;
	
	/** Assumes the opening brace has already been consumed! */
	public ArrayParserContext() {
		
	}
	
	@Override
	public boolean consume(int codePoint, JsonLoader loader) throws SyntaxError {
		if (foundClosingBrace) return false;
		if (Character.isWhitespace(codePoint)) return true;
		
		switch(codePoint) {
		case ',':
			return true;
		case '[':
			loader.push(new ArrayParserContext(), (it)->{
				result.add(it, comment);
				comment = null;
			});
			return true; //Arrays assume we *have* consumed the opening bracket
		case '{':
			loader.push(new ObjectParserContext(), (it)->{
				result.add(it, comment);
				comment = null;
			});
			return false; //Objects assume we *haven't* consumed the opening brace
		case '\"':
		case '\'':
			loader.push(new StringParserContext(codePoint), (it)->{
				result.add(it, comment);
				comment = null;
			});
			return true;
		case ']':
			foundClosingBrace = true;
			return true;
		case '/':
			loader.push(new CommentParserContext(codePoint), (it)->{
				comment = it;
			});
			return true;
		default:
			if (Character.isDigit(codePoint)) {
				loader.push(new NumberParserContext(codePoint), (it)->{
					result.add(it, comment);
					comment = null;
				});
				return true;
			} else {
				loader.push(new TokenParserContext(codePoint), (it)->{
					if (it.asString().toLowerCase(Locale.ROOT).equals("null")) {
						result.add(JsonNull.INSTANCE, comment);
						comment = null;
					} else if (it.asString().toLowerCase(Locale.ROOT).equals("true")) {
						result.add(new JsonPrimitive(Boolean.TRUE), comment);
						comment = null;
					}  else if (it.asString().toLowerCase(Locale.ROOT).equals("false")) {
						result.add(new JsonPrimitive(Boolean.FALSE), comment);
						comment = null;
					} else {
						loader.throwDelayed(new SyntaxError("Found unrecognized token '"+it.asString()+"' while parsing array elements"));
					}
				});
				
				return true;
			}
		}
	}

	@Override
	public void eof() throws SyntaxError {
		if (foundClosingBrace) return;
		throw new SyntaxError("Unexpected end-of-file in the middle of a list! Are you missing a ']'?");
	}

	@Override
	public boolean isComplete() {
		return foundClosingBrace;
	}

	@Override
	public JsonArray getResult() throws SyntaxError {
		return result;
	}

}
