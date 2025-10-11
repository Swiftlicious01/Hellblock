package com.swiftlicious.hellblock.utils.adapters;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import com.swiftlicious.hellblock.HellblockPlugin;

public class ListSerializer<T> implements JsonSerializer<List<T>>, JsonDeserializer<List<T>> {

	private final Class<T> collectionClassType;

	/**
	 * Constructor of the class.
	 *
	 * @param collectionClassType class type of the key objects
	 */
	public ListSerializer(Class<T> collectionClassType) {
		this.collectionClassType = collectionClassType;
	}

	/**
	 * Method to serialize the given type of object.
	 *
	 * @param list                     list that must be serialized
	 * @param type                     serialization type
	 * @param jsonSerializationContext the serialization context
	 * @return the serialized object
	 */
	@Override
	public JsonElement serialize(List<T> list, Type type, JsonSerializationContext jsonSerializationContext) {
		if (list == null || list.isEmpty()) {
			return JsonNull.INSTANCE;
		}
		final JsonArray jsonArray = new JsonArray();

		list.forEach(entry -> jsonArray.add(jsonSerializationContext.serialize(entry)));

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
	public List<T> deserialize(JsonElement jsonElement, Type type,
			JsonDeserializationContext jsonDeserializationContext) {
		final Gson gson = HellblockPlugin.getInstance().getStorageManager().getGson();

		final JsonArray jsonArray = (JsonArray) jsonElement;

		final JsonArray collection = jsonArray.getAsJsonArray();

		final List<T> reAssembledArrayList = new ArrayList<>();
		for (int i = 0; i < collection.size(); i++) {
			reAssembledArrayList.add(gson.fromJson(collection.get(i), collectionClassType));
		}
		return reAssembledArrayList;
	}
}