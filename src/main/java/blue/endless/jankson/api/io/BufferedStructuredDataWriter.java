/*
 * MIT License
 *
 * Copyright (c) 2018-2025 Falkreon (Isaac Ellingson)
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
import java.io.Writer;

import blue.endless.jankson.api.SyntaxError;
import blue.endless.jankson.api.document.ValueElement;
import blue.endless.jankson.api.function.CheckedConsumer;

/**
 * A StructuredDataWriter that cannot operate in streaming mode, but CAN skip streaming and buffering if paired with a
 * ValueElement or a ValueElementReader. Generally these writers target file formats or memory representations that can
 * not be constructed from an in-order traversal of elements. Instead, these writers perform random-access on the data.
 * 
 * <p>
 * As with other readers and writers that deal with user-accessible values, it is invalid to modify the value while it
 * is being written. If you think there may be threading concerns, either clone your value or write it to a
 * StructuredDataBuffer.
 */
public interface BufferedStructuredDataWriter extends StructuredDataWriter {
	
	/**
	 * Writes a ValueElement directly using this Writer. Should typically only be called once per Writer instance.
	 * @param value        A ValueElement. Some Writers may restrict the kind of elements that may be written.
	 * @throws SyntaxError If the ValueElement is improperly structured for this written type (such as writing a root
	 *                     Array to a TOML document) or if too many root elements were written.
	 * @throws IOException If there was a problem writing to the underlying Writer.
	 */
	public void write(ValueElement value) throws SyntaxError, IOException;
	
	/**
	 * Uses function composition to create a BufferedStructuredDataWriter out of a lambda or method reference.
	 * @param valueWriter A lambda that will take the fully buffered ValueElement and write it to the underlying media.
	 * @return The newly-created BufferedStructuredDataWriter
	 */
	public static BufferedStructuredDataWriter of(CheckedConsumer<ValueElement, IOException> valueWriter) {
		return new ComposedBufferedStructuredDataWriter(valueWriter);
	}
	
	public static abstract class AbstractBufferedStructuredDataWriter implements BufferedStructuredDataWriter {
		private final ValueElementWriter valueWriter = new ValueElementWriter();
		private boolean complete = false;
		
		@Override
		public final void write(StructuredData data) throws SyntaxError, IOException {
			if (complete && data.type().isSemantic()) throw new SyntaxError("Too much data found while buffering a value");
			
			valueWriter.write(data);
			
			if (valueWriter.isComplete()) {
				complete = true;
				write(valueWriter.getResult());
			}
		}
	}
	
	public static class ComposedBufferedStructuredDataWriter extends AbstractBufferedStructuredDataWriter {
		private final CheckedConsumer<ValueElement, IOException> valueWriter;
		public ComposedBufferedStructuredDataWriter(CheckedConsumer<ValueElement, IOException> valueWriter) {
			this.valueWriter = valueWriter;
		}
		
		@Override
		public void write(ValueElement value) throws SyntaxError, IOException {
			valueWriter.accept(value);
		}
	}
	
}
