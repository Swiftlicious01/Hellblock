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
	public void write(JsonWriter out, Location location) throws IOException {
		if (location == null || location.getWorld() == null) {
			out.nullValue();
			return;
		}
		out.beginArray();
		out.value(location.getWorld().getName());
		out.value(LocationUtils.round(location.getX(), 3));
		out.value(LocationUtils.round(location.getY(), 3));
		out.value(LocationUtils.round(location.getZ(), 3));
		// This is required for 1.19-1.19.2 compatibility.
		out.value(LocationUtils.round((double) location.getYaw(), 3));
		out.value(LocationUtils.round((double) location.getPitch(), 3));
		out.endArray();
	}

	@Override
	public Location read(JsonReader in) throws IOException {
		if (in.peek() == JsonToken.NULL) {
			in.nextNull();
			return null;
		}
		in.beginArray();
		World world = Bukkit.getWorld(in.nextString());
		double x = in.nextDouble();
		double y = in.nextDouble();
		double z = in.nextDouble();
		float yaw = (float) in.nextDouble();
		float pitch = (float) in.nextDouble();
		in.endArray();
		return new Location(world, x, y, z, yaw, pitch);
	}
}
