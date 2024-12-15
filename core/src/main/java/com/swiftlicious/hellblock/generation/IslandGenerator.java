package com.swiftlicious.hellblock.generation;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.saicone.rtag.RtagBlock;
import com.saicone.rtag.RtagItem;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.context.Context;
import com.swiftlicious.hellblock.creation.item.CustomItem;
import com.swiftlicious.hellblock.handlers.AdventureHelper;
import com.swiftlicious.hellblock.player.UserData;
import com.swiftlicious.hellblock.utils.extras.Pair;
import com.swiftlicious.hellblock.utils.extras.TextValue;

public class IslandGenerator {

	protected final HellblockPlugin instance;

	public IslandGenerator(HellblockPlugin plugin) {
		this.instance = plugin;
	}

	public CompletableFuture<Void> generateHellblockSchematic(@NotNull World world, @NotNull Location location,
			@NotNull Player player, @NotNull String schematic) {
		return CompletableFuture.runAsync(() -> {
			Optional<UserData> onlineUser = instance.getStorageManager().getOnlineUser(player.getUniqueId());
			if (onlineUser.isEmpty())
				return;
			instance.getSchematicManager()
					.pasteSchematic(world, schematic, onlineUser.get().getHellblockData().getBoundingBox())
					.thenRunAsync(() -> {
						for (int x = -15; x <= 15; ++x) {
							for (int y = -15; y <= 15; ++y) {
								for (int z = -15; z <= 15; ++z) {
									if (world.getBlockAt(location.getBlockX() + x, location.getBlockY() + y,
											location.getBlockZ() + z).getType() == Material.CHEST) {
										final int finalX = x;
										final int finalY = y;
										final int finalZ = z;
										instance.getScheduler().executeSync(
												() -> this.generateChest(world,
														new Location(world, (double) (location.getBlockX() + finalX),
																(double) (location.getBlockY() + finalY),
																(double) (location.getBlockZ() + finalZ)),
														player),
												location);
										break;
									}
								}
							}
						}
					});
		});
	}

	public CompletableFuture<Void> generateDefaultHellblock(@NotNull World world, @NotNull Location location,
			@NotNull Player player) {
		return CompletableFuture.runAsync(() -> {
			Map<Block, Material> blockChanges = new LinkedHashMap<>();
			int x = location.getBlockX();
			int z = location.getBlockZ();
			Block block = world.getBlockAt(location);
			blockChanges.put(block, Material.BEDROCK);
			int y = location.getBlockY() + 4;

			int operateX;
			int operateZ;
			for (operateX = x - 3; operateX <= x + 3; ++operateX) {
				for (operateZ = z - 3; operateZ <= z + 3; ++operateZ) {
					block = world.getBlockAt(operateX, y, operateZ);
					blockChanges.put(block, Material.SOUL_SAND);
				}
			}

			block = world.getBlockAt(x - 3, y, z + 3);
			blockChanges.put(block, Material.AIR);
			block = world.getBlockAt(x - 3, y, z - 3);
			blockChanges.put(block, Material.AIR);
			block = world.getBlockAt(x + 3, y, z - 3);
			blockChanges.put(block, Material.AIR);
			block = world.getBlockAt(x + 3, y, z + 3);
			blockChanges.put(block, Material.AIR);
			y = location.getBlockY() + 3;

			for (operateX = x - 2; operateX <= x + 2; ++operateX) {
				for (operateZ = z - 2; operateZ <= z + 2; ++operateZ) {
					block = world.getBlockAt(operateX, y, operateZ);
					blockChanges.put(block, Material.SOUL_SAND);
				}
			}

			block = world.getBlockAt(x - 3, y, z);
			blockChanges.put(block, Material.SOUL_SAND);
			block = world.getBlockAt(x + 3, y, z);
			blockChanges.put(block, Material.SOUL_SAND);
			block = world.getBlockAt(x, y, z - 3);
			blockChanges.put(block, Material.SOUL_SAND);
			block = world.getBlockAt(x, y, z + 3);
			blockChanges.put(block, Material.SOUL_SAND);
			block = world.getBlockAt(x, y, z);
			blockChanges.put(block, Material.GRASS_BLOCK);
			y = location.getBlockY() + 2;

			for (operateX = x - 1; operateX <= x + 1; ++operateX) {
				for (operateZ = z - 1; operateZ <= z + 1; ++operateZ) {
					block = world.getBlockAt(operateX, y, operateZ);
					blockChanges.put(block, Material.SOUL_SAND);
				}
			}

			block = world.getBlockAt(x - 2, y, z);
			blockChanges.put(block, Material.SOUL_SAND);
			block = world.getBlockAt(x + 2, y, z);
			blockChanges.put(block, Material.SOUL_SAND);
			block = world.getBlockAt(x, y, z - 2);
			blockChanges.put(block, Material.SOUL_SAND);
			block = world.getBlockAt(x, y, z + 2);
			blockChanges.put(block, Material.SOUL_SAND);
			block = world.getBlockAt(x, y, z);
			blockChanges.put(block, Material.DIRT);
			y = location.getBlockY() + 1;
			block = world.getBlockAt(x - 1, y, z);
			blockChanges.put(block, Material.SOUL_SAND);
			block = world.getBlockAt(x + 1, y, z);
			blockChanges.put(block, Material.SOUL_SAND);
			block = world.getBlockAt(x, y, z - 1);
			blockChanges.put(block, Material.SOUL_SAND);
			block = world.getBlockAt(x, y, z + 1);
			blockChanges.put(block, Material.SOUL_SAND);
			block = world.getBlockAt(x, y, z);
			blockChanges.put(block, Material.DIRT);
			instance.getScheduler().executeSync(() -> blockChanges.forEach((change, type) -> change.setType(type)),
					location);
			y = (int) (location.getY() + 5.0D);
			final int chestY = y;
			this.generateGlowstoneTree(world, new Location(world, (double) x, (double) y, (double) z), player)
					.thenRun(() -> {
						instance.getScheduler()
								.executeSync(() -> this.generateChest(world,
										new Location(world, (double) x, (double) chestY, (double) (z + 1)), player),
										location);
					});
		});
	}

