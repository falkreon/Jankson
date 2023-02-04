package blue.endless.jankson.impl.io;

import java.io.Reader;
import java.util.ArrayDeque;
import java.util.Deque;

import javax.annotation.Nullable;

import blue.endless.jankson.api.document.PrimitiveElement;
import blue.endless.jankson.api.io.ElementType;
import blue.endless.jankson.api.io.StructuredDataReader;
import blue.endless.jankson.impl.context.ParserContext;

public class AbstractStructuredDataReader implements StructuredDataReader {
	private final Reader src;
	//private final CodePointReader in;
	private Deque<ParserContext<?>> context = new ArrayDeque<>();
	
	public AbstractStructuredDataReader(Reader src) {
		this.src = src;
		//if (src instanceof CodePointReader r) {
		//	in = r;
		//} else {
		//	in = new CodePointReader(src);
		//}
	}

	@Override
	public PrimitiveElement getValue() {
		// TODO Auto-generated method stub
		return null;
	}

	private @Nullable ElementType nextCharacter() {
		return null;
	}
	
	@Override
	public ElementType next() {
		
		
		
		// TODO Auto-generated method stub
		return null;
	}
}
