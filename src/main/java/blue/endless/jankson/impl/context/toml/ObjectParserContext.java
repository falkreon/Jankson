/*
 * MIT License
 *
 * Copyright (c) 2018-2020 Falkreon (Isaac Ellingson)
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
