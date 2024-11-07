package com.swiftlicious.hellblock.generation;

import java.util.concurrent.CompletableFuture;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
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
		HellblockPlayer pi = instance.getHellblockHandler().getActivePlayer(player);
		World world = instance.getHellblockHandler().getHellblockWorld();
		int x = location.getBlockX();
		int y = instance.getHellblockHandler().getHeight();
		int z = location.getBlockZ();
		Block highest = world.getHighestBlockAt(x, z);
		if (instance.getHellblockHandler().getIslandOptions().isEmpty()) {
			LogUtils.severe(
					"An error occurred while retrieving the options for hellblock islands to choose from. Defaulting to classic island.");
			return instance.getIslandGenerator().generateClassicHellblock(location, player).thenRun(() -> {
				pi.setHome(new Location(world, highest.getX() - 0.5, y + 3, highest.getZ() - 0.5, 90, 0));
			});
		}
		IslandOptions choice = pi.getIslandChoice();
		switch (choice) {
		case IslandOptions.DEFAULT:
			if (instance.getHellblockHandler().getIslandOptions().contains(IslandOptions.DEFAULT.getName())) {
				return instance.getIslandGenerator().generateDefaultHellblock(location, player).thenRun(() -> {
					pi.setHome(new Location(world, highest.getX() + 0.5, y + 5, highest.getZ() + 2.5, -175, 0));
				});
			} else {
				instance.getAdventureManager().sendMessageWithPrefix(player,
						"<red>The default hellblock island type isn't available to generate!");
			}
		case IslandOptions.CLASSIC:
			if (instance.getHellblockHandler().getIslandOptions().contains(IslandOptions.CLASSIC.getName())) {
				return instance.getIslandGenerator().generateClassicHellblock(location, player).thenRun(() -> {
					pi.setHome(new Location(world, highest.getX() - 0.5, y + 3, highest.getZ() - 0.5, 90, 0));
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
							pi.setHome(new Location(world, highest.getX() + 0.5, y + 5, highest.getZ() + 2.5, -175, 0));
						});
			} else {
				instance.getAdventureManager().sendMessageWithPrefix(player,
						"<red>No schematic types are available to choose from for your hellblock!");
			}
		default:
			return instance.getIslandGenerator().generateClassicHellblock(location, player).thenRun(() -> {
				pi.setHome(new Location(world, highest.getX() - 0.5, y + 3, highest.getZ() - 0.5, 90, 0));
			});
		}
	}
}
