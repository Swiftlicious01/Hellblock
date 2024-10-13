package com.swiftlicious.hellblock.generation;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import com.jeff_media.customblockdata.CustomBlockData;
import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.playerdata.HellblockPlayer;
import com.swiftlicious.hellblock.utils.LogUtils;

public class IslandGenerator {

	private final HellblockPlugin instance;

	public IslandGenerator(HellblockPlugin plugin) {
		this.instance = plugin;
	}

	public void generateHellblock(Location location, Player player) {
		HellblockPlayer pi = (HellblockPlayer) instance.getHellblockHandler().getActivePlayers()
				.get(player.getUniqueId());
		boolean generatedIsland = false;
		if (instance.getHellblockHandler().getSchematics().length > 0
				&& instance.getWorldEditHandler().getWorldEdit() != null) {
			String schematic = "";

			int i;
			int x;
			int y;
			int z;
			for (i = 0; i < instance.getHellblockHandler().getSchematics().length; ++i) {
				if (!generatedIsland) {
					if (instance.getHellblockHandler().getSchematics()[i].getName().lastIndexOf(46) > 0) {
						schematic = instance.getHellblockHandler().getSchematics()[i].getName().substring(0,
								instance.getHellblockHandler().getSchematics()[i].getName().lastIndexOf(46));
					} else {
						schematic = instance.getHellblockHandler().getSchematics()[i].getName();
					}

					if (player.hasPermission("hellblock.schematic." + schematic)) {
						try {
							if (instance.getWorldEditHandler().loadIslandSchematic(
									instance.getHellblockHandler().getHellblockWorld(),
									instance.getHellblockHandler().getSchematics()[i])) {
								for (x = -15; x <= 15; ++x) {
									for (y = -15; y <= 15; ++y) {
										for (z = -15; z <= 15; ++z) {
											if (instance.getHellblockHandler().getHellblockWorld()
													.getBlockAt(location.getBlockX() + x, location.getBlockY() + y,
															location.getBlockZ() + z)
													.getType() == Material.CHEST) {
												this.generateChest(
														new Location(instance.getHellblockHandler().getHellblockWorld(),
																(double) (location.getBlockX() + x),
																(double) (location.getBlockY() + y),
																(double) (location.getBlockZ() + z)));
											}
										}
									}
								}

								generatedIsland = true;
								pi.setHome(location);
							}
						} catch (Exception var13) {
							LogUtils.severe("An error occured while pasting hellblock schematic.", var13);
						}
					}
				}
			}

			if (!generatedIsland) {
				for (i = 0; i < instance.getHellblockHandler().getSchematics().length; ++i) {
					if (instance.getHellblockHandler().getSchematics()[i].getName().lastIndexOf(46) > 0) {
						schematic = instance.getHellblockHandler().getSchematics()[i].getName().substring(0,
								instance.getHellblockHandler().getSchematics()[i].getName().lastIndexOf(46));
					} else {
						schematic = instance.getHellblockHandler().getSchematics()[i].getName();
					}

					if (schematic.equalsIgnoreCase(instance.getHellblockHandler().getSchematic())) {
						try {
							if (instance.getWorldEditHandler().loadIslandSchematic(
									instance.getHellblockHandler().getHellblockWorld(),
									instance.getHellblockHandler().getSchematics()[i])) {
								for (x = -15; x <= 15; ++x) {
									for (y = -15; y <= 15; ++y) {
										for (z = -15; z <= 15; ++z) {
											if (instance.getHellblockHandler().getHellblockWorld()
													.getBlockAt(location.getBlockX() + x, location.getBlockY() + y,
															location.getBlockZ() + z)
													.getType() == Material.CHEST) {
												this.generateChest(
														new Location(instance.getHellblockHandler().getHellblockWorld(),
																(double) (location.getBlockX() + x),
																(double) (location.getBlockY() + y),
																(double) (location.getBlockZ() + z)));
											}
										}
									}
								}

								generatedIsland = true;
								pi.setHome(location);
							}
						} catch (Exception var12) {
							LogUtils.severe("An error occured while pasting hellblock schematic.", var12);
						}
					}
				}
			}
		}

		if (!generatedIsland) {
			World world = location.getWorld();
			double x = location.getX();
			double y = location.getY();
			double z = location.getZ();
			if (!instance.getHellblockHandler().isClassicShape()) {
				this.generateHellblock(location);
				pi.setHome(new Location(world, x, y + 6.0D, z - 1.0D));
			} else {
				this.generateClassicHellblock(location);
				pi.setHome(new Location(world, x - 4.0D, y + 3.0D, z - 1.0D));
			}
		}

	}

