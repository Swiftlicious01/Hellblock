package com.swiftlicious.hellblock.creation.entity;

import java.util.Map;
import java.util.Optional;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;

import io.lumine.mythic.api.adapters.AbstractLocation;
import io.lumine.mythic.api.mobs.MythicMob;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.bukkit.utils.serialize.Position;
import io.lumine.mythic.core.mobs.ActiveMob;

public class MythicEntityProvider implements EntityProvider {

	private MythicBukkit mythicBukkit;

	@Override
	public String identifier() {
		return "MythicMobs";
	}

	@NotNull
	@Override
	public Entity spawn(@NotNull Location location, @NotNull String id, @NotNull Map<String, Object> propertyMap) {
		if (this.mythicBukkit == null || mythicBukkit.isClosed()) {
			this.mythicBukkit = MythicBukkit.inst();
		}
		Optional<MythicMob> mythicMob = mythicBukkit.getMobManager().getMythicMob(id);
		if (mythicMob.isPresent()) {
			MythicMob theMob = mythicMob.get();
			Position position = Position.of(location);
			AbstractLocation abstractLocation = new AbstractLocation(position);
			ActiveMob activeMob = theMob.spawn(abstractLocation,
					((Number) propertyMap.getOrDefault("level", 0d)).doubleValue());
			return activeMob.getEntity().getBukkitEntity();
		}
		throw new NullPointerException("MythicMobs: " + id + " doesn't exist.");
	}
}