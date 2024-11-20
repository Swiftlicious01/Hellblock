package com.swiftlicious.hellblock.utils.adapters;

import java.io.IOException;

import org.bukkit.util.BoundingBox;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import com.swiftlicious.hellblock.HellblockPlugin;

public class BoundingBoxAdapter extends TypeAdapter<BoundingBox> {

	@Override
	public void write(JsonWriter out, BoundingBox boundingBox) throws IOException {
		if (boundingBox == null) {
			out.nullValue();
			return;
		}
		out.beginArray();
		out.value(boundingBox.getMinX());
		out.value(boundingBox.getMinZ());
		out.value(boundingBox.getMaxX());
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
		double minY = HellblockPlugin.getInstance().getHellblockHandler().getHellblockWorld().getMinHeight();
		double minZ = in.nextDouble();
		double maxX = in.nextDouble();
		double maxY = HellblockPlugin.getInstance().getHellblockHandler().getHellblockWorld().getMaxHeight();
		double maxZ = in.nextDouble();
		in.endArray();
		return new BoundingBox(minX, minY, minZ, maxX, maxY, maxZ);
	}
}
