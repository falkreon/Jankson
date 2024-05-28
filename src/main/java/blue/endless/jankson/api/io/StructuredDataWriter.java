/*
 * MIT License
 *
 * Copyright (c) 2018-2024 Falkreon (Isaac Ellingson)
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

public interface StructuredDataWriter {
	
	public void write(StructuredData data) throws IOException;
	/*
	public void writeComment(String value, CommentType type) throws IOException;
	
	public void writeWhitespace(String value) throws IOException;
	
	public void writeKey(String key) throws IOException;
	
	public void writeObjectStart() throws IOException;
	
	public void writeObjectEnd() throws IOException;
	
	public void writeArrayStart() throws IOException;
	
	public void writeArrayEnd() throws IOException;
	
	public void writeStringLiteral(String value) throws IOException;
	
	public void writeLongLiteral(long value) throws IOException;
	
	public void writeDoubleLiteral(double value) throws IOException;
	
	public void writeBooleanLiteral(boolean value) throws IOException;
	
	public void writeNullLiteral() throws IOException;
	*/
}
