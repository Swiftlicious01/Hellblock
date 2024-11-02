package com.swiftlicious.hellblock.generation;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.StateFlag.State;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.playerdata.HellblockPlayer;
import com.swiftlicious.hellblock.protection.HellblockFlag;
import com.swiftlicious.hellblock.scheduler.CancellableTask;

import java.util.Objects;
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

	private final HellblockPlugin instance;

	private final UUID playerUUID;
	private final CancellableTask cancellableTask;

	private static final int Y_HEIGHT_DIFFERENCE = 10;
	private static final double Z_GAP = 2;
	private static final double X_GAP = 2;
	private static final double PLAYER_DISTANCE = 50;

	public HellblockBorderTask(HellblockPlugin plugin, @NotNull UUID playerUUID) {
		instance = plugin;
		this.playerUUID = playerUUID;
		this.cancellableTask = plugin.getScheduler().runTaskSyncTimer(this, null, 0, 20);
	}

	@Override
	public void run() {
		if (Bukkit.getPlayer(playerUUID) == null || !Bukkit.getPlayer(playerUUID).isOnline())
			return;
		Player player = Bukkit.getPlayer(playerUUID);
		if (!player.getWorld().getName().equalsIgnoreCase(instance.getHellblockHandler().getWorldName()))
			return;

		HellblockPlayer pi = instance.getHellblockHandler().getActivePlayer(playerUUID);
		UUID owner = pi.getHellblockOwner();
		Set<ProtectedRegion> regions = instance.getWorldGuardHandler().getRegions(playerUUID);
		ProtectedRegion hellblockRegion = instance.getWorldGuardHandler().getRegion(playerUUID, pi.getID());
		for (ProtectedRegion region : regions) {
			if (region == null)
				return;
			if (region.equals(instance.getWorldGuardHandler().getSpawnRegion()))
				continue;

			if ((hellblockRegion != null && region.equals(hellblockRegion)) || (owner != null && instance
					.getHellblockHandler().getActivePlayer(owner).getHellblockParty().contains(playerUUID))) {
				spawnBorderParticles(player, region.getId(), new BlueBorder());
			} else {
				State flag = region.getFlag(
						instance.getIslandProtectionManager().convertToWorldGuardFlag(HellblockFlag.FlagType.ENTRY));
				UUID ownerUUID = region.getOwners().getUniqueIds().stream().findFirst().orElse(null);
				if ((flag == null || flag == StateFlag.State.ALLOW)
						|| (ownerUUID != null && (!instance.getCoopManager().trackBannedPlayer(ownerUUID, playerUUID)
								|| instance.getCoopManager().checkIfVisitorIsWelcome(player, ownerUUID)))) {
					spawnBorderParticles(player, region.getId(), new GreenBorder());
				} else {
					spawnBorderParticles(player, region.getId(), new RedBorder());
				}
			}
		}
	}

	public void cancelBorderShowcase() {
		if (!this.cancellableTask.isCancelled())
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
				HellblockPlugin.getInstance().getScheduler().runTaskSyncLater(
						() -> onParticle(min, max, defaultLocation), player.getLocation(), delayTicks);
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
			player.spawnParticle(Particle.DUST, x, y, z, 0, 0, 0, 0, 0, new Particle.DustOptions(Color.LIME, 1.5F));
		}
	}

	public class RedBorder implements PlayerBorder {

		@Override
		public void sendBorderDisplay(Player player, double x, double y, double z) {
			player.spawnParticle(Particle.DUST, x, y, z, 0, 0, 0, 0, 0, new Particle.DustOptions(Color.RED, 1.5F));
		}
	}

	public class BlueBorder implements PlayerBorder {

		@Override
		public void sendBorderDisplay(Player player, double x, double y, double z) {
			player.spawnParticle(Particle.DUST, x, y, z, 0, 0, 0, 0, 0, new Particle.DustOptions(Color.BLUE, 1.5F));
		}
	}
}