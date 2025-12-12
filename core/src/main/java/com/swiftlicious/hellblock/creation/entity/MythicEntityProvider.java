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

	@NotNull
	@Override
	public Entity spawn(@NotNull Location location, @NotNull String id, @NotNull Map<String, Object> propertyMap) {
		if (this.mythicBukkit == null || mythicBukkit.isClosed()) {
			this.mythicBukkit = MythicBukkit.inst();
		}
		final Optional<MythicMob> mythicMob = mythicBukkit.getMobManager().getMythicMob(id);
		if (mythicMob.isPresent()) {
			final MythicMob theMob = mythicMob.get();
			final Position position = Position.of(location);
			final AbstractLocation abstractLocation = new AbstractLocation(position);
			final ActiveMob activeMob = theMob.spawn(abstractLocation,
					((Number) propertyMap.getOrDefault("level", 0d)).doubleValue());
			return activeMob.getEntity().getBukkitEntity();
		}
		throw new NullPointerException("MythicMobs: " + id + " doesn't exist.");
	}

	/**
	 * Checks whether an entity corresponds to a specific MythicMob ID.
	 */
	public boolean isMythicMob(@NotNull Entity entity, @NotNull String targetId) {
		if (this.mythicBukkit == null || mythicBukkit.isClosed()) {
			this.mythicBukkit = MythicBukkit.inst();
		}

		final Optional<ActiveMob> activeMobOpt = mythicBukkit.getMobManager().getActiveMob(entity.getUniqueId());
		if (activeMobOpt.isPresent()) {
			ActiveMob activeMob = activeMobOpt.get();
			MythicMob mythicMob = activeMob.getType();
			String internalName = mythicMob.getInternalName();
			return internalName.equalsIgnoreCase(targetId);
		}
		return false;
	}

	@Override
	public String identifier() {
		return "MythicMobs";
	}
}