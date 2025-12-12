package com.swiftlicious.hellblock.utils.adapters;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class MapSerializer<K, V> implements JsonSerializer<Map<K, V>>, JsonDeserializer<Map<K, V>> {

	private final Class<K> keyClassType;
	private final Class<V> valueClassType;

	/**
	 * Constructor of the class.
	 *
	 * @param keyClassType   class type of the key objects
	 * @param valueClassType class type of the value objects
	 */
	public MapSerializer(Class<K> keyClassType, Class<V> valueClassType) {
		this.keyClassType = keyClassType;
		this.valueClassType = valueClassType;
	}

	/**
	 * Method to serialize the given type of object.
	 *
	 * @param kvMap                    hashmap that must be serialized
	 * @param type                     serialization type
	 * @param jsonSerializationContext the serialization context
	 * @return the serialized object
	 */
	@Override
	public JsonElement serialize(Map<K, V> map, Type type, JsonSerializationContext context) {
		if (map == null || map.isEmpty()) {
			return JsonNull.INSTANCE;
		}
		JsonObject jsonObject = new JsonObject();
		map.entrySet().stream().filter(entry -> entry.getKey() != null && entry.getValue() != null)
				.forEach(entry -> jsonObject.add(entry.getKey().toString(), context.serialize(entry.getValue())));
		return jsonObject;
	}

	/**
	 * Method to deserialize the given json.
	 *
	 * @param jsonElement                json to deserialize
	 * @param type                       deserialization type
	 * @param jsonDeserializationContext deserialization context
	 * @return deserialized hashmap
	 */
	@Override
	public Map<K, V> deserialize(JsonElement json, Type type, JsonDeserializationContext context) {
		if (json == null || json.isJsonNull()) {
			return new HashMap<>();
		}
		JsonObject jsonObject = json.getAsJsonObject();
		Map<K, V> result = new HashMap<>();
		jsonObject.entrySet().forEach(entry -> {
			K key = context.deserialize(new JsonPrimitive(entry.getKey()), keyClassType);
			V value = context.deserialize(entry.getValue(), valueClassType);
			if (key != null && value != null)
				result.put(key, value);
		});
		return result;
	}
}