package com.swiftlicious.hellblock.generation;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.FireworkEffect;
import org.bukkit.FireworkEffect.Type;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Tag;
import org.bukkit.TreeType;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.banner.PatternType;
import org.bukkit.block.data.Directional;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.Nullable;

import com.saicone.rtag.RtagItem;
import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.listeners.GlowstoneTree.GlowTree;
import com.swiftlicious.hellblock.playerdata.HellblockPlayer;
import com.swiftlicious.hellblock.utils.LocationCache;
import com.swiftlicious.hellblock.utils.LogUtils;
import com.swiftlicious.hellblock.utils.wrappers.ShadedAdventureComponentWrapper;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import lombok.NonNull;
import xyz.xenondevs.invui.item.builder.BannerBuilder;
import xyz.xenondevs.invui.item.builder.FireworkBuilder;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.builder.PotionBuilder;
import xyz.xenondevs.invui.item.builder.SkullBuilder;

public class IslandGenerator {

	private final HellblockPlugin instance;

	public IslandGenerator(HellblockPlugin plugin) {
		this.instance = plugin;
	}

	public void generateHellblockSchematic(Location location, Player player) {
		HellblockPlayer pi = instance.getHellblockHandler().getActivePlayer(player);
		boolean generatedIsland = false;
		if (instance.getHellblockHandler().getSchematics().length > 0
				&& instance.getWorldEditHandler().getWorldEdit() != null) {
			String schematic = "";

			int i, x, y, z;
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
							LogUtils.severe("An error occurred while pasting hellblock schematic.", var13);
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

					if (instance.getHellblockHandler().getIslandOptions().contains(schematic)) {
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
							LogUtils.severe("An error occurred while pasting hellblock schematic.", var12);
						}
					}
				}
			}
		}
	}

	public void generateDefaultHellblock(Location location) {
		World world = location.getWorld();
		if (world == null) {
			LogUtils.severe("An error occurred while generating default hellblock island.");
			return;
		}
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
		if (world == null) {
			LogUtils.severe("An error occurred while generating classic hellblock island.");
			return;
		}
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
		if (world == null) {
			LogUtils.severe("An error occurred while generating glowstone tree.");
			return;
		}
		int x = (int) location.getX();
		int y = (int) location.getY();
		int z = (int) location.getZ();
		List<BlockState> glowTreeStates = new ArrayList<>();
		Block block = world.getBlockAt(x, y, z);
		block.setType(Material.GRAVEL);
		glowTreeStates.add(block.getState());
		block = world.getBlockAt(x, y + 1, z);
		block.setType(Material.GRAVEL);
		glowTreeStates.add(block.getState());
		block = world.getBlockAt(x, y + 2, z);
		block.setType(Material.GRAVEL);
		glowTreeStates.add(block.getState());
		y = (int) (location.getY() + 3.0D);

		int x_operate;
		int z_operate;
		for (x_operate = x - 2; x_operate <= x + 2; ++x_operate) {
			for (z_operate = z - 2; z_operate <= z + 2; ++z_operate) {
				block = world.getBlockAt(x_operate, y, z_operate);
				block.setType(Material.GLOWSTONE);
				glowTreeStates.add(block.getState());
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
		glowTreeStates.add(block.getState());
		y = (int) (location.getY() + 4.0D);

		for (x_operate = x - 1; x_operate <= x + 1; ++x_operate) {
			for (z_operate = z - 1; z_operate <= z + 1; ++z_operate) {
				block = world.getBlockAt(x_operate, y, z_operate);
				block.setType(Material.GLOWSTONE);
				glowTreeStates.add(block.getState());
			}
		}

		block = world.getBlockAt(x - 2, y, z);
		block.setType(Material.GLOWSTONE);
		glowTreeStates.add(block.getState());
		block = world.getBlockAt(x + 2, y, z);
		block.setType(Material.GLOWSTONE);
		glowTreeStates.add(block.getState());
		block = world.getBlockAt(x, y, z - 2);
		block.setType(Material.GLOWSTONE);
		glowTreeStates.add(block.getState());
		block = world.getBlockAt(x, y, z + 2);
		block.setType(Material.GLOWSTONE);
		glowTreeStates.add(block.getState());
		block = world.getBlockAt(x, y, z);
		block.setType(Material.GRAVEL);
		glowTreeStates.add(block.getState());
		y = (int) (location.getY() + 5.0D);
		block = world.getBlockAt(x - 1, y, z);
		block.setType(Material.GLOWSTONE);
		glowTreeStates.add(block.getState());
		block = world.getBlockAt(x + 1, y, z);
		block.setType(Material.GLOWSTONE);
		glowTreeStates.add(block.getState());
		block = world.getBlockAt(x, y, z - 1);
		block.setType(Material.GLOWSTONE);
		glowTreeStates.add(block.getState());
		block = world.getBlockAt(x, y, z + 1);
		block.setType(Material.GLOWSTONE);
		glowTreeStates.add(block.getState());
		block = world.getBlockAt(x, y, z);
		block.setType(Material.GRAVEL);
		glowTreeStates.add(block.getState());
		block = world.getBlockAt(x, y + 1, z);
		block.setType(Material.GLOWSTONE);
		glowTreeStates.add(block.getState());
		GlowTree glowTree = instance.getGlowstoneTree().new GlowTree(TreeType.TREE, glowTreeStates);
		instance.getGlowstoneTree().glowTreeCache
				.put(LocationCache.getCachedLocation(world.getBlockAt(x, y, z).getLocation()), glowTree);
	}

	public void generateChest(Location location) {
		World world = location.getWorld();
		if (world == null) {
			LogUtils.severe("An error occurred while generating hellblock chest.");
			return;
		}
		int x = (int) location.getX();
		int y = (int) location.getY();
		int z = (int) location.getZ();
		Block block = world.getBlockAt(x, y, z);
		block.setType(Material.CHEST);
		Directional directional = (Directional) block.getBlockData();
		directional.setFacing(getChestDirection(location));
		block.setBlockData(directional);
		Chest chest = (Chest) block.getState();
		String chestName = instance.getConfig("config.yml").getString("hellblock.starter-chest.inventory-name");
		if (chestName != null && !chestName.isEmpty() && chestName.length() <= 35) {
			chest.customName(instance.getAdventureManager().getComponentFromMiniMessage(chestName));
		}
		Inventory inventory = chest.getInventory();
		inventory.clear();
		chest.update();

		final Registry<Enchantment> enchantmentRegistry = RegistryAccess.registryAccess()
				.getRegistry(RegistryKey.ENCHANTMENT);

		final Registry<PotionEffectType> potionRegistry = RegistryAccess.registryAccess()
				.getRegistry(RegistryKey.MOB_EFFECT);

		final Registry<PatternType> bannerRegistry = RegistryAccess.registryAccess()
				.getRegistry(RegistryKey.BANNER_PATTERN);

		for (String path : instance.getConfig("config.yml").getConfigurationSection("hellblock.starter-chest")
				.getKeys(false)) {
			if (path.equals("inventory-name") || !StringUtils.isNumeric(path))
				continue;
			try {
				String material = instance.getConfig("config.yml")
						.getString("hellblock.starter-chest." + path + ".material", "AIR");
				if (material.equalsIgnoreCase("AIR") || material == null || material.isEmpty())
					continue;
				int amount = instance.getConfig("config.yml").getInt("hellblock.starter-chest." + path + ".amount", 1);
				int slot = instance.getConfig("config.yml").getInt("hellblock.starter-chest." + path + ".slot");
				ItemBuilder item = new ItemBuilder(
						Material.getMaterial(material.toUpperCase()) != null
								? Material.getMaterial(material.toUpperCase())
								: Material.AIR,
						amount > 0 && amount <= Material.getMaterial(material.toUpperCase()).getMaxStackSize() ? amount
								: 1);

				if (Tag.AIR.isTagged(item.get().getType()))
					continue;

				String displayName = instance.getConfig("config.yml")
						.getString("hellblock.starter-chest." + path + ".name");
				if (displayName != null && !displayName.isEmpty()) {
					item.setDisplayName(new ShadedAdventureComponentWrapper(
							instance.getAdventureManager().getComponentFromMiniMessage(displayName)));
				}

				List<String> lore = instance.getConfig("config.yml")
						.getStringList("hellblock.starter-chest." + path + ".lore");
				if (lore != null && !lore.isEmpty()) {
					for (String newLore : lore) {
						item.addLoreLines(new ShadedAdventureComponentWrapper(
								instance.getAdventureManager().getComponentFromMiniMessage(newLore)));
					}
				}

				List<String> enchantments = instance.getConfig("config.yml")
						.getStringList("hellblock.starter-chest." + path + ".enchantments");
				if (enchantments != null && !enchantments.isEmpty()) {
					for (String enchants : enchantments) {
						String[] split = enchants.split(":");
						Enchantment enchantment = enchantmentRegistry
								.getOrThrow(NamespacedKey.fromString(split[0].toLowerCase()));
						int level = 1;
						try {
							level = Integer.parseInt(split[1]);
						} catch (NumberFormatException ex) {
							LogUtils.severe(String.format("Invalid quantity: %s!", split[1]));
							continue;
						}
						item.addEnchantment(enchantment, level, false);
					}
				}

				int damage = instance.getConfig("config.yml").getInt("hellblock.starter-chest." + path + ".damage");
				item.setDamage(damage >= 0 && damage <= item.get().getType().getMaxDurability() ? damage : 0);

				int customModelData = instance.getConfig("config.yml")
						.getInt("hellblock.starter-chest." + path + ".custom-model-data");
				item.setCustomModelData(customModelData);

				boolean unbreakable = instance.getConfig("config.yml")
						.getBoolean("hellblock.starter-chest." + path + ".unbreakable", false);
				item.setUnbreakable(unbreakable);

				if (Material.getMaterial(material) == Material.POTION
						|| Material.getMaterial(material) == Material.SPLASH_POTION
						|| Material.getMaterial(material) == Material.LINGERING_POTION) {
					PotionBuilder pm = new PotionBuilder(item.get());
					String potion = instance.getConfig("config.yml")
							.getString("hellblock.starter-chest." + path + ".potion.effect");
					int duration = instance.getConfig("config.yml")
							.getInt("hellblock.starter-chest." + path + ".potion.duration");
					int amplifier = instance.getConfig("config.yml")
							.getInt("hellblock.starter-chest." + path + ".potion.amplifier");
					PotionEffectType potionType = potion != null && !potion.isEmpty()
							? potionRegistry.getOrThrow(NamespacedKey.fromString(potion.toLowerCase()))
							: PotionEffectType.FIRE_RESISTANCE;
					PotionEffect effect = new PotionEffect(potionType, duration * (20 * 60), amplifier);
					pm.addEffect(effect);
					ItemStack data = setChestData(pm.get(), true);
					if (slot >= 0 && slot <= 26) {
						inventory.setItem(slot, data);
					} else {
						inventory.addItem(data);
					}
					continue;
				}

				if (Material.getMaterial(material) == Material.PLAYER_HEAD) {
					String skullUUID = instance.getConfig("config.yml")
							.getString("hellblock.starter-chest." + path + ".skull.player-uuid");
					SkullBuilder sm = null;
					try {
						sm = new SkullBuilder(UUID.fromString(skullUUID));
					} catch (IllegalArgumentException ex) {
						LogUtils.warn("The UUID wasn't found to set for this player head texture.", ex);
						continue;
					}
					if (sm != null) {
						ItemStack data = setChestData(sm.get(), true);
						if (slot >= 0 && slot <= 26) {
							inventory.setItem(slot, data);
						} else {
							inventory.addItem(data);
						}
					}
					continue;
				}

				if (Tag.ITEMS_BANNERS.isTagged(Material.getMaterial(material))) {
					BannerBuilder bm = new BannerBuilder(item.get());
					List<String> patterns = instance.getConfig("config.yml")
							.getStringList("hellblock.starter-chest." + path + ".banner.patterns");
					for (String pat : patterns) {
						String[] split = pat.split(":");
						String dyeColor = split[0];
						String type = split[1];
						PatternType patternType = type != null && !type.isEmpty()
								? bannerRegistry.getOrThrow(NamespacedKey.fromString(type.toLowerCase()))
								: PatternType.BASE;
						Pattern pattern = new Pattern(DyeColor.valueOf(dyeColor), patternType);
						bm.addPattern(pattern);
					}
					ItemStack data = setChestData(bm.get(), true);
					if (slot >= 0 && slot <= 26) {
						inventory.setItem(slot, data);
					} else {
						inventory.addItem(data);
					}
					continue;
				}

				if (Material.getMaterial(material) == Material.FIREWORK_ROCKET) {
					FireworkBuilder fm = new FireworkBuilder(item.get());
					int power = instance.getConfig("config.yml")
							.getInt("hellblock.starter-chest." + path + ".firework.power");
					fm.setPower(power);
					for (String fireworkEffect : instance.getConfig("config.yml")
							.getConfigurationSection("hellblock.starter-chest." + path + ".firework.effects")
							.getKeys(false)) {
						FireworkEffect.Builder builder = FireworkEffect.builder();
						String type = instance.getConfig("config.yml").getString(
								"hellblock.starter-chest." + path + ".firework.effects." + fireworkEffect + ".type");
						boolean flicker = instance.getConfig("config.yml").getBoolean(
								"hellblock.starter-chest." + path + ".firework.effects." + fireworkEffect + ".flicker",
								false);
						boolean trail = instance.getConfig("config.yml").getBoolean(
								"hellblock.starter-chest." + path + ".firework.effects." + fireworkEffect + ".trail",
								false);
						List<String> colors = instance.getConfig("config.yml").getStringList("hellblock.starter-chest."
								+ path + ".firework.effects." + fireworkEffect + ".main-colors");
						List<String> fades = instance.getConfig("config.yml").getStringList("hellblock.starter-chest."
								+ path + ".firework.effects." + fireworkEffect + ".fade-colors");
						List<Color> mainColors = new ArrayList<>();
						for (String color : colors) {
							if (color.contains(":")) {
								String[] split = color.split(":");
								for (String string : split) {
									Color mainColor = instance.getNetherBrewing().getColor(string);
									mainColors.add(mainColor);
								}
							} else {
								mainColors.add(instance.getNetherBrewing().getColor(color));
							}
						}
						List<Color> fadeColors = new ArrayList<>();
						for (String fade : fades) {
							if (fade.contains(":")) {
								String[] split = fade.split(":");
								for (String string : split) {
									Color fadeColor = instance.getNetherBrewing().getColor(string);
									fadeColors.add(fadeColor);
								}
							} else {
								fadeColors.add(instance.getNetherBrewing().getColor(fade));
							}
						}
						builder.flicker(flicker).trail(trail)
								.with(type != null && !type.isEmpty() ? Type.valueOf(type) : Type.BURST)
								.withColor(mainColors).withFade(fadeColors);
						fm.addFireworkEffect(builder.build());
					}
					ItemStack data = setChestData(fm.get(), true);
					if (slot >= 0 && slot <= 26) {
						inventory.setItem(slot, data);
					} else {
						inventory.addItem(data);
					}
					continue;
				}

				ItemStack data = setChestData(item.get(), true);

				if (slot >= 0 && slot <= 26) {
					inventory.setItem(slot, data);
				} else {
					inventory.addItem(data);
				}
			} catch (Exception ex) {
				LogUtils.severe(
						String.format("Unable to create the defined item for the starter chest: %s", instance
								.getConfig("config.yml").getString("hellblock.starter-chest." + path + ".material")),
						ex);
			}
		}
	}

	private static final BlockFace[] FACES = new BlockFace[] { BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST,
			BlockFace.WEST };

	private @NonNull BlockFace getChestDirection(@NonNull Location location) {

		BlockFace finalFace = BlockFace.SOUTH;
		for (BlockFace face : FACES) {
			if (location.getBlock().getRelative(face).getType() != Material.AIR)
				continue;
			if (location.getBlock().getRelative(face).getRelative(BlockFace.DOWN).getType() == Material.AIR)
				continue;
			finalFace = face;
			break;
		}

		return finalFace;
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
