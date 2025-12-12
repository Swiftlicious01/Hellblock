package com.swiftlicious.hellblock.utils.adapters;

import java.io.IOException;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import com.swiftlicious.hellblock.player.InvasionData;

public class InvasionDataAdapter extends TypeAdapter<InvasionData> {

	@Override
	public void write(JsonWriter out, InvasionData data) throws IOException {
		if (data == null || data.isEmpty()) {
			out.nullValue();
			return;
		}

		out.beginObject();
		if (data.getTotalInvasions() > 0)
			out.name("totalInvasions").value(data.getTotalInvasions());

		if (data.getSuccessfulInvasions() > 0)
			out.name("successfulInvasions").value(data.getSuccessfulInvasions());

		if (data.getFailedInvasions() > 0)
			out.name("failedInvasions").value(data.getFailedInvasions());

		if (data.getBossKills() > 0)
			out.name("bossKills").value(data.getBossKills());

		if (data.getCurrentStreak() > 0)
			out.name("currentStreak").value(data.getCurrentStreak());

		if (data.getLastInvasionTime() > 0)
			out.name("lastInvasionTime").value(data.getLastInvasionTime());

		if (data.getHighestDifficultyTierReached() > 0)
			out.name("highestDifficultyTierReached").value(data.getHighestDifficultyTierReached());

		out.endObject();
	}

	@Override
	public InvasionData read(JsonReader in) throws IOException {
		if (in.peek() == JsonToken.NULL) {
			in.nextNull();
			return null;
		}

		InvasionData data = new InvasionData();

		in.beginObject();
		while (in.hasNext()) {
			String name = in.nextName();
			switch (name) {
			case "totalInvasions" -> data.setTotalInvasions(in.nextInt());
			case "successfulInvasions" -> data.setSuccessfulInvasions(in.nextInt());
			case "failedInvasions" -> data.setFailedInvasions(in.nextInt());
			case "bossKills" -> data.setBossKills(in.nextInt());
			case "currentStreak" -> data.setCurrentStreak(in.nextInt());
			case "lastInvasionTime" -> data.setLastInvasionTime(in.nextLong());
			case "highestDifficultyTierReached" -> data.setHighestDifficultyTierReached(in.nextInt());
			default -> in.skipValue(); // Gracefully ignore unknown fields
			}
		}
		in.endObject();

		return data;
	}
}