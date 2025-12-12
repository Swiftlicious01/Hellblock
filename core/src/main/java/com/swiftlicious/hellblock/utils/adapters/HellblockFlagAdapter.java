package com.swiftlicious.hellblock.utils.adapters;

import java.io.IOException;

import com.google.gson.JsonParseException;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import com.swiftlicious.hellblock.protection.HellblockFlag;
import com.swiftlicious.hellblock.protection.HellblockFlag.AccessType;
import com.swiftlicious.hellblock.protection.HellblockFlag.FlagType;

public class HellblockFlagAdapter extends TypeAdapter<HellblockFlag> {

	@Override
	public void write(JsonWriter out, HellblockFlag value) throws IOException {
		if (value == null) {
			out.nullValue();
			return;
		}

		out.beginObject();
		out.name("flagType").value(value.getFlag().name());
		out.name("allowedStatus").value(value.getStatus().name());
		out.name("stringData").value(value.getData());
		out.endObject();
	}

	@Override
	public HellblockFlag read(JsonReader in) throws IOException {
		if (in.peek() == JsonToken.NULL) {
			in.nextNull();
			return null;
		}

		FlagType flag = null;
		AccessType status = null;
		String data = null;

		in.beginObject();
		while (in.hasNext()) {
			String name = in.nextName();
			switch (name) {
			case "flagType" -> flag = FlagType.valueOf(in.nextString());
			case "allowedStatus" -> status = AccessType.valueOf(in.nextString());
			case "stringData" -> {
				if (in.peek() != JsonToken.NULL) {
					data = in.nextString();
				} else {
					in.nextNull();
					data = null;
				}
			}
			default -> in.skipValue();
			}
		}
		in.endObject();

		if (flag == null || status == null) {
			throw new JsonParseException("Missing required flag fields during deserialization.");
		}

		return new HellblockFlag(flag, status, data);
	}
}