package com.swiftlicious.hellblock.utils.adapters;

import java.io.IOException;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

public class IntegerAdapter extends TypeAdapter<Integer> {
	
	@Override
	public void write(JsonWriter out, Integer value) throws IOException {
		if (value == null || value == 0) {
			out.nullValue();
			return;
		}
		out.value(value);
	}

	@Override
	public Integer read(JsonReader in) throws IOException {
		if (in.peek() == JsonToken.NULL) {
			in.nextNull();
			return 0;
		}
		return in.nextInt();
	}
}