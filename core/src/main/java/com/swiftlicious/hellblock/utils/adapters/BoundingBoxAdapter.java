package com.swiftlicious.hellblock.utils.adapters;

import java.io.IOException;

import org.bukkit.util.BoundingBox;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

public class BoundingBoxAdapter extends TypeAdapter<BoundingBox> {

	@Override
	public void write(JsonWriter out, BoundingBox box) throws IOException {
		if (box == null) {
			out.nullValue();
			return;
		}

		out.beginObject();
		out.name("minX").value(box.getMinX());
		out.name("minY").value(box.getMinY());
		out.name("minZ").value(box.getMinZ());
		out.name("maxX").value(box.getMaxX());
		out.name("maxY").value(box.getMaxY());
		out.name("maxZ").value(box.getMaxZ());
		out.endObject();
	}

	@Override
	public BoundingBox read(JsonReader in) throws IOException {
		if (in.peek() == JsonToken.NULL) {
			in.nextNull();
			return null;
		}

		double minX = 0, minY = 0, minZ = 0, maxX = 0, maxY = 0, maxZ = 0;

		in.beginObject();
		while (in.hasNext()) {
			switch (in.nextName()) {
			case "minX" -> minX = in.nextDouble();
			case "minY" -> minY = in.nextDouble();
			case "minZ" -> minZ = in.nextDouble();
			case "maxX" -> maxX = in.nextDouble();
			case "maxY" -> maxY = in.nextDouble();
			case "maxZ" -> maxZ = in.nextDouble();
			}
		}
		in.endObject();

		return new BoundingBox(minX, minY, minZ, maxX, maxY, maxZ);
	}
}