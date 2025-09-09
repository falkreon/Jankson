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

package blue.endless.jankson.api;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;

import blue.endless.jankson.api.document.ObjectElement;
import blue.endless.jankson.api.document.ValueElement;
import blue.endless.jankson.api.io.ObjectReaderFactory;
import blue.endless.jankson.api.io.ObjectWriter;
import blue.endless.jankson.api.io.StructuredDataReader;
import blue.endless.jankson.api.io.ValueElementWriter;
import blue.endless.jankson.api.io.json.JsonReader;
import blue.endless.jankson.api.io.json.JsonReaderOptions;
import blue.endless.jankson.api.io.json.JsonWriter;
import blue.endless.jankson.api.io.json.JsonWriterOptions;


public class Jankson {
	
	/**
	 * Reads in json data from a String using the settings provided.
	 * @param s    the String to interpret as json
	 * @param opts hints and settings to control the reading process
	 * @return     a ValueElement representing the document root
	 * @throws IOException if there was a problem reading the String. This should almost never happen
	 * @throws SyntaxError if there was a problem with the syntax or structure of the json document
	 */
	public static ValueElement readJson(String s, JsonReaderOptions.Access opts) throws IOException, SyntaxError {
		JsonReader reader = new JsonReader(new StringReader(s), opts);
		ValueElementWriter writer = new ValueElementWriter();
		reader.transferTo(writer);
		return writer.getResult();
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
	public static ValueElement readJson(Reader r, JsonReaderOptions.Access opts) throws IOException, SyntaxError {
		JsonReader reader = new JsonReader(r, opts);
		ValueElementWriter writer = new ValueElementWriter();
		reader.transferTo(writer);
		return writer.getResult();
	}
	
	/**
	 * Reads in json data from an InputStream, using the settings provided. The data will be interpreted as UTF-8
	 * character data. Characters will be read until the end of the stream, but the stream will not be closed by this
	 * method.
	 * @param in   the InputStream that is reading the json document
	 * @param opts hints and settings to control the reading process
	 * @return     a ValueElement representing the document root
	 * @throws IOException if there was a problem reading the String. This should almost never happen
	 * @throws SyntaxError if there was a problem with the syntax or structure of the json document
	 */
	public static ValueElement readJson(InputStream in, JsonReaderOptions.Access opts) throws IOException, SyntaxError {
		JsonReader reader = new JsonReader(new InputStreamReader(in, StandardCharsets.UTF_8), opts);
		ValueElementWriter writer = new ValueElementWriter();
		reader.transferTo(writer);
		return writer.getResult();
	}
	
	/**
	 * Reads in json data from a String using the default settings.
	 * @see #readJson(String, JsonReaderOptions.Access)
	 */
	public static ValueElement readJson(String s) throws IOException, SyntaxError {
		return readJson(s, JsonReaderOptions.UNSPECIFIED);
	}
	
	/**
	 * Reads in json data from a Reader, using the default settings.
	 * @see #readJson(Reader, JsonReaderOptions.Access)
	 */
	public static ValueElement readJson(Reader r) throws IOException, SyntaxError {
		return readJson(r, JsonReaderOptions.UNSPECIFIED);
	}
	
	/**
	 * Reads in json data from an InputStream, using the default settings.
	 * @see #readJson(InputStream, JsonReaderOptions.Access)
	 */
	public static ValueElement readJson(InputStream in) throws IOException, SyntaxError {
		return readJson(in, JsonReaderOptions.UNSPECIFIED);
	}
	
	/**
	 * Reads in a json object from a String using the settings provided.
	 * @param s    the String to interpret as json
	 * @param opts hints and settings to control the reading process
	 * @return     a ValueElement representing the document root
	 * @throws IOException if there was a problem reading the String. This should almost never happen
	 * @throws SyntaxError if there was a problem with the syntax or structure of the json document, or if the value is not a json Object element.
	 */
	public static ObjectElement readJsonObject(String s, JsonReaderOptions.Access opts) throws IOException, SyntaxError {
		ValueElement elem = readJson(s, opts);
		if (elem instanceof ObjectElement obj) {
			return obj;
		} else {
			throw new SyntaxError("Object expected, but found "+elem.getClass().getSimpleName());
		}
	}
	
	/**
	 * Reads in a json object from a Reader, using the settings provided. The Reader will be read all the way to the end
	 * of the stream, but will not be closed.
	 * @param r    the Reader that is reading json character data
	 * @param opts hints and settings to control the reading process
	 * @return     a ValueElement representing the document root
	 * @throws IOException if there was a problem reading the String. This should almost never happen
	 * @throws SyntaxError if there was a problem with the syntax or structure of the json document, or if the value is not a json Object element.
	 */
	public static ObjectElement readJsonObject (Reader r, JsonReaderOptions.Access opts) throws IOException, SyntaxError {
		ValueElement elem = readJson(r, opts);
		if (elem instanceof ObjectElement obj) {
			return obj;
		} else {
			throw new SyntaxError("Object expected, but found "+elem.getClass().getSimpleName());
		}
	}
	
	/**
	 * Reads in a json object from an InputStream, using the settings provided. The data will be interpreted as UTF-8
	 * character data. Characters will be read until the end of the stream, but the stream will not be closed by this
	 * method.
	 * @param in   the InputStream that is reading the json document
	 * @param opts hints and settings to control the reading process
	 * @return     a ValueElement representing the document root
	 * @throws IOException if there was a problem reading the String. This should almost never happen
	 * @throws SyntaxError if there was a problem with the syntax or structure of the json document, or if the value is not a json Object element.
	 */
	public static ObjectElement readJsonObject(InputStream in, JsonReaderOptions.Access opts) throws IOException, SyntaxError {
		ValueElement elem = readJson(in, opts);
		if (elem instanceof ObjectElement obj) {
			return obj;
		} else {
			throw new SyntaxError("Object expected, but found "+elem.getClass().getSimpleName());
		}
	}
	
	/**
	 * Reads in a json object from a String using the default settings.
	 * @see #readJsonObject(String, JsonReaderOptions.Access)
	 */
	public static ObjectElement readJsonObject(String s) throws IOException, SyntaxError {
		return readJsonObject(s, JsonReaderOptions.UNSPECIFIED);
	}
	
	/**
	 * Reads in a json object from a Reader using the default settings.
	 * @see #readJsonObject(Reader, JsonReaderOptions.Access)
	 */
	public static ObjectElement readJsonObject(Reader r) throws IOException, SyntaxError {
		return readJsonObject(r, JsonReaderOptions.UNSPECIFIED);
	}
	
	/**
	 * Reads in a json object from an InputStream using the default settings.
	 * @see #readJsonObject(InputStream, JsonReaderOptions.Access)
	 */
	public static ObjectElement readJsonObject(InputStream in) throws IOException, SyntaxError {
		return readJsonObject(in, JsonReaderOptions.UNSPECIFIED);
	}
	
	/**
	 * Reads in json character data and produces an object of the specified Type. This object will be
	 * cast to T and returned, so it's best if T==type.
	 * @param <T> the type of object to produce
	 * @param r a Reader supplying json character data
	 * @param opts hints and settings to control the reading process
	 * @param type the type of object to produce
	 * @return an object of type Type, cast to T, carrying the data represented in the json input
	 * @throws IOException if there was a problem reading in data
	 * @throws SyntaxError if there was a problem with the json data, or if there was a problem creating the object
	 */
	public static <T> T readJson(Reader r, JsonReaderOptions.Access opts, Type type) throws IOException, SyntaxError {
		JsonReader reader = new JsonReader(r);
		ObjectWriter<T> writer = new ObjectWriter<>(type);
		reader.transferTo(writer);
		return writer.toObject();
	}
	
	/**
	 * Reads in json character data and produces an object of the specified Class. This is the preferred
	 * "simple" method to read in config data.
	 * 
	 * @param <T> The type of object to produce
	 * @param r a Reader supplying json character data
	 * @param opts hints and settings to control the reading process
	 * @param clazz the Class of the object to produce
	 * @return an object of the specified Class, configured with the data represented in the json input
	 * @throws IOException if there was a problem reading in data
	 * @throws SyntaxError if there was a problem with the json data, or if there was a problem creating the object
	 * 
	 * @see #readJsonObject(Reader, JsonReaderOptions.Access)
	 * @see #writeJson(Object, Writer)
	 */
	public static <T> T readJson(Reader r, JsonReaderOptions.Access opts, Class<T> clazz) throws IOException, SyntaxError {
		JsonReader reader = new JsonReader(r);
		ObjectWriter<T> writer = new ObjectWriter<>(clazz);
		reader.transferTo(writer);
		return writer.toObject();
	}
	
	public static void writeJson(Object obj, Writer writer) throws SyntaxError, IOException {
		writeJson(obj, new ObjectReaderFactory(), writer, JsonWriterOptions.DEFAULTS);
	}
	
	public static void writeJson(Object obj, ObjectReaderFactory factory, Writer writer, JsonWriterOptions.Access options) throws SyntaxError, IOException {
		StructuredDataReader r = factory.getReader(obj);
		JsonWriter w = new JsonWriter(writer, options);
		r.transferTo(w);
		writer.flush();
	}
	
	public static String writeJsonString(Object obj, ObjectReaderFactory factory, JsonWriterOptions.Access options) throws SyntaxError, IOException {
		try(StringWriter sw = new StringWriter()) {
			StructuredDataReader r = factory.getReader(obj);
			JsonWriter w = new JsonWriter(sw, options);
			r.transferTo(w);
			sw.flush();
			return sw.toString();
		}
	}
	
	public static void writeJson(ValueElement elem, Writer writer) throws SyntaxError, IOException {
		JsonWriter out = new JsonWriter(writer);
		elem.write(out);
	}

	public static void writeJson(ValueElement elem, Writer writer, JsonWriterOptions.Access options) throws SyntaxError, IOException {
		JsonWriter out = new JsonWriter(writer, options);
		elem.write(out);
	}
	
	public static String toJsonString(ValueElement elem, JsonWriterOptions.Access options) throws SyntaxError, IOException {
		try(StringWriter sw = new StringWriter()) {
			JsonWriter out = new JsonWriter(sw, options);
			elem.write(out);
			return sw.toString();
		}
	}
}
