package com.swiftlicious.hellblock.protection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.NotNull;

import com.sk89q.worldguard.WorldGuard;
import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.api.Reloadable;
import com.swiftlicious.hellblock.config.locale.MessageConstants;
import com.swiftlicious.hellblock.player.UserData;

import net.kyori.adventure.audience.Audience;

public class ProtectionManager implements ProtectionManagerInterface, Reloadable {

	protected final HellblockPlugin instance;

	public IslandProtection islandProtection;

	private final Map<UUID, HellblockCuboid> cachedHellblocks;

	private final boolean worldGuard = Bukkit.getPluginManager().isPluginEnabled("WorldGuard");

	public ProtectionManager(HellblockPlugin plugin) {
		instance = plugin;
		this.cachedHellblocks = new HashMap<>();
		setProtectionFromConfig();
	}

	@Override
	public void reload() {
		setProtectionFromConfig();
		this.cachedHellblocks.clear();
		loadHellblockCuboids();
	}

	private void loadHellblockCuboids() {
		// If using worldguard ignore this method.
		if (islandProtection instanceof ProtectionAsync) {
			for (UUID playerData : instance.getStorageManager().getDataSource().getUniqueUsers()) {
				instance.getStorageManager().getOfflineUserData(playerData, instance.getConfigManager().lockData())
						.thenAccept((result) -> {
							if (result.isEmpty())
								return;
							UserData offlineUser = result.get();
							UUID ownerUUID = offlineUser.getHellblockData().getOwnerUUID();
							if (ownerUUID != null && ownerUUID.equals(playerData)) {
								BoundingBox bounds = offlineUser.getHellblockData().getBoundingBox();
								if (bounds != null) {
									String world = instance.getConfigManager().perPlayerWorlds() ? ownerUUID.toString()
											: instance.getConfigManager().worldName();
									HellblockCuboid cuboid = new HellblockCuboid(String.format("%s_%s",
											ownerUUID.toString(), offlineUser.getHellblockData().getID()), world,
											bounds);
									getCachedHellblocks().putIfAbsent(ownerUUID, cuboid);
								}
							}
						});
			}
		}
	}

	public Map<UUID, HellblockCuboid> getCachedHellblocks() {
		return this.cachedHellblocks;
	}

	private void setProtectionFromConfig() {
		if ((worldGuard) && instance.getConfigManager().worldguardProtect() && WorldGuardHook.isWorking()) {
			if (instance.getIntegrationManager().isHooked("WorldGuard", "7")) {
				islandProtection = new WorldGuardHook();
			} else {
				String version = WorldGuard.getVersion();
				if (!version.startsWith("7.")) {
					instance.getPluginLogger()
							.warn("WorldGuard version must be 7.0 or higher to be able to use it for Hellblock.");
				}
			}
		}

		if ((worldGuard) && instance.getConfigManager().worldguardProtect() && !WorldGuardHook.isWorking()) {
			instance.getPluginLogger().warn(
					"WorldGuard version doesn't support this minecraft version, disabling WorldGuard integration.");
		}

		if (islandProtection == null) {
			islandProtection = new ProtectionAsync();
			ProtectionEvents events = new ProtectionEvents(instance);
			events.reload();
		}
	}

	public IslandProtection getIslandProtection() {
		return this.islandProtection;
	}

	@Override
	public void changeProtectionFlag(@NotNull World world, @NotNull UUID id, @NotNull HellblockFlag flag) {
		Optional<UserData> user = instance.getStorageManager().getOnlineUser(id);
		if (user.isEmpty() || !user.get().isOnline()) {
			return;
		}
		Audience audience = instance.getSenderFactory().getAudience(user.get().getPlayer());
		if (user.get().getHellblockData().isAbandoned()) {
			audience.sendMessage(
					instance.getTranslationManager().render(MessageConstants.MSG_HELLBLOCK_IS_ABANDONED.build()));
			return;
		}
		if (!user.get().getHellblockData().hasHellblock()) {
			audience.sendMessage(
					instance.getTranslationManager().render(MessageConstants.MSG_HELLBLOCK_NOT_FOUND.build()));
			return;
		}
		if (user.get().getHellblockData().getOwnerUUID() == null) {
			throw new NullPointerException("Owner reference returned null, please report this to the developer.");
		}
		if (user.get().getHellblockData().getOwnerUUID() != null
				&& !user.get().getHellblockData().getOwnerUUID().equals(id)) {
			audience.sendMessage(
					instance.getTranslationManager().render(MessageConstants.MSG_NOT_OWNER_OF_HELLBLOCK.build()));
			return;
		}

		user.get().getHellblockData().setProtectionValue(flag);
		islandProtection.changeHellblockFlag(world, user.get(), flag);
	}

