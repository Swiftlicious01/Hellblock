package com.swiftlicious.hellblock.utils.adapters;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import com.swiftlicious.hellblock.player.WitherData;

import java.io.IOException;

public class WitherDataAdapter extends TypeAdapter<WitherData> {

	@Override
	public void write(JsonWriter out, WitherData data) throws IOException {
		if (data == null || data.isEmpty()) {
			out.nullValue();
			return;
		}

		out.beginObject();
		if (data.getTotalSpawns() > 0)
			out.name("totalSpawns").value(data.getTotalSpawns());

		if (data.getKills() > 0)
			out.name("kills").value(data.getKills());

		if (data.getDespawns() > 0)
			out.name("despawns").value(data.getDespawns());

		if (data.getLongestFightMillis() > 0)
			out.name("longestFightMillis").value(data.getLongestFightMillis());

		if (data.getShortestFightMillis() > 0)
			out.name("shortestFightMillis").value(data.getShortestFightMillis());

		if (data.getTotalHeals() > 0)
			out.name("totalHeals").value(data.getTotalHeals());

		if (data.getTotalMinionWaves() > 0)
			out.name("totalMinionWaves").value(data.getTotalMinionWaves());

		if (data.getLastSpawnTime() > 0)
			out.name("lastSpawnTime").value(data.getLastSpawnTime());

		out.endObject();
	}

	@Override
	public WitherData read(JsonReader in) throws IOException {
		if (in.peek() == JsonToken.NULL) {
			in.nextNull();
			return null;
		}

		WitherData data = new WitherData();

		in.beginObject();
		while (in.hasNext()) {
			String name = in.nextName();
			switch (name) {
			case "totalSpawns" -> data.setTotalSpawns(in.nextInt());
			case "kills" -> data.setKills(in.nextInt());
			case "despawns" -> data.setDespawns(in.nextInt());
			case "longestFightMillis" -> data.setLongestFightMillis(in.nextLong());
			case "shortestFightMillis" -> data.setShortestFightMillis(in.nextLong());
			case "totalHeals" -> data.setTotalHeals(in.nextInt());
			case "totalMinionWaves" -> data.setTotalMinionWaves(in.nextInt());
			case "lastSpawnTime" -> data.setLastSpawnTime(in.nextLong());
			default -> in.skipValue();
			}
		}
		in.endObject();

		return data;
	}
}