	public void generateHellblock(Location location) {
		World world = location.getWorld();
		int x = (int) location.getX();
		int z = (int) location.getZ();
		Block block = world.getBlockAt(location);
		block.setType(Material.BEDROCK);
		int y = (int) (location.getY() + 4.0D);

		int x_operate;
		int z_operate;
		for (x_operate = x - 3; x_operate <= x + 3; ++x_operate) {
			for (z_operate = z - 3; z_operate <= z + 3; ++z_operate) {
				block = world.getBlockAt(x_operate, y, z_operate);
				block.setType(Material.SOUL_SAND);
			}
		}

		block = world.getBlockAt(x - 3, y, z + 3);
		block.setType(Material.AIR);
		block = world.getBlockAt(x - 3, y, z - 3);
		block.setType(Material.AIR);
		block = world.getBlockAt(x + 3, y, z - 3);
		block.setType(Material.AIR);
		block = world.getBlockAt(x + 3, y, z + 3);
		block.setType(Material.AIR);
		y = (int) (location.getY() + 3.0D);

		for (x_operate = x - 2; x_operate <= x + 2; ++x_operate) {
			for (z_operate = z - 2; z_operate <= z + 2; ++z_operate) {
				block = world.getBlockAt(x_operate, y, z_operate);
				block.setType(Material.SOUL_SAND);
			}
		}

		block = world.getBlockAt(x - 3, y, z);
		block.setType(Material.SOUL_SAND);
		block = world.getBlockAt(x + 3, y, z);
		block.setType(Material.SOUL_SAND);
		block = world.getBlockAt(x, y, z - 3);
		block.setType(Material.SOUL_SAND);
		block = world.getBlockAt(x, y, z + 3);
		block.setType(Material.SOUL_SAND);
		block = world.getBlockAt(x, y, z);
		block.setType(Material.GRASS_BLOCK);
		y = (int) (location.getY() + 2.0D);

		for (x_operate = x - 1; x_operate <= x + 1; ++x_operate) {
			for (z_operate = z - 1; z_operate <= z + 1; ++z_operate) {
				block = world.getBlockAt(x_operate, y, z_operate);
				block.setType(Material.SOUL_SAND);
			}
		}

		block = world.getBlockAt(x - 2, y, z);
		block.setType(Material.SOUL_SAND);
		block = world.getBlockAt(x + 2, y, z);
		block.setType(Material.SOUL_SAND);
		block = world.getBlockAt(x, y, z - 2);
		block.setType(Material.SOUL_SAND);
		block = world.getBlockAt(x, y, z + 2);
		block.setType(Material.SOUL_SAND);
		block = world.getBlockAt(x, y, z);
		block.setType(Material.DIRT);
		y = (int) (location.getY() + 1.0D);
		block = world.getBlockAt(x - 1, y, z);
		block.setType(Material.SOUL_SAND);
		block = world.getBlockAt(x + 1, y, z);
		block.setType(Material.SOUL_SAND);
		block = world.getBlockAt(x, y, z - 1);
		block.setType(Material.SOUL_SAND);
		block = world.getBlockAt(x, y, z + 1);
		block.setType(Material.SOUL_SAND);
		block = world.getBlockAt(x, y, z);
		block.setType(Material.DIRT);
		y = (int) (location.getY() + 5.0D);
		this.generateTree(new Location(world, (double) x, (double) y, (double) z));
		this.generateChest(new Location(world, (double) x, (double) y, (double) (z + 1)));
	}

