package com.swiftlicious.hellblock.utils.adapters;

import java.io.IOException;

import org.bukkit.util.BoundingBox;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

public class BoundingBoxAdapter extends TypeAdapter<BoundingBox> {

	@Override
	public void write(JsonWriter out, BoundingBox boundingBox) throws IOException {
		if (boundingBox == null) {
			out.nullValue();
			return;
		}
		out.beginArray();
		out.value(boundingBox.getMinX());
		out.value(boundingBox.getMinY());
		out.value(boundingBox.getMinZ());
		out.value(boundingBox.getMaxX());
		out.value(boundingBox.getMaxY());
		out.value(boundingBox.getMaxZ());
		out.endArray();
	}

	@Override
	public BoundingBox read(JsonReader in) throws IOException {
		if (in.peek() == JsonToken.NULL) {
			in.nextNull();
			return null;
		}
		in.beginArray();
		double minX = in.nextDouble();
		double minY = in.nextDouble();
		double minZ = in.nextDouble();
		double maxX = in.nextDouble();
		double maxY = in.nextDouble();
		double maxZ = in.nextDouble();
		in.endArray();
		return new BoundingBox(minX, minY, minZ, maxX, maxY, maxZ);
	}
}
