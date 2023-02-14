package blue.endless.jankson.impl.io.context;

import java.util.Arrays;

public class ParserConstants {
	public static final String TRIPLE_QUOTE = "\"\"\"";
	public static final int[] NUMBER_VALUE_START = createSortedLookup("-+.0123456789");
	public static final int[] NUMBER_VALUE_CHAR = createSortedLookup("-+.0123456789ABCDEFabcdefINintxy");
	
	
	public static int[] createSortedLookup(String s) {
		int[] arr = s.chars().toArray();
		Arrays.sort(arr);
		return arr;
	}
}
