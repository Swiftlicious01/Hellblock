package com.swiftlicious.hellblock.commands.sub;

import java.util.UUID;
import java.util.stream.Collectors;

import org.bukkit.Location;
import org.bukkit.World;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.playerdata.HellblockPlayer;
import com.swiftlicious.hellblock.playerdata.UUIDFetcher;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.StringArgument;

public class HellblockAdminCommand {

	public static HellblockAdminCommand INSTANCE = new HellblockAdminCommand();

	public CommandAPICommand getAdminCommand() {
		return new CommandAPICommand("admin").withPermission(CommandPermission.OP).withPermission("hellblock.admin")
				.withSubcommands(genSpawnCommand("genspawn"), teleportCommand("goto"));
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
					HellblockPlayer ti = null;
					if (HellblockPlugin.getInstance().getHellblockHandler().getActivePlayers().containsKey(id)) {
						ti = HellblockPlugin.getInstance().getHellblockHandler().getActivePlayers().get(id);
					} else {
						ti = new HellblockPlayer(id);
					}

					if (ti.hasHellblock()) {
						player.teleportAsync(ti.getHomeLocation());
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
			HellblockPlugin.getInstance().getHellblockHandler().generateSpawn();

			player.teleportAsync(new Location(world, 0.0D,
					(double) (HellblockPlugin.getInstance().getHellblockHandler().getHeight() + 1), 0.0D));
			HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
					"<red>Spawn area has been generated!");
		});
	}
}