	public CompletableFuture<Void> generateClassicHellblock(@NotNull World world, @NotNull Location location,
			@NotNull Player player) {
		return CompletableFuture.runAsync(() -> {
			Map<Block, Material> blockChanges = new LinkedHashMap<>();
			int x = location.getBlockX();
			int y = location.getBlockY();
			int z = location.getBlockZ();
			Block block = world.getBlockAt(location);
			blockChanges.put(block, Material.SOUL_SAND);

			int operateX;
			int operateZ;
			for (operateX = x - 5; operateX <= x; ++operateX) {
				for (operateZ = z - 2; operateZ <= z; ++operateZ) {
					block = world.getBlockAt(operateX, y, operateZ);
					blockChanges.put(block, Material.SOUL_SAND);
				}
			}

			for (operateX = x - 2; operateX <= x; ++operateX) {
				for (operateZ = z - 5; operateZ <= z; ++operateZ) {
					block = world.getBlockAt(operateX, y, operateZ);
					blockChanges.put(block, Material.SOUL_SAND);
				}
			}

			y = location.getBlockY() + 1;

			for (operateX = x - 5; operateX <= x; ++operateX) {
				for (operateZ = z - 2; operateZ <= z; ++operateZ) {
					block = world.getBlockAt(operateX, y, operateZ);
					blockChanges.put(block, Material.SOUL_SAND);
				}
			}

			for (operateX = x - 2; operateX <= x; ++operateX) {
				for (operateZ = z - 5; operateZ <= z; ++operateZ) {
					block = world.getBlockAt(operateX, y, operateZ);
					blockChanges.put(block, Material.SOUL_SAND);
				}
			}

			y = location.getBlockY() + 2;

			for (operateX = x - 5; operateX <= x; ++operateX) {
				for (operateZ = z - 2; operateZ <= z; ++operateZ) {
					block = world.getBlockAt(operateX, y, operateZ);
					blockChanges.put(block, Material.SOUL_SAND);
				}
			}

			for (operateX = x - 2; operateX <= x; ++operateX) {
				for (operateZ = z - 5; operateZ <= z; ++operateZ) {
					block = world.getBlockAt(operateX, y, operateZ);
					blockChanges.put(block, Material.SOUL_SAND);
				}
			}

			y = location.getBlockY() + 1;
			block = world.getBlockAt(x - 1, y, z - 1);
			blockChanges.put(block, Material.GRASS_BLOCK);
			block = world.getBlockAt(x - 3, y, z - 1);
			blockChanges.put(block, Material.SOUL_SAND);
			block = world.getBlockAt(x - 4, y, z - 1);
			blockChanges.put(block, Material.DIRT);
			block = world.getBlockAt(x - 1, y, z - 3);
			blockChanges.put(block, Material.SOUL_SAND);
			block = world.getBlockAt(x - 1, y, z - 4);
			blockChanges.put(block, Material.DIRT);
			block = world.getBlockAt(x - 1, y - 1, z - 1);
			blockChanges.put(block, Material.BEDROCK);
			instance.getScheduler().executeSync(() -> blockChanges.forEach((change, type) -> change.setType(type)),
					location);
			y = location.getBlockY() + 3;
			final int chestY = y;
			this.generateGlowstoneTree(world, new Location(world, (double) x, (double) y, (double) (z - 5)), player)
					.thenRun(() -> {
						instance.getScheduler().executeSync(() -> this.generateChest(world,
								new Location(world, (double) (x - 5), (double) chestY, (double) (z - 1)), player),
								location);
					});
		});
	}