	@Override
	public void changeLockStatus(@NotNull World world, @NotNull UUID id) {
		Optional<UserData> user = instance.getStorageManager().getOnlineUser(id);
		if (user.isEmpty() || !user.get().isOnline()) {
			return;
		}
		Audience audience = instance.getSenderFactory().getAudience(user.get().getPlayer());
		if (user.get().getHellblockData().isAbandoned()) {
			audience.sendMessage(
					instance.getTranslationManager().render(MessageConstants.MSG_HELLBLOCK_IS_ABANDONED.build()));
			return;
		}
		if (!user.get().getHellblockData().hasHellblock()) {
			audience.sendMessage(
					instance.getTranslationManager().render(MessageConstants.MSG_HELLBLOCK_NOT_FOUND.build()));
			return;
		}
		if (user.get().getHellblockData().getOwnerUUID() == null) {
			throw new NullPointerException("Owner reference returned null, please report this to the developer.");
		}
		if (user.get().getHellblockData().getOwnerUUID() != null
				&& !user.get().getHellblockData().getOwnerUUID().equals(id)) {
			audience.sendMessage(
					instance.getTranslationManager().render(MessageConstants.MSG_NOT_OWNER_OF_HELLBLOCK.build()));
			return;
		}

		boolean locked = user.get().getHellblockData().isLocked();
		HellblockFlag flag = new HellblockFlag(HellblockFlag.FlagType.ENTRY,
				locked ? HellblockFlag.AccessType.DENY : HellblockFlag.AccessType.ALLOW);
		user.get().getHellblockData().setProtectionValue(flag);
		islandProtection.lockHellblock(world, user.get());
	}

	@Override
	public void clearHellblockEntities(@NotNull World world, @NotNull BoundingBox bounds) {
		instance.getScheduler().executeSync(() -> {
			world.getEntities().stream()
					.filter(entity -> entity.getType() != EntityType.PLAYER && bounds.contains(entity.getBoundingBox()))
					.forEach(Entity::remove);
		});
	}

	@Override
	public CompletableFuture<List<Block>> getHellblockBlocks(@NotNull World world, @NotNull UUID id) {
		CompletableFuture<List<Block>> blockSupplier = new CompletableFuture<>();
		instance.getStorageManager().getOfflineUserData(id, instance.getConfigManager().lockData())
				.thenAccept((result) -> {
					if (result.isEmpty())
						return;
					UserData offlineUser = result.get();
					BoundingBox bounds = offlineUser.getHellblockData().getBoundingBox();
					if (bounds == null)
						blockSupplier.complete(new ArrayList<>());

					int minX = (int) Math.min(bounds.getMinX(), bounds.getMaxX());
					int minY = (int) Math.min(bounds.getMinY(), bounds.getMaxY());
					int minZ = (int) Math.min(bounds.getMinZ(), bounds.getMaxZ());
					int maxX = (int) Math.max(bounds.getMinX(), bounds.getMaxX());
					int maxY = (int) Math.max(bounds.getMinY(), bounds.getMaxY());
					int maxZ = (int) Math.max(bounds.getMinZ(), bounds.getMaxZ());

					List<Block> blocks = new ArrayList<>();
					for (int x = minX; x <= maxX; x++) {
						for (int y = minY; y <= maxY; y++) {
							for (int z = minZ; z <= maxZ; z++) {
								Block block = world.getBlockAt(x, y, z);
								blocks.add(block);
							}
						}
					}
					blockSupplier.complete(blocks);
				});
		return blockSupplier;
	}
}