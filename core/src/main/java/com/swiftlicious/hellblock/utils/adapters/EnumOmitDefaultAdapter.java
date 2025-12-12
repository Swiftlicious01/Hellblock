package com.swiftlicious.hellblock.utils.adapters;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class EnumOmitDefaultAdapter<T extends Enum<T>> implements JsonSerializer<T>, JsonDeserializer<T> {

	private final T defaultValue;
	private final Map<String, T> nameToEnum = new HashMap<>();

	public EnumOmitDefaultAdapter(Class<T> enumClass, T defaultValue) {
		this.defaultValue = defaultValue;
		for (T constant : enumClass.getEnumConstants()) {
			nameToEnum.put(constant.name().toLowerCase(), constant);
		}
	}

	@Override
	public JsonElement serialize(T src, Type typeOfSrc, JsonSerializationContext context) {
		if (src == null || src == defaultValue) {
			return null;
		}
		return new JsonPrimitive(src.name().toLowerCase());
	}

	@Override
	public T deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
		if (json == null || json.isJsonNull()) {
			return defaultValue;
		}
		String name = json.getAsString().toLowerCase();
		return nameToEnum.getOrDefault(name, defaultValue);
	}
}