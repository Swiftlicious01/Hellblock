package com.swiftlicious.hellblock.creation.entity;

import java.util.Map;
import java.util.Optional;

import org.bukkit.Location;
import org.bukkit.entity.Entity;

import com.swiftlicious.hellblock.HellblockPlugin;

import io.lumine.mythic.api.adapters.AbstractLocation;
import io.lumine.mythic.api.mobs.MythicMob;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.bukkit.utils.serialize.Position;
import io.lumine.mythic.core.mobs.ActiveMob;

public class MythicMobsEntity implements EntityLibrary {

	private MythicBukkit mythicBukkit;

	public MythicMobsEntity() {
		this.mythicBukkit = MythicBukkit.inst();
	}

	@Override
	public String identification() {
		return "MythicMobs";
	}

	@Override
	public Entity spawn(Location location, String id, Map<String, Object> propertyMap) {
		if (this.mythicBukkit == null || mythicBukkit.isClosed()) {
			this.mythicBukkit = MythicBukkit.inst();
		}
		Optional<MythicMob> mythicMob = mythicBukkit.getMobManager().getMythicMob(id);
		if (mythicMob.isPresent()) {
			MythicMob theMob = mythicMob.get();
			Position position = Position.of(location);
			AbstractLocation abstractLocation = new AbstractLocation(position);
			ActiveMob activeMob = theMob.spawn(abstractLocation, HellblockPlugin.getInstance().getConfigUtils().getDoubleValue(propertyMap.get("level")));
			return activeMob.getEntity().getBukkitEntity();
		}
		throw new NullPointerException("MythicMobs: " + id + " doesn't exist.");
	}
}
