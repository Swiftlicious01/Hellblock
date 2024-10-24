package com.swiftlicious.hellblock.commands.sub;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.io.Files;
import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.generation.HellBiome;
import com.swiftlicious.hellblock.gui.hellblock.HellblockMenu;
import com.swiftlicious.hellblock.gui.hellblock.IslandChoiceMenu;
import com.swiftlicious.hellblock.playerdata.HellblockPlayer;
import com.swiftlicious.hellblock.playerdata.UUIDFetcher;
import com.swiftlicious.hellblock.utils.LocationUtils;
import com.swiftlicious.hellblock.utils.LogUtils;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.StringArgument;

public class HellblockUserCommand {

	public static HellblockUserCommand INSTANCE = new HellblockUserCommand();

	private static final Cache<UUID, Boolean> visitCache = CacheBuilder.newBuilder().expireAfterWrite(1, TimeUnit.HOURS)
			.build();

	public CommandAPICommand getMenuCommand() {
		return new CommandAPICommand("menu").withPermission(CommandPermission.NONE).withPermission("hellblock.user")
				.executesPlayer((player, args) -> {
					new HellblockMenu(player);
				});
	}

	public CommandAPICommand getResetCommand() {
		return new CommandAPICommand("reset").withAliases("restart").withPermission(CommandPermission.NONE)
				.withPermission("hellblock.user").executesPlayer((player, args) -> {
					HellblockPlayer pi = HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(player);
					if (!pi.hasHellblock()) {
						HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
								"<red>You don't have a hellblock!");
						return;
					}
					if (pi.getHellblockOwner() != null && !pi.getHellblockOwner().equals(player.getUniqueId())) {
						HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
								"<red>Only the owner of the hellblock island can change this!");
						return;
					}
					if (pi.getResetCooldown() > 0) {
						HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
								String.format(
										"<red>You have recently rest your hellblock already, you must wait for %s!",
										HellblockPlugin.getInstance().getFormattedCooldown(pi.getResetCooldown())));
						return;
					}

