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

package blue.endless.jankson.api.io;

import java.io.IOException;
import java.io.Reader;

import blue.endless.jankson.api.SyntaxError;
import blue.endless.jankson.impl.io.AbstractStructuredDataReader;
import blue.endless.jankson.impl.io.context.ObjectParserContext;
import blue.endless.jankson.impl.io.context.ParserContext;

public class JsonReader extends AbstractStructuredDataReader {
	private final JsonReaderOptions options;
	
	public JsonReader(Reader source) {
		this(source, new JsonReaderOptions());
	}
	
	public JsonReader(Reader source, JsonReaderOptions options) {
		super(source);
		this.options = options;
		pushContext(new ObjectParserContext(options)); //TODO: Push a root context first
	}
	
	@Override
	protected void readNext() throws IOException, SyntaxError {
		ParserContext context = getContext();
		//System.out.println("ReadNext > Context: "+context);
		if (context==null) throw new IllegalStateException("Root context was popped");
		if (context.isComplete(src)) {
			//System.out.println("IsComplete.");
			popContext();
			if (getContext()==null) {
				enqueueOutput(ElementType.EOF, null);
			}
		} else {
			context.parse(
					src,
					this::enqueueOutput,
					this::pushContext
					);
		}
	}
}
