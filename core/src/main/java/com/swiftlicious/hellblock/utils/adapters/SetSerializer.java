package com.swiftlicious.hellblock.utils.adapters;

import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import com.swiftlicious.hellblock.HellblockPlugin;

public class SetSerializer<T> implements JsonSerializer<Set<T>>, JsonDeserializer<Set<T>> {

	private final Class<T> collectionClassType;

	/**
	 * Constructor of the class.
	 *
	 * @param collectionClassType class type of the key objects
	 */
	public SetSerializer(Class<T> collectionClassType) {
		this.collectionClassType = collectionClassType;
	}

	/**
	 * Method to serialize the given type of object.
	 *
	 * @param set                      set that must be serialized
	 * @param type                     serialization type
	 * @param jsonSerializationContext the serialization context
	 * @return the serialized object
	 */
	@Override
	public JsonElement serialize(Set<T> set, Type type, JsonSerializationContext jsonSerializationContext) {
		if (set == null || set.isEmpty()) {
			return JsonNull.INSTANCE;
		}
		final JsonArray jsonArray = new JsonArray();

		set.forEach(entry -> jsonArray.add(jsonSerializationContext.serialize(entry)));

		return jsonArray;
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
	public Set<T> deserialize(JsonElement jsonElement, Type type,
			JsonDeserializationContext jsonDeserializationContext) {
		final Gson gson = HellblockPlugin.getInstance().getStorageManager().getGson();

		final JsonArray JsonArray = (JsonArray) jsonElement;

		final JsonArray collection = JsonArray.getAsJsonArray();

		final Set<T> reAssembledHashSet = new HashSet<>();
		for (int i = 0; i < collection.size(); i++) {
			reAssembledHashSet.add(gson.fromJson(collection.get(i), collectionClassType));
		}
		return reAssembledHashSet;
	}
}
