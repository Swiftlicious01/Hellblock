package com.swiftlicious.hellblock.listeners;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.PortalType;
import org.bukkit.Tag;
import org.bukkit.World.Environment;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Firework;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityPortalEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerRespawnEvent.RespawnReason;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.event.world.PortalCreateEvent;
import org.bukkit.event.world.PortalCreateEvent.CreateReason;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.gui.hellblock.IslandChoiceMenu;
import com.swiftlicious.hellblock.playerdata.HellblockPlayer;
import com.swiftlicious.hellblock.playerdata.UUIDFetcher;
import com.swiftlicious.hellblock.scheduler.CancellableTask;
import com.swiftlicious.hellblock.utils.ChunkUtils;
import com.swiftlicious.hellblock.utils.LocationUtils;
import com.swiftlicious.hellblock.utils.LogUtils;

import io.papermc.paper.event.player.AsyncChatEvent;
import io.papermc.paper.event.player.PlayerBedFailEnterEvent;
import lombok.NonNull;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

public class PlayerListener implements Listener {

	private final HellblockPlugin instance;

	private final Map<UUID, CancellableTask> cancellablePortal;

	private final Set<UUID> linkPortalCatcher;

	public PlayerListener(HellblockPlugin plugin) {
		instance = plugin;
		this.cancellablePortal = new HashMap<>();
		this.linkPortalCatcher = new HashSet<>();
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

		instance.getIslandLevelManager().loadCache(id);
		instance.getNetherrackGenerator().loadPistons(id);

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

			File playerFile = pi.getPlayerFile();
			YamlConfiguration playerConfig = pi.getHellblockPlayer();
			if (playerConfig.getBoolean("player.in-unsafe-island", false)) {
				playerConfig.set("player.in-unsafe-island", null);
				instance.getAdventureManager().sendMessageWithPrefix(player,
						"<red>You logged out in an unsafe hellblock environment because it was reset or deleted.");
				try {
					playerConfig.save(playerFile);
				} catch (IOException ex) {
					LogUtils.severe(String.format("Unable to save player file for %s!", player.getName()), ex);
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

		instance.getIslandLevelManager().saveCache(id);
		instance.getNetherrackGenerator().savePistons(id);

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
		if (this.linkPortalCatcher.contains(id))
			this.linkPortalCatcher.remove(id);
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
	public void onFireworkDamage(EntityDamageByEntityEvent event) {
		if (event.getEntity() instanceof Player player) {
			if (!player.getWorld().getName().equalsIgnoreCase(instance.getHellblockHandler().getWorldName()))
				return;
			if (event.getDamager() instanceof Firework firework) {
				if (firework.getFireworkMeta().getPersistentDataContainer()
						.has(new NamespacedKey(instance, "challenge-firework"), PersistentDataType.BOOLEAN)) {
					event.setCancelled(true);
				}
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
					if (pi.hasLinkedHellblock()) {
						HellblockPlayer ti = null;
						if (instance.getHellblockHandler().getActivePlayers().get(pi.getLinkedHellblock()) != null) {
							ti = instance.getHellblockHandler().getActivePlayers().get(pi.getLinkedHellblock());
						} else {
							ti = new HellblockPlayer(pi.getLinkedHellblock());
						}
						if (!LocationUtils.isSafeLocation(ti.getHomeLocation())) {
							instance.getAdventureManager().sendMessageWithPrefix(player,
									"<red>This hellblock home location was deemed not safe, teleporting to your hellblock instead!");
							if (!LocationUtils.isSafeLocation(pi.getHomeLocation())) {
								instance.getAdventureManager().sendMessageWithPrefix(player,
										"<red>This hellblock home location was deemed not safe, resetting to bedrock location!");
								pi.setHome(instance.getHellblockHandler().locateBedrock(player.getUniqueId()));
								instance.getCoopManager().updateParty(player.getUniqueId(), "home",
										pi.getHomeLocation());
							}
							ChunkUtils.teleportAsync(player, pi.getHomeLocation(), TeleportCause.PLUGIN);
							return;
						}
						ChunkUtils.teleportAsync(player, ti.getHomeLocation(), TeleportCause.PLUGIN);
					} else {
						if (!LocationUtils.isSafeLocation(pi.getHomeLocation())) {
							instance.getAdventureManager().sendMessageWithPrefix(player,
									"<red>This hellblock home location was deemed not safe, resetting to bedrock location!");
							pi.setHome(instance.getHellblockHandler().locateBedrock(player.getUniqueId()));
							instance.getCoopManager().updateParty(player.getUniqueId(), "home", pi.getHomeLocation());
						}
						ChunkUtils.teleportAsync(player, pi.getHomeLocation(), TeleportCause.PLUGIN);
					}
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
	public void onLinkPortal(PortalCreateEvent event) {
		if (!event.getWorld().getName().equalsIgnoreCase(instance.getHellblockHandler().getWorldName()))
			return;
		if (event.getWorld().getEnvironment() != Environment.NETHER)
			return;
		if (event.getReason() != CreateReason.FIRE)
			return;
		if (event.getEntity() != null && event.getEntity() instanceof Player player) {
			final UUID id = player.getUniqueId();
			HellblockPlayer pi = instance.getHellblockHandler().getActivePlayer(id);
			if (pi.hasHellblock()) {
				if (!this.linkPortalCatcher.contains(id)) {
					instance.getAdventureManager().sendMessageWithPrefix(player,
							"<red>Link your home nether portal to another player's hellblock if you wish or keep it as your own!");
					instance.getAdventureManager().sendMessage(player,
							"<red>You can always change this just by clicking on your nether portal!");
					String owner = pi.getLinkedHellblock() != null ? (Bukkit.getPlayer(pi.getLinkedHellblock()) != null
							? Bukkit.getPlayer(pi.getLinkedHellblock()).getName()
							: Bukkit.getOfflinePlayer(pi.getLinkedHellblock()).getName()) : null;
					instance.getAdventureManager().sendMessage(player,
							"<red>Current Linked Hellblock: " + (owner != null ? owner : "None"));
					instance.getAdventureManager().sendMessage(player,
							"<red>Type the player's username of the hellblock you wish to link (Type none for yourself):");
					this.linkPortalCatcher.add(id);
				}
			}
		}
	}

	@EventHandler
	public void onLinkPortal(PlayerInteractEvent event) {
		final Player player = event.getPlayer();
		if (!player.getWorld().getName().equalsIgnoreCase(instance.getHellblockHandler().getWorldName()))
			return;
		if (player.getWorld().getEnvironment() != Environment.NETHER)
			return;
		Block block = event.getClickedBlock();
		if (block != null && block.getType() == Material.NETHER_PORTAL) {
			final UUID id = player.getUniqueId();
			HellblockPlayer pi = instance.getHellblockHandler().getActivePlayer(id);
			if (pi.hasHellblock()) {
				if (!this.linkPortalCatcher.contains(id)) {
					instance.getAdventureManager().sendMessageWithPrefix(player,
							"<red>Link your home nether portal to another player's hellblock if you wish or keep it as your own!");
					instance.getAdventureManager().sendMessage(player,
							"<red>You can always change this just by clicking on your nether portal!");
					String owner = pi.getLinkedHellblock() != null ? (Bukkit.getPlayer(pi.getLinkedHellblock()) != null
							? Bukkit.getPlayer(pi.getLinkedHellblock()).getName()
							: Bukkit.getOfflinePlayer(pi.getLinkedHellblock()).getName()) : null;
					instance.getAdventureManager().sendMessage(player,
							"<red>Current Linked Hellblock: " + (owner != null ? owner : "None"));
					instance.getAdventureManager().sendMessage(player,
							"<red>Type the player's username of the hellblock you wish to link (Type none for yourself):");
					this.linkPortalCatcher.add(id);
				}
			}
		}
	}

	@EventHandler
	public void onLinkPortalChat(AsyncChatEvent event) {
		final Player player = event.getPlayer();
		final UUID id = player.getUniqueId();
		if (this.linkPortalCatcher.contains(id)) {
			HellblockPlayer pi = instance.getHellblockHandler().getActivePlayer(id);
			event.setCancelled(true);
			String username = PlainTextComponentSerializer.plainText().serialize(event.message());
			if (username.equalsIgnoreCase("none") || username.equalsIgnoreCase(player.getName())) {
				instance.getAdventureManager().sendMessageWithPrefix(player,
						"<red>The portal has kept it's link to your own hellblock!");
				this.linkPortalCatcher.remove(id);
				return;
			}
			Player link = Bukkit.getPlayer(username);
			if (link != null) {
				if ((pi.getHellblockOwner() != null && pi.getHellblockOwner().equals(link.getUniqueId()))
						|| pi.getHellblockParty().contains(link.getUniqueId())) {
					instance.getAdventureManager().sendMessageWithPrefix(player,
							"<red>The player you typed is a part of your hellblock already.");
					instance.getAdventureManager().sendMessageWithPrefix(player,
							"<red>The portal has kept it's link to your own hellblock!");
					this.linkPortalCatcher.remove(id);
					return;
				}
				HellblockPlayer ti = null;
				if (instance.getHellblockHandler().getActivePlayers().get(link.getUniqueId()) != null) {
					ti = instance.getHellblockHandler().getActivePlayers().get(link.getUniqueId());
				} else {
					ti = new HellblockPlayer(link.getUniqueId());
				}
				if (!ti.hasHellblock()) {
					instance.getAdventureManager().sendMessageWithPrefix(player,
							"<red>The player you typed in doesn't have a hellblock to link to!");
					return;
				}
				pi.setLinkedHellblock(link.getUniqueId());
				instance.getAdventureManager().sendMessageWithPrefix(player, String
						.format("<red>You have linked your nether portal to <dark_red>%s<red>'s hellblock!", username));
			} else {
				UUID linkID = UUIDFetcher.getUUID(username);
				if (linkID == null) {
					instance.getAdventureManager().sendMessageWithPrefix(player,
							"<red>The player you typed in doesn't exist!");
					return;
				}
				if (!Bukkit.getOfflinePlayer(linkID).hasPlayedBefore()) {
					instance.getAdventureManager().sendMessageWithPrefix(player,
							"<red>The player you typed in doesn't exist!");
					return;
				}
				if ((pi.getHellblockOwner() != null && pi.getHellblockOwner().equals(linkID))
						|| pi.getHellblockParty().contains(linkID)) {
					instance.getAdventureManager().sendMessageWithPrefix(player,
							"<red>The player you typed is a part of your hellblock already.");
					instance.getAdventureManager().sendMessageWithPrefix(player,
							"<red>The portal has kept it's link to your own hellblock!");
					this.linkPortalCatcher.remove(id);
					return;
				}
				HellblockPlayer ti = null;
				if (instance.getHellblockHandler().getActivePlayers().get(linkID) != null) {
					ti = instance.getHellblockHandler().getActivePlayers().get(linkID);
				} else {
					ti = new HellblockPlayer(linkID);
				}
				if (!ti.hasHellblock()) {
					instance.getAdventureManager().sendMessageWithPrefix(player,
							"<red>The player you typed in doesn't have a hellblock to link to!");
					return;
				}
				pi.setLinkedHellblock(linkID);
				instance.getAdventureManager().sendMessageWithPrefix(player, String
						.format("<red>You have linked your nether portal to <dark_red>%s<red>'s hellblock!", username));
			}
			this.linkPortalCatcher.remove(id);
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

	public boolean playerInPortal(@NonNull Player player) {
		try {
			Material portal = player.getTargetBlock(null, 1).getType();
			return portal == Material.NETHER_PORTAL && player.getLocation() != null
					&& player.getLocation().getBlock().getRelative(BlockFace.DOWN).getType() == Material.NETHER_PORTAL;
		} catch (IllegalStateException ex) {
			// ignored.. some weird block iterator error but can't be fixed.
			return false;
		}
	}
}