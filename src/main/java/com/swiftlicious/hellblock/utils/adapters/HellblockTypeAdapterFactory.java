package com.swiftlicious.hellblock.utils.adapters;

import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.util.BoundingBox;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;

public class HellblockTypeAdapterFactory implements TypeAdapterFactory {

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
		Class<?> rawType = type.getRawType();
		if (Location.class.isAssignableFrom(rawType)) {
			// Use our current location adapter for backward compatibility
			return (TypeAdapter<T>) new LocationAdapter();
		} else if (BoundingBox.class.isAssignableFrom(rawType)) {
			return (TypeAdapter<T>) new BoundingBoxAdapter();
		} else if (Enum.class.isAssignableFrom(rawType)) {
			return new EnumAdapter(rawType);
		} else if (UUID.class.isAssignableFrom(rawType)) {
			return (TypeAdapter<T>) new UUIDAdapter();
		}
		return null;
	}
}
