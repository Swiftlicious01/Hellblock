package com.swiftlicious.hellblock.commands.sub;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.io.Files;
import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.generation.HellBiome;
import com.swiftlicious.hellblock.gui.hellblock.HellblockMenu;
import com.swiftlicious.hellblock.gui.hellblock.IslandChoiceMenu;
import com.swiftlicious.hellblock.playerdata.HellblockPlayer;
import com.swiftlicious.hellblock.playerdata.UUIDFetcher;
import com.swiftlicious.hellblock.utils.ChunkUtils;
import com.swiftlicious.hellblock.utils.LocationUtils;
import com.swiftlicious.hellblock.utils.LogUtils;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.StringArgument;

public class HellblockUserCommand {

	public static HellblockUserCommand INSTANCE = new HellblockUserCommand();

	private static final Cache<UUID, Boolean> visitCache = CacheBuilder.newBuilder().expireAfterWrite(1, TimeUnit.HOURS)
			.build();

	public CommandAPICommand getMenuCommand() {
		return new CommandAPICommand("menu").withPermission(CommandPermission.NONE).withPermission("hellblock.user")
				.executesPlayer((player, args) -> {
					HellblockPlayer pi = HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(player);
					if (pi.isAbandoned()) {
						HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
								"<red>This hellblock is abandoned, you can't do anything until it's recovered!");
						return;
					}
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
					if (pi.isAbandoned()) {
						HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
								"<red>This hellblock is abandoned, you can't do anything until it's recovered!");
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
										"<red>You have recently reset your hellblock already, you must wait for %s!",
										HellblockPlugin.getInstance().getFormattedCooldown(pi.getResetCooldown())));
						return;
					}

					HellblockPlugin.getInstance().getHellblockHandler().resetHellblock(player.getUniqueId(), false);
					player.performCommand(HellblockPlugin.getInstance().getHellblockHandler().getNetherCMD());
					new IslandChoiceMenu(player, true);
				});
	}

	public CommandAPICommand getCreateCommand() {
		return new CommandAPICommand("create").withPermission(CommandPermission.NONE).withPermission("hellblock.user")
				.executesPlayer((player, args) -> {
					HellblockPlayer pi = HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(player);
					if (!pi.hasHellblock()) {
						if (pi.getResetCooldown() > 0) {
							HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
									String.format(
											"<red>You have recently reset your hellblock already, you must wait for %s!",
											HellblockPlugin.getInstance().getFormattedCooldown(pi.getResetCooldown())));
							return;
						}
						new IslandChoiceMenu(player, false);
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
						if (pi.isAbandoned()) {
							HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
									"<red>This hellblock is abandoned, you can't do anything until it's recovered!");
							return;
						}
						if (pi.getHellblockOwner() != null && !pi.getHellblockOwner().equals(player.getUniqueId())) {
							HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
									"<red>Only the owner of the hellblock island can change this!");
							return;
						}
						pi.setLockedStatus(!pi.getLockedStatus());
						HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
								String.format("<red>You have just <dark_red>%s <red>your hellblock island!",
										(pi.getLockedStatus() ? "locked" : "unlocked")));
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

	public CommandAPICommand getTopCommand() {
		return new CommandAPICommand("top").withPermission(CommandPermission.NONE).withPermission("hellblock.user")
				.executesPlayer((player, args) -> {
					if (!HellblockPlugin.getInstance().getIslandLevelManager().getTopTenHellblocks().entrySet()
							.isEmpty()) {
						int i = 0;
						HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
								"<red>Top Ten Level Hellblocks:");
						for (Entry<UUID, Float> ten : HellblockPlugin.getInstance().getIslandLevelManager()
								.getTopTenHellblocks().reversed().entrySet()) {

							UUID id = ten.getKey();
							if (!Bukkit.getOfflinePlayer(id).hasPlayedBefore())
								continue;
							if (Bukkit.getOfflinePlayer(id).getName() == null)
								continue;
							float level = ten.getValue().floatValue();
							HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
									String.format("<dark_red>%s. <red>%s <gray>(Lvl %s)", ++i,
											Bukkit.getOfflinePlayer(id).getName(), level));
							if (i >= 10) {
								break;
							}
						}
					} else {
						HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
								"<red>No hellblocks to list for the top ten!");
					}
				});
	}

	public CommandAPICommand getBiomeCommand() {
		return new CommandAPICommand("changebiome").withAliases("setbiome", "biome")
				.withPermission(CommandPermission.NONE).withPermission("hellblock.user")
				.withArguments(new StringArgument("biome").replaceSuggestions(
						ArgumentSuggestions.stringCollection(info -> Arrays.stream(HellBiome.values())
								.map(biome -> biome.toString()).collect(Collectors.toList()))))
				.executesPlayer((player, args) -> {
					HellblockPlayer pi = HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(player);
					if (pi.hasHellblock()) {
						if (pi.isAbandoned()) {
							HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
									"<red>This hellblock is abandoned, you can't do anything until it's recovered!");
							return;
						}
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
						HellblockPlugin.getInstance().getBiomeHandler().changeHellblockBiome(pi, biome, false, false);
						HellblockPlugin.getInstance().getCoopManager().updateParty(player.getUniqueId(), "biome",
								pi.getHellblockBiome());
					} else {
						HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
								"<red>You don't have a hellblock!");
					}
				});
	}

	public CommandAPICommand getVisitCommand() {
		return new CommandAPICommand("visit").withAliases("warp").withPermission(CommandPermission.NONE)
				.withPermission("hellblock.user").withArguments(
						new StringArgument("player").replaceSuggestions(ArgumentSuggestions.stringCollection(info -> {
							if (info.sender() instanceof Player player) {
								HellblockPlayer pi = HellblockPlugin.getInstance().getHellblockHandler()
										.getActivePlayer(player);
								return HellblockPlugin.getInstance().getHellblockHandler().getActivePlayers().values()
										.stream()
										.filter(hbPlayer -> hbPlayer.getPlayer() != null && hbPlayer.hasHellblock()
												&& hbPlayer.getHomeLocation() != null
												&& !pi.getHellblockParty().contains(hbPlayer.getPlayer().getUniqueId())
												&& !hbPlayer.getPlayer().getName().equalsIgnoreCase(player.getName()))
										.map(hbPlayer -> hbPlayer.getPlayer().getName()).collect(Collectors.toList());
							} else {
								return Collections.emptyList();
							}
						})))
				.executesPlayer((player, args) -> {
					String user = (String) args.getOrDefault("player", player);
					UUID id = Bukkit.getPlayer(user) != null ? Bukkit.getPlayer(user).getUniqueId()
							: UUIDFetcher.getUUID(user);
					if (id == null) {
						HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
								"<red>The player's hellblock you're trying to visit doesn't exist!");
						return;
					}
					if (!Bukkit.getOfflinePlayer(id).hasPlayedBefore()) {
						HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
								"<red>The player's hellblock you're trying to visit doesn't exist!");
						return;
					}
					HellblockPlayer pi = HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(player);
					
					HellblockPlayer ti = null;
					if (HellblockPlugin.getInstance().getHellblockHandler().getActivePlayers().containsKey(id)) {
						ti = HellblockPlugin.getInstance().getHellblockHandler().getActivePlayers().get(id);
					} else {
						ti = new HellblockPlayer(id);
					}
					
					if (id.equals(player.getUniqueId()) || ti.getHellblockParty().contains(player.getUniqueId())) {
						if (pi.isAbandoned()) {
							HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
									"<red>This hellblock is abandoned, you can't do anything until it's recovered!");
							return;
						}
						if (pi.getHomeLocation() != null) {
							if (!LocationUtils.isSafeLocation(pi.getHomeLocation())) {
								HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
										"<red>This hellblock home location was deemed not safe, resetting to bedrock location!");
								pi.setHome(HellblockPlugin.getInstance().getHellblockHandler()
										.locateBedrock(player.getUniqueId()));
								HellblockPlugin.getInstance().getCoopManager().updateParty(player.getUniqueId(), "home",
										pi.getHomeLocation());
							}
							HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
									"<red>Teleporting you to your hellblock!");
							ChunkUtils.teleportAsync(player, pi.getHomeLocation(), TeleportCause.PLUGIN);
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
							throw new NullPointerException();
						}
						return;
					}

					if (ti.hasHellblock()) {
						if (ti.isAbandoned()) {
							HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
									"<red>This hellblock is abandoned, you can't visit it at this time!");
							return;
						}
						if (!ti.getLockedStatus()
								&& HellblockPlugin.getInstance().getCoopManager().checkIfVisitorIsWelcome(player, id)) {
							if (!LocationUtils.isSafeLocation(ti.getHomeLocation())) {
								HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
										"<red>This hellblock is not safe to visit right now!");
								return;
							}
							ChunkUtils.teleportAsync(player, ti.getHomeLocation(), TeleportCause.PLUGIN);
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
									HellblockPlugin.getInstance().getCoopManager().updateParty(id, "visit", 1);
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
						if (pi.isAbandoned()) {
							HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
									"<red>This hellblock is abandoned, you can't do anything until it's recovered!");
							return;
						}
						if (pi.getHomeLocation() != null) {
							if (!LocationUtils.isSafeLocation(pi.getHomeLocation())) {
								HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
										"<red>This hellblock home location was deemed not safe, resetting to bedrock location!");
								pi.setHome(HellblockPlugin.getInstance().getHellblockHandler()
										.locateBedrock(player.getUniqueId()));
								HellblockPlugin.getInstance().getCoopManager().updateParty(player.getUniqueId(), "home",
										pi.getHomeLocation());
							}
							HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
									"<red>Teleporting you to your hellblock!");
							ChunkUtils.teleportAsync(player, pi.getHomeLocation(), TeleportCause.PLUGIN);
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
							throw new NullPointerException();
						}
					}
				});
	}

	public CommandAPICommand getBanCommand() {
		return new CommandAPICommand("ban").withPermission(CommandPermission.NONE).withPermission("hellblock.user")
				.withArguments(
						new StringArgument("player").replaceSuggestions(ArgumentSuggestions.stringCollection(info -> {
							if (info.sender() instanceof Player player) {
								HellblockPlayer pi = HellblockPlugin.getInstance().getHellblockHandler()
										.getActivePlayer(player);
								return HellblockPlugin.getInstance().getHellblockHandler().getActivePlayers().values()
										.stream()
										.filter(hbPlayer -> hbPlayer.getPlayer() != null
												&& !pi.getWhoTrusted().contains(hbPlayer.getPlayer().getUniqueId())
												&& !pi.getHellblockParty().contains(hbPlayer.getPlayer().getUniqueId())
												&& !hbPlayer.getPlayer().getName().equalsIgnoreCase(player.getName()))
										.map(hbPlayer -> hbPlayer.getPlayer().getName()).collect(Collectors.toList());
							} else {
								return Collections.emptyList();
							}
						})))
				.executesPlayer((player, args) -> {
					HellblockPlayer pi = HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(player);
					if (!pi.hasHellblock()) {
						HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
								"<red>You don't have a hellblock!");
					} else {
						if (pi.isAbandoned()) {
							HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
									"<red>This hellblock is abandoned, you can't do anything until it's recovered!");
							return;
						}
						String user = (String) args.getOrDefault("player", player);
						UUID id = Bukkit.getPlayer(user) != null ? Bukkit.getPlayer(user).getUniqueId()
								: UUIDFetcher.getUUID(user);
						if (id == null) {
							HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
									"<red>The player you are trying to ban doesn't exist!");
							return;
						}
						if (!Bukkit.getOfflinePlayer(id).hasPlayedBefore()) {
							HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
									"<red>The player you are trying to ban doesn't exist!");
							return;
						}
						if (id.equals(player.getUniqueId())) {
							HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
									"<red>You can't do this to yourself!");
							return;
						}
						if (pi.getHellblockParty().contains(id) || pi.getWhoTrusted().contains(id)) {
							HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
									"<red>You can't use this command on a party member or trusted player!");
							return;
						}
						if (pi.getBannedPlayers().contains(id)) {
							HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
									"<red>This player is already banned from your island!");
							return;
						}

						pi.banPlayer(id);
						HellblockPlugin.getInstance().getCoopManager().updateParty(player.getUniqueId(), "ban", id);
						if (Bukkit.getPlayer(user) != null) {
							if (HellblockPlugin.getInstance().getCoopManager().trackBannedPlayer(id)) {
								HellblockPlayer ti = HellblockPlugin.getInstance().getHellblockHandler()
										.getActivePlayer(id);
								if (ti.hasHellblock()) {
									if (!LocationUtils.isSafeLocation(ti.getHomeLocation())) {
										HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(
												Bukkit.getPlayer(user),
												"<red>This hellblock home location was deemed not safe, resetting to bedrock location!");
										ti.setHome(
												HellblockPlugin.getInstance().getHellblockHandler().locateBedrock(id));
										HellblockPlugin.getInstance().getCoopManager().updateParty(id, "home",
												ti.getHomeLocation());
									}
									ChunkUtils.teleportAsync(Bukkit.getPlayer(user), ti.getHomeLocation(),
											TeleportCause.PLUGIN);
								} else {
									Bukkit.getPlayer(user).performCommand(
											HellblockPlugin.getInstance().getHellblockHandler().getNetherCMD());

								}
							}
						}
						HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
								String.format("<red>You've banned <dark_red>%s <red>from your hellblock!", user));
					}
				});
	}

	public CommandAPICommand getUnbanCommand() {
		return new CommandAPICommand("unban").withPermission(CommandPermission.NONE).withPermission("hellblock.user")
				.withArguments(
						new StringArgument("player").replaceSuggestions(ArgumentSuggestions.stringCollection(info -> {
							if (info.sender() instanceof Player player) {
								HellblockPlayer pi = HellblockPlugin.getInstance().getHellblockHandler()
										.getActivePlayer(player);
								return HellblockPlugin.getInstance().getHellblockHandler().getActivePlayers().values()
										.stream()
										.filter(hbPlayer -> hbPlayer.getPlayer() != null
												&& !pi.getWhoTrusted().contains(hbPlayer.getPlayer().getUniqueId())
												&& !pi.getHellblockParty().contains(hbPlayer.getPlayer().getUniqueId())
												&& !hbPlayer.getPlayer().getName().equalsIgnoreCase(player.getName()))
										.map(hbPlayer -> hbPlayer.getPlayer().getName()).collect(Collectors.toList());
							} else {
								return Collections.emptyList();
							}
						})))
				.executesPlayer((player, args) -> {
					HellblockPlayer pi = HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(player);
					if (!pi.hasHellblock()) {
						HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
								"<red>You don't have a hellblock!");
					} else {
						if (pi.isAbandoned()) {
							HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
									"<red>This hellblock is abandoned, you can't do anything until it's recovered!");
							return;
						}
						String user = (String) args.getOrDefault("player", player);
						UUID id = Bukkit.getPlayer(user) != null ? Bukkit.getPlayer(user).getUniqueId()
								: UUIDFetcher.getUUID(user);
						if (id == null) {
							HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
									"<red>The player you are trying to unban doesn't exist!");
							return;
						}
						if (!Bukkit.getOfflinePlayer(id).hasPlayedBefore()) {
							HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
									"<red>The player you are trying to unban doesn't exist!");
							return;
						}
						if (id.equals(player.getUniqueId())) {
							HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
									"<red>You can't do this to yourself!");
							return;
						}
						if (pi.getHellblockParty().contains(id) || pi.getWhoTrusted().contains(id)) {
							HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
									"<red>You can't use this command on a party member or trusted player!");
							return;
						}
						if (!pi.getBannedPlayers().contains(id)) {
							HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
									"<red>This player is not banned from your island!");
							return;
						}

						pi.unbanPlayer(id);
						HellblockPlugin.getInstance().getCoopManager().updateParty(player.getUniqueId(), "unban", id);
						HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
								String.format("<red>You've unbanned <dark_red>%s <red>from your hellblock!", user));
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
						if (pi.isAbandoned()) {
							HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
									"<red>This hellblock is abandoned, you can't do anything until it's recovered!");
							return;
						}
						String partyString = "", trustedString = "", bannedString = "";
						for (UUID id : pi.getHellblockParty()) {
							if (Bukkit.getOfflinePlayer(id).hasPlayedBefore()
									&& Bukkit.getOfflinePlayer(id).getName() != null) {
								partyString = "<dark_red>" + Bukkit.getOfflinePlayer(id).getName() + "<red>, ";
							}
						}
						for (UUID id : HellblockPlugin.getInstance().getHellblockHandler()
								.getActivePlayer(pi.getHellblockOwner()).getWhoTrusted()) {
							if (Bukkit.getOfflinePlayer(id).hasPlayedBefore()
									&& Bukkit.getOfflinePlayer(id).getName() != null) {
								trustedString = "<dark_red>" + Bukkit.getOfflinePlayer(id).getName() + "<red>, ";
							}
						}
						for (UUID id : pi.getBannedPlayers()) {
							if (Bukkit.getOfflinePlayer(id).hasPlayedBefore()
									&& Bukkit.getOfflinePlayer(id).getName() != null) {
								bannedString = "<dark_red>" + Bukkit.getOfflinePlayer(id).getName() + "<red>, ";
							}
						}
						HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
								String.format("<dark_red>Hellblock Information (ID: <red>%s<dark_red>):", pi.getID()));
						HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
								"<red>Owner: <dark_red>" + (pi.getHellblockOwner() != null
										&& Bukkit.getOfflinePlayer(pi.getHellblockOwner()).hasPlayedBefore()
										&& Bukkit.getOfflinePlayer(pi.getHellblockOwner()).getName() != null
												? Bukkit.getOfflinePlayer(pi.getHellblockOwner()).getName()
												: "Unknown"));
						HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
								"<red>Level: <dark_red>" + pi.getLevel());
						HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
								"<red>Creation Date: <dark_red>" + pi.getCreationTime());
						HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
								"<red>Visitor Status: <dark_red>" + (pi.getLockedStatus() ? "Closed" : "Open"));
						HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
								"<red>Total Visits: <dark_red>" + pi.getTotalVisitors());
						HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
								"<red>Island Type: <dark_red>"
										+ StringUtils.capitalize(pi.getIslandChoice().getName()));
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
						HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
								"<red>Banned Players: <dark_red>" + (!bannedString.isEmpty()
										? bannedString.substring(0, bannedString.length() - 2)
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
						if (pi.isAbandoned()) {
							HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
									"<red>This hellblock is abandoned, you can't do anything until it's recovered!");
							return;
						}
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
							if (pi.getHomeLocation().getWorld() != null
									&& pi.getHomeLocation().getWorld().getName().equals(player.getWorld().getName())
									&& pi.getHomeLocation().getX() == player.getLocation().getX()
									&& pi.getHomeLocation().getY() == player.getLocation().getY()
									&& pi.getHomeLocation().getZ() == player.getLocation().getZ()
									&& pi.getHomeLocation().getYaw() == player.getLocation().getYaw()) {
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
								float pitch = location.getPitch();

								if (!(offlineFile.getString("player.home.world").equalsIgnoreCase(world)
										|| offlineFile.getDouble("player.home.x") == x
										|| offlineFile.getDouble("player.home.y") == y
										|| offlineFile.getDouble("player.home.z") == z
										|| (float) offlineFile.getDouble("player.home.yaw") == yaw
										|| (float) offlineFile.getDouble("player.home.pitch") == pitch)) {
									offlineFile.set("player.home.world", world);
									offlineFile.set("player.home.x", x);
									offlineFile.set("player.home.y", y);
									offlineFile.set("player.home.z", z);
									offlineFile.set("player.home.yaw", yaw);
									offlineFile.set("player.home.pitch", pitch);
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
											player.getLocation().getBlockX(), player.getLocation().getBlockY(),
											player.getLocation().getBlockZ(), LocationUtils.getFacing(player)));
						} else {
							HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
									"<red>Error setting your home location!");
						}
					}
				});
	}

	public CommandAPICommand getHelpCommand() {
		return new CommandAPICommand("help").withPermission(CommandPermission.NONE).withPermission("hellblock.user")
				.withArguments(new IntegerArgument("page")
						.replaceSuggestions(ArgumentSuggestions.stringCollection(info -> Arrays.asList("1", "2"))))
				.executesPlayer((player, args) -> {
					int page = (int) args.getOrDefault("page", 1);
					HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
							String.format("<dark_red>Hellblock Commands (Page %s):", page));
					if (page == 1) {
						HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
								"<red>/hellblock create: Create your island");
						HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
								"<red>/hellblock reset: Reset your island");
						HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
								"<red>/hellblock info: See information about your island");
						HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
								"<red>/hellblock home: Teleport to your island home");
						HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
								"<red>/hellblock sethome: Set the new home location of your island");
						HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
								"<red>/hellblock lock/unlock: Change whether or not visitors can access your island");
						HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
								"<red>/hellblock setbiome <biome>: Change the biome of your island");
						HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
								"<red>/hellblock ban/unban <player>: Deny access to this player to your island");
						;
						HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
								"<red>/hellblock visit <player>: Visit another player's island");
						HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
								"<red>/hellblock top: View the hellblocks with the top levels");
					} else {
						HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
								"<red>/hellblock coop invite <player>: Invite another player to your island");
						HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
								"<red>/hellblock coop accept <player>: Accept an invite to another player's island");
						HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
								"<red>/hellblock coop decline <player>: Reject an invite to another player's island");
						HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
								"<red>/hellblock coop invitations: See a list of invitations sent to you in the past 24 hours!");
						HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
								"<red>/hellblock coop cancel <player>: Cancel an invite you sent to a player to join your island");
						HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
								"<red>/hellblock coop kick <player>: Kick the player from your party");
						HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
								"<red>/hellblock coop leave: Leave the island you're apart of");
						HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
								"<red>/hellblock coop setowner <player>: Set the new owner of your island");
						HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
								"<red>/hellblock coop trust <player>: Trust a player to your island without inviting them");
						HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
								"<red>/hellblock coop untrust <player>: Untrusts a player from your island");
					}
				});
	}
}
