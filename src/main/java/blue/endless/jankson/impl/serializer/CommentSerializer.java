package blue.endless.jankson.impl.serializer;

import blue.endless.jankson.JsonGrammar;

public class CommentSerializer {
	public static void print(StringBuilder builder, String comment, int indent, JsonGrammar grammar) {
		boolean comments = grammar.hasComments();
		boolean whitespace = grammar.shouldOutputWhitespace();
		print(builder, comment, indent, comments, whitespace);
	}
		
	public static void print(StringBuilder builder, String comment, int indent, boolean comments, boolean whitespace) {
		if (!comments) return;
		if (comment==null || comment.trim().isEmpty()) return;
		
		if (whitespace) {
			if (comment.contains("\n")) {
				//Use /* */ comment
				builder.append("/* ");
				String[] lines = comment.split("\\n");
				for(int i=0; i<lines.length; i++) {
					String line = lines[i];
					if (i!=0) builder.append("   ");
					builder.append(line);
					builder.append('\n');
					for(int j=0; j<indent+1; j++) {
						builder.append('\t');
					}
				}
				builder.append("*/\n");
				for(int i=0; i<indent+1; i++) {
					builder.append('\t');
				}
			} else {
				//Use a single-line comment
				builder.append("// ");
				builder.append(comment);
				builder.append('\n');
				for(int i=0; i<indent+1; i++) {
					builder.append('\t');
				}
			}
		} else {
			//Always use /* */ comments
			
			if (comment.contains("\n")) {
				//Split the lines into separate /* */ comments and string them together inline.
				
				String[] lines = comment.split("\\n");
				for(int i=0; i<lines.length; i++) {
					String line = lines[i];
					builder.append("/* ");
					builder.append(line);
					builder.append(" */ ");
				}
			} else {
				builder.append("/* ");
				builder.append(comment);
				builder.append(" */ ");
			}
		}
	}
}
