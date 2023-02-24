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

package blue.endless.jankson.api;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;

import blue.endless.jankson.api.document.DocumentBuilder;
import blue.endless.jankson.api.document.ValueElement;
import blue.endless.jankson.api.io.JsonReader;
import blue.endless.jankson.api.io.JsonReaderOptions;


public class Jankson {
	
	/**
	 * Reads in json data from a String using the settings provided.
	 * @param s    the String to interpret as json
	 * @param opts hints and settings to control the reading process
	 * @return     a ValueElement representing the document root
	 * @throws IOException if there was a problem reading the String. This should almost never happen
	 * @throws SyntaxError if there was a problem with the syntax or structure of the json document
	 */
	public static ValueElement readJson(String s, JsonReaderOptions opts) throws IOException, SyntaxError {
		JsonReader reader = new JsonReader(new StringReader(s), opts);
		return DocumentBuilder.build(reader);
	}
	
	/**
	 * Reads in json data from a Reader, using the settings provided. The Reader will be read all the way to the end of
	 * the stream, but will not be closed.
	 * @param r    the Reader that is reading json character data
	 * @param opts hints and settings to control the reading process
	 * @return     a ValueElement representing the document root
	 * @throws IOException if there was a problem reading the String. This should almost never happen
	 * @throws SyntaxError if there was a problem with the syntax or structure of the json document
	 */
	public static ValueElement readJson(Reader r, JsonReaderOptions opts) throws IOException, SyntaxError {
		return DocumentBuilder.build(new JsonReader(r));
	}
	
	/**
	 * Reads in json data from an InputStream, using the settings provided. The data will be interpreted as UTF-8
	 * character data. Characters will be read until the end of the stream, but the stream will not be closed by this
	 * method.
	 * @param r    the InputStream that is reading the json document
	 * @param opts hints and settings to control the reading process
	 * @return     a ValueElement representing the document root
	 * @throws IOException if there was a problem reading the String. This should almost never happen
	 * @throws SyntaxError if there was a problem with the syntax or structure of the json document
	 */
	public static ValueElement readJson(InputStream in, JsonReaderOptions opts) throws IOException, SyntaxError {
		JsonReader reader = new JsonReader(new InputStreamReader(in, StandardCharsets.UTF_8), opts);
		return DocumentBuilder.build(reader);
	}
	
	/**
	 * Reads in json data from a String using the default settings.
	 * @see #readJson(String, JsonReaderOptions)
	 */
	public static ValueElement readJson(String s) throws IOException, SyntaxError {
		return readJson(s, JsonReaderOptions.UNSPECIFIED);
	}
	
	/**
	 * Reads in json data from a Reader, using the default settings.
	 * @see #readJson(Reader, JsonReaderOptions)
	 */
	public static ValueElement readJson(Reader r) throws IOException, SyntaxError {
		return readJson(r, JsonReaderOptions.UNSPECIFIED);
	}
	
	/**
	 * Reads in json data from an InputStream, using the default settings.
	 * @see #readJson(InputStream, JsonReaderOptions)
	 */
	public static ValueElement readJson(InputStream in) throws IOException, SyntaxError {
		return readJson(in, JsonReaderOptions.UNSPECIFIED);
	}
}
