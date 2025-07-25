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

package blue.endless.jankson.api.codec;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import blue.endless.jankson.api.io.Deserializer;
import blue.endless.jankson.api.io.StructuredDataReader;

public class CodecHolder implements CodecManager {
	private List<StructuredDataCodec> codecs = new ArrayList<>();

	@Override
	public @Nullable StructuredDataReader getReader(Object o) {
		for(StructuredDataCodec codec : codecs) {
			if (codec.appliesTo(o)) return codec.getReader(o);
		}
		
		return null;
	}

	@Override
	public @Nullable <T> Deserializer<T> getWriter(@Nonnull T existingValue) {
		for(StructuredDataCodec codec : codecs) {
			if (codec.appliesTo(existingValue.getClass())) return codec.getWriter(existingValue);
		}
		
		return null;
	}

	@Override
	public @Nullable <T> Deserializer<T> getWriter(Type t) {
		for(StructuredDataCodec codec : codecs) {
			if (codec.appliesTo(t)) return codec.getWriter();
		}
		
		return null;
	}
	
	
}
