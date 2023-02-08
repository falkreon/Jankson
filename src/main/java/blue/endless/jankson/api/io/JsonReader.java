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
import blue.endless.jankson.api.document.CommentElement;
import blue.endless.jankson.api.document.CommentType;
import blue.endless.jankson.api.io.JsonReaderOptions.Hint;
import blue.endless.jankson.impl.io.AbstractStructuredDataReader;

public class JsonReader extends AbstractStructuredDataReader {
	private final JsonReaderOptions options;
	
	public JsonReader(Reader source) {
		this(source, new JsonReaderOptions());
	}
	
	public JsonReader(Reader source, JsonReaderOptions options) {
		super(source);
		this.options = options;
	}
	
	@Override
	protected void nextCharacter() throws IOException, SyntaxError {
		ReaderState state = peekState();
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
		}
	}
	
	
}
