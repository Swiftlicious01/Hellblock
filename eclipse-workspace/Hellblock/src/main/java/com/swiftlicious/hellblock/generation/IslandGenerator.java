package com.swiftlicious.hellblock.generation;

import java.util.List;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.block.data.Directional;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.saicone.rtag.RtagItem;
import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.playerdata.HellblockPlayer;
import com.swiftlicious.hellblock.utils.LogUtils;
import com.swiftlicious.hellblock.utils.wrappers.ShadedAdventureComponentWrapper;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.builder.PotionBuilder;

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
		block.setType(Material.GRAVEL);
		block = world.getBlockAt(x, y + 1, z);
		block.setType(Material.GRAVEL);
		block = world.getBlockAt(x, y + 2, z);
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

		final Registry<Enchantment> enchantmentRegistry = RegistryAccess.registryAccess()
				.getRegistry(RegistryKey.ENCHANTMENT);

		final Registry<PotionEffectType> potionRegistry = RegistryAccess.registryAccess()
				.getRegistry(RegistryKey.MOB_EFFECT);

		for (String path : instance.getConfig("config.yml").getConfigurationSection("hellblock.starter-chest")
				.getKeys(false)) {
			try {
				String material = instance.getConfig("config.yml")
						.getString("hellblock.starter-chest." + path + ".material", "AIR");
				int amount = instance.getConfig("config.yml").getInt("hellblock.starter-chest." + path + ".amount", 1);
				int slot = instance.getConfig("config.yml").getInt("hellblock.starter-chest." + path + ".slot");
				ItemBuilder item = new ItemBuilder(Material.getMaterial(material.toUpperCase()), amount);

				String displayName = instance.getConfig("config.yml")
						.getString("hellblock.starter-chest." + path + ".name");
				if (displayName != null)
					item.setDisplayName(new ShadedAdventureComponentWrapper(
							instance.getAdventureManager().getComponentFromMiniMessage(displayName)));

				List<String> lore = instance.getConfig("config.yml")
						.getStringList("hellblock.starter-chest." + path + ".lore");
				for (String newLore : lore) {
					item.addLoreLines(new ShadedAdventureComponentWrapper(
							instance.getAdventureManager().getComponentFromMiniMessage(newLore)));
				}

				List<String> enchantments = instance.getConfig("config.yml")
						.getStringList("hellblock.starter-chest." + path + ".enchantments");
				for (String enchants : enchantments) {
					String[] split = enchants.split(":");
					Enchantment enchantment = enchantmentRegistry.getOrThrow(NamespacedKey.fromString(split[0].toLowerCase()));
					int level = 1;
					try {
						level = Integer.parseInt(split[1]);
					} catch (NumberFormatException ex) {
						LogUtils.severe("Invalid quantity: " + split[1] + "!");
					}
					item.addEnchantment(enchantment, level, false);
				}

				int durability = instance.getConfig("config.yml")
						.getInt("hellblock.starter-chest." + path + ".durability");
				item.setDamage(durability);

				if (Material.getMaterial(material) == Material.POTION) {
					PotionBuilder pm = new PotionBuilder(item.get());
					String potion = instance.getConfig("config.yml")
							.getString("hellblock.starter-chest." + path + ".potion.effect");
					int duration = instance.getConfig("config.yml")
							.getInt("hellblock.starter-chest." + path + ".potion.duration");
					int amplifier = instance.getConfig("config.yml")
							.getInt("hellblock.starter-chest." + path + ".potion.amplifier");
					PotionEffectType potionType = potion != null
							? potionRegistry.getOrThrow(NamespacedKey.fromString(potion.toUpperCase()))
							: PotionEffectType.FIRE_RESISTANCE;
					PotionEffect effect = new PotionEffect(potionType, duration, amplifier);
					pm.addEffect(effect);
					Map<String, Object> data = Map.of("HellblockChest", List.of(Map.of("starterChestItem", true)));
					RtagItem tagItem = new RtagItem(pm.get());
					tagItem.set(data);
					inventory.setItem(slot, tagItem.load());
					return;
				}
				
				Map<String, Object> data = Map.of("HellblockChest", List.of(Map.of("starterChestItem", true)));
				RtagItem tagItem = new RtagItem(item.get());
				tagItem.set(data);

				inventory.setItem(slot, tagItem.load());
			} catch (Exception ex) {
				LogUtils.severe("Unable to create the defined item for the starter chest: "
						+ instance.getConfig("config.yml").getString("hellblock.starter-chest." + path + ".material"),
						ex);
			}
		}
	}
}
