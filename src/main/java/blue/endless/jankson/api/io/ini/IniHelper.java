package blue.endless.jankson.api.io.ini;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.regex.Pattern;

import blue.endless.jankson.impl.io.LookaheadCodePointReader;

public class IniHelper {
	public static <T extends Enum<T>> Optional<T> castToEnum(String value, Class<T> enumType) {
		// Exact match?
		for(T t : enumType.getEnumConstants()) {
			if (t.name().toLowerCase(Locale.ROOT).equals(value)) {
				return Optional.of(t);
			}
		}
		
		// Case-insensitive match?
		String comparisonValue = value.toLowerCase(Locale.ROOT);
		for(T t : enumType.getEnumConstants()) {
			if (t.name().toLowerCase(Locale.ROOT).equals(comparisonValue)) {
				return Optional.of(t);
			}
		}
		
		// Nope. Try it without the underscores
		comparisonValue = comparisonValue.replaceAll(Pattern.quote("_"), "");
		for(T t : enumType.getEnumConstants()) {
			if (t.name().toLowerCase(Locale.ROOT).equals(comparisonValue)) {
				return Optional.of(t);
			}
		}
		
		return Optional.empty();
	}
	
	public static OptionalInt castToInt(String value) {
		try {
			return OptionalInt.of(Integer.parseInt(value));
		} catch (Throwable t) {
			return OptionalInt.empty();
		}
	}
	
	public static OptionalDouble castToDouble(String value) {
		try {
			return OptionalDouble.of(Double.parseDouble(value));
		} catch (Throwable t) {
			return OptionalDouble.empty();
		}
	}
	
	private static final Optional<Boolean> TRUE = Optional.of(Boolean.TRUE);
	private static final Optional<Boolean> FALSE = Optional.of(Boolean.FALSE);
	public static Optional<Boolean> castToBoolean(String value) {
		String compValue = value.toLowerCase(Locale.ROOT);
		switch(compValue) {
			case "true"  -> { return TRUE;  }
			case "false" -> { return FALSE; }
			case "yes"   -> { return TRUE;  }
			case "no"    -> { return FALSE; }
			case "on"    -> { return TRUE;  }
			case "off"   -> { return FALSE; }
			case "1"     -> { return TRUE;  }
			case "0"     -> { return FALSE; }
			default -> { return Optional.empty(); }
		}
	}
	
	public static List<String> castToList(String s) {
		List<String> result = new ArrayList<>();
		StringBuilder buf = new StringBuilder();
		
		try (LookaheadCodePointReader reader = new LookaheadCodePointReader(new StringReader(s))) {
			int ch = reader.read();
			while(ch != -1) {
				if (ch == ',') {
					result.add(buf.toString().trim());
					buf.setLength(0);
				} else {
					buf.appendCodePoint(ch);
				}
				
				ch = reader.read();
			}
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
		
		if (buf.length() > 0) {
			result.add(buf.toString().trim());
		}
		
		return result;
	}
}
