package com.swiftlicious.hellblock.generation;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.handlers.VersionHelper;
import com.swiftlicious.hellblock.player.UserData;
import com.swiftlicious.hellblock.protection.HellblockFlag;
import com.swiftlicious.hellblock.protection.HellblockFlag.AccessType;
import com.swiftlicious.hellblock.scheduler.SchedulerTask;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.NotNull;

public class BorderHandler implements Runnable {

	protected final HellblockPlugin instance;
	protected final Particle dustParticle;

	private final UUID playerUUID;
	private final SchedulerTask cancellableTask;

	private static final int Y_HEIGHT_DIFFERENCE = 10;
	private static final double Z_GAP = 2;
	private static final double X_GAP = 2;
	private static final double PLAYER_DISTANCE = 50;

	public BorderHandler(HellblockPlugin plugin, @NotNull UUID playerUUID) {
		instance = plugin;
		this.playerUUID = playerUUID;
		this.cancellableTask = plugin.getScheduler().sync().runRepeating(this, 0, 20, null);
		this.dustParticle = VersionHelper.isVersionNewerThan1_20_5() ? Particle.valueOf("DUST")
				: Particle.valueOf("REDSTONE");
	}

	@Override
	public void run() {
		Player player = Bukkit.getPlayer(playerUUID);
		if (player == null || !player.isOnline())
			return;
		if (!instance.getHellblockHandler().isInCorrectWorld(player))
			return;
		if (player.getLocation() == null)
			return;

		Optional<UserData> onlineUser = instance.getStorageManager().getOnlineUser(playerUUID);
		if (onlineUser.isEmpty())
			return;
		instance.getCoopManager().getHellblockOwnerOfVisitingIsland(player).thenAccept(ownerUUID -> {
			if (ownerUUID == null)
				return;

			instance.getStorageManager().getOfflineUserData(ownerUUID, instance.getConfigManager().lockData())
					.thenAccept((result) -> {
						if (result.isEmpty())
							return;
						UserData offlineUser = result.get();
						if (offlineUser.getHellblockData().isAbandoned())
							return;
						BoundingBox bounds = offlineUser.getHellblockData().getBoundingBox();
						if (bounds == null)
							return;
						if (bounds.contains(player.getBoundingBox()) && (ownerUUID.equals(player.getUniqueId())
								|| offlineUser.getHellblockData().getParty().contains(player.getUniqueId()))) {
							spawnBorderParticles(player, bounds, new BlueBorder());
						} else {
							AccessType flag = offlineUser.getHellblockData()
									.getProtectionValue(HellblockFlag.FlagType.ENTRY);
							CompletableFuture<Boolean> banStatus = instance.getCoopManager()
									.trackBannedPlayer(ownerUUID, playerUUID);
							CompletableFuture<Boolean> visitStatus = instance.getCoopManager()
									.checkIfVisitorIsWelcome(player, ownerUUID);
							CompletableFuture<Void> results = CompletableFuture.allOf(banStatus, visitStatus);
							results.thenRun(() -> {
								if (flag == AccessType.ALLOW && !banStatus.join() && visitStatus.join()) {
									spawnBorderParticles(player, bounds, new GreenBorder());
								} else {
									spawnBorderParticles(player, bounds, new RedBorder());
								}
							});
						}
					});
		});
	}

	public void cancelBorderShowcase() {
		if (!this.cancellableTask.isCancelled())
			this.cancellableTask.cancel();
	}

	public void spawnBorderParticles(Player player, BoundingBox bounds, PlayerBorder playerBorder, int seconds) {
		ChunkOutlineEntry entry = new ChunkOutlineEntry(player, bounds, player.getLocation().getBlockY(), playerBorder);
		entry.display(seconds);
	}

	public void spawnBorderParticles(Player player, BoundingBox bounds, PlayerBorder playerBorder) {
		spawnBorderParticles(player, bounds, playerBorder, 10);
	}

	public interface PlayerBorder {
		void sendBorderDisplay(Player player, double x, double y, double z);
	}

	public record ChunkOutlineEntry(Player player, BoundingBox bounds, int yHeight, PlayerBorder playerBorder) {

		public void display(int seconds) {
			Location min = new Location(player.getWorld(), bounds.getMinX(), bounds.getMinY(), bounds.getMinZ());
			Location max = new Location(player.getWorld(), bounds.getMaxX(), bounds.getMaxY(), bounds.getMaxZ());
			Location defaultLocation = player.getLocation();

			for (int i = 0; i < seconds; i++) {
				int delayTicks = i * 20;
				HellblockPlugin.getInstance().getScheduler().sync()
						.runLater(() -> onParticle(min, max, defaultLocation), delayTicks, player.getLocation());
			}
		}

		private void onParticle(Location min, Location max, Location defaultLocation) {
			if (!Objects.requireNonNull(defaultLocation.getWorld()).equals(player.getWorld())) {
				return;
			}

			if (defaultLocation.distanceSquared(player.getLocation()) > 250) {
				return;
			}

			for (int y = yHeight - Y_HEIGHT_DIFFERENCE; y <= yHeight + Y_HEIGHT_DIFFERENCE; y++) {
				for (int x = min.getBlockX(), xCount = 0; x <= max.x(); x++, xCount++) {
					if (xCount % X_GAP == 0) {
						spawnParticle(x, y, min.z());
						spawnParticle(x, y, max.z());
					}
				}

				for (int z = min.getBlockZ(), zCount = 0; z <= max.z(); z++, zCount++) {
					if (zCount % Z_GAP == 0) {
						spawnParticle(min.x(), y, z);
						spawnParticle(max.x(), y, z);
					}
				}
			}
		}

		private void spawnParticle(double x, double y, double z) {
			Location particleLocation = new Location(player.getWorld(), x + 0.5, y + 0.5, z + 0.5);
			double playerDistance = player.getLocation().distance(particleLocation);

			if (playerDistance <= PLAYER_DISTANCE) {
				playerBorder.sendBorderDisplay(player, x + 0.5, y + 0.5, z + 0.5);
			}
		}
	}

	public class GreenBorder implements PlayerBorder {

		@Override
		public void sendBorderDisplay(Player player, double x, double y, double z) {
			player.spawnParticle(dustParticle, x, y, z, 0, 0, 0, 0, 0, new Particle.DustOptions(Color.LIME, 1.5F));
		}
	}

	public class RedBorder implements PlayerBorder {

		@Override
		public void sendBorderDisplay(Player player, double x, double y, double z) {
			player.spawnParticle(dustParticle, x, y, z, 0, 0, 0, 0, 0, new Particle.DustOptions(Color.RED, 1.5F));
		}
	}

	public class BlueBorder implements PlayerBorder {

		@Override
		public void sendBorderDisplay(Player player, double x, double y, double z) {
			player.spawnParticle(dustParticle, x, y, z, 0, 0, 0, 0, 0, new Particle.DustOptions(Color.BLUE, 1.5F));
		}
	}
}