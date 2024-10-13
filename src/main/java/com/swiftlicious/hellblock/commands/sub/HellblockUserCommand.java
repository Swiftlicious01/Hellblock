package com.swiftlicious.hellblock.commands.sub;

import java.util.ArrayList;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.playerdata.HellblockPlayer;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;

public class HellblockUserCommand {

	public static HellblockUserCommand INSTANCE = new HellblockUserCommand();

	public CommandAPICommand getUserCommand() {
		return new CommandAPICommand("user").withPermission(CommandPermission.NONE).withPermission("hellblock.user")
				.withSubcommands(createCommand("create"), resetCommand("reset"), homeCommand("home"));
	}

	private CommandAPICommand resetCommand(String namespace) {
		return new CommandAPICommand(namespace).executesPlayer((player, args) -> {
			HellblockPlayer pi = (HellblockPlayer) HellblockPlugin.getInstance().getHellblockHandler()
					.getActivePlayers().get(player.getUniqueId());
			if (!pi.hasHellblock()) {
				HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
						"<red>You don't have a hellblock!");
				return;
			}
			int z_operate;
			Location loc = pi.getHellblockLocation();
			double y = loc.getY();
			z_operate = (int) loc.getX() - HellblockPlugin.getInstance().getHellblockHandler().getDistance();

			while (true) {
				if (z_operate > (int) loc.getX() + HellblockPlugin.getInstance().getHellblockHandler().getDistance()) {
					HellblockPlugin.getInstance().getWorldGuardHandler().unprotectHellblock(player);
					pi.setHellblock(false, (Location) null);
					pi.setHellblockOwner(null);
					pi.setHellblockParty(new ArrayList<>());
					HellblockPlugin.getInstance().getHellblockHandler().createHellblock(player);
					break;
				}

				for (int x_operate = (int) loc.getZ() - HellblockPlugin.getInstance().getHellblockHandler()
						.getDistance(); z_operate <= (int) loc.getZ()
								+ HellblockPlugin.getInstance().getHellblockHandler().getDistance(); ++x_operate) {
					Block block = loc.getWorld().getBlockAt(x_operate, (int) y, z_operate);
					if (block.getType() != Material.AIR) {
						block.setType(Material.AIR);
					}
				}

				++z_operate;
			}
		});
	}

	private CommandAPICommand createCommand(String namespace) {
		return new CommandAPICommand(namespace).executesPlayer((player, args) -> {
			HellblockPlayer pi = (HellblockPlayer) HellblockPlugin.getInstance().getHellblockHandler()
					.getActivePlayers().get(player.getUniqueId());
			if (!pi.hasHellblock()) {
				HellblockPlugin.getInstance().getHellblockHandler().createHellblock(player);
			} else {
				HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
						"<red>You already have a hellblock!");
			}
		});
	}

	private CommandAPICommand homeCommand(String namespace) {
		return new CommandAPICommand(namespace).executesPlayer((player, args) -> {
			HellblockPlayer pi = (HellblockPlayer) HellblockPlugin.getInstance().getHellblockHandler()
					.getActivePlayers().get(player.getUniqueId());
			if (!pi.hasHellblock()) {
				HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
						"<red>You don't have a hellblock!");
			} else {
				HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
						"<red>Teleporting you to your hellblock!");
				player.teleport(pi.getHomeLocation());
			}
		});
	}
}
