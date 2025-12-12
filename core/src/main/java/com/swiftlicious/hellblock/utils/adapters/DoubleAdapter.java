package com.swiftlicious.hellblock.utils.adapters;

import java.io.IOException;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

public class DoubleAdapter extends TypeAdapter<Double> {
	
	@Override
	public void write(JsonWriter out, Double value) throws IOException {
		if (value == null || value == 0.0D) {
			out.nullValue();
			return;
		}
		out.value(value);
	}

	@Override
	public Double read(JsonReader in) throws IOException {
		if (in.peek() == JsonToken.NULL) {
			in.nextNull();
			return 0.0D;
		}
		return in.nextDouble();
	}
}