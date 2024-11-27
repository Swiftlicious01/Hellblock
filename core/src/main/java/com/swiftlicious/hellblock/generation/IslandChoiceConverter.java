package com.swiftlicious.hellblock.generation;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.player.UserData;

public class IslandChoiceConverter {

	protected final HellblockPlugin instance;

	public IslandChoiceConverter(HellblockPlugin plugin) {
		instance = plugin;
	}

	public CompletableFuture<Void> convertIslandChoice(@NotNull Player player, @NotNull Location location) {
		return convertIslandChoice(player, location, null);
	}

	public CompletableFuture<Void> convertIslandChoice(@NotNull Player player, @NotNull Location location,
			@Nullable String schematic) {
		Optional<UserData> onlineUser = instance.getStorageManager().getOnlineUser(player.getUniqueId());
		if (onlineUser.isEmpty())
			return CompletableFuture.completedFuture(null);
		World world = instance.getHellblockHandler().getHellblockWorld();
		int x = location.getBlockX();
		int y = instance.getConfigManager().height();
		int z = location.getBlockZ();
		Block highest = world.getHighestBlockAt(x, z);
		if (instance.getConfigManager().islandOptions().isEmpty()) {
			instance.getPluginLogger().severe(
					"An error occurred while retrieving the options for hellblock islands to choose from. Defaulting to classic island.");
			return instance.getIslandGenerator().generateClassicHellblock(location, player).thenRun(() -> {
				onlineUser.get().getHellblockData()
						.setHomeLocation(new Location(world, highest.getX() - 0.5, y + 3, highest.getZ() - 0.5, 90, 0));
			});
		}
		IslandOptions choice = onlineUser.get().getHellblockData().getIslandChoice();
		switch (choice) {
		case IslandOptions.DEFAULT:
			if (instance.getConfigManager().islandOptions().contains(IslandOptions.DEFAULT.getName())) {
				return instance.getIslandGenerator().generateDefaultHellblock(location, player).thenRun(() -> {
					onlineUser.get().getHellblockData().setHomeLocation(
							new Location(world, highest.getX() + 0.5, y + 5, highest.getZ() + 2.5, -175, 0));
				});
			}
		case IslandOptions.CLASSIC:
			if (instance.getConfigManager().islandOptions().contains(IslandOptions.CLASSIC.getName())) {
				return instance.getIslandGenerator().generateClassicHellblock(location, player).thenRun(() -> {
					onlineUser.get().getHellblockData().setHomeLocation(
							new Location(world, highest.getX() - 0.5, y + 3, highest.getZ() - 0.5, 90, 0));
				});
			}
		case IslandOptions.SCHEMATIC:
			boolean schematicsAvailable = false;
			for (String list : instance.getConfigManager().islandOptions()) {
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
							onlineUser.get().getHellblockData().setHomeLocation(
									new Location(world, highest.getX() + 0.5, y + 5, highest.getZ() + 2.5, -175, 0));
						});
			}
		default:
			return instance.getIslandGenerator().generateClassicHellblock(location, player).thenRun(() -> {
				onlineUser.get().getHellblockData()
						.setHomeLocation(new Location(world, highest.getX() - 0.5, y + 3, highest.getZ() - 0.5, 90, 0));
			});
		}
	}
}