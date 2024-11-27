package com.swiftlicious.hellblock.generation;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.StateFlag.State;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.player.UserData;
import com.swiftlicious.hellblock.protection.HellblockFlag;
import com.swiftlicious.hellblock.scheduler.SchedulerTask;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class HellblockBorderTask implements Runnable {

	protected final HellblockPlugin instance;
	protected final Particle dustParticle;

	private final UUID playerUUID;
	private final SchedulerTask cancellableTask;

	private static final int Y_HEIGHT_DIFFERENCE = 10;
	private static final double Z_GAP = 2;
	private static final double X_GAP = 2;
	private static final double PLAYER_DISTANCE = 50;

	public HellblockBorderTask(HellblockPlugin plugin, @NotNull UUID playerUUID) {
		instance = plugin;
		this.playerUUID = playerUUID;
		this.cancellableTask = plugin.getScheduler().sync().runRepeating(this, 0, 20, null);
		this.dustParticle = plugin.getVersionManager().isVersionNewerThan1_20_5() ? Particle.valueOf("DUST")
				: Particle.valueOf("REDSTONE");
	}

	@Override
	public void run() {
		Player player = Bukkit.getPlayer(playerUUID);
		if (player == null || !player.isOnline())
			return;
		if (!player.getWorld().getName().equalsIgnoreCase(instance.getConfigManager().worldName()))
			return;

		if (instance.getConfigManager().worldguardProtect()) {
			Optional<UserData> onlineUser = instance.getStorageManager().getOnlineUser(playerUUID);
			if (onlineUser.isEmpty())
				return;
			UUID owner = onlineUser.get().getHellblockData().getOwnerUUID();
			Set<ProtectedRegion> regions = instance.getWorldGuardHandler().getRegions(playerUUID);
			ProtectedRegion hellblockRegion = instance.getWorldGuardHandler().getRegion(playerUUID,
					onlineUser.get().getHellblockData().getID());
			for (ProtectedRegion region : regions) {
				if (region == null)
					return;
				if (region.equals(instance.getWorldGuardHandler().getSpawnRegion()))
					continue;

				instance.getStorageManager().getOfflineUserData(owner, instance.getConfigManager().lockData()).thenAccept((result) -> {
					if (result.isEmpty())
						return;
					UserData offlineUser = result.get();
					if ((hellblockRegion != null && region.equals(hellblockRegion))
							|| (owner != null && offlineUser.getHellblockData().getParty().contains(playerUUID))) {
						spawnBorderParticles(player, region.getId(), new BlueBorder());
					} else {
						State flag = region.getFlag(instance.getIslandProtectionManager()
								.convertToWorldGuardFlag(HellblockFlag.FlagType.ENTRY));
						UUID ownerUUID = region.getOwners().getUniqueIds().stream().findFirst().orElse(null);
						if ((flag == null || flag == StateFlag.State.ALLOW) || (ownerUUID != null
								&& (!instance.getCoopManager().trackBannedPlayer(ownerUUID, playerUUID)
										|| instance.getCoopManager().checkIfVisitorIsWelcome(player, ownerUUID)))) {
							spawnBorderParticles(player, region.getId(), new GreenBorder());
						} else {
							spawnBorderParticles(player, region.getId(), new RedBorder());
						}
					}
				});
			}
		} else {
			// TODO: using plugin protection
		}
	}

	public void cancelBorderShowcase() {
		if (this.cancellableTask != null)
			this.cancellableTask.cancel();
	}

	public void spawnBorderParticles(Player player, String regionName, PlayerBorder playerBorder, int seconds) {
		World world = player.getWorld();
		RegionManager regionManager = instance.getWorldGuardHandler().getWorldGuardPlatform().getRegionContainer()
				.get(BukkitAdapter.adapt(world));
		if (regionManager == null)
			return;
		ProtectedRegion region = regionManager.getRegion(regionName);
		if (region == null)
			return;

		ChunkOutlineEntry entry = new ChunkOutlineEntry(player, region, player.getLocation().getBlockY(), playerBorder);
		entry.display(seconds);
	}

	public void spawnBorderParticles(Player player, String regionName, PlayerBorder playerBorder) {
		spawnBorderParticles(player, regionName, playerBorder, 10);
	}

	public interface PlayerBorder {
		void sendBorderDisplay(Player player, double x, double y, double z);
	}

	public record ChunkOutlineEntry(Player player, ProtectedRegion region, int yHeight, PlayerBorder playerBorder) {

		public void display(int seconds) {
			BlockVector3 min = region.getMinimumPoint();
			BlockVector3 max = region.getMaximumPoint();
			Location defaultLocation = player.getLocation();

			for (int i = 0; i < seconds; i++) {
				int delayTicks = i * 20;
				HellblockPlugin.getInstance().getScheduler().sync()
						.runLater(() -> onParticle(min, max, defaultLocation), delayTicks, player.getLocation());
			}
		}

		private void onParticle(BlockVector3 min, BlockVector3 max, Location defaultLocation) {
			if (!Objects.requireNonNull(defaultLocation.getWorld()).equals(player.getWorld())) {
				return;
			}

			if (defaultLocation.distanceSquared(player.getLocation()) > 250) {
				return;
			}

			for (int y = yHeight - Y_HEIGHT_DIFFERENCE; y <= yHeight + Y_HEIGHT_DIFFERENCE; y++) {
				for (int x = min.x(), xCount = 0; x <= max.x(); x++, xCount++) {
					if (xCount % X_GAP == 0) {
						spawnParticle(x, y, min.z());
						spawnParticle(x, y, max.z());
					}
				}

				for (int z = min.z(), zCount = 0; z <= max.z(); z++, zCount++) {
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