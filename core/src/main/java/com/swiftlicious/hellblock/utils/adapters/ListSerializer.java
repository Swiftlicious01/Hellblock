package com.swiftlicious.hellblock.utils.adapters;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

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
	public JsonElement serialize(List<T> list, Type type, JsonSerializationContext context) {
		if (list == null || list.isEmpty()) {
			return JsonNull.INSTANCE;
		}
		JsonArray jsonArray = new JsonArray();
		list.stream().filter(Objects::nonNull).forEach(entry -> jsonArray.add(context.serialize(entry)));
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
	public List<T> deserialize(JsonElement json, Type type, JsonDeserializationContext context) {
		if (json == null || json.isJsonNull()) {
			return new ArrayList<>();
		}
		JsonArray jsonArray = json.getAsJsonArray();
		List<T> result = new ArrayList<>();
		for (JsonElement element : jsonArray) {
			result.add(context.deserialize(element, collectionClassType));
		}
		return result;
	}
}