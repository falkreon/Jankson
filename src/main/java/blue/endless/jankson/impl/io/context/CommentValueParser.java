package blue.endless.jankson.impl.io.context;

import java.io.IOException;

import blue.endless.jankson.api.SyntaxError;
import blue.endless.jankson.api.document.CommentElement;
import blue.endless.jankson.api.document.CommentType;
import blue.endless.jankson.impl.io.Lookahead;
import blue.endless.jankson.impl.io.LookaheadCodePointReader;

public class CommentValueParser implements ValueParser {
	
	public static boolean canReadStatic(Lookahead lookahead) throws IOException {
		int ch = lookahead.peek();
		if (ch=='#') return true;
		String s = lookahead.peekString(2);
		if (s.equals("//")) return true;
		if (s.equals("/*")) return true;
		
		return false;
	}
	
	@Override
	public boolean canRead(Lookahead lookahead) throws IOException {
		return canReadStatic(lookahead);
	}

	public static Object readStatic(LookaheadCodePointReader reader) throws IOException, SyntaxError {
		int ch = reader.peek();
		
		if (ch=='#') {
			reader.read(); // Consume the octothorpe
			String commentText = readToLineEnd(reader);
			return new CommentElement(commentText, CommentType.OCTOTHORPE);
		} else {
			String firstTwo = reader.peekString(2);
			if (firstTwo.equals("//")) {
				String commentText = readToLineEnd(reader);
				return new CommentElement(commentText, CommentType.LINE_END);
			} else if (firstTwo.equals("/*")){
				reader.read();
				reader.read(); //Discard those two
				
				CommentType commentType = CommentType.MULTILINE;
				ch = reader.peek();
				if (ch=='*') {
					commentType = CommentType.DOC;
					reader.read();
				}
				
				StringBuilder sb = new StringBuilder();
				while(true) {
					String maybeEnd = reader.peekString(2);
					if (maybeEnd.equals("*/")) {
						reader.read();
						reader.read();
						return new CommentElement(sb.toString(), commentType);
					}
					
					sb.appendCodePoint(reader.read());
				}
			}
		}
		
		throw new IllegalStateException();
	}
	
	@Override
	public Object read(LookaheadCodePointReader reader) throws IOException, SyntaxError {
		return readStatic(reader);
	}
	
	private static String readToLineEnd(LookaheadCodePointReader reader) throws IOException {
		//Read the line-end comment, consuming the newline in the process.
		StringBuilder sb = new StringBuilder();
		int ch = reader.read();
		while(ch!=-1 && ch!='\n') {
			sb.appendCodePoint(ch);
			ch = reader.read();
		}
		
		return sb.toString();
	}
}
