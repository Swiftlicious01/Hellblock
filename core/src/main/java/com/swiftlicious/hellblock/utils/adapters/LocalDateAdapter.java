package com.swiftlicious.hellblock.utils.adapters;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

public class LocalDateAdapter extends TypeAdapter<LocalDate> {

	@Override
	public void write(JsonWriter out, LocalDate date) throws IOException {
		if (date == null) {
			out.nullValue();
			return;
		}
		// ISO-8601 format (e.g., "2025-10-04")
		out.value(date.toString());
	}

	@Override
	public LocalDate read(JsonReader in) throws IOException {
		if (in.peek() == JsonToken.NULL) {
			in.nextNull();
			return null;
		}

		String value = in.nextString();
		try {
			// Parse the ISO-8601 string into a LocalDate
			return LocalDate.parse(value);
		} catch (DateTimeParseException ex) {
			// Optionally log or rethrow if you want strict validation
			// For safety, return null on invalid date strings
			return null;
		}
	}
}