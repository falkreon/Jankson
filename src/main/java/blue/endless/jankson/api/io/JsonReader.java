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
		pushContext(new ObjectParserContext()); //TODO: Push a root context first
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
		
		/*ReaderState state = peekState();
		switch(state) {
			case ROOT:
				if (options.hasHint(Hint.ALLOW_BARE_ROOT_OBJECT)) {
					//We need to provisionally read until we hit either an opening brace, an opening bracket, something
					//which looks like a string, or something that looks like a bare token
					skipNonBreakingWhitespace();
					
				} else {
					
					skipNonBreakingWhitespace();
					int ch = src.peek();
					if (ch=='{') {
						src.read(); //Discard brace
						this.pushState(ReaderState.OBJECT);
						this.enqueueOutput(ElementType.OBJECT_START);
					} else if (ch=='[') {
						src.read(); //Discard bracket
						this.pushState(ReaderState.ARRAY);
						this.enqueueOutput(ElementType.ARRAY_START);
					} else if (ch=='/') {
						String commentStarter = src.peekString(2);
						if (commentStarter.equals("//")) {
							src.read(); //Discard slashes
							src.read();
							
							StringBuilder sb = new StringBuilder();
							ch = src.read();
							while(ch!='\n' && ch!=-1) {
								sb.appendCodePoint(ch);
								ch = src.read();
							}
							String commentText = sb.toString();
							this.setLatestValue(new CommentElement(commentText, CommentType.LINE_END));
							this.enqueueOutput(ElementType.COMMENT);
						} else if (commentStarter.equals("/*")) {
							//This is a multiline comment - but if the first character is an additional asterisk, omit
							//that too and log this as a doc comment
						}
						
					} else if (ch=='#') {
						//Read until line end
						src.read(); //Discard octothorpe
						
						StringBuilder sb = new StringBuilder();
						ch = src.read();
						while(ch!='\n' && ch!=-1) {
							sb.appendCodePoint(ch);
							ch = src.read();
						}
						String commentText = sb.toString();
						this.setLatestValue(new CommentElement(commentText, CommentType.OCTOTHORPE));
						this.enqueueOutput(ElementType.COMMENT);
					}
				}
				break;
			case OBJECT: {
				skipNonBreakingWhitespace();
				int ch = src.peek();
				//TODO: List of forbidden characters
				if (ch=='}') {
					this.popState();
					this.enqueueOutput(ElementType.OBJECT_END);
				} else if (ch=='\n') {
					this.enqueueOutput(ElementType.NEWLINE);
				}
			}
				break;
			case ARRAY: {
				skipNonBreakingWhitespace();
				int ch = src.peek();
				//TODO: List of forbidden characters
				if (ch==']') {
					this.popState();
					this.enqueueOutput(ElementType.ARRAY_END);
				} else if (ch=='\n') {
					this.enqueueOutput(ElementType.NEWLINE);
				}
			}
				break;
		}*/
	}
	
	private void scanObject() throws IOException, SyntaxError {
		/*
		 * Object parsing has 5 distinct phases:
		 * - Before initial brace
		 * - Before key
		 * - Before delimiter
		 * - Before value (which may result in a push and a state change)
		 * - Before newline (where we clean up any end-of-line comments and switch to "before key" before we hit anything semantic)
		 * 
		 * When we walk down from the stack we'll always be at "before newline". BUT, if you don't care about comment attribution,
		 * which we don't at this level, then "before newline" is the same as "before key".
		 * 
		 * We can also get rid of "before delimiter" and actively grab the delimiter as part of the key parsing process.
		 * 
		 * That leaves us with:
		 * - Before initial brace
		 * - Before key
		 * - Before value
		 */
	}
	
	private void scanArray() throws IOException, SyntaxError {
		/*
		 * Array parsing:
		 * - Before initial bracket
		 * - Before value
		 * (an end bracket is permitted to occur Before Value).
		 */
	}
}
