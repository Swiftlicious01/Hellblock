package com.swiftlicious.hellblock.listeners;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.PortalType;
import org.bukkit.Tag;
import org.bukkit.World.Environment;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPortalEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerRespawnEvent.RespawnReason;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.gui.hellblock.IslandChoiceMenu;
import com.swiftlicious.hellblock.playerdata.HellblockPlayer;
import com.swiftlicious.hellblock.scheduler.CancellableTask;
import com.swiftlicious.hellblock.utils.ChunkUtils;
import com.swiftlicious.hellblock.utils.LocationUtils;
import com.swiftlicious.hellblock.utils.LogUtils;

import io.papermc.paper.event.player.PlayerBedFailEnterEvent;

public class PlayerListener implements Listener {

	private final HellblockPlugin instance;

	private final Map<UUID, CancellableTask> cancellablePortal;

	public PlayerListener(HellblockPlugin plugin) {
		instance = plugin;
		this.cancellablePortal = new HashMap<>();
		Bukkit.getPluginManager().registerEvents(this, instance);
	}

	@EventHandler
	public void onJoin(PlayerJoinEvent event) {
		final Player player = event.getPlayer();
		final UUID id = player.getUniqueId();
		instance.debug(String.format("Loading player info for %s!", player.getName()));

		HellblockPlayer pi = new HellblockPlayer(id);
		instance.getHellblockHandler().addActivePlayer(player, pi);
		instance.getNetherFarming().trackNetherFarms(pi);
		if (player.hasPermission("hellblock.updates") && instance.isUpdateAvailable()) {
			instance.getAdventureManager().sendMessageWithPrefix(player,
					"<red>There is a new update available!: <dark_red><u>https://github.com/Swiftlicious01/Hellblock<!u>");
		}

		if (pi.isAbandoned()) {
			HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
					String.format("<red>Your hellblock was deemed abandoned for not logging in for the past %s days!",
							instance.getConfig("config.yml").getInt("hellblock.abandon-after-days")));
			HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
					"<red>You've lost access to your island, if you wish to recover it speak to an administrator.");
		}

		instance.getScheduler().runTaskSyncLater(() -> {
			if (!player.getWorld().getName().equalsIgnoreCase(instance.getHellblockHandler().getWorldName()))
				return;

			if (player.getLocation() != null && !LocationUtils.isSafeLocation(player.getLocation())) {
				if (pi.hasHellblock() && pi.getHomeLocation() != null) {
					if (!LocationUtils.isSafeLocation(pi.getHomeLocation())) {
						HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
								"<red>This hellblock home location was deemed not safe, resetting to bedrock location!");
						pi.setHome(HellblockPlugin.getInstance().getHellblockHandler()
								.locateBedrock(player.getUniqueId()));
						HellblockPlugin.getInstance().getCoopManager().updateParty(player.getUniqueId(), "home",
								pi.getHomeLocation());
					}
					ChunkUtils.teleportAsync(player, pi.getHomeLocation(), TeleportCause.PLUGIN);
				} else {
					player.performCommand(instance.getHellblockHandler().getNetherCMD());
				}
			}

			if (instance.getCoopManager().getHellblockOwnerOfVisitingIsland(player) != null) {
				instance.getCoopManager()
						.kickVisitorsIfLocked(instance.getCoopManager().getHellblockOwnerOfVisitingIsland(player));
			}

			if (instance.getCoopManager().trackBannedPlayer(id)) {
				if (pi.hasHellblock()) {
					if (!LocationUtils.isSafeLocation(pi.getHomeLocation())) {
						HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
								"<red>This hellblock home location was deemed not safe, resetting to bedrock location!");
						pi.setHome(HellblockPlugin.getInstance().getHellblockHandler()
								.locateBedrock(player.getUniqueId()));
						HellblockPlugin.getInstance().getCoopManager().updateParty(player.getUniqueId(), "home",
								pi.getHomeLocation());
					}
					ChunkUtils.teleportAsync(player, pi.getHomeLocation(), TeleportCause.PLUGIN);
				} else {
					player.performCommand(instance.getHellblockHandler().getNetherCMD());
				}
			}

			// if raining give player a bit of protection
			if (instance.getLavaRain().getLavaRainTask() != null
					&& instance.getLavaRain().getLavaRainTask().isLavaRaining()
					&& instance.getLavaRain().getHighestBlock(player.getLocation()) != null
					&& !instance.getLavaRain().getHighestBlock(player.getLocation()).isEmpty()) {
				player.setNoDamageTicks(5 * 20);
			}

			File playerFile = new File(
					instance.getHellblockHandler().getPlayersDirectory() + File.separator + id + ".yml");
			YamlConfiguration playerConfig = YamlConfiguration.loadConfiguration(playerFile);
			if (playerConfig.getBoolean("player.in-unsafe-island")) {
				playerConfig.set("player.in-unsafe-island", null);
				instance.getAdventureManager().sendMessageWithPrefix(player,
						"<red>You logged out in an unsafe hellblock environment because it was reset or deleted.");
				try {
					playerConfig.save(playerFile);
				} catch (IOException ex) {
					LogUtils.severe(String.format("Unable to save player file for %s!", id), ex);
				}
			}

			if (instance.getNetherArmor().gsNightVisionArmor && instance.getNetherArmor().gsArmor) {
				ItemStack[] armorSet = player.getInventory().getArmorContents();
				boolean checkArmor = false;
				if (armorSet != null) {
					for (ItemStack item : armorSet) {
						if (item == null || item.getType() == Material.AIR)
							continue;
						if (instance.getNetherArmor().checkNightVisionArmorStatus(item)
								&& instance.getNetherArmor().getNightVisionArmorStatus(item)) {
							checkArmor = true;
							break;
						}
					}

					if (checkArmor) {
						player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 1));
						instance.getHellblockHandler().getActivePlayer(player).isWearingGlowstoneArmor(true);
					}
				}
			}

			if (instance.getNetherTools().gsNightVisionTool && instance.getNetherTools().gsTools) {
				ItemStack tool = player.getInventory().getItemInMainHand();
				if (tool.getType() == Material.AIR) {
					tool = player.getInventory().getItemInOffHand();
					if (tool.getType() == Material.AIR) {
						return;
					}
				}

				if (instance.getNetherTools().checkNightVisionToolStatus(tool)
						&& instance.getNetherTools().getNightVisionToolStatus(tool)) {
					player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 1));
					instance.getHellblockHandler().getActivePlayer(player).isHoldingGlowstoneTool(true);
				}
			}
		}, player.getLocation(), 5, TimeUnit.MILLISECONDS);
	}

	@EventHandler
	public void onQuit(PlayerQuitEvent event) {
		final Player player = event.getPlayer();
		final UUID id = player.getUniqueId();
		instance.debug(String.format("Saving player info for %s!", player.getName()));

		if (instance.getHellblockHandler().getActivePlayer(player).hasGlowstoneToolEffect()
				|| instance.getHellblockHandler().getActivePlayer(player).hasGlowstoneArmorEffect()) {
			if (player.hasPotionEffect(PotionEffectType.NIGHT_VISION)) {
				player.removePotionEffect(PotionEffectType.NIGHT_VISION);
				instance.getHellblockHandler().getActivePlayer(player).isHoldingGlowstoneTool(false);
				instance.getHellblockHandler().getActivePlayer(player).isWearingGlowstoneArmor(false);
			}
		}
		if (this.cancellablePortal.containsKey(id) && this.cancellablePortal.get(id) != null) {
			if (!this.cancellablePortal.get(id).isCancelled())
				this.cancellablePortal.get(id).cancel();
			this.cancellablePortal.remove(id);
		}
		// Cleanup
		instance.getNetherrackGenerator().getGenManager().cleanupExpiredPistons(id);
		instance.getNetherrackGenerator().getGenManager().cleanupExpiredLocations();
		instance.getHellblockHandler().removeActivePlayer(player);
	}

	@EventHandler
	public void onBedClick(PlayerBedFailEnterEvent event) {
		if (!instance.getHellblockHandler().isDisableBedExplosions())
			return;
		final Player player = event.getPlayer();
		if (!player.getWorld().getName().equalsIgnoreCase(instance.getHellblockHandler().getWorldName()))
			return;

		Block bed = event.getBed();
		if (bed != null && Tag.BEDS.isTagged(bed.getType())) {
			if (bed.getWorld().getEnvironment() == Environment.NETHER) {
				event.setWillExplode(false);
			}
		}
	}

	@EventHandler
	public void onPortal(PlayerPortalEvent event) {
		final Player player = event.getPlayer();
		if (player.getWorld().getEnvironment() == Environment.NETHER
				&& event.getCause() == TeleportCause.NETHER_PORTAL) {
			event.setCancelled(true);
			HellblockPlayer pi = instance.getHellblockHandler().getActivePlayer(player);
			CancellableTask portalTask = instance.getScheduler().runTaskSyncLater(() -> {
				if (pi.hasHellblock()) {
					if (!LocationUtils.isSafeLocation(pi.getHomeLocation())) {
						instance.getAdventureManager().sendMessageWithPrefix(player,
								"<red>This hellblock home location was deemed not safe, resetting to bedrock location!");
						pi.setHome(instance.getHellblockHandler().locateBedrock(player.getUniqueId()));
						instance.getCoopManager().updateParty(player.getUniqueId(), "home", pi.getHomeLocation());
					}
					ChunkUtils.teleportAsync(player, pi.getHomeLocation(), TeleportCause.PLUGIN);
					// if raining give player a bit of protection
					if (instance.getLavaRain().getLavaRainTask() != null
							&& instance.getLavaRain().getLavaRainTask().isLavaRaining()
							&& instance.getLavaRain().getHighestBlock(player.getLocation()) != null
							&& !instance.getLavaRain().getHighestBlock(player.getLocation()).isEmpty()) {
						player.setNoDamageTicks(5 * 20);
					}
				} else {
					new IslandChoiceMenu(player, false);
				}
			}, pi.getHomeLocation(), 5, TimeUnit.SECONDS);
			this.cancellablePortal.putIfAbsent(player.getUniqueId(), portalTask);
		}
	}

	@EventHandler
	public void onChangeWorld(PlayerChangedWorldEvent event) {
		if (event.getFrom().getName().equals(instance.getHellblockHandler().getWorldName())) {
			final Player player = event.getPlayer();
			if (instance.getHellblockHandler().getActivePlayer(player).hasGlowstoneToolEffect()
					|| instance.getHellblockHandler().getActivePlayer(player).hasGlowstoneArmorEffect()) {
				if (player.hasPotionEffect(PotionEffectType.NIGHT_VISION)) {
					player.removePotionEffect(PotionEffectType.NIGHT_VISION);
					instance.getHellblockHandler().getActivePlayer(player).isHoldingGlowstoneTool(false);
					instance.getHellblockHandler().getActivePlayer(player).isWearingGlowstoneArmor(false);
				}
			}
		}
	}

	@EventHandler
	public void onMoveIfInPortal(PlayerMoveEvent event) {
		final Player player = event.getPlayer();
		if (!player.getWorld().getName().equalsIgnoreCase(instance.getHellblockHandler().getWorldName()))
			return;
		if (playerInPortal(player))
			return;
		final UUID id = player.getUniqueId();
		if (this.cancellablePortal.containsKey(id) && this.cancellablePortal.get(id) != null) {
			if (!this.cancellablePortal.get(id).isCancelled())
				this.cancellablePortal.get(id).cancel();
			this.cancellablePortal.remove(id);
		}
	}

	@EventHandler
	public void onEntityPortal(EntityPortalEvent event) {
		if (event.getEntity() instanceof LivingEntity entity) {
			if (entity.getWorld().getEnvironment() == Environment.NETHER
					&& event.getPortalType() == PortalType.NETHER) {
				event.setCancelled(true);
			}
		}
	}

	@EventHandler
	public void onRespawn(PlayerRespawnEvent event) {
		final Player player = event.getPlayer();
		if (player.getPotentialBedLocation() != null
				&& player.getPotentialBedLocation().getBlock().getType() == Material.RESPAWN_ANCHOR)
			return;
		if (event.getRespawnReason() == RespawnReason.DEATH) {
			HellblockPlayer pi = instance.getHellblockHandler().getActivePlayer(player);
			if (pi.hasHellblock()) {
				if (!LocationUtils.isSafeLocation(pi.getHomeLocation())) {
					instance.getAdventureManager().sendMessageWithPrefix(player,
							"<red>This hellblock home location was deemed not safe, resetting to bedrock location!");
					pi.setHome(instance.getHellblockHandler().locateBedrock(player.getUniqueId()));
					instance.getCoopManager().updateParty(player.getUniqueId(), "home", pi.getHomeLocation());
				}
				event.setRespawnLocation(pi.getHomeLocation());
			}
		}
	}

	@EventHandler
	public void onFallInVoid(PlayerMoveEvent event) {
		if (!instance.getHellblockHandler().isVoidTeleport())
			return;
		final Player player = event.getPlayer();
		if (!player.getWorld().getName().equalsIgnoreCase(instance.getHellblockHandler().getWorldName()))
			return;
		final UUID id = player.getUniqueId();
		if (player.getLocation().getY() <= 0) {
			HellblockPlayer pi = instance.getHellblockHandler().getActivePlayer(id);
			if (pi.hasHellblock()) {
				if (!LocationUtils.isSafeLocation(pi.getHomeLocation())) {
					instance.getAdventureManager().sendMessageWithPrefix(player,
							"<red>This hellblock home location was deemed not safe, resetting to bedrock location!");
					pi.setHome(instance.getHellblockHandler().locateBedrock(player.getUniqueId()));
					instance.getCoopManager().updateParty(player.getUniqueId(), "home", pi.getHomeLocation());
				}
				ChunkUtils.teleportAsync(player, pi.getHomeLocation(), TeleportCause.PLUGIN);
			} else {
				player.performCommand(instance.getHellblockHandler().getNetherCMD());
			}
		}
	}

	@EventHandler
	public void onDeath(PlayerDeathEvent event) {
		final Player player = event.getPlayer();
		if (!player.getWorld().getName().equalsIgnoreCase(instance.getHellblockHandler().getWorldName()))
			return;
		if (!event.getKeepInventory()) {
			if (instance.getHellblockHandler().getActivePlayer(player).hasGlowstoneToolEffect()
					|| instance.getHellblockHandler().getActivePlayer(player).hasGlowstoneArmorEffect()) {
				if (player.hasPotionEffect(PotionEffectType.NIGHT_VISION)) {
					player.removePotionEffect(PotionEffectType.NIGHT_VISION);
					instance.getHellblockHandler().getActivePlayer(player).isHoldingGlowstoneTool(false);
					instance.getHellblockHandler().getActivePlayer(player).isWearingGlowstoneArmor(false);
				}
			}
		}
	}

	public boolean playerInPortal(Player player) {
		try {
			Material portal = player.getTargetBlock(null, 1).getType();
			return portal == Material.NETHER_PORTAL;
		} catch (IllegalStateException ex) {
			// ignored.. some weird block iterator error but can't be fixed.
			return false;
		}
	}
}