package com.swiftlicious.hellblock.utils.adapters;

import java.io.IOException;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import com.swiftlicious.hellblock.player.DisplaySettings;
import com.swiftlicious.hellblock.player.DisplaySettings.DisplayChoice;

public class DisplaySettingsAdapter extends TypeAdapter<DisplaySettings> {

	@Override
	public void write(JsonWriter out, DisplaySettings display) throws IOException {
		if (display == null || display.isEmpty()) {
			out.nullValue();
			return;
		}

		out.beginObject();

		// Only serialize non-empty island name
		if (!display.getIslandName().isEmpty()) {
			out.name("islandName").value(display.getIslandName());
		}

		// Only serialize non-empty island bio
		if (!display.getIslandBio().isEmpty()) {
			out.name("islandBio").value(display.getIslandBio());
		}

		if (display.getDisplayChoice() != DisplayChoice.CHAT) {
			out.name("displayChoice").value(display.getDisplayChoice().name());
		}

		// Only serialize booleans if false
		if (!display.isDefaultIslandName()) {
			out.name("defaultIslandName").value(false);
		}

		if (!display.isDefaultIslandBio()) {
			out.name("defaultIslandBio").value(false);
		}

		out.endObject();
	}

	@Override
	public DisplaySettings read(JsonReader in) throws IOException {
		if (in.peek() == JsonToken.NULL) {
			in.nextNull();
			return null;
		}

		String name = null;
		String bio = null;
		DisplayChoice choice = DisplayChoice.CHAT; // default fallback
		boolean defaultName = false;
		boolean defaultBio = false;

		in.beginObject();
		while (in.hasNext()) {
			switch (in.nextName()) {
			case "islandName" -> name = in.nextString();
			case "islandBio" -> bio = in.nextString();
			case "displayChoice" -> choice = DisplayChoice.valueOf(in.nextString());
			case "defaultIslandName" -> defaultName = in.nextBoolean();
			case "defaultIslandBio" -> defaultBio = in.nextBoolean();
			}
		}
		in.endObject();

		DisplaySettings display = new DisplaySettings(name, bio, choice);

		if (defaultName) {
			display.setAsDefaultIslandName();
		} else {
			display.isNotDefaultIslandName();
		}

		if (defaultBio) {
			display.setAsDefaultIslandBio();
		} else {
			display.isNotDefaultIslandBio();
		}

		return display;
	}
}