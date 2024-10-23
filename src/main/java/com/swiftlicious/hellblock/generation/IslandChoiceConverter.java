package com.swiftlicious.hellblock.generation;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.playerdata.HellblockPlayer;
import com.swiftlicious.hellblock.utils.LogUtils;

public class IslandChoiceConverter {

	private final HellblockPlugin instance;

	public IslandChoiceConverter(HellblockPlugin plugin) {
		instance = plugin;
	}

	public void convertIslandChoice(Player player, Location location) {
		HellblockPlayer pi = instance.getHellblockHandler().getActivePlayer(player);
		World world = location.getWorld();
		if (world == null) {
			LogUtils.severe("An error occurred while generating this hellblock island.");
			return;
		}
		double x = location.getX();
		double y = location.getY();
		double z = location.getZ();
		if (instance.getHellblockHandler().getIslandOptions().isEmpty()) {
			LogUtils.severe(
					"An error occurred while retrieving the options for hellblock islands to choose from. Defaulting to classic island.");
			instance.getIslandGenerator().generateClassicHellblock(location);
			pi.setHome(new Location(world, x - 4.0D, y + 3.0D, z - 1.0D));
			return;
		}
		IslandOptions choice = pi.getIslandChoice();
		switch (choice) {
		case IslandOptions.DEFAULT:
			if (instance.getHellblockHandler().getIslandOptions().contains("default")) {
				instance.getIslandGenerator().generateDefaultHellblock(location);
				pi.setHome(new Location(world, x, y + 6.0D, z - 1.0D));
			} else {
				instance.getAdventureManager().sendMessageWithPrefix(player,
						"<red>The default hellblock island type is not available to generate!");
			}
			break;
		case IslandOptions.CLASSIC:
			if (instance.getHellblockHandler().getIslandOptions().contains("classic")) {
				instance.getIslandGenerator().generateClassicHellblock(location);
				pi.setHome(new Location(world, x - 4.0D, y + 3.0D, z - 1.0D));
			} else {
				instance.getAdventureManager().sendMessageWithPrefix(player,
						"<red>The classic hellblock island type is not available to generate!");
			}
			break;
		case IslandOptions.SCHEMATIC:
			instance.getIslandGenerator().generateHellblockSchematic(location, player);
			break;
		default:
			instance.getIslandGenerator().generateClassicHellblock(location);
			pi.setHome(new Location(world, x - 4.0D, y + 3.0D, z - 1.0D));
			break;
		}
	}
}
