package com.swiftlicious.hellblock.utils.adapters;

import java.io.IOException;
import java.util.UUID;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

public class UUIDAdapter extends TypeAdapter<UUID> {

	@Override
	public void write(JsonWriter out, UUID uuid) throws IOException {
		if (uuid == null) {
			out.nullValue();
			return;
		}
		out.value(uuid.toString());
	}

	@Override
	public UUID read(JsonReader in) throws IOException {
		if (in.peek() == JsonToken.NULL) {
			in.nextNull();
			return null;
		}
		String uuid = in.nextString();
		try {
			return UUID.fromString(uuid);
		} catch (IllegalArgumentException ignored) {

		}
		return null;
	}
}
