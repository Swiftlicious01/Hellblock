package com.swiftlicious.hellblock.generation;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.playerdata.HellblockPlayer;
import com.swiftlicious.hellblock.utils.LogUtils;

import lombok.NonNull;

public class IslandChoiceConverter {

	private final HellblockPlugin instance;

	public IslandChoiceConverter(HellblockPlugin plugin) {
		instance = plugin;
	}

	public boolean convertIslandChoice(@NonNull Player player, @NonNull Location location) {
		HellblockPlayer pi = instance.getHellblockHandler().getActivePlayer(player);
		World world = location.getWorld();
		if (world == null) {
			LogUtils.severe("An error occurred while generating this hellblock island.");
			return false;
		}
		double x = location.getX();
		double y = location.getY();
		double z = location.getZ();
		if (instance.getHellblockHandler().getIslandOptions().isEmpty()) {
			LogUtils.severe(
					"An error occurred while retrieving the options for hellblock islands to choose from. Defaulting to classic island.");
			instance.getIslandGenerator().generateClassicHellblock(location, player);
			pi.setHome(new Location(world, x - 4.0D, y + 3.0D, z - 1.0D));
			return false;
		}
		IslandOptions choice = pi.getIslandChoice();
		switch (choice) {
		case IslandOptions.DEFAULT:
			if (instance.getHellblockHandler().getIslandOptions().contains(IslandOptions.DEFAULT.toString())) {
				instance.getIslandGenerator().generateDefaultHellblock(location, player);
				pi.setHome(new Location(world, x, y + 6.0D, z - 1.0D));
				return true;
			} else {
				instance.getAdventureManager().sendMessageWithPrefix(player,
						"<red>The default hellblock island type isn't available to generate!");
				return false;
			}
		case IslandOptions.CLASSIC:
			if (instance.getHellblockHandler().getIslandOptions().contains(IslandOptions.CLASSIC.toString())) {
				instance.getIslandGenerator().generateClassicHellblock(location, player);
				pi.setHome(new Location(world, x - 4.0D, y + 3.0D, z - 1.0D));
				return true;
			} else {
				instance.getAdventureManager().sendMessageWithPrefix(player,
						"<red>The classic hellblock island type isn't available to generate!");
				return false;
			}
		case IslandOptions.SCHEMATIC:
			boolean schematicsAvailable = false;
			for (String list : instance.getHellblockHandler().getIslandOptions()) {
				if (list.equalsIgnoreCase(IslandOptions.CLASSIC.toString())
						|| list.equalsIgnoreCase(IslandOptions.DEFAULT.toString()))
					continue;
				if (!instance.getSchematicManager().schematicFiles.containsKey(list))
					continue;

				schematicsAvailable = true;
				break;
			}
			if (schematicsAvailable) {
				instance.getIslandGenerator().generateHellblockSchematic(location, player);
				return true;
			} else {
				instance.getAdventureManager().sendMessageWithPrefix(player,
						"<red>No schematic types are available to choose from for your hellblock!");
				return false;
			}
		default:
			instance.getIslandGenerator().generateClassicHellblock(location, player);
			pi.setHome(new Location(world, x - 4.0D, y + 3.0D, z - 1.0D));
			return false;
		}
	}
}
