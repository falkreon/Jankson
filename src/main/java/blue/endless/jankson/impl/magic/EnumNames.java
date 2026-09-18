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

package blue.endless.jankson.impl.magic;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import blue.endless.jankson.api.annotation.SerializedName;

/** Resolves and validates enum wire names. */
public final class EnumNames {
	private EnumNames() {}
	private record Metadata(Map<String, Enum<?>> byWireName, String[] wireNames) {}
	private static final ClassValue<Metadata> METADATA = new ClassValue<>() {
		@Override protected Metadata computeValue(Class<?> enumType) {
			Map<String, Enum<?>> byWireName = new LinkedHashMap<>();
			Object[] constants = enumType.getEnumConstants();
			if (constants == null) throw new IllegalArgumentException(enumType.getTypeName()+" is not an enum type");
			String[] wireNames = new String[constants.length];
			for (Object value : constants) {
				Enum<?> constant = (Enum<?>) value;
				String wireName = constant.name();
				try {
					SerializedName annotation = enumType.getField(constant.name()).getAnnotation(SerializedName.class);
					if (annotation != null) wireName = annotation.value();
				} catch (NoSuchFieldException e) {
					throw new IllegalStateException("Could not inspect enum constant "+constant, e);
				}
				Enum<?> previous = byWireName.putIfAbsent(wireName, constant);
				if (previous != null) throw new IllegalArgumentException("Duplicate serialized enum name '"+wireName
						+"' in "+enumType.getTypeName()+" for constants '"+previous.name()+"' and '"+constant.name()+"'.");
				wireNames[constant.ordinal()] = wireName;
			}
			return new Metadata(Collections.unmodifiableMap(byWireName), wireNames);
		}
	};

	public static Map<String, Enum<?>> of(Class<?> enumType) {
		return METADATA.get(enumType).byWireName();
	}

	public static String wireName(Enum<?> value) {
		return METADATA.get(value.getDeclaringClass()).wireNames()[value.ordinal()];
	}
}
