package com.swiftlicious.hellblock.utils.adapters;

import java.io.IOException;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import com.swiftlicious.hellblock.player.SkysiegeData;

public class SkysiegeDataAdapter extends TypeAdapter<SkysiegeData> {

	@Override
	public void write(JsonWriter out, SkysiegeData data) throws IOException {
		if (data == null || data.isEmpty()) {
			out.nullValue();
			return;
		}

		out.beginObject();
		if (data.getTotalSkysieges() > 0)
			out.name("totalSkysieges").value(data.getTotalSkysieges());

		if (data.getSuccessfulSkysieges() > 0)
			out.name("successfulSkysieges").value(data.getSuccessfulSkysieges());

		if (data.getFailedSkysieges() > 0)
			out.name("failedSkysieges").value(data.getFailedSkysieges());

		if (data.getQueenKills() > 0)
			out.name("queenKills").value(data.getQueenKills());

		if (data.getTotalWavesCompleted() > 0)
			out.name("totalWavesCompleted").value(data.getTotalWavesCompleted());

		if (data.getTotalGhastsKilled() > 0)
			out.name("totalGhastsKilled").value(data.getTotalGhastsKilled());

		if (data.getLongestDurationMillis() > 0)
			out.name("longestDurationMillis").value(data.getLongestDurationMillis());

		if (data.getShortestDurationMillis() > 0)
			out.name("shortestDurationMillis").value(data.getShortestDurationMillis());

		if (data.getLastSkysiegeTime() > 0)
			out.name("lastSkysiegeTime").value(data.getLastSkysiegeTime());

		out.endObject();
	}

	@Override
	public SkysiegeData read(JsonReader in) throws IOException {
		if (in.peek() == JsonToken.NULL) {
			in.nextNull();
			return null;
		}

		SkysiegeData data = new SkysiegeData();

		in.beginObject();
		while (in.hasNext()) {
			String name = in.nextName();
			switch (name) {
			case "totalSkysieges" -> data.setTotalSkysieges(in.nextInt());
			case "successfulSkysieges" -> data.setSuccessfulSkysieges(in.nextInt());
			case "failedSkysieges" -> data.setFailedSkysieges(in.nextInt());
			case "queenKills" -> data.setQueenKills(in.nextInt());
			case "totalWavesCompleted" -> data.setTotalWavesCompleted(in.nextInt());
			case "totalGhastsKilled" -> data.setTotalGhastsKilled(in.nextInt());
			case "longestDurationMillis" -> data.setLongestDurationMillis(in.nextLong());
			case "shortestDurationMillis" -> data.setShortestDurationMillis(in.nextLong());
			case "lastSkysiegeTime" -> data.setLastSkysiegeTime(in.nextLong());
			default -> in.skipValue(); // Ignore unknown or new future fields
			}
		}
		in.endObject();

		return data;
	}
}