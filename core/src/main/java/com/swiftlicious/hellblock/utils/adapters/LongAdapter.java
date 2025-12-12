package com.swiftlicious.hellblock.utils.adapters;

import java.io.IOException;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

public class LongAdapter extends TypeAdapter<Long> {
	
	@Override
	public void write(JsonWriter out, Long value) throws IOException {
		if (value == null || value == 0L) {
			out.nullValue();
			return;
		}
		out.value(value);
	}

	@Override
	public Long read(JsonReader in) throws IOException {
		if (in.peek() == JsonToken.NULL) {
			in.nextNull();
			return 0L;
		}
		return in.nextLong();
	}
}