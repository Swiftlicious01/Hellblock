package com.swiftlicious.hellblock.protection;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.sk89q.worldguard.WorldGuard;
import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.api.Reloadable;
import com.swiftlicious.hellblock.config.locale.MessageConstants;
import com.swiftlicious.hellblock.player.HellblockData;
import com.swiftlicious.hellblock.player.UserData;
import com.swiftlicious.hellblock.sender.Sender;
import com.swiftlicious.hellblock.world.HellblockWorld;

public class ProtectionManager implements ProtectionManagerInterface, Reloadable {

	protected final HellblockPlugin instance;

	public IslandProtection islandProtection;

	private final boolean worldGuard = Bukkit.getPluginManager().isPluginEnabled("WorldGuard");

	public ProtectionManager(HellblockPlugin plugin) {
		instance = plugin;
	}

	@Override
	public void load() {
		setProtectionFromConfig();
	}

	private void setProtectionFromConfig() {
		if ((worldGuard) && instance.getConfigManager().worldguardProtect() && WorldGuardHook.isWorking()) {
			if (instance.getIntegrationManager().isHooked("WorldGuard", "7")) {
				islandProtection = new WorldGuardHook(instance);
			} else {
				final String version = WorldGuard.getVersion();
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

		if (islandProtection != null) {
			return;
		}
		islandProtection = new DefaultProtection(instance);
		final ProtectionEvents events = new ProtectionEvents(instance);
		events.reload();
	}

	public IslandProtection getIslandProtection() {
		return this.islandProtection;
	}

	@Override
	public void changeProtectionFlag(@NotNull World world, @NotNull UUID id, @NotNull HellblockFlag flag) {
		validateUser(id, world).ifPresent(user -> {
			user.getHellblockData().setProtectionValue(flag);
			islandProtection.changeHellblockFlag(world, user, flag);
		});
	}

	@Override
	public void changeLockStatus(@NotNull World world, @NotNull UUID id) {
		validateUser(id, world).ifPresent(user -> {
			final boolean locked = user.getHellblockData().isLocked();
			final HellblockFlag flag = new HellblockFlag(HellblockFlag.FlagType.ENTRY,
					locked ? HellblockFlag.AccessType.DENY : HellblockFlag.AccessType.ALLOW);
			user.getHellblockData().setProtectionValue(flag);
			islandProtection.lockHellblock(world, user);
		});
	}

	@Override
	public void restoreIsland(@NotNull HellblockData data) {
		final UUID ownerUUID = data.getOwnerUUID();
		if (ownerUUID == null) {
			instance.getPluginLogger()
					.severe("Tried to restore island with null owner UUID (ID: " + data.getID() + ")");
			throw new IllegalStateException("Cannot restore island without a valid owner UUID.");
		}

		final Optional<HellblockWorld<?>> worldOpt = instance.getWorldManager()
				.getWorld(instance.getWorldManager().getHellblockWorldFormat(data.getID()));

		if (worldOpt.isEmpty()) {
			throw new IllegalStateException(
					"Could not restore island because its world is missing (ID: " + data.getID() + ")");
		}

		final World bukkitWorld = worldOpt.get().bukkitWorld();

		// Update abandoned flag in data
		data.setAsAbandoned(false);

		// Reapply protection regions/messages
		islandProtection.updateHellblockMessages(bukkitWorld, ownerUUID);

		// Re-register island ownership with protection handlers
		islandProtection.restoreFlags(bukkitWorld, ownerUUID);

		instance.debug("Island for " + ownerUUID + " restored successfully.");
	}

	private Optional<UserData> validateUser(UUID id, World world) {
		final Optional<UserData> userOpt = instance.getStorageManager().getOnlineUser(id);

		if (userOpt.isEmpty() || !userOpt.get().isOnline()) {
			return Optional.empty();
		}

		final UserData user = userOpt.get();
		final Sender audience = instance.getSenderFactory().wrap(user.getPlayer());

		if (user.getHellblockData().isAbandoned()) {
			audience.sendMessage(
					instance.getTranslationManager().render(MessageConstants.MSG_HELLBLOCK_IS_ABANDONED.build()));
			return Optional.empty();
		}

		if (!user.getHellblockData().hasHellblock()) {
			audience.sendMessage(
					instance.getTranslationManager().render(MessageConstants.MSG_HELLBLOCK_NOT_FOUND.build()));
			return Optional.empty();
		}

		if (user.getHellblockData().getOwnerUUID() == null) {
			throw new NullPointerException("Owner reference returned null, please report this to the developer.");
		}

		if (user.getHellblockData().getOwnerUUID().equals(id)) {
			return Optional.of(user);
		}

		audience.sendMessage(
				instance.getTranslationManager().render(MessageConstants.MSG_NOT_OWNER_OF_HELLBLOCK.build()));
		return Optional.empty();
	}

	@Override
	public void clearHellblockEntities(@NotNull World world, @NotNull BoundingBox bounds) {
		instance.getScheduler()
				.executeSync(() -> world.getEntities().stream().filter(
						entity -> entity.getType() != EntityType.PLAYER && bounds.contains(entity.getBoundingBox()))
						.forEach(Entity::remove));
	}

	@Override
	public CompletableFuture<@Nullable BoundingBox> getHellblockBounds(@NotNull World world, @NotNull UUID ownerId) {
		if (!instance.getHellblockHandler().isInCorrectWorld(world)) {
			return CompletableFuture.completedFuture(null);
		}

		return instance.getStorageManager().getOfflineUserData(ownerId, instance.getConfigManager().lockData())
				.thenApply(result -> {
					if (result.isEmpty()) {
						return null;
					}

					UserData userData = result.get();
					BoundingBox bounds = userData.getHellblockData().getBoundingBox();

					if (bounds == null) {
						return null;
					}

					return bounds;
				});
	}

	@Override
	public CompletableFuture<Boolean> isInsideIsland(@NotNull UUID ownerId, @NotNull Location location) {
		final World world = location.getWorld();

		// Fail early if world isn't valid
		if (!instance.getHellblockHandler().isInCorrectWorld(world)) {
			return CompletableFuture.completedFuture(false);
		}

		return getHellblockBounds(world, ownerId).thenApply(bounds -> {
			if (bounds == null)
				return false;

			double x = location.getX();
			double y = location.getY();
			double z = location.getZ();

			return bounds.contains(x, y, z);
		});
	}

	@Override
	public CompletableFuture<Boolean> isInsideIsland2D(@NotNull UUID ownerId, @NotNull Location location) {
		final World world = location.getWorld();

		if (!instance.getHellblockHandler().isInCorrectWorld(world)) {
			return CompletableFuture.completedFuture(false);
		}

		return getHellblockBounds(world, ownerId).thenApply(bounds -> {
			if (bounds == null)
				return false;

			double x = location.getX();
			double z = location.getZ();

			return bounds.getMinX() <= x && x <= bounds.getMaxX() && bounds.getMinZ() <= z && z <= bounds.getMaxZ();
		});
	}

	private final Map<UUID, CompletableFuture<List<Block>>> activeBlockScans = new ConcurrentHashMap<>();

	@Override
	public CompletableFuture<List<Block>> getHellblockBlocks(@NotNull World world, @NotNull UUID id) {
		// Cancel previous scan for this UUID if running
		cancelBlockScan(id);

		final CompletableFuture<List<Block>> blockSupplier = new CompletableFuture<>();
		activeBlockScans.put(id, blockSupplier);

		instance.getStorageManager().getOfflineUserData(id, instance.getConfigManager().lockData())
				.thenAccept(result -> {
					if (result.isEmpty()) {
						blockSupplier.complete(new ArrayList<>());
						activeBlockScans.remove(id);
						return;
					}

					final UserData offlineUser = result.get();
					final BoundingBox bounds = offlineUser.getHellblockData().getBoundingBox();
					if (bounds == null) {
						blockSupplier.complete(new ArrayList<>());
						activeBlockScans.remove(id);
						return;
					}

					final int minX = (int) Math.floor(bounds.getMinX());
					final int minY = (int) Math.floor(bounds.getMinY());
					final int minZ = (int) Math.floor(bounds.getMinZ());
					final int maxX = (int) Math.ceil(bounds.getMaxX());
					final int maxY = (int) Math.ceil(bounds.getMaxY());
					final int maxZ = (int) Math.ceil(bounds.getMaxZ());

					final List<Block> collectedBlocks = new ArrayList<>();
					final Queue<int[]> positions = new ArrayDeque<>();

					// Collect coords async
					instance.getScheduler().executeAsync(() -> {
						for (int x = minX; x <= maxX; x++) {
							for (int y = minY; y <= maxY; y++) {
								for (int z = minZ; z <= maxZ; z++) {
									positions.add(new int[] { x, y, z });
								}
							}
						}

						// Start batched processing
						processBatch(id, world, positions, collectedBlocks, blockSupplier);
					});
				});

		return blockSupplier;
	}

	private void processBatch(UUID id, World world, Queue<int[]> positions, List<Block> collectedBlocks,
			CompletableFuture<List<Block>> future) {
		// If scan was cancelled, stop immediately
		if (!activeBlockScans.containsKey(id) || future.isCancelled()) {
			return;
		}

		final int batchSize = 500;
		int processed = 0;

		while (processed < batchSize && !positions.isEmpty()) {
			final int[] coords = positions.poll();
			final Block block = world.getBlockAt(coords[0], coords[1], coords[2]);
			if (!block.getType().isAir()) {
				collectedBlocks.add(block);
			}
			processed++;
		}

		if (positions.isEmpty()) {
			// Done
			future.complete(collectedBlocks);
			activeBlockScans.remove(id);
		} else {
			// Schedule next tick
			instance.getScheduler().executeSync(() -> processBatch(id, world, positions, collectedBlocks, future));
		}
	}

	public void cancelBlockScan(@NotNull UUID id) {
		final CompletableFuture<List<Block>> future = activeBlockScans.remove(id);
		if (future != null && !future.isDone()) {
			future.cancel(true);
		}
	}
}