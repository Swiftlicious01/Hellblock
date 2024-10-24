package com.swiftlicious.hellblock.commands.sub;

import java.util.UUID;
import java.util.stream.Collectors;

import org.bukkit.Location;
import org.bukkit.World;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.playerdata.HellblockPlayer;
import com.swiftlicious.hellblock.playerdata.UUIDFetcher;
import com.swiftlicious.hellblock.utils.LocationUtils;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.StringArgument;

public class HellblockAdminCommand {

	public static HellblockAdminCommand INSTANCE = new HellblockAdminCommand();

	public CommandAPICommand getAdminCommand() {
		return new CommandAPICommand("admin").withPermission(CommandPermission.OP).withPermission("hellblock.admin")
				.withSubcommands(genSpawnCommand("genspawn"), teleportCommand("goto"), deleteCommand("delete"));
	}

	private CommandAPICommand teleportCommand(String namespace) {
		return new CommandAPICommand(namespace)
				.withArguments(new StringArgument("player").replaceSuggestions(ArgumentSuggestions
						.stringCollection(collection -> HellblockPlugin.getInstance().getHellblockHandler()
								.getActivePlayers().values().stream().filter(hbPlayer -> hbPlayer.getPlayer() != null)
								.map(hbPlayer -> hbPlayer.getPlayer().getName()).collect(Collectors.toList()))))
				.executesPlayer((player, args) -> {
					String user = (String) args.getOrDefault("player", player);
					UUID id = UUIDFetcher.getUUID(user);
					if (id == null) {
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

	private CommandAPICommand deleteCommand(String namespace) {
		return new CommandAPICommand(namespace)
				.withArguments(new StringArgument("player").replaceSuggestions(ArgumentSuggestions
						.stringCollection(collection -> HellblockPlugin.getInstance().getHellblockHandler()
								.getActivePlayers().values().stream().filter(hbPlayer -> hbPlayer.getPlayer() != null)
								.map(hbPlayer -> hbPlayer.getPlayer().getName()).collect(Collectors.toList()))))
				.executesPlayer((player, args) -> {
					String user = (String) args.getOrDefault("player", player);
					UUID id = UUIDFetcher.getUUID(user);
					if (id == null) {
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
						HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
								String.format("<red>You have forcefully deleted <dark_red>%s<red>'s hellblock!", user));
						return;
					}

					HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
							"<red>That player does not have a hellblock!");
					return;
				});
	}

	private CommandAPICommand genSpawnCommand(String namespace) {
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