	public CompletableFuture<Void> generateGlowstoneTree(@NotNull World world, @NotNull Location location,
			@NotNull Player player) {
		return CompletableFuture.runAsync(() -> {
			Map<Block, Material> blockChanges = new LinkedHashMap<>();
			int x = location.getBlockX();
			int y = location.getBlockY();
			int z = location.getBlockZ();
			Block block = world.getBlockAt(x, y, z);
			if (block.getType().isAir() || Tag.SAPLINGS.isTagged(block.getType()))
				blockChanges.put(block, Material.GRAVEL);
			block = world.getBlockAt(x, y + 1, z);
			if (block.getType().isAir())
				blockChanges.put(block, Material.GRAVEL);
			block = world.getBlockAt(x, y + 2, z);
			if (block.getType().isAir())
				blockChanges.put(block, Material.GRAVEL);
			y = location.getBlockY() + 3;

			int operateX;
			int operateZ;
			for (operateX = x - 2; operateX <= x + 2; ++operateX) {
				for (operateZ = z - 2; operateZ <= z + 2; ++operateZ) {
					block = world.getBlockAt(operateX, y, operateZ);
					if (!block.getType().isAir())
						continue;
					blockChanges.put(block, Material.GLOWSTONE);
				}
			}

			block = world.getBlockAt(x + 2, y, z + 2);
			if (block.getType() == Material.GLOWSTONE || block.getType().isAir())
				blockChanges.put(block, Material.AIR);
			block = world.getBlockAt(x + 2, y, z - 2);
			if (block.getType() == Material.GLOWSTONE || block.getType().isAir())
				blockChanges.put(block, Material.AIR);
			block = world.getBlockAt(x - 2, y, z + 2);
			if (block.getType() == Material.GLOWSTONE || block.getType().isAir())
				blockChanges.put(block, Material.AIR);
			block = world.getBlockAt(x - 2, y, z - 2);
			if (block.getType() == Material.GLOWSTONE || block.getType().isAir())
				blockChanges.put(block, Material.AIR);
			block = world.getBlockAt(x, y, z);
			if (block.getType() == Material.GLOWSTONE || block.getType().isAir())
				blockChanges.put(block, Material.GRAVEL);
			y = location.getBlockY() + 4;

			for (operateX = x - 1; operateX <= x + 1; ++operateX) {
				for (operateZ = z - 1; operateZ <= z + 1; ++operateZ) {
					block = world.getBlockAt(operateX, y, operateZ);
					if (!block.getType().isAir())
						continue;
					blockChanges.put(block, Material.GLOWSTONE);
				}
			}

			block = world.getBlockAt(x - 2, y, z);
			if (block.getType().isAir())
				blockChanges.put(block, Material.GLOWSTONE);
			block = world.getBlockAt(x + 2, y, z);
			if (block.getType().isAir())
				blockChanges.put(block, Material.GLOWSTONE);
			block = world.getBlockAt(x, y, z - 2);
			if (block.getType().isAir())
				blockChanges.put(block, Material.GLOWSTONE);
			block = world.getBlockAt(x, y, z + 2);
			if (block.getType().isAir())
				blockChanges.put(block, Material.GLOWSTONE);
			block = world.getBlockAt(x, y, z);
			if (block.getType() == Material.GLOWSTONE || block.getType().isAir())
				blockChanges.put(block, Material.GRAVEL);
			y = location.getBlockY() + 5;
			block = world.getBlockAt(x - 1, y, z);
			if (block.getType().isAir())
				blockChanges.put(block, Material.GLOWSTONE);
			block = world.getBlockAt(x + 1, y, z);
			if (block.getType().isAir())
				blockChanges.put(block, Material.GLOWSTONE);
			block = world.getBlockAt(x, y, z - 1);
			if (block.getType().isAir())
				blockChanges.put(block, Material.GLOWSTONE);
			block = world.getBlockAt(x, y, z + 1);
			if (block.getType().isAir())
				blockChanges.put(block, Material.GLOWSTONE);
			block = world.getBlockAt(x, y, z);
			if (block.getType() == Material.GLOWSTONE || block.getType().isAir())
				blockChanges.put(block, Material.GRAVEL);
			block = world.getBlockAt(x, y + 1, z);
			if (block.getType().isAir())
				blockChanges.put(block, Material.GLOWSTONE);
			instance.getScheduler().executeSync(() -> blockChanges.forEach((change, type) -> change.setType(type)),
					location);
		});
	}

