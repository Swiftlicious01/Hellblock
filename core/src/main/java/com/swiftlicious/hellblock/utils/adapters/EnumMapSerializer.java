package com.swiftlicious.hellblock.utils.adapters;

import java.lang.reflect.Type;
import java.util.EnumMap;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class EnumMapSerializer<K extends Enum<K>, V>
		implements JsonSerializer<EnumMap<K, V>>, JsonDeserializer<EnumMap<K, V>> {

	private final Class<K> keyType;
	private final Class<V> valueType;

	public EnumMapSerializer(Class<K> keyType, Class<V> valueType) {
		this.keyType = keyType;
		this.valueType = valueType;
	}

	@Override
	public JsonElement serialize(EnumMap<K, V> map, Type typeOfSrc, JsonSerializationContext context) {
		if (map == null || map.isEmpty()) {
			return JsonNull.INSTANCE;
		}
		JsonObject jsonObject = new JsonObject();
		map.entrySet().forEach(entry -> jsonObject.add(entry.getKey().name(), context.serialize(entry.getValue())));
		return jsonObject;
	}

	@Override
	public EnumMap<K, V> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
			throws JsonParseException {
		if (json == null || json.isJsonNull()) {
			return new EnumMap<>(keyType);
		}
		EnumMap<K, V> map = new EnumMap<>(keyType);
		JsonObject obj = json.getAsJsonObject();

		obj.entrySet().forEach(entry -> {
			K key = Enum.valueOf(keyType, entry.getKey());
			V value = context.deserialize(entry.getValue(), valueType);
			map.put(key, value);
		});
		return map;
	}
}