package com.swiftlicious.hellblock.utils.adapters;

import java.io.IOException;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

public class BooleanAdapter extends TypeAdapter<Boolean> {

	@Override
	public void write(JsonWriter out, Boolean value) throws IOException {
		if (value == null || !value) {
			out.nullValue();
			return;
		}
		out.value(value);
	}

	@Override
	public Boolean read(JsonReader in) throws IOException {
		if (in.peek() == JsonToken.NULL) {
			in.nextNull();
			return false;
		}
		return in.nextBoolean();
	}
}