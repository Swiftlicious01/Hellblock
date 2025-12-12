package com.swiftlicious.hellblock.utils.adapters;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class NestedMapSerializer<K, V>
		implements JsonSerializer<Map<K, Map<K, V>>>, JsonDeserializer<Map<K, Map<K, V>>> {

	private final Class<K> keyClass;
	private final Class<V> valueClass;

	public NestedMapSerializer(Class<K> keyClass, Class<V> valueClass) {
		this.keyClass = keyClass;
		this.valueClass = valueClass;
	}

	@Override
	public JsonElement serialize(Map<K, Map<K, V>> src, Type typeOfSrc, JsonSerializationContext context) {
		if (src == null || src.isEmpty()) {
			return JsonNull.INSTANCE;
		}
		JsonObject outer = new JsonObject();
		src.entrySet().forEach(entry -> {
			String outerKey = String.valueOf(entry.getKey());
			JsonObject inner = new JsonObject();
			entry.getValue().entrySet().forEach(innerEntry -> {
				String innerKey = String.valueOf(innerEntry.getKey());
				inner.add(innerKey, context.serialize(innerEntry.getValue(), valueClass));
			});
			outer.add(outerKey, inner);
		});
		return outer;
	}

	@Override
	public Map<K, Map<K, V>> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
			throws JsonParseException {
		if (json == null || json.isJsonNull()) {
			return new HashMap<>();
		}
		Map<K, Map<K, V>> result = new HashMap<>();
		JsonObject outer = json.getAsJsonObject();

		outer.entrySet().forEach(outerEntry -> {
			K outerKey = context.deserialize(new JsonPrimitive(outerEntry.getKey()), keyClass);

			JsonObject innerObject = outerEntry.getValue().getAsJsonObject();
			Map<K, V> innerMap = new HashMap<>();

			innerObject.entrySet().forEach(innerEntry -> {
				K innerKey = context.deserialize(new JsonPrimitive(innerEntry.getKey()), keyClass);
				V value = context.deserialize(innerEntry.getValue(), valueClass);
				innerMap.put(innerKey, value);
			});

			result.put(outerKey, innerMap);
		});

		return result;
	}
}