package com.swiftlicious.hellblock.utils.adapters;

import java.io.IOException;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import com.swiftlicious.hellblock.utils.LocationUtils;

public class LocationAdapter extends TypeAdapter<Location> {

	@Override
	public void write(JsonWriter out, Location loc) throws IOException {
		if (loc == null || loc.getWorld() == null) {
			out.nullValue();
			return;
		}

		out.beginObject();
		out.name("world").value(loc.getWorld().getName());
		out.name("x").value(LocationUtils.round(loc.getX(), 3));
		out.name("y").value(LocationUtils.round(loc.getY(), 3));
		out.name("z").value(LocationUtils.round(loc.getZ(), 3));
		out.name("yaw").value(LocationUtils.round((double) loc.getYaw(), 3));
		out.name("pitch").value(LocationUtils.round((double) loc.getPitch(), 3));
		out.endObject();
	}

	@Override
	public Location read(JsonReader in) throws IOException {
		if (in.peek() == JsonToken.NULL) {
			in.nextNull();
			return null;
		}

		String worldName = null;
		double x = 0, y = 0, z = 0;
		float yaw = 0, pitch = 0;

		in.beginObject();
		while (in.hasNext()) {
			switch (in.nextName()) {
			case "world" -> worldName = in.nextString();
			case "x" -> x = in.nextDouble();
			case "y" -> y = in.nextDouble();
			case "z" -> z = in.nextDouble();
			case "yaw" -> yaw = (float) in.nextDouble();
			case "pitch" -> pitch = (float) in.nextDouble();
			}
		}
		in.endObject();

		World world = Bukkit.getWorld(worldName);
		return (world != null) ? new Location(world, x, y, z, yaw, pitch) : null;
	}
}