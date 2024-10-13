package com.swiftlicious.hellblock.commands.sub;

import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.playerdata.HellblockPlayer;
import com.swiftlicious.hellblock.playerdata.UUIDFetcher;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.PlayerArgument;

public class HellblockAdminCommand {

	public static HellblockAdminCommand INSTANCE = new HellblockAdminCommand();

	public CommandAPICommand getAdminCommand() {
		return new CommandAPICommand("admin").withPermission(
				CommandPermission.OP).withPermission("hellblock.admin")
				.withSubcommands(genSpawnCommand("genspawn"), teleportCommand("goto"));
	}

	private CommandAPICommand teleportCommand(String namespace) {
		return new CommandAPICommand(namespace).withArguments(new PlayerArgument("player"))
				.executesPlayer((player, args) -> {
					Player user = (Player) args.getOrDefault("player", player);
					UUIDFetcher fetcher = new UUIDFetcher(Arrays.asList(user.getName()));
					Map<String, UUID> response = null;

					try {
						response = fetcher.call();
					} catch (Exception var13) {
						HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
								"<red>Exception while fetching UUID of <dark_red>" + user.getName() + "<red>!");
						
					}

					UUID id = (UUID) response.get(user.getName());
					HellblockPlayer ti = null;
					if (HellblockPlugin.getInstance().getHellblockHandler().getActivePlayers().containsKey(id)) {
						ti = (HellblockPlayer) HellblockPlugin.getInstance().getHellblockHandler().getActivePlayers()
								.get(id);
					} else {
						ti = new HellblockPlayer(id);
					}

					if (ti.hasHellblock()) {
						player.teleport(ti.getHomeLocation());
						return;
					}

					HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
							"<red>That player does not have a hellblock!");
					return;
				});
	}

	private CommandAPICommand genSpawnCommand(String namespace) {
		return new CommandAPICommand(namespace).executesPlayer((player, args) -> {
			HellblockPlayer pi = (HellblockPlayer) HellblockPlugin.getInstance().getHellblockHandler()
					.getActivePlayers().get(player.getUniqueId());
			if (!pi.hasHellblock()) {
				HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
						"<red>You don't have a hellblock!");
				return;
			}
			int z_operate;
			World world = Bukkit.getWorld(HellblockPlugin.getInstance().getHellblockHandler().getWorldName());
			int y = HellblockPlugin.getInstance().getHellblockHandler().getHeight();

			for (int x_operate = 0
					- HellblockPlugin.getInstance().getHellblockHandler().getSpawnSize(); x_operate <= HellblockPlugin
							.getInstance().getHellblockHandler().getSpawnSize(); ++x_operate) {
				for (z_operate = 0 - HellblockPlugin.getInstance().getHellblockHandler()
						.getSpawnSize(); z_operate <= HellblockPlugin.getInstance().getHellblockHandler()
								.getSpawnSize(); ++z_operate) {
					Block block = world.getBlockAt(x_operate, y, z_operate);
					block.setType(Material.NETHER_BRICK);
				}
			}

			player.teleport(new Location(world, 0.0D,
					(double) (HellblockPlugin.getInstance().getHellblockHandler().getHeight() + 1), 0.0D));
			HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
					"<red>Spawn area has been generated!");
			if (HellblockPlugin.getInstance().getHellblockHandler().isWorldguardProtect()) {
				HellblockPlugin.getInstance().getWorldGuardHandler().protectSpawn(player);
				HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
						"<red>Spawn area protected with WorldGuard!");
			}

		});
	}
}
