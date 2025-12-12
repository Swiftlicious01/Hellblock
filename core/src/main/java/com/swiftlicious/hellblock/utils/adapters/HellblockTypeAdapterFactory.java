package com.swiftlicious.hellblock.utils.adapters;

import java.io.IOException;
import java.time.LocalDate;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.util.BoundingBox;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.swiftlicious.hellblock.challenges.ChallengeResult;
import com.swiftlicious.hellblock.challenges.ChallengeType;
import com.swiftlicious.hellblock.player.NotificationSettings;
import com.swiftlicious.hellblock.protection.HellblockFlag;

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
		} else if (UUID.class.isAssignableFrom(rawType)) {
			return (TypeAdapter<T>) new UUIDAdapter();
		} else if (LocalDate.class.isAssignableFrom(rawType)) {
			return (TypeAdapter<T>) new LocalDateAdapter();
		} else if (NotificationSettings.class.isAssignableFrom(rawType)) {
			return (TypeAdapter<T>) new NotificationSettingsAdapter();
		} else if (HellblockFlag.class.isAssignableFrom(rawType)) {
			return (TypeAdapter<T>) new HellblockFlagAdapter();
		} else if (ChallengeType.class.isAssignableFrom(rawType)) {
			return (TypeAdapter<T>) new ChallengeTypeAdapter();
		} else if (ChallengeResult.class.isAssignableFrom(rawType)) {
			return (TypeAdapter<T>) new ChallengeResultAdapter();
		} else if (Enum.class.isAssignableFrom(rawType)) {
			return new EnumAdapter(rawType);
		} else if (EmptyCheck.class.isAssignableFrom(rawType)) {
			return new TypeAdapter<T>() {
				@Override
				public void write(JsonWriter out, T value) throws IOException {
					if (value == null || ((EmptyCheck) value).isEmpty()) {
						out.nullValue(); // causes field to be skipped if serializeNulls is off
					} else {
						gson.getDelegateAdapter(HellblockTypeAdapterFactory.this, type).write(out, value);
					}
				}

				@Override
				public T read(JsonReader in) throws IOException {
					return gson.getDelegateAdapter(HellblockTypeAdapterFactory.this, type).read(in);
				}
			};
		}
		return null;
	}

	public interface EmptyCheck {
		boolean isEmpty();
	}
}