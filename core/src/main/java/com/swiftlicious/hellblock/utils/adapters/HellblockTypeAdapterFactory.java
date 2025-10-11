package com.swiftlicious.hellblock.utils.adapters;

import java.time.LocalDate;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.util.BoundingBox;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.swiftlicious.hellblock.player.DisplaySettings;
import com.swiftlicious.hellblock.player.VisitData;

public class HellblockTypeAdapterFactory implements TypeAdapterFactory {

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
		final Class<?> rawType = type.getRawType();
		if (Location.class.isAssignableFrom(rawType)) {
			// Use our current location adapter for backward compatibility
			return (TypeAdapter<T>) new LocationAdapter();
		} else if (BoundingBox.class.isAssignableFrom(rawType)) {
			return (TypeAdapter<T>) new BoundingBoxAdapter();
		} else if (Enum.class.isAssignableFrom(rawType)) {
			return new EnumAdapter(rawType);
		} else if (UUID.class.isAssignableFrom(rawType)) {
			return (TypeAdapter<T>) new UUIDAdapter();
		} else if (LocalDate.class.isAssignableFrom(rawType)) {
			return (TypeAdapter<T>) new LocalDateAdapter();
		} else if (DisplaySettings.class.isAssignableFrom(rawType)) {
			return (TypeAdapter<T>) new DisplaySettingsAdapter();
		} else if (VisitData.class.isAssignableFrom(rawType)) {
			return (TypeAdapter<T>) new VisitDataAdapter();
		}
		return null;
	}
}
