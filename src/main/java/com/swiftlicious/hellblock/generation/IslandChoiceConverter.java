package com.swiftlicious.hellblock.generation;

import java.util.concurrent.CompletableFuture;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.playerdata.HellblockPlayer;
import com.swiftlicious.hellblock.utils.LogUtils;

import lombok.NonNull;

public class IslandChoiceConverter {

	private final HellblockPlugin instance;

	public IslandChoiceConverter(HellblockPlugin plugin) {
		instance = plugin;
	}

	public CompletableFuture<Void> convertIslandChoice(@NonNull Player player, @NonNull Location location) {
		return convertIslandChoice(player, location, null);
	}

	public CompletableFuture<Void> convertIslandChoice(@NonNull Player player, @NonNull Location location,
			@Nullable String schematic) {
		World world = instance.getHellblockHandler().getHellblockWorld();
		double x = location.getX();
		double y = location.getY();
		double z = location.getZ();
		HellblockPlayer pi = instance.getHellblockHandler().getActivePlayer(player);
		if (instance.getHellblockHandler().getIslandOptions().isEmpty()) {
			LogUtils.severe(
					"An error occurred while retrieving the options for hellblock islands to choose from. Defaulting to classic island.");
			return instance.getIslandGenerator().generateClassicHellblock(location, player).thenRun(() -> {
				pi.setHome(new Location(world, x - 4.0D, y + 3.0D, z - 1.0D));
			});
		}
		IslandOptions choice = pi.getIslandChoice();
		switch (choice) {
		case IslandOptions.DEFAULT:
			if (instance.getHellblockHandler().getIslandOptions().contains(IslandOptions.DEFAULT.getName())) {
				return instance.getIslandGenerator().generateDefaultHellblock(location, player).thenRun(() -> {
					pi.setHome(new Location(world, x, y + 6.0D, z - 1.0D));
				});
			} else {
				instance.getAdventureManager().sendMessageWithPrefix(player,
						"<red>The default hellblock island type isn't available to generate!");
			}
		case IslandOptions.CLASSIC:
			if (instance.getHellblockHandler().getIslandOptions().contains(IslandOptions.CLASSIC.getName())) {
				return instance.getIslandGenerator().generateClassicHellblock(location, player).thenRun(() -> {
					pi.setHome(new Location(world, x - 4.0D, y + 3.0D, z - 1.0D));
				});
			} else {
				instance.getAdventureManager().sendMessageWithPrefix(player,
						"<red>The classic hellblock island type isn't available to generate!");
			}
		case IslandOptions.SCHEMATIC:
			boolean schematicsAvailable = false;
			for (String list : instance.getHellblockHandler().getIslandOptions()) {
				if (list.equalsIgnoreCase(IslandOptions.CLASSIC.getName())
						|| list.equalsIgnoreCase(IslandOptions.DEFAULT.getName()))
					continue;
				if (!instance.getSchematicManager().availableSchematics.contains(list))
					continue;

				schematicsAvailable = true;
				break;
			}
			if (schematicsAvailable) {
				return instance.getIslandGenerator().generateHellblockSchematic(location, player, schematic)
						.thenRun(() -> {
							pi.setHome(new Location(world, x, world.getHighestBlockYAt(location), z));
						});
			} else {
				instance.getAdventureManager().sendMessageWithPrefix(player,
						"<red>No schematic types are available to choose from for your hellblock!");
			}
		default:
			return instance.getIslandGenerator().generateClassicHellblock(location, player).thenRun(() -> {
				pi.setHome(new Location(world, x - 4.0D, y + 3.0D, z - 1.0D));
			});
		}
	}
}