					HellblockPlugin.getInstance().getHellblockHandler().resetHellblock(player.getUniqueId(), false);
					new IslandChoiceMenu(player);
				});
	}

	public CommandAPICommand getCreateCommand() {
		return new CommandAPICommand("create").withPermission(CommandPermission.NONE).withPermission("hellblock.user")
				.executesPlayer((player, args) -> {
					HellblockPlayer pi = HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(player);
					if (!pi.hasHellblock()) {
						new IslandChoiceMenu(player);
					} else {
						HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
								"<red>You already have a hellblock!");
					}
				});
	}

	public CommandAPICommand getLockCommand() {
		return new CommandAPICommand("lock").withAliases("unlock").withPermission(CommandPermission.NONE)
				.withPermission("hellblock.user").executesPlayer((player, args) -> {
					HellblockPlayer pi = HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(player);
					if (pi.hasHellblock()) {
						if (pi.getHellblockOwner() != null && !pi.getHellblockOwner().equals(player.getUniqueId())) {
							HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
									"<red>Only the owner of the hellblock island can change this!");
							return;
						}
						pi.setLockedStatus(!pi.getLockedStatus());
						HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
								String.format("<red>You have just <dark_red>%s <red>your hellblock island!",
										(pi.getLockedStatus() ? "unlocked" : "locked")));
						if (pi.getLockedStatus()) {
							HellblockPlugin.getInstance().getCoopManager().kickVisitorsIfLocked(player.getUniqueId());
							HellblockPlugin.getInstance().getCoopManager().changeLockStatus(player);
						}
					} else {
						HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
								"<red>You don't have a hellblock!");
					}
				});
	}

	public CommandAPICommand getBiomeCommand() {
		return new CommandAPICommand("changebiome").withAliases("setbiome", "biome")
				.withPermission(CommandPermission.NONE).withPermission("hellblock.user")
				.withArguments(new StringArgument("biome").replaceSuggestions(
						ArgumentSuggestions.stringCollection(collection -> Arrays.stream(HellBiome.values())
								.map(biome -> biome.getName()).collect(Collectors.toList()))))
				.executesPlayer((player, args) -> {
					HellblockPlayer pi = HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(player);
					if (pi.hasHellblock()) {
						if (pi.getHellblockOwner() != null && !pi.getHellblockOwner().equals(player.getUniqueId())) {
							HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
									"<red>Only the owner of the hellblock island can change this!");
							return;
						}
						HellBiome biome = HellBiome
								.valueOf((String) args.getOrDefault("biome", HellBiome.NETHER_WASTES));
						if (pi.getHellblockBiome() == biome) {
							HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
									String.format("<red>Your hellblock biome is already set to <dark_red>%s<red>!",
											biome.getName()));
							return;
						}
						HellblockPlugin.getInstance().getBiomeHandler().changeHellblockBiome(pi, biome, false);
					} else {
						HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
								"<red>You don't have a hellblock!");
					}
				});
	}

	public CommandAPICommand getVisitCommand() {
		return new CommandAPICommand("visit").withAliases("warp").withPermission(CommandPermission.NONE)
				.withPermission("hellblock.user")
				.withArguments(new StringArgument("player")
						.replaceSuggestions(ArgumentSuggestions.stringCollection(collection -> HellblockPlugin
								.getInstance().getHellblockHandler().getActivePlayers().values().stream()
								.filter(hbPlayer -> hbPlayer.getPlayer() != null && !hbPlayer.getLockedStatus())
								.map(hbPlayer -> hbPlayer.getPlayer().getName()).collect(Collectors.toList()))))
				.executesPlayer((player, args) -> {
					String user = (String) args.getOrDefault("player", player);
					UUID id = UUIDFetcher.getUUID(user);
					if (id == null) {
						HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
								"<red>The player's hellblock you're trying to visit doesn't exist!");
						return;
					}
					HellblockPlayer ti = null;
					if (HellblockPlugin.getInstance().getHellblockHandler().getActivePlayers().containsKey(id)) {
						ti = HellblockPlugin.getInstance().getHellblockHandler().getActivePlayers().get(id);
					} else {
						ti = new HellblockPlayer(id);
					}

					if (ti.hasHellblock()) {
						if (!ti.getLockedStatus()
								&& HellblockPlugin.getInstance().getCoopManager().checkIfVisitorIsWelcome(player, id)) {
							player.teleportAsync(ti.getHomeLocation());
							// if raining give player a bit of protection
							if (HellblockPlugin.getInstance().getLavaRain().getLavaRainTask() != null
									&& HellblockPlugin.getInstance().getLavaRain().getLavaRainTask().isLavaRaining()
									&& HellblockPlugin.getInstance().getLavaRain()
											.getHighestBlock(player.getLocation()) != null
									&& !HellblockPlugin.getInstance().getLavaRain()
											.getHighestBlock(player.getLocation()).isEmpty()) {
								player.setNoDamageTicks(5 * 20);
							}
							if (visitCache.getIfPresent(player.getUniqueId()) != null
									&& !visitCache.getIfPresent(player.getUniqueId()).booleanValue()) {
								if (!(ti.getHellblockOwner().equals(player.getUniqueId())
										|| ti.getHellblockParty().contains(player.getUniqueId())
										|| ti.getWhoTrusted().contains(player.getUniqueId()))) {
									ti.addTotalVisit();
									visitCache.put(player.getUniqueId(), true);
								}
							}
							HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
									String.format("<red>You are visiting <dark_red>%s<red>'s hellblock!", user));
							return;
						} else {
							HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
									String.format(
											"<red>The player <dark_red>%s<red>'s hellblock is currently locked from having visitors!",
											user));
							return;
						}
					}

					HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
							"<red>That player does not have a hellblock!");
				});
	}

	public CommandAPICommand getHomeCommand() {
		return new CommandAPICommand("home").withPermission(CommandPermission.NONE).withPermission("hellblock.user")
				.executesPlayer((player, args) -> {
					HellblockPlayer pi = HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(player);
					if (!pi.hasHellblock()) {
						HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
								"<red>You don't have a hellblock!");
					} else {
						if (pi.getHomeLocation() != null) {
							HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
									"<red>Teleporting you to your hellblock!");
							player.teleportAsync(pi.getHomeLocation());
							// if raining give player a bit of protection
							if (HellblockPlugin.getInstance().getLavaRain().getLavaRainTask() != null
									&& HellblockPlugin.getInstance().getLavaRain().getLavaRainTask().isLavaRaining()
									&& HellblockPlugin.getInstance().getLavaRain()
											.getHighestBlock(player.getLocation()) != null
									&& !HellblockPlugin.getInstance().getLavaRain()
											.getHighestBlock(player.getLocation()).isEmpty()) {
								player.setNoDamageTicks(5 * 20);
							}
						} else {
							HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
									"<red>Error teleporting you to your hellblock!");
						}
					}
				});
	}

	public CommandAPICommand getInfoCommand() {
		return new CommandAPICommand("info").withAliases("information", "data").withPermission(CommandPermission.NONE)
				.withPermission("hellblock.user").executesPlayer((player, args) -> {
					HellblockPlayer pi = HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(player);
					if (!pi.hasHellblock()) {
						HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
								"<red>You don't have a hellblock!");
					} else {
						String partyString = "", trustedString = "";
						for (UUID id : pi.getHellblockParty()) {
							if (Bukkit.getOfflinePlayer(id).hasPlayedBefore()
									&& Bukkit.getOfflinePlayer(id).getName() != null) {
								partyString = "<dark_red>" + Bukkit.getOfflinePlayer(id).getName() + "<red>, ";
							}
						}
						for (UUID id : pi.getWhoTrusted()) {
							if (Bukkit.getOfflinePlayer(id).hasPlayedBefore()
									&& Bukkit.getOfflinePlayer(id).getName() != null) {
								trustedString = "<dark_red>" + Bukkit.getOfflinePlayer(id).getName() + "<red>, ";
							}
						}
						HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
								String.format("<dark_red>Hellblock Information (ID: <red>%s<dark_red):", pi.getID()));
						HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
								"<red>Owner: <dark_red>" + (pi.getHellblockOwner() != null
										&& Bukkit.getOfflinePlayer(pi.getHellblockOwner()).hasPlayedBefore()
										&& Bukkit.getOfflinePlayer(pi.getHellblockOwner()).getName() != null
												? Bukkit.getOfflinePlayer(pi.getHellblockOwner()).getName()
												: "Unknown"));
						HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
								"<red>Creation Date: <dark_red>" + pi.getCreationTime());
						HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
								"<red>Visitor Status: <dark_red>" + (pi.getLockedStatus() ? "Closed" : "Open"));
						HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
								"<red>Total Visits: <dark_red>" + pi.getTotalVisitors());
						HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
								"<red>Island Type: <dark_red>" + pi.getIslandChoice().name());
						HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
								"<red>Biome: <dark_red>" + pi.getHellblockBiome().getName());
						HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
								"<red>Party Size: <dark_red>" + pi.getHellblockParty().size() + " <red>/<dark_red> "
										+ HellblockPlugin.getInstance().getCoopManager().getPartySizeLimit());
						HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
								"<red>Party Members: <dark_red>"
										+ (!partyString.isEmpty() ? partyString.substring(0, partyString.length() - 2)
												: "None"));
						HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
								"<red>Trusted Members: <dark_red>" + (!trustedString.isEmpty()
										? trustedString.substring(0, trustedString.length() - 2)
										: "None"));
					}
				});
	}

	public CommandAPICommand getSetHomeCommand() {
		return new CommandAPICommand("sethome").withPermission(CommandPermission.NONE).withPermission("hellblock.user")
				.executesPlayer((player, args) -> {
					HellblockPlayer pi = HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(player);
					if (!pi.hasHellblock()) {
						HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
								"<red>You don't have a hellblock!");
					} else {
						if (pi.getHomeLocation() != null) {
							if (pi.getHellblockOwner() != null
									&& !pi.getHellblockOwner().equals(player.getUniqueId())) {
								HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
										"<red>Only the owner of the hellblock island can change this!");
								return;
							}
							if (!LocationUtils.isSafeLocation(player.getLocation())) {
								HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
										"<red>The location you're standing at is not safe for a new home!");
								return;
							}
							if (pi.getHomeLocation().equals(player.getLocation())) {
								HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
										"<red>The location you're standing at is already set as your home!");
								return;
							}
							pi.setHome(player.getLocation());
							pi.saveHellblockPlayer();
							for (HellblockPlayer active : HellblockPlugin.getInstance().getHellblockHandler()
									.getActivePlayers().values()) {
								if (active == null || active.getPlayer() == null)
									continue;
								if (active.getHellblockOwner().equals(player.getUniqueId())) {
									active.setHome(pi.getHomeLocation());
								}
								active.saveHellblockPlayer();
							}
							for (File offline : HellblockPlugin.getInstance().getHellblockHandler()
									.getPlayersDirectory().listFiles()) {
								if (!offline.isFile() || !offline.getName().endsWith(".yml"))
									continue;
								String uuid = Files.getNameWithoutExtension(offline.getName());
								UUID id = null;
								try {
									id = UUID.fromString(uuid);
								} catch (IllegalArgumentException ignored) {
									// ignored
									continue;
								}
								if (id != null && HellblockPlugin.getInstance().getHellblockHandler().getActivePlayers()
										.keySet().contains(id))
									continue;
								YamlConfiguration offlineFile = YamlConfiguration.loadConfiguration(offline);
								Location location = pi.getHomeLocation();
								String world = location.getWorld().getName();
								double x = location.getX();
								double y = location.getY();
								double z = location.getZ();
								float yaw = location.getYaw();

								if (!(offlineFile.getString("player.home.world").equalsIgnoreCase(world)
										|| offlineFile.getDouble("player.home.x") == x
										|| offlineFile.getDouble("player.home.y") == y
										|| offlineFile.getDouble("player.home.z") == z
										|| (float) offlineFile.getDouble("player.home.yaw") == yaw)) {
									offlineFile.set("player.home.world", world);
									offlineFile.set("player.home.x", x);
									offlineFile.set("player.home.y", y);
									offlineFile.set("player.home.z", z);
									offlineFile.set("player.home.yaw", yaw);
								}
								try {
									offlineFile.save(offline);
								} catch (IOException ex) {
									LogUtils.warn(String.format("Could not save the offline data file for %s", uuid),
											ex);
									continue;
								}
							}
							HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
									String.format(
											"<red>You have set your new hellblock home location to x:%s, y:%s, z:%s facing %s!",
											player.getLocation().getX(), player.getLocation().getY(),
											player.getLocation().getZ(), LocationUtils.getFacing(player)));
						} else {
							HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
									"<red>Error setting your home location!");
						}
					}
				});
	}
}
