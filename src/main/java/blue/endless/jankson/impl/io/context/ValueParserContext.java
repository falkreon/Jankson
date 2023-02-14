/*
 * MIT License
 *
 * Copyright (c) 2018-2023 Falkreon (Isaac Ellingson)
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

package blue.endless.jankson.impl.io.context;

import java.io.IOException;

import blue.endless.jankson.api.SyntaxError;
import blue.endless.jankson.api.document.PrimitiveElement;
import blue.endless.jankson.api.io.ElementType;
import blue.endless.jankson.api.io.JsonReaderOptions;
import blue.endless.jankson.api.io.JsonReaderOptions.Hint;
import blue.endless.jankson.impl.io.LookaheadCodePointReader;

@Deprecated(forRemoval=true)
public class ValueParserContext implements ParserContext {
	private final JsonReaderOptions opts;
	private final boolean isRoot;
	
	public ValueParserContext(JsonReaderOptions opts) {
		this.opts = opts;
		this.isRoot = false;
	}
	
	protected ValueParserContext(JsonReaderOptions opts, boolean isRoot) {
		this.opts = opts;
		this.isRoot = isRoot;
	}
	
	@Override
	public ElementType parse(LookaheadCodePointReader reader) throws IOException, SyntaxError {
		int ch = reader.peek();
		
		switch(ch) {
			case '/':
			case '#':
				//Comment
				return ElementType.COMMENT;
				//break;
				
			case '\'':
			case '"':
				/*
				 * if ALLOW_BARE_ROOT_OBJECT && isRoot, instead of treating this as a root String object, we're going to
				 * treat it as a bare-root with a String key in it, so kick us down one level into an Object context. In
				 * any other circumstance, this will be a String value.
				 */
				
				if (opts.hasHint(Hint.ALLOW_BARE_ROOT_OBJECT) && isRoot) {
					//Start an object and start parsing the key
					return ElementType.OBJECT_START;
				} else {
					//String
					return ElementType.PRIMITIVE;
				}
				//break;
				
			case '{':
				//Object
				return ElementType.OBJECT_START;
				//break;
				
			case '[':
				//Array
				return ElementType.ARRAY_START;
				//break;
			
			case '}':
			case ']':
				//Explicitly Forbidden
				throw new SyntaxError("Unmatched symbol");
			
			default:
				/*
				 * If we reached this point we've arrived at a bare identifier. What we do will depend on the
				 * JsonReaderOptions and our own state. If ALLOW_BARE_ROOT_OBJECT && ALLOW_UNQUOTED_KEYS && isRoot, we're
				 * "looking for" a bare key identifier, and will want to interpret `true` as a key with the name "true"
				 * rather than the boolean value. Otherwise, we're going to parse this as a token value (like "true" or
				 * "NaN"), or a number.
				 */
				
				if (opts.hasHint(Hint.ALLOW_BARE_ROOT_OBJECT) && opts.hasHint(Hint.ALLOW_UNQUOTED_KEYS) && isRoot) {
					//start an object context
					return ElementType.OBJECT_KEY;
				} else {
					//start a token context
					return ElementType.PRIMITIVE;
				}
				//break;
		}
	}

	@Override
	public String getStringValue() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public PrimitiveElement getValue() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean canEOFHere() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isComplete(LookaheadCodePointReader reader) {
		// TODO Auto-generated method stub
		return false;
	}
	
	public static ValueParserContext create(JsonReaderOptions opts) {
		return new ValueParserContext(opts);
	}
	
	public static ValueParserContext createRoot(JsonReaderOptions opts) {
		return new ValueParserContext(opts);
	}
}
