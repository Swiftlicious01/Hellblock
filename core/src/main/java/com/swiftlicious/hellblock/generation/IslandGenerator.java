package com.swiftlicious.hellblock.generation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.apache.commons.lang3.StringUtils;
import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.FireworkEffect;
import org.bukkit.FireworkEffect.Type;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
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
import com.swiftlicious.hellblock.player.UserData;
import com.swiftlicious.hellblock.utils.ColorUtils;
import com.swiftlicious.hellblock.utils.wrappers.ShadedAdventureComponentWrapper;

import dev.dejvokep.boostedyaml.block.implementation.Section;
import lombok.NonNull;
import xyz.xenondevs.invui.item.builder.BannerBuilder;
import xyz.xenondevs.invui.item.builder.FireworkBuilder;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.builder.PotionBuilder;
import xyz.xenondevs.invui.item.builder.SkullBuilder;
import xyz.xenondevs.invui.util.MojangApiUtils.MojangApiException;

public class IslandGenerator {

	protected final HellblockPlugin instance;

	public IslandGenerator(HellblockPlugin plugin) {
		this.instance = plugin;
	}

	public CompletableFuture<Void> generateHellblockSchematic(@NonNull Location location, @NonNull Player player,
			@NonNull String schematic) {
		return CompletableFuture.runAsync(() -> {
			Optional<UserData> onlineUser = instance.getStorageManager().getOnlineUser(player.getUniqueId());
			if (onlineUser.isEmpty())
				return;
			World world = instance.getHellblockHandler().getHellblockWorld();
			if (instance.getConfigManager().worldguardProtect()) {
				instance.getSchematicManager().pasteSchematic(schematic, instance.getWorldGuardHandler()
						.getRegion(onlineUser.get().getUUID(), onlineUser.get().getHellblockData().getID()))
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
													() -> this.generateChest(new Location(world,
															(double) (location.getBlockX() + finalX),
															(double) (location.getBlockY() + finalY),
															(double) (location.getBlockZ() + finalZ)), player),
													location);
											break;
										}
									}
								}
							}
						});
			}
		});
	}

	public CompletableFuture<Void> generateDefaultHellblock(@NonNull Location location, @NonNull Player player) {
		return CompletableFuture.runAsync(() -> {
			Map<Block, Material> blockChanges = new LinkedHashMap<>();
			World world = instance.getHellblockHandler().getHellblockWorld();
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
			this.generateGlowstoneTree(new Location(world, (double) x, (double) y, (double) z)).thenRun(() -> {
				instance.getScheduler()
						.executeSync(
								() -> this.generateChest(
										new Location(world, (double) x, (double) chestY, (double) (z + 1)), player),
								location);
			});
		});
	}

	public CompletableFuture<Void> generateClassicHellblock(@NonNull Location location, @NonNull Player player) {
		return CompletableFuture.runAsync(() -> {
			Map<Block, Material> blockChanges = new LinkedHashMap<>();
			World world = instance.getHellblockHandler().getHellblockWorld();
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
			this.generateGlowstoneTree(new Location(world, (double) x, (double) y, (double) (z - 5))).thenRun(() -> {
				instance.getScheduler()
						.executeSync(() -> this.generateChest(
								new Location(world, (double) (x - 5), (double) chestY, (double) (z - 1)), player),
								location);
			});
		});
	}

	public CompletableFuture<Void> generateGlowstoneTree(@NonNull Location location) {
		return CompletableFuture.runAsync(() -> {
			Map<Block, Material> blockChanges = new LinkedHashMap<>();
			World world = instance.getHellblockHandler().getHellblockWorld();
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

	public void generateChest(@NonNull Location location, @NonNull Player player) {
		World world = instance.getHellblockHandler().getHellblockWorld();
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
		if (chestName != null && !chestName.isEmpty() && chestName.length() <= 35) {
			chest.customName(instance.getAdventureManager().getComponentFromMiniMessage(chestName));
		}
		Inventory inventory = chest.getInventory();
		inventory.clear();
		chest.update();

		for (Map.Entry<String, Object> entry : instance.getConfigManager().chestItems()
				.getStringRouteMappedValues(false).entrySet()) {
			if (!StringUtils.isNumeric(entry.getKey()))
				continue;
			if (entry.getValue() instanceof Section inner) {
				try {
					String material = inner.getString("material", "AIR");
					if (material.equalsIgnoreCase("AIR") || material == null || material.isEmpty())
						continue;
					int amount = inner.getInt("amount", 1);
					int slot = inner.getInt("slot");
					ItemBuilder item = new ItemBuilder(
							Material.getMaterial(material.toUpperCase()) != null
									? Material.getMaterial(material.toUpperCase())
									: Material.AIR,
							amount > 0 && amount <= Material.getMaterial(material.toUpperCase()).getMaxStackSize()
									? amount
									: 1);

					if (Tag.AIR.isTagged(item.get().getType()))
						continue;

					String displayName = inner.getString("name");
					if (displayName != null && !displayName.isEmpty()) {
						item.setDisplayName(new ShadedAdventureComponentWrapper(
								instance.getAdventureManager().getComponentFromMiniMessage(displayName)));
					}

					List<String> lore = inner.getStringList("lore");
					if (lore != null && !lore.isEmpty()) {
						for (String newLore : lore) {
							item.addLoreLines(new ShadedAdventureComponentWrapper(
									instance.getAdventureManager().getComponentFromMiniMessage(newLore)));
						}
					}

					List<String> enchantments = inner.getStringList("enchantments");
					if (enchantments != null && !enchantments.isEmpty()) {
						for (String enchants : enchantments) {
							String[] split = enchants.split(":");
							Enchantment enchantment = instance.getHellblockHandler().getEnchantmentRegistry()
									.getOrThrow(NamespacedKey.fromString(split[0].toLowerCase()));
							int level = 1;
							try {
								level = Integer.parseInt(split[1]);
							} catch (NumberFormatException ex) {
								instance.getPluginLogger().severe(String.format("Invalid quantity: %s!", split[1]));
								continue;
							}
							item.addEnchantment(enchantment, level, false);
						}
					}

					int damage = inner.getInt("damage");
					item.setDamage(damage >= 0 && damage <= item.get().getType().getMaxDurability() ? damage : 0);

					int customModelData = inner.getInt("custom-model-data");
					item.setCustomModelData(customModelData);

					boolean unbreakable = inner.getBoolean("unbreakable", false);
					item.setUnbreakable(unbreakable);

					if (Material.getMaterial(material) == Material.POTION
							|| Material.getMaterial(material) == Material.SPLASH_POTION
							|| Material.getMaterial(material) == Material.LINGERING_POTION
							|| Material.getMaterial(material) == Material.TIPPED_ARROW) {
						PotionBuilder pm = new PotionBuilder(item.get());
						String potion = inner.getString("potion.effect");
						int duration = inner.getInt("potion.duration");
						int amplifier = inner.getInt("potion.amplifier");
						PotionEffectType potionType = potion != null && !potion.isEmpty()
								? instance.getHellblockHandler().getPotionEffectRegistry()
										.getOrThrow(NamespacedKey.fromString(potion.toLowerCase()))
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
						String skullUUID = inner.getString("skull.player-uuid");
						SkullBuilder sm = null;
						try {
							sm = new SkullBuilder(UUID.fromString(skullUUID));
						} catch (IllegalArgumentException | MojangApiException | IOException ex) {
							instance.getPluginLogger().warn(String.format(
									"The UUID %s wasn't found to set for this player head texture.", skullUUID), ex);
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
						List<String> patterns = inner.getStringList("banner.patterns");
						for (String pat : patterns) {
							String[] split = pat.split(":");
							String dyeColor = split[0];
							String type = split[1];
							PatternType patternType = type != null && !type.isEmpty() ? instance.getHellblockHandler()
									.getBannerRegistry().getOrThrow(NamespacedKey.fromString(type.toLowerCase()))
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
						int power = inner.getInt("firework.power");
						fm.setPower(power);
						for (Entry<String, Object> fireworkEffect : inner.getSection("firework.effects")
								.getStringRouteMappedValues(false).entrySet()) {
							if (!StringUtils.isNumeric(fireworkEffect.getKey()))
								continue;
							if (fireworkEffect.getValue() instanceof Section innerEffect) {
								FireworkEffect.Builder builder = FireworkEffect.builder();
								String type = innerEffect.getString("type");
								boolean flicker = innerEffect.getBoolean("flicker", false);
								boolean trail = innerEffect.getBoolean("trail", false);
								List<String> colors = innerEffect.getStringList(".main-colors");
								List<String> fades = innerEffect.getStringList("fade-colors");
								List<Color> mainColors = new ArrayList<>();
								for (String color : colors) {
									if (color.contains(":")) {
										String[] split = color.split(":");
										for (String string : split) {
											Color mainColor = ColorUtils.getColor(string);
											mainColors.add(mainColor);
										}
									} else {
										mainColors.add(ColorUtils.getColor(color));
									}
								}
								List<Color> fadeColors = new ArrayList<>();
								for (String fade : fades) {
									if (fade.contains(":")) {
										String[] split = fade.split(":");
										for (String string : split) {
											Color fadeColor = ColorUtils.getColor(string);
											fadeColors.add(fadeColor);
										}
									} else {
										fadeColors.add(ColorUtils.getColor(fade));
									}
								}
								builder.flicker(flicker).trail(trail);
								try {
									builder.with(type != null && !type.isEmpty() ? Type.valueOf(type) : Type.BURST);
									builder.withColor(mainColors).withFade(fadeColors);
								} catch (IllegalArgumentException ex) {
									instance.getPluginLogger()
											.warn("The type or color of this firework effect returned null.", ex);
									continue;
								}
								fm.addFireworkEffect(builder.build());
							}
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
				} catch (IllegalArgumentException ex) {
					instance.getPluginLogger()
							.severe(String.format("Unable to create the defined item for the starter chest: %s",
									inner.getString("material")), ex);
					continue;
				}
			}
		}
	}

	private static final BlockFace[] FACES = new BlockFace[] { BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST,
			BlockFace.WEST };

	private @NonNull BlockFace getChestDirection(@NonNull Location location, @NonNull IslandOptions option) {

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
