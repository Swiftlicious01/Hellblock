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
		if (display == null) {
			out.nullValue();
			return;
		}

		out.beginObject();
		out.name("islandName").value(display.getIslandName());
		out.name("islandBio").value(display.getIslandBio());
		out.name("displayChoice").value(display.getDisplayChoice().name());
		out.name("defaultIslandName").value(display.isDefaultIslandName());
		out.name("defaultIslandBio").value(display.isDefaultIslandBio());
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