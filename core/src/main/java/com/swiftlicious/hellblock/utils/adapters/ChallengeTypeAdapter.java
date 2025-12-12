package com.swiftlicious.hellblock.utils.adapters;

import java.io.IOException;

import com.google.gson.JsonParseException;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.challenges.ChallengeType;

public class ChallengeTypeAdapter extends TypeAdapter<ChallengeType> {

	@Override
	public void write(JsonWriter out, ChallengeType value) throws IOException {
		if (value == null) {
			out.nullValue();
			return;
		}

		out.beginObject();
		out.name("challengeId").value(value.getChallengeId());
		out.endObject();
	}

	@Override
	public ChallengeType read(JsonReader in) throws IOException {
		if (in.peek() == JsonToken.NULL) {
			in.nextNull();
			return null;
		}

		in.beginObject();
		String id = null;
		while (in.hasNext()) {
			if ("challengeId".equals(in.nextName())) {
				id = in.nextString();
			} else {
				in.skipValue();
			}
		}
		in.endObject();

		if (id == null) {
			throw new JsonParseException("Missing challengeId");
		}

		return HellblockPlugin.getInstance().getChallengeManager().getById(id);
	}
}