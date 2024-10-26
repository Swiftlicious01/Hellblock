package com.swiftlicious.hellblock.commands.sub;

import java.io.File;
import java.io.IOException;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;

import com.google.common.io.Files;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.playerdata.HellblockPlayer;
import com.swiftlicious.hellblock.playerdata.UUIDFetcher;
import com.swiftlicious.hellblock.utils.LocationUtils;
import com.swiftlicious.hellblock.utils.LogUtils;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.StringArgument;

public class HellblockAdminCommand {

	public static HellblockAdminCommand INSTANCE = new HellblockAdminCommand();

	public CommandAPICommand getAdminCommand() {
		return new CommandAPICommand("admin").withPermission(CommandPermission.OP).withPermission("hellblock.admin")
				.withSubcommands(getGenSpawnCommand("genspawn"), getPurgeCommand("purge"), getTeleportCommand("goto"),
						getDeleteCommand("delete"));
	}

	private CommandAPICommand getTeleportCommand(String namespace) {
		return new CommandAPICommand(namespace)
				.withArguments(new StringArgument("player").replaceSuggestions(ArgumentSuggestions.stringCollection(
						collection -> HellblockPlugin.getInstance().getHellblockHandler().getActivePlayers().values()
								.stream().filter(hbPlayer -> hbPlayer.getPlayer() != null && hbPlayer.hasHellblock())
								.map(hbPlayer -> hbPlayer.getPlayer().getName()).collect(Collectors.toList()))))
				.executesPlayer((player, args) -> {
					String user = (String) args.getOrDefault("player", player);
					UUID id = Bukkit.getPlayer(user) != null ? Bukkit.getPlayer(user).getUniqueId()
							: UUIDFetcher.getUUID(user);
					if (id == null) {
						HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
								"<red>The player's hellblock you're trying to teleport to doesn't exist!");
						return;
					}
					if (!Bukkit.getOfflinePlayer(id).hasPlayedBefore()) {
						HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
								"<red>The player's hellblock you're trying to teleport to doesn't exist!");
						return;
					}
					HellblockPlayer ti = null;
					if (HellblockPlugin.getInstance().getHellblockHandler().getActivePlayers().containsKey(id)) {
						ti = HellblockPlugin.getInstance().getHellblockHandler().getActivePlayers().get(id);
					} else {
						ti = new HellblockPlayer(id);
					}

					if (ti.hasHellblock()) {
						if (!LocationUtils.isSafeLocation(ti.getHomeLocation())) {
							HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
									"<red>This hellblock home location was deemed not safe, resetting to bedrock location!");
							ti.setHome(HellblockPlugin.getInstance().getHellblockHandler().locateBedrock(id));
							HellblockPlugin.getInstance().getCoopManager().updateParty(player.getUniqueId(), "home",
									ti.getHomeLocation());
						}
						player.teleportAsync(ti.getHomeLocation());
						HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
								String.format("<red>You have been teleported to <dark_red>%s<red>'s hellblock!", user));
						return;
					}

					HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
							"<red>That player does not have a hellblock!");
					return;
				});
	}

	private CommandAPICommand getPurgeCommand(String namespace) {
		return new CommandAPICommand(namespace).withArguments(new IntegerArgument("days")).executes((sender, args) -> {
			int purgeDays = (int) args.get("days");
			if (HellblockPlugin.getInstance().getHellblockHandler().getPlayersDirectory().listFiles().length == 0) {
				HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(sender,
						"<red>No hellblock player data to purge available!");
				return;
			}
			if (purgeDays <= 0) {
				HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(sender,
						"<red>Please enter a positive number above 0!");
				return;
			}
			final int purgeTime = Integer.parseInt(String.valueOf(purgeDays), 10) * 24;
			int purgeCount = 0;
			for (File playerData : HellblockPlugin.getInstance().getHellblockHandler().getPlayersDirectory()
					.listFiles()) {
				if (!playerData.isFile() || !playerData.getName().endsWith(".yml"))
					continue;
				String uuid = Files.getNameWithoutExtension(playerData.getName());
				UUID id = null;
				try {
					id = UUID.fromString(uuid);
				} catch (IllegalArgumentException ignored) {
					// ignored
					continue;
				}
				if (id == null)
					continue;
				if (!Bukkit.getOfflinePlayer(id).hasPlayedBefore())
					continue;

				OfflinePlayer player = Bukkit.getOfflinePlayer(id);
				if (player.getLastLogin() == 0)
					continue;
				if (player.getLastLogin() > (System.currentTimeMillis() - (purgeTime * 3600000L))) {
					YamlConfiguration playerConfig = YamlConfiguration.loadConfiguration(playerData);
					String ownerID = playerConfig.getString("player.owner");
					UUID ownerUUID = null;
					try {
						ownerUUID = UUID.fromString(ownerID);
					} catch (IllegalArgumentException ignored) {
						// ignored
						continue;
					}
					if (ownerUUID == null)
						continue;
					if (HellblockPlugin.getInstance().getHellblockHandler().isHellblockOwner(id, ownerUUID)) {

						playerConfig.set("player.abandoned", true);
						try {
							playerConfig.save(playerData);
						} catch (IOException ex) {
							LogUtils.warn(
									String.format("Could not save the player data file as abandoned for %s", uuid), ex);
							continue;
						}
						if (HellblockPlugin.getInstance().getWorldGuardHandler().getRegion(ownerUUID) != null) {
							HellblockPlugin.getInstance().getWorldGuardHandler().updateHellblockMessages(ownerUUID,
									HellblockPlugin.getInstance().getWorldGuardHandler().getRegion(ownerUUID));
							HellblockPlugin.getInstance().getWorldGuardHandler().abandonIsland(ownerUUID,
									HellblockPlugin.getInstance().getWorldGuardHandler().getRegion(ownerUUID));
							purgeCount++;
						}
					}
				}
			}

			if (purgeCount > 0) {
				HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(sender,
						String.format("<red>You purged a total of %s hellblocks!", purgeCount));
			} else {
				HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(sender,
						"<red>No hellblock data was purged with your inputted settings!");
			}
		});
	}

	private CommandAPICommand getDeleteCommand(String namespace) {
		return new CommandAPICommand(namespace)
				.withArguments(new StringArgument("player").replaceSuggestions(ArgumentSuggestions.stringCollection(
						collection -> HellblockPlugin.getInstance().getHellblockHandler().getActivePlayers().values()
								.stream().filter(hbPlayer -> hbPlayer.getPlayer() != null && hbPlayer.hasHellblock())
								.map(hbPlayer -> hbPlayer.getPlayer().getName()).collect(Collectors.toList()))))
				.executesPlayer((player, args) -> {
					String user = (String) args.getOrDefault("player", player);
					UUID id = Bukkit.getPlayer(user) != null ? Bukkit.getPlayer(user).getUniqueId()
							: UUIDFetcher.getUUID(user);
					if (id == null) {
						HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
								"<red>The player's hellblock you're trying to delete doesn't exist!");
						return;
					}
					if (!Bukkit.getOfflinePlayer(id).hasPlayedBefore()) {
						HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
								"<red>The player's hellblock you're trying to delete doesn't exist!");
						return;
					}
					HellblockPlayer ti = null;
					if (HellblockPlugin.getInstance().getHellblockHandler().getActivePlayers().containsKey(id)) {
						ti = HellblockPlugin.getInstance().getHellblockHandler().getActivePlayers().get(id);
					} else {
						ti = new HellblockPlayer(id);
					}

					if (ti.hasHellblock()) {
						HellblockPlugin.getInstance().getHellblockHandler().resetHellblock(id, true);
						if (Bukkit.getPlayer(id) != null && Bukkit.getPlayer(id).isOnline()) {
							Bukkit.getPlayer(id)
									.performCommand(HellblockPlugin.getInstance().getHellblockHandler().getNetherCMD());
						}
						HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
								String.format("<red>You have forcefully deleted <dark_red>%s<red>'s hellblock!", user));
						return;
					}

					HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
							"<red>That player does not have a hellblock!");
					return;
				});
	}

	private CommandAPICommand getGenSpawnCommand(String namespace) {
		return new CommandAPICommand(namespace).executesPlayer((player, args) -> {
			if (!player.getWorld().getName()
					.equalsIgnoreCase(HellblockPlugin.getInstance().getHellblockHandler().getWorldName())) {
				HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
						"<red>You aren't in the correct world to generate the spawn!");
				return;
			}

			World world = HellblockPlugin.getInstance().getHellblockHandler().getHellblockWorld();
			if (HellblockPlugin.getInstance().getHellblockHandler().isWorldguardProtect()) {
				com.sk89q.worldedit.world.World weWorld = BukkitAdapter.adapt(world);
				if (HellblockPlugin.getInstance().getWorldGuardHandler().getWorldGuardPlatform() != null
						&& HellblockPlugin.getInstance().getWorldGuardHandler().getWorldGuardPlatform()
								.getRegionContainer().get(weWorld).hasRegion("Spawn")) {
					HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
							"<red>The spawn area has already been generated!");
					return;
				}
			} else {
				// TODO: plugin protection
			}
			HellblockPlugin.getInstance().getHellblockHandler().generateSpawn();

			player.teleportAsync(new Location(world, 0.0D,
					(double) (HellblockPlugin.getInstance().getHellblockHandler().getHeight() + 1), 0.0D));
			HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
					"<red>Spawn area has been generated!");
		});
	}
}
