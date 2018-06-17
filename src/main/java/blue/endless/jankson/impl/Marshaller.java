package blue.endless.jankson.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import javax.annotation.Nullable;

import blue.endless.jankson.JsonArray;
import blue.endless.jankson.JsonElement;
import blue.endless.jankson.JsonNull;
import blue.endless.jankson.JsonObject;
import blue.endless.jankson.JsonPrimitive;

public class Marshaller {
	private static Map<Class<?>, Function<Object,?>> primitiveMarshallers = new HashMap<>();
	
	private static <T> void register(Class<T> clazz, Function<Object, T> marshaller) {
		primitiveMarshallers.put(clazz, marshaller);
	}
	
	static {
		register(Void.class, (it)->null);
		
		register(String.class, (it)->(it instanceof String) ? (String)it : it.toString());
		
		register(Byte.class, (it)->(it instanceof Number) ? ((Number)it).byteValue() : null);
		register(Short.class, (it)->(it instanceof Number) ? ((Number)it).shortValue() : null);
		register(Integer.class, (it)->(it instanceof Number) ? ((Number)it).intValue() : null);
		register(Long.class, (it)->(it instanceof Number) ? ((Number)it).longValue() : null);
		register(Float.class, (it)->(it instanceof Number) ? ((Number)it).floatValue() : null);
		register(Double.class, (it)->(it instanceof Number) ? ((Number)it).doubleValue() : null);
		register(Boolean.class, (it)->(it instanceof Boolean) ? (Boolean)it : null);
	}
	
	
	@SuppressWarnings("unchecked")
	@Nullable
	public static <T> T marshall(Class<T> clazz, JsonElement elem) {
		if (clazz.isAssignableFrom(elem.getClass())) return (T)elem;
		
		if (elem instanceof JsonPrimitive) {
			Function<Object, ?> func = primitiveMarshallers.get(clazz);
			if (func!=null) {
				return (T)func.apply(((JsonPrimitive)elem).getValue());
			} else {
				return null;
			}
		} else {
			if (clazz.equals(String.class)) {
				//Almost everything has a String representation
				if (elem instanceof JsonObject) return (T)((JsonObject)elem).toJson(false, false);
				if (elem instanceof JsonArray) return (T)((JsonArray)elem).toJson(false, false);
				if (elem instanceof JsonNull) return (T)"null";
				return null;
			}
		}
		
		return null;
	}
}
