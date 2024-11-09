package com.swiftlicious.hellblock.utils.adapters;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;
import com.swiftlicious.hellblock.HellblockPlugin;

public class MapSerializer<K, V> implements JsonSerializer<Map<K, V>>, JsonDeserializer<Map<K, V>> {

	private final Class<K> keysClassType;
	private final Class<V> valuesClassType;

	/**
	 * Constructor of the class.
	 *
	 * @param keysClassType   class type of the key objects
	 * @param valuesClassType class type of the value objects
	 */
	public MapSerializer(Class<K> keysClassType, Class<V> valuesClassType) {
		this.keysClassType = keysClassType;
		this.valuesClassType = valuesClassType;
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
	public JsonElement serialize(Map<K, V> kvMap, Type type, JsonSerializationContext jsonSerializationContext) {
		if (kvMap == null || kvMap.isEmpty())
			return JsonNull.INSTANCE;
		JsonObject jsonObject = new JsonObject();

		jsonObject.add("keys", jsonSerializationContext.serialize(kvMap.keySet(), (new TypeToken<Set<K>>() {
		}).getType()));

		jsonObject.add("values", jsonSerializationContext.serialize(kvMap.values(), (new TypeToken<Collection<V>>() {
		}).getType()));

		return jsonObject;
	}

	/**
	 * Method to deserialize the given json.
	 *
	 * @param jsonElement                json to deserialize
	 * @param type                       deserialization type
	 * @param jsonDeserializationContext deserialization context
	 * @return deserialized hashmap
	 * @throws JsonParseException if json is not parsed correctly
	 */
	@Override
	public Map<K, V> deserialize(JsonElement jsonElement, Type type,
			JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
		Gson gson = HellblockPlugin.getInstance().getStorageManager().getGson();

		JsonObject jsonObject = (JsonObject) jsonElement;

		JsonArray keys = jsonObject.get("keys").getAsJsonArray();
		JsonArray values = jsonObject.get("values").getAsJsonArray();

		Map<K, V> reAssembledHashMap = new HashMap<>();
		for (int i = 0; i < keys.size(); i++) {

			reAssembledHashMap.put(gson.fromJson(keys.get(i), keysClassType),
					gson.fromJson(values.get(i), valuesClassType));
		}
		return reAssembledHashMap;
	}
}