	public void generateChest(@NotNull World world, @NotNull Location location, @NotNull Player player) {
		int x = location.getBlockX();
		int y = location.getBlockY();
		int z = location.getBlockZ();
		Block block = world.getBlockAt(x, y, z);
		block.setType(Material.CHEST);
		Optional<UserData> onlineUser = instance.getStorageManager().getOnlineUser(player.getUniqueId());
		Directional directional = (Directional) block.getBlockData();
		if (!onlineUser.isEmpty())
			directional.setFacing(getChestDirection(location, onlineUser.get().getHellblockData().getIslandChoice()));
		block.setBlockData(directional);
		Chest chest = (Chest) block.getState();
		String chestName = instance.getConfigManager().chestName();
		Context<Player> context = Context.player(player);
		if (chestName != null && !chestName.isEmpty() && chestName.length() <= 35) {
			RtagBlock chestTag = new RtagBlock(block);
			TextValue<Player> customChestName = TextValue.auto("<!i><black>" + chestName);
			chestTag.setCustomName(
					AdventureHelper.componentToJson(AdventureHelper.miniMessage(customChestName.render(context))));
			chest = (Chest) chestTag.load().getState();
		}
		Inventory inventory = chest.getInventory();
		inventory.clear();
		chest.update();

		for (Pair<Integer, CustomItem> entry : instance.getConfigManager().chestItems().values()) {
			ItemStack data = setChestData(entry.right().build(context), true);
			int slot = entry.left();
			if (slot >= 0 && slot <= 26)
				inventory.setItem(slot, data);
			else {
				if (inventory.firstEmpty() != -1)
					inventory.addItem(data);
				else
					continue;
			}
		}
	}

	private static final BlockFace[] FACES = new BlockFace[] { BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST,
			BlockFace.WEST };

	private @NotNull BlockFace getChestDirection(@NotNull Location location, @NotNull IslandOptions option) {

		BlockFace finalFace = BlockFace.SOUTH;
		for (BlockFace face : FACES) {
			if (location.getBlock().getRelative(face).getType() != Material.AIR)
				continue;
			if (location.getBlock().getRelative(face).getRelative(BlockFace.DOWN).getType() == Material.AIR)
				continue;
			finalFace = face;
			break;
		}

		return option == IslandOptions.CLASSIC ? BlockFace.EAST
				: option == IslandOptions.DEFAULT ? BlockFace.SOUTH : finalFace;
	}

	public boolean checkChestData(@Nullable ItemStack item) {
		if (item == null || item.getType() == Material.AIR)
			return false;

		return new RtagItem(item).hasTag("HellblockChest", "isStarterChestItem");
	}

	public boolean getChestData(@Nullable ItemStack item) {
		if (item == null || item.getType() == Material.AIR)
			return false;

		return new RtagItem(item).getOptional("HellblockChest", "isStarterChestItem").asBoolean();
	}

	public @Nullable ItemStack setChestData(@Nullable ItemStack item, boolean data) {
		if (item == null || item.getType() == Material.AIR)
			return null;

		return RtagItem.edit(item, tag -> {
			tag.set(data, "HellblockChest", "isStarterChestItem");
		});
	}
}