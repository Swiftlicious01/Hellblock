package com.swiftlicious.hellblock.utils.adapters;

import java.io.IOException;
import java.util.Arrays;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import com.swiftlicious.hellblock.challenges.ChallengeResult;
import com.swiftlicious.hellblock.challenges.HellblockChallenge.CompletionStatus;

public class ChallengeResultAdapter extends TypeAdapter<ChallengeResult> {

	@Override
	public void write(JsonWriter out, ChallengeResult value) throws IOException {
		if (value == null) {
			out.nullValue();
			return;
		}

		out.beginObject();
		out.name("completionStatus").value(value.getStatus().name());
		out.name("progress").value(value.getProgress());
		out.name("claimedReward").value(value.isRewardClaimed());
		out.endObject();
	}

	@Override
	public ChallengeResult read(JsonReader in) throws IOException {
		if (in.peek() == JsonToken.NULL) {
			in.nextNull();
			return null;
		}

		ChallengeResult result = new ChallengeResult(CompletionStatus.NOT_STARTED, 0, false);

		in.beginObject();
		while (in.hasNext()) {
			String name = in.nextName();
			switch (name) {
			case "completionStatus" -> {
				String statusName = in.nextString();
				result.setStatus(
						Arrays.stream(CompletionStatus.values()).filter(s -> s.name().equalsIgnoreCase(statusName))
								.findFirst().orElse(CompletionStatus.NOT_STARTED));
			}
			case "progress" -> result.setProgress(in.nextInt());
			case "claimedReward" -> result.setRewardClaimed(in.nextBoolean());
			default -> in.skipValue();
			}
		}
		in.endObject();

		return result;
	}
}