	public void generateClassicHellblock(Location location) {
		World world = location.getWorld();
		int x = (int) location.getX();
		int y = (int) location.getY();
		int z = (int) location.getZ();
		Block block = world.getBlockAt(location);
		block.setType(Material.SOUL_SAND);

		int x_operate;
		int z_operate;
		for (x_operate = x - 5; x_operate <= x; ++x_operate) {
			for (z_operate = z - 2; z_operate <= z; ++z_operate) {
				block = world.getBlockAt(x_operate, y, z_operate);
				block.setType(Material.SOUL_SAND);
			}
		}

		for (x_operate = x - 2; x_operate <= x; ++x_operate) {
			for (z_operate = z - 5; z_operate <= z; ++z_operate) {
				block = world.getBlockAt(x_operate, y, z_operate);
				block.setType(Material.SOUL_SAND);
			}
		}

		y = (int) location.getY() + 1;

		for (x_operate = x - 5; x_operate <= x; ++x_operate) {
			for (z_operate = z - 2; z_operate <= z; ++z_operate) {
				block = world.getBlockAt(x_operate, y, z_operate);
				block.setType(Material.SOUL_SAND);
			}
		}

		for (x_operate = x - 2; x_operate <= x; ++x_operate) {
			for (z_operate = z - 5; z_operate <= z; ++z_operate) {
				block = world.getBlockAt(x_operate, y, z_operate);
				block.setType(Material.SOUL_SAND);
			}
		}

		y = (int) location.getY() + 2;

		for (x_operate = x - 5; x_operate <= x; ++x_operate) {
			for (z_operate = z - 2; z_operate <= z; ++z_operate) {
				block = world.getBlockAt(x_operate, y, z_operate);
				block.setType(Material.SOUL_SAND);
			}
		}

		for (x_operate = x - 2; x_operate <= x; ++x_operate) {
			for (z_operate = z - 5; z_operate <= z; ++z_operate) {
				block = world.getBlockAt(x_operate, y, z_operate);
				block.setType(Material.SOUL_SAND);
			}
		}

		y = (int) location.getY() + 1;
		block = world.getBlockAt(x - 1, y, z - 1);
		block.setType(Material.GRASS_BLOCK);
		block = world.getBlockAt(x - 3, y, z - 1);
		block.setType(Material.SOUL_SAND);
		block = world.getBlockAt(x - 4, y, z - 1);
		block.setType(Material.DIRT);
		block = world.getBlockAt(x - 1, y, z - 3);
		block.setType(Material.SOUL_SAND);
		block = world.getBlockAt(x - 1, y, z - 4);
		block.setType(Material.DIRT);
		block = world.getBlockAt(x - 1, y - 1, z - 1);
		block.setType(Material.BEDROCK);
		y = (int) location.getY() + 3;
		this.generateTree(new Location(world, (double) x, (double) y, (double) (z - 5)));
		this.generateChest(new Location(world, (double) (x - 5), (double) y, (double) (z - 1)));
	}

