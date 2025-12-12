package com.swiftlicious.hellblock.utils.adapters;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.TypeAdapter;
import com.google.gson.annotations.SerializedName;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

public final class EnumAdapter<T extends Enum<T>> extends TypeAdapter<T> {
	
	private final Map<String, T> nameToEnum = new HashMap<>();
	private final Map<T, String> enumToName = new HashMap<>();

	public EnumAdapter(Class<T> enumClass) {
		if (!enumClass.getPackage().getName().startsWith("com.swiftlicious.hellblock")) {
			throw new IllegalArgumentException(
					"Only plugin-specific enums in 'com.swiftlicious.hellblock' are supported.");
		}

		for (T value : enumClass.getEnumConstants()) {
			String name = value.name();
			try {
				Field field = enumClass.getField(name);
				SerializedName annotation = field.getAnnotation(SerializedName.class);
				if (annotation != null) {
					name = annotation.value();
					for (String alt : annotation.alternate()) {
						nameToEnum.put(alt, value);
					}
				}
			} catch (NoSuchFieldException ignored) {
			}

			nameToEnum.put(name, value);
			enumToName.put(value, name);
		}
	}

	@Override
	public void write(JsonWriter out, T value) throws IOException {
		if (value == null) {
			out.nullValue();
		} else {
			out.value(enumToName.get(value));
		}
	}

	@Override
	public T read(JsonReader in) throws IOException {
		if (in.peek() == JsonToken.NULL) {
			in.nextNull();
			return null;
		}
		return nameToEnum.get(in.nextString());
	}
}