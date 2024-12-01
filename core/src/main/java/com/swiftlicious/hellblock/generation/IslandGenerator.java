package com.swiftlicious.hellblock.generation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
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
import com.swiftlicious.hellblock.creation.item.AbstractItem;
import com.swiftlicious.hellblock.creation.item.Item;
import com.swiftlicious.hellblock.creation.item.ItemEditor;
import com.swiftlicious.hellblock.handlers.AdventureHelper;
import com.swiftlicious.hellblock.player.Context;
import com.swiftlicious.hellblock.player.UserData;
import com.swiftlicious.hellblock.utils.ItemStackUtils;
import com.swiftlicious.hellblock.utils.StringUtils;
import com.swiftlicious.hellblock.utils.extras.TextValue;

import dev.dejvokep.boostedyaml.block.implementation.Section;

public class IslandGenerator {

	protected final HellblockPlugin instance;

	public IslandGenerator(HellblockPlugin plugin) {
		this.instance = plugin;
	}

	public CompletableFuture<Void> generateHellblockSchematic(@NotNull Location location, @NotNull Player player,
			@NotNull String schematic) {
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

	public CompletableFuture<Void> generateDefaultHellblock(@NotNull Location location, @NotNull Player player) {
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

	public CompletableFuture<Void> generateClassicHellblock(@NotNull Location location, @NotNull Player player) {
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

	public CompletableFuture<Void> generateGlowstoneTree(@NotNull Location location) {
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

	public void generateChest(@NotNull Location location, @NotNull Player player) {
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
		Context<Player> context = Context.player(player);
		if (chestName != null && !chestName.isEmpty() && chestName.length() <= 35) {
			RtagBlock chestTag = new RtagBlock(block);
			TextValue<Player> customChestName = TextValue.auto(chestName);
			chestTag.setCustomName(
					AdventureHelper.componentToJson(AdventureHelper.miniMessage(customChestName.render(context))));
			chestTag.update();
			chestTag.load();
		}
		Inventory inventory = chest.getInventory();
		inventory.clear();
		chest.update();

		for (Map.Entry<String, Object> entry : instance.getConfigManager().chestItems()
				.getStringRouteMappedValues(false).entrySet()) {
			if (StringUtils.isNotInteger(entry.getKey()))
				continue;
			if (entry.getValue() instanceof Section inner) {
				try {
					String material = inner.getString("material", "AIR");
					if (material == null || material.isEmpty() || material.equalsIgnoreCase("AIR"))
						continue;
					int amount = inner.getInt("amount", 1);
					int slot = inner.getInt("slot");
					Material type = Material.getMaterial(material.toUpperCase()) != null
							? Material.getMaterial(material.toUpperCase())
							: Material.AIR;
					Item<ItemStack> item = instance.getItemManager()
							.wrap(new ItemStack(type, amount > 0 && amount <= type.getMaxStackSize() ? amount : 1));

					if (item.getItem().getType() == Material.AIR)
						continue;

					String displayName = inner.getString("display.name");
					if (displayName != null && !displayName.isEmpty()) {
						TextValue<Player> name = TextValue.auto("<!i><white>" + displayName);
						item.displayName(AdventureHelper.miniMessageToJson(name.render(context)));
					}

					List<String> displayLore = inner.getStringList("display.lore");
					if (displayLore != null && !displayLore.isEmpty()) {
						List<TextValue<Player>> lore = new ArrayList<>();
						for (String text : displayLore) {
							lore.add(TextValue.auto("<!i><white>" + text));
						}
						item.lore(lore.stream().map(it -> AdventureHelper.miniMessageToJson(it.render(context)))
								.toList());
					}

					Section enchantments = inner
							.getSection(type == Material.ENCHANTED_BOOK ? "stored-enchantments" : "enchantments");
					if (enchantments != null) {
						if (type == Material.ENCHANTED_BOOK)
							item.storedEnchantments(instance.getConfigManager().getEnchantments(enchantments));
						else
							item.enchantments(instance.getConfigManager().getEnchantments(enchantments));
					}

					int damage = inner.getInt("damage");
					item.damage(damage >= 0 && damage < item.getItem().getType().getMaxDurability() ? damage : 0);

					int customModelData = inner.getInt("custom-model-data");
					item.customModelData(customModelData);

					boolean unbreakable = inner.getBoolean("unbreakable", false);
					if (unbreakable)
						item.unbreakable(true);

					if (instance.getVersionManager().isVersionNewerThan1_20_5()) {
						Section components = inner.getSection("item-components");
						if (components != null) {
							List<ItemEditor> editors = new ArrayList<>();
							ItemStackUtils.sectionToComponentEditor(components, editors);
							for (ItemEditor editor : editors) {
								editor.apply(((AbstractItem<RtagItem, ItemStack>) item).getRTagItem(), context);
							}
						}
					} else {
						List<String> flags = inner.getStringList("item-flags");
						if (flags != null && !flags.isEmpty())
							item.itemFlags(flags);
					}

					if (type == Material.POTION || type == Material.SPLASH_POTION || type == Material.LINGERING_POTION
							|| type == Material.TIPPED_ARROW) {
						String potion = inner.getString("potion.effect");
						if (potion != null && !potion.isEmpty())
							item.potionEffect(potion);
					}

					if (type == Material.PLAYER_HEAD) {
						String base64 = inner.getString("head64");
						if (base64 != null && !base64.isEmpty())
							item.skull(base64);
					}

					ItemStack data = setChestData(item.load(), true);

					if (slot >= 0 && slot <= 26) {
						inventory.setItem(slot, data);
					} else {
						if (inventory.firstEmpty() != -1)
							inventory.addItem(data);
						else
							continue;
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