	public void generateTree(Location location) {
		World world = location.getWorld();
		int x = (int) location.getX();
		int y = (int) location.getY();
		int z = (int) location.getZ();
		Block block = world.getBlockAt(x, y, z);
		PersistentDataContainer customBlockData = new CustomBlockData(block, instance);
		NamespacedKey key = instance.getGlowstoneTree().getGlowTreeKey();
		customBlockData.set(key, PersistentDataType.BOOLEAN, true);
		block.setType(Material.GRAVEL);
		block = world.getBlockAt(x, y + 1, z);
		customBlockData = new CustomBlockData(block, instance);
		customBlockData.set(key, PersistentDataType.BOOLEAN, true);
		block.setType(Material.GRAVEL);
		block = world.getBlockAt(x, y + 2, z);
		customBlockData = new CustomBlockData(block, instance);
		customBlockData.set(key, PersistentDataType.BOOLEAN, true);
		block.setType(Material.GRAVEL);
		y = (int) (location.getY() + 3.0D);

		int x_operate;
		int z_operate;
		for (x_operate = x - 2; x_operate <= x + 2; ++x_operate) {
			for (z_operate = z - 2; z_operate <= z + 2; ++z_operate) {
				block = world.getBlockAt(x_operate, y, z_operate);
				block.setType(Material.GLOWSTONE);
			}
		}

		block = world.getBlockAt(x + 2, y, z + 2);
		block.setType(Material.AIR);
		block = world.getBlockAt(x + 2, y, z - 2);
		block.setType(Material.AIR);
		block = world.getBlockAt(x - 2, y, z + 2);
		block.setType(Material.AIR);
		block = world.getBlockAt(x - 2, y, z - 2);
		block.setType(Material.AIR);
		block = world.getBlockAt(x, y, z);
		customBlockData = new CustomBlockData(block, instance);
		customBlockData.set(key, PersistentDataType.BOOLEAN, true);
		block.setType(Material.GRAVEL);
		y = (int) (location.getY() + 4.0D);

		for (x_operate = x - 1; x_operate <= x + 1; ++x_operate) {
			for (z_operate = z - 1; z_operate <= z + 1; ++z_operate) {
				block = world.getBlockAt(x_operate, y, z_operate);
				block.setType(Material.GLOWSTONE);
			}
		}

		block = world.getBlockAt(x - 2, y, z);
		block.setType(Material.GLOWSTONE);
		block = world.getBlockAt(x + 2, y, z);
		block.setType(Material.GLOWSTONE);
		block = world.getBlockAt(x, y, z - 2);
		block.setType(Material.GLOWSTONE);
		block = world.getBlockAt(x, y, z + 2);
		block.setType(Material.GLOWSTONE);
		block = world.getBlockAt(x, y, z);
		customBlockData = new CustomBlockData(block, instance);
		customBlockData.set(key, PersistentDataType.BOOLEAN, true);
		block.setType(Material.GRAVEL);
		y = (int) (location.getY() + 5.0D);
		block = world.getBlockAt(x - 1, y, z);
		block.setType(Material.GLOWSTONE);
		block = world.getBlockAt(x + 1, y, z);
		block.setType(Material.GLOWSTONE);
		block = world.getBlockAt(x, y, z - 1);
		block.setType(Material.GLOWSTONE);
		block = world.getBlockAt(x, y, z + 1);
		block.setType(Material.GLOWSTONE);
		block = world.getBlockAt(x, y, z);
		customBlockData = new CustomBlockData(block, instance);
		customBlockData.set(key, PersistentDataType.BOOLEAN, true);
		block.setType(Material.GRAVEL);
		block = world.getBlockAt(x, y + 1, z);
		block.setType(Material.GLOWSTONE);
	}

	public void generateChest(Location location) {
		World world = location.getWorld();
		int x = (int) location.getX();
		int y = (int) location.getY();
		int z = (int) location.getZ();
		Block block = world.getBlockAt(x, y, z);
		block.setType(Material.CHEST);
		Directional directional = (Directional) block.getBlockData();
		directional.setFacing(instance.getHellblockHandler().isClassicShape() ? BlockFace.EAST : BlockFace.SOUTH);
		block.setBlockData(directional);
		Chest chest = (Chest) block.getState();
		Inventory inventory = chest.getInventory();
		inventory.clear();
		List<ItemStack> items = new ArrayList<>();
		ItemStack[] var13;
		int var12 = (var13 = instance.getHellblockHandler()
				.parseItems(instance.getHellblockHandler().getChestItems().toArray(new String[0]))).length;

		for (int var11 = 0; var11 < var12; ++var11) {
			ItemStack item = var13[var11];
			if (items.size() < 27) {
				items.add(item);
			}
		}

		inventory.setContents((ItemStack[]) items.toArray(new ItemStack[items.size()]));
	}
}
