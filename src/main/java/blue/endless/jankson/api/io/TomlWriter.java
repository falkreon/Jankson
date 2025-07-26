package blue.endless.jankson.api.io;

import java.io.IOException;
import java.io.Writer;
import java.util.Map;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import blue.endless.jankson.api.SyntaxError;
import blue.endless.jankson.api.document.ArrayElement;
import blue.endless.jankson.api.document.KeyValuePairElement;
import blue.endless.jankson.api.document.ObjectElement;
import blue.endless.jankson.api.document.PrimitiveElement;
import blue.endless.jankson.api.document.ValueElement;
import blue.endless.jankson.api.io.BufferedStructuredDataWriter.AbstractBufferedStructuredDataWriter;

public class TomlWriter extends AbstractBufferedStructuredDataWriter {
	private final Writer writer;
	private static final Predicate<String> UNQUOTED_KEY_PREDICATE = Pattern.compile("^[A-Za-z0-9_-]*$").asMatchPredicate();
	private static final Map<Character, Character> ESCAPES = Map.of(
			'\b', 'b',
			'\t', 't',
			'\n', 'n',
			'\f', 'f',
			'\r', 'r',
			'\"', '"',
			'\\', '\\' // \ -> \\, I know it looks weird but this is correct
			);
	
	public TomlWriter(Writer writer) {
		this.writer = writer;
	}
	
	@Override
	public void write(ValueElement value) throws SyntaxError, IOException {
		if (value instanceof ObjectElement obj) {
			writeObject("", null, obj, 0);
		} else {
			throw new SyntaxError("In TOML, the root element MUST be an Object (found: "+value.getClass().getSimpleName()+")");
		}
	}
	
	private void writePrimitive(String path, String key, PrimitiveElement prim, int indent) throws IOException {
		writeKey(key);
		writer.write(" = ");
		
		if (prim.getValue().isEmpty()) {
			writer.write("null");
		} else {
			switch(prim.getValue().get()) {
				case String str -> {
					writer.write('"');
					writer.write(quote(str));
					writer.write('"');
				}
				
				case Long l -> {
					writer.write(Long.toString(l));
				}
				
				case Double d -> {
					writer.write(Double.toString(d));
				}
				
				case Boolean b -> {
					writer.write(Boolean.toString(b));
				}
				
				default -> {
					throw new IOException("Don't know how to deal with Primitive value of type "+prim.getValue().get().getClass().getCanonicalName());
				}
			}
			
		}
	}
	
	private void writeArray(String path, String key, ArrayElement arr, int indent) throws IOException {
		
	}
	
	private void writeObject(String path, String key, ObjectElement obj, int indent) throws IOException {
		final boolean wrap = indent > 0 && key != null;
		final String subpath = subpath(path, key);
		// Do we need to write the key and opening brace?
		if (wrap) {
			writer.write("[ ");
			writer.write(key);
			writer.write(" ]");
		}
		
		// First things first, collect and write all the root primitives
		for(KeyValuePairElement elem : obj) {
			if (elem.getValue() instanceof PrimitiveElement prim) {
				writePrimitive(subpath, elem.getKey(), prim, indent + 1);
			}
		}
		
		// Then, write all the root-level arrays
		for(KeyValuePairElement elem : obj) {
			if (elem.getValue() instanceof ArrayElement arr) {
				writeArray(subpath, elem.getKey(), arr, indent + 1);
			}
		}
		
		// Next, write all the root-level objects
		for(KeyValuePairElement elem : obj) {
			if (elem.getValue() instanceof ObjectElement obj2) {
				writeObject(subpath, elem.getKey(), obj2, indent + 1);
			}
		}
		
		// Finally, do we need to write a closing element?
		if (wrap) {
			// We're using toml hash notation - no closing needed
		}
	}
	
	private String subpath(String path, String key) {
		if (key == null) return path;
		if (path.isBlank()) return key;
		return path + "." + key;
	}
	
	private String quote(String s) {
		String result = s;
		for(Map.Entry<Character, Character> entry : ESCAPES.entrySet()) {
			Pattern pattern = Pattern.compile(Pattern.quote(""+entry.getKey()));
			Matcher matcher = pattern.matcher(result);
			result = matcher.replaceAll("\\"+entry.getValue());
		}
		
		for(int i=0; i<=0x7F; i++) {
			if (i == 0x09) continue;
			if (i > 0x1F && i<0x7F) continue;
			
			Pattern pattern = Pattern.compile(Pattern.quote(""+(char) i));
			Matcher matcher = pattern.matcher(result);
			String unicodeValue = Integer.toHexString(i);
			while(unicodeValue.length() < 4) unicodeValue = "0" + unicodeValue;
			result = matcher.replaceAll("\\u"+unicodeValue);
		}
		
		return result;
	}
	
	private void writeKey(String key) throws IOException {
		boolean bare = UNQUOTED_KEY_PREDICATE.test(key);
		
		if (bare) {
			writer.write(key);
			return;
		}
		
		writer.write('"');
		writer.write(quote(key));
		writer.write('"');
	}
	
}
