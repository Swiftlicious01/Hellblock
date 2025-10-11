package com.swiftlicious.hellblock.utils.adapters;

import java.io.IOException;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import com.swiftlicious.hellblock.player.VisitData;
import com.swiftlicious.hellblock.utils.LocationUtils;

public class VisitDataAdapter extends TypeAdapter<VisitData> {

	@Override
	public void write(JsonWriter out, VisitData visitData) throws IOException {
		if (visitData == null) {
			out.nullValue();
			return;
		}

		out.beginObject();
		out.name("totalVisits").value(visitData.getTotalVisits());
		out.name("visitsToday").value(visitData.getDailyVisits());
		out.name("visitsThisWeek").value(visitData.getWeeklyVisits());
		out.name("visitsThisMonth").value(visitData.getMonthlyVisits());
		out.name("lastVisitReset").value(visitData.getLastVisitReset());
		out.name("featuredUntil").value(visitData.getFeaturedUntil());

		// Serialize warp location if present
		Location warp = visitData.getWarpLocation();
		if (warp != null) {
			out.name("warp");
			out.beginObject();
			out.name("world").value(warp.getWorld().getName());
			out.name("x").value(LocationUtils.round(warp.getX(), 3));
			out.name("y").value(LocationUtils.round(warp.getY(), 3));
			out.name("z").value(LocationUtils.round(warp.getZ(), 3));
			out.name("yaw").value(LocationUtils.round((double) warp.getYaw(), 3));
			out.name("pitch").value(LocationUtils.round((double) warp.getPitch(), 3));
			out.endObject();
		}

		out.endObject();
	}

	@Override
	public VisitData read(JsonReader in) throws IOException {
		if (in.peek() == JsonToken.NULL) {
			in.nextNull();
			return null;
		}

		VisitData visitData = new VisitData();
		Location warp = null;

		String world = null;
		double x = 0, y = 0, z = 0;
		float yaw = 0, pitch = 0;

		in.beginObject();
		while (in.hasNext()) {
			String name = in.nextName();
			switch (name) {
			case "totalVisits" -> visitData.setTotalVisits(in.nextInt());
			case "visitsToday" -> visitData.setVisitsToday(in.nextInt());
			case "visitsThisWeek" -> visitData.setVisitsThisWeek(in.nextInt());
			case "visitsThisMonth" -> visitData.setVisitsThisMonth(in.nextInt());
			case "lastVisitReset" -> visitData.setLastVisitReset(in.nextLong());
			case "featuredUntil" -> visitData.setFeaturedUntil(in.nextLong());

			case "warp" -> {
				in.beginObject();
				while (in.hasNext()) {
					switch (in.nextName()) {
					case "world" -> world = in.nextString();
					case "x" -> x = in.nextDouble();
					case "y" -> y = in.nextDouble();
					case "z" -> z = in.nextDouble();
					case "yaw" -> yaw = (float) in.nextDouble();
					case "pitch" -> pitch = (float) in.nextDouble();
					}
				}
				in.endObject();
			}
			}
		}
		in.endObject();

		// If warp data was found and world is valid, reconstruct Location
		if (world != null) {
			World bukkitWorld = Bukkit.getWorld(world);
			if (bukkitWorld != null) {
				warp = new Location(bukkitWorld, x, y, z, yaw, pitch);
				visitData.setWarpLocation(warp);
			}
		}

		return visitData;
	}
}