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

package blue.endless.jankson;

import blue.endless.jankson.api.Jankson;
import blue.endless.jankson.api.SyntaxError;
import blue.endless.jankson.api.document.*;
import blue.endless.jankson.api.io.JsonWriterOptions;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringWriter;

public class TestJsonCommentNewline {
    private static final JsonWriterOptions JSON_WRITER_OPTIONS = new JsonWriterOptions("    ",
            JsonWriterOptions.Hint.WRITE_COMMENTS, JsonWriterOptions.Hint.WRITE_NEWLINES);
    private static final String EXPECTED_RESULT = """
            {
                // Comment
                "key": "value"
            }""";

    @Test
    public void testJsonCommentNewlineWriter() throws SyntaxError, IOException {
        ObjectElement objectElement = new ObjectElement();
        KeyValuePairElement keyValuePairElement = new KeyValuePairElement("key", PrimitiveElement.of("value"));
        keyValuePairElement.getPrologue().add(new CommentElement(" Comment", CommentType.LINE_END));
        objectElement.add(keyValuePairElement);
        StringWriter stringWriter = new StringWriter();
        Jankson.writeJson(objectElement, stringWriter, JSON_WRITER_OPTIONS);
        Assertions.assertEquals(EXPECTED_RESULT, stringWriter.getBuffer().toString());
    }

    @Test
    public void testJsonCommentNewlineReaderWriter() throws SyntaxError, IOException {
        ValueElement valueElement = Jankson.readJson(EXPECTED_RESULT);
        StringWriter stringWriter = new StringWriter();
        Jankson.writeJson(valueElement, stringWriter, JSON_WRITER_OPTIONS);
        Assertions.assertEquals(EXPECTED_RESULT, stringWriter.getBuffer().toString());
    }
}
