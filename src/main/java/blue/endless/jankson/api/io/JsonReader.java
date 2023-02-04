package blue.endless.jankson.api.io;

import java.io.Reader;

import javax.annotation.Nullable;

import blue.endless.jankson.api.document.PrimitiveElement;
import blue.endless.jankson.impl.io.AbstractStructuredDataReader;

public class JsonReader extends AbstractStructuredDataReader {
private final JsonReaderOptions options;
	
	private ElementType cur = ElementType.WHITESPACE;
	private PrimitiveElement value = PrimitiveElement.NULL;
	
	public JsonReader(Reader source) {
		this(source, new JsonReaderOptions());
	}
	
	public JsonReader(Reader source, JsonReaderOptions options) {
		super(source);
		this.options = options;
	}
	
	/**
	 * Gets the value at this location in the document.
	 * @return the value of the element we just parsed, if it has a value. Otherwise, PrimitiveElement.NULL.
	 */
	public PrimitiveElement getValue() {
		return (value==null) ? PrimitiveElement.NULL : value;
	}
	
	/**
	 * Advance one character and adjust the parser state.
	 * @return an ElementType if we're ready to pause and give the API user some information.
	 */
	private @Nullable ElementType advance() {
		return null;
	}
	
	@Override
	public ElementType next() {
		
		
		return ElementType.OBJECT_END;
	}
}
