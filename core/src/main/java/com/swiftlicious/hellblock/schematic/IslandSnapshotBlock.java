package com.swiftlicious.hellblock.schematic;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Banner;
import org.bukkit.block.Beacon;
import org.bukkit.block.Beehive;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.BrewingStand;
import org.bukkit.block.Campfire;
import org.bukkit.block.CommandBlock;
import org.bukkit.block.Container;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.block.EnchantingTable;
import org.bukkit.block.EndGateway;
import org.bukkit.block.Furnace;
import org.bukkit.block.Jukebox;
import org.bukkit.block.Lectern;
import org.bukkit.block.ShulkerBox;
import org.bukkit.block.Sign;
import org.bukkit.block.Skull;
import org.bukkit.block.Structure;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.banner.PatternType;
import org.bukkit.block.structure.UsageMode;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Hanging;
import org.bukkit.entity.Shulker;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.BoundingBox;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.handlers.PotionEffectResolver;
import com.swiftlicious.hellblock.handlers.VersionHelper;
import com.swiftlicious.hellblock.world.CustomBlock;
import com.swiftlicious.hellblock.world.CustomBlockState;
import com.swiftlicious.hellblock.world.CustomBlockTypes;
import com.swiftlicious.hellblock.world.Pos3;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.BinaryTagType;
import net.kyori.adventure.nbt.BinaryTagTypes;
import net.kyori.adventure.nbt.ByteBinaryTag;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.DoubleBinaryTag;
import net.kyori.adventure.nbt.FloatBinaryTag;
import net.kyori.adventure.nbt.IntBinaryTag;
import net.kyori.adventure.nbt.ListBinaryTag;
import net.kyori.adventure.nbt.ListBinaryTag.Builder;
import net.kyori.adventure.nbt.LongBinaryTag;
import net.kyori.adventure.nbt.ShortBinaryTag;
import net.kyori.adventure.nbt.StringBinaryTag;
import net.kyori.adventure.text.Component;

/**
 * A fully serializable representation of a single block in an IslandSnapshot.
 * Captures block type, data, inventories, tile entity data, and attached
 * entities.
 */
public record IslandSnapshotBlock(@JsonProperty("x") int x, @JsonProperty("y") int y, @JsonProperty("z") int z,
		@JsonProperty("type") Material type, @JsonProperty("blockData") String blockData,
		@JsonProperty("inventory") Map<Integer, ItemStack> inventory,
		@JsonProperty("tileData") CompoundBinaryTag tileData, @JsonProperty("entities") List<EntitySnapshot> entities)
		implements Serializable {

	@JsonCreator
	public IslandSnapshotBlock {
		Objects.requireNonNull(type, "Block type cannot be null");
		Objects.requireNonNull(blockData, "Block data cannot be null");
		if (tileData == null) {
			tileData = CompoundBinaryTag.empty();
		}
		if (inventory == null) {
			inventory = new HashMap<>();
		}
		if (entities == null) {
			entities = new ArrayList<>();
		}
		if (type.isAir()) {
			throw new IllegalArgumentException("Block type cannot be AIR");
		}
		if (x < -30000000 || x > 30000000 || z < -30000000 || z > 30000000) {
			throw new IllegalArgumentException("Block coordinates out of bounds: " + x + ", " + z);
		}
		if (y < 0 || y > 255) {
			throw new IllegalArgumentException("Block Y coordinate out of bounds: " + y);
		}
	}

	/**
	 * Convert a BlockState into an IslandSnapshotBlock, capturing block type, data,
	 * inventories, tile entity data, and attached entities.
	 * 
	 * @param state          The BlockState to capture
	 * @param nearbyEntities A list of nearby EntitySnapshots (for performance, only
	 *                       entities within 5 blocks should be provided)
	 * @return The captured IslandSnapshotBlock
	 */
	@SuppressWarnings("deprecation")
	public static IslandSnapshotBlock fromBlockState(BlockState state, List<EntitySnapshot> nearbyEntities) {
		final Map<Integer, ItemStack> inv = new HashMap<>();
		final CompoundBinaryTag.Builder tileBuilder = CompoundBinaryTag.builder();

		// Capture container inventory
		if (state instanceof Container container) {
			deepCloneInventory(container.getInventory().getContents(), contents -> {
				for (int slot = 0; slot < contents.length; slot++) {
					if (contents[slot] != null) {
						inv.put(slot, contents[slot]);
					}
				}
			});
		}

		// Shulker
		if (state instanceof ShulkerBox shulker) {
			final CompoundBinaryTag shulkerTag = inventoryToNBT(shulker.getInventory());
			if (!shulkerTag.isEmpty()) {
				tileBuilder.put("shulkerInv", shulkerTag);
			}
		}

		// Sign
		if (state instanceof Sign sign) {
			CompoundBinaryTag.Builder signSides = CompoundBinaryTag.builder();

			if (SignReflectionHelper.isDualSided()) {
				// 1.20+: Save both sides
				for (Object side : SignReflectionHelper.getSideEnumConstants()) {
					try {
						Object signSide = SignReflectionHelper.invokeGetSide(sign, side);
						ListBinaryTag.Builder<StringBinaryTag> sideLines = ListBinaryTag.builder(BinaryTagTypes.STRING);

						for (int i = 0; i < 4; i++) {
							Component line = SignReflectionHelper.invokeGetLine(signSide, i);
							sideLines.add(StringBinaryTag.stringBinaryTag(AdventureMetadata.serialize(line)));
						}

						signSides.put(side.toString(), sideLines.build());

					} catch (Exception ex) {
						HellblockPlugin.getInstance().getPluginLogger()
								.warn("Failed to serialize sign side " + side + ": " + ex.getMessage());
					}
				}
			} else {
				// 1.19.4 or earlier: Save single side
				ListBinaryTag.Builder<StringBinaryTag> sideLines = ListBinaryTag.builder(BinaryTagTypes.STRING);
				for (int i = 0; i < 4; i++) {
					Component line = SignReflectionHelper.getLine(sign, i);
					sideLines.add(StringBinaryTag.stringBinaryTag(AdventureMetadata.serialize(line)));
				}
				signSides.put("FRONT", sideLines.build()); // Use "FRONT" as default name
			}

			tileBuilder.put("signSides", signSides.build());
		}

		// Skull
		if (state instanceof Skull skull && skull.getOwningPlayer() != null) {
			tileBuilder.putString("owner", skull.getOwningPlayer().getUniqueId().toString());
		}

		// Banner
		if (state instanceof Banner banner) {
			tileBuilder.putString("baseColor", banner.getBaseColor().name());

			final Builder<StringBinaryTag> patternList = ListBinaryTag.builder(BinaryTagTypes.STRING);
			banner.getPatterns().forEach(pattern -> {
				@SuppressWarnings("removal")
				final String str = pattern.getColor().name() + "|" + pattern.getPattern().getIdentifier();
				patternList.add(StringBinaryTag.stringBinaryTag(str));
			});
			tileBuilder.put("patterns", patternList.build());
		}

		// Lectern
		if (state instanceof Lectern lectern) {
			final ItemStack book = lectern.getInventory().getItem(0);
			if (book != null) {
				tileBuilder.put("book", itemStackToNBT(book));
			}
			tileBuilder.putInt("page", lectern.getPage());
		}

		// Beehive
		if (state instanceof Beehive beehive && beehive.getFlower() != null) {
			tileBuilder.put("flower", locationToNBT(beehive.getFlower()));
		}

		// Jukebox
		if (state instanceof Jukebox jukebox && JukeboxReflectionHelper.hasRecord(jukebox)) {
			tileBuilder.put("jukeboxRecord", itemStackToNBT(jukebox.getRecord()));
		}

		// Brewing Stand
		if (state instanceof BrewingStand brewing) {
			tileBuilder.put("brewingInv", inventoryToNBT(brewing.getInventory()));
			tileBuilder.putInt("brewTime", brewing.getBrewingTime());
			tileBuilder.putInt("fuelLevel", brewing.getFuelLevel());
		}

		// Furnace
		if (state instanceof Furnace furnace) {
			tileBuilder.put("furnaceInv", inventoryToNBT(furnace.getInventory()));
			tileBuilder.putShort("burnTime", furnace.getBurnTime());
			tileBuilder.putShort("cookTime", furnace.getCookTime());
			tileBuilder.putInt("cookTimeTotal", furnace.getCookTimeTotal());
		}

		// Beacon
		if (state instanceof Beacon beacon) {
			if (beacon.getPrimaryEffect() != null) {
				NamespacedKey primaryEffect = PotionEffectResolver
						.getPotionEffectKey(beacon.getPrimaryEffect().getType());
				if (primaryEffect != null)
					tileBuilder.putString("primaryEffect", primaryEffect.getKey());
			}
			if (beacon.getSecondaryEffect() != null) {
				NamespacedKey secondaryEffect = PotionEffectResolver
						.getPotionEffectKey(beacon.getSecondaryEffect().getType());
				if (secondaryEffect != null)
					tileBuilder.putString("secondaryEffect", secondaryEffect.getKey());
			}
		}

		// Enchanting Table
		if (state instanceof EnchantingTable table) {
			Component name = AdventureMetadata.getEnchantingTableName(table);
			if (name != null) {
				tileBuilder.putString("customName", AdventureMetadata.serialize(name));
			}
		}

		// Command Block
		if (state instanceof CommandBlock cmd) {
			tileBuilder.putString("command", cmd.getCommand());
			tileBuilder.putString("name", AdventureMetadata.serialize(AdventureMetadata.getCommandBlockName(cmd)));
			if (VersionHelper.isPaperFork()) {
				tileBuilder.putInt("successCount", PaperReflection.getCommandBlockSuccessCount(cmd));
			}
		}

		// Structure Block
		if (state instanceof Structure structure) {
			tileBuilder.putString("structureData", structure.getStructureName());
			tileBuilder.putString("usageMode", structure.getUsageMode().name());
			tileBuilder.putString("author", structure.getAuthor());
			tileBuilder.putFloat("integrity", structure.getIntegrity());
			tileBuilder.putLong("seed", structure.getSeed());
		}

		// Spawner
		if (state instanceof CreatureSpawner spawner && spawner.getSpawnedType() != null) {
			tileBuilder.putString("spawnedType", spawner.getSpawnedType().name());
			tileBuilder.putInt("spawnDelay", spawner.getDelay());
			tileBuilder.putInt("minSpawnDelay", spawner.getMinSpawnDelay());
			tileBuilder.putInt("maxSpawnDelay", spawner.getMaxSpawnDelay());
			tileBuilder.putInt("spawnCount", spawner.getSpawnCount());
			tileBuilder.putInt("maxNearbyEntities", spawner.getMaxNearbyEntities());
			tileBuilder.putInt("requiredPlayerRange", spawner.getRequiredPlayerRange());
			tileBuilder.putInt("spawnRange", spawner.getSpawnRange());
		}

		// Campfire
		if (state instanceof Campfire campfire) {
			final CompoundBinaryTag.Builder campfireInv = CompoundBinaryTag.builder();
			final CompoundBinaryTag.Builder cookTimes = CompoundBinaryTag.builder();
			final CompoundBinaryTag.Builder cookTimesTotal = CompoundBinaryTag.builder();

			for (int i = 0; i < campfire.getSize(); i++) {
				final ItemStack item = campfire.getItem(i);
				if (item != null) {
					campfireInv.put(String.valueOf(i), itemStackToNBT(item));
				}
				cookTimes.putInt(String.valueOf(i), campfire.getCookTime(i));
				cookTimesTotal.putInt(String.valueOf(i), campfire.getCookTimeTotal(i));
			}
			if (!campfireInv.build().isEmpty()) {
				tileBuilder.put("campfireInv", campfireInv.build());
			}
			tileBuilder.put("campfireCookTimes", cookTimes.build());
			tileBuilder.put("campfireCookTimesTotal", cookTimesTotal.build());
		}

		// Chiseled Bookshelf (Only available in 1.19.3+)
		if (VersionHelper.isVersionNewerThan1_19_3() && PaperReflection.isChiseledBookshelf(state)) {
			tileBuilder.put("bookshelfInv", inventoryToNBT(PaperReflection.getChiseledBookshelfInventory(state)));
		}

		// End Gateway
		if (state instanceof EndGateway gateway) {
			if (gateway.getExitLocation() != null) {
				tileBuilder.put("exitLocation", locationToNBT(gateway.getExitLocation()));
			}
			tileBuilder.putBoolean("exactTeleport", gateway.isExactTeleport());
		}

		HellblockPlugin.getInstance().getWorldManager().getWorld(state.getWorld()).ifPresent(hellblockWorld -> {
			CompletableFuture<Optional<CustomBlockState>> customState = hellblockWorld
					.getBlockState(Pos3.from(state.getLocation()));
			customState.thenAccept(blockState -> blockState
					.ifPresent(cs -> tileBuilder.putString("customBlockId", cs.type().type().key().asString())));
		});

		// Entities attached to the block
		final List<EntitySnapshot> entitySnapshots = findAttachedEntities(state.getLocation());

		return new IslandSnapshotBlock(state.getX(), state.getY(), state.getZ(), state.getType(),
				state.getBlockData().getAsString(), inv.isEmpty() ? null : inv,
				tileBuilder.build().isEmpty() ? null : tileBuilder.build(),
				entitySnapshots.isEmpty() ? null : entitySnapshots);
	}

	/**
	 * Restore this block snapshot to the given world, including block type, data,
	 * inventories, tile entity data, and attached entities.
	 * 
	 * @param world The world to restore the block in
	 * @return The restored BlockState
	 */
	@SuppressWarnings("deprecation")
	public BlockState restore(World world) {
		final Block block = world.getBlockAt(x, y, z);
		block.setType(type, false);
		block.setBlockData(Bukkit.createBlockData(blockData), false);

		final BlockState state = block.getState();

		// Restore container inventories
		if (inventory != null && state instanceof Container container) {
			container.getInventory().clear();
			inventory.forEach((slot, item) -> container.getInventory().setItem(slot, deepCloneItem(item)));
			container.update(true, false);
		}

		// Only proceed if tileData is set
		if (tileData != null) {
			// Restore shulker contents
			if (state instanceof ShulkerBox shulker && tileData.get("shulkerInv") instanceof CompoundBinaryTag invTag) {
				shulker.getInventory().clear();
				inventoryFromNBT(invTag, shulker.getInventory());
				shulker.update(true, false);
			}

			// Restore sign
			if (state instanceof Sign sign && tileData.get("signSides") instanceof CompoundBinaryTag signTag) {
				if (SignReflectionHelper.isDualSided()) {
					// 1.20+: Restore each side
					for (Object side : SignReflectionHelper.getSideEnumConstants()) {
						if (signTag.get(side.toString()) instanceof ListBinaryTag sideLines) {
							try {
								Object signSide = SignReflectionHelper.invokeGetSide(sign, side);
								for (int i = 0; i < sideLines.size(); i++) {
									String json = ((StringBinaryTag) sideLines.get(i)).value();
									Component component = AdventureMetadata.deserialize(json);
									SignReflectionHelper.invokeSetLine(signSide, i, component);
								}
							} catch (Exception ex) {
								HellblockPlugin.getInstance().getPluginLogger()
										.warn("Failed to restore sign side " + side + ": " + ex.getMessage());
							}
						}
					}
				} else {
					// legacy fallback: restore only one side (use FRONT or any available key)
					if (signTag.get("FRONT") instanceof ListBinaryTag sideLines) {
						for (int i = 0; i < sideLines.size(); i++) {
							String json = ((StringBinaryTag) sideLines.get(i)).value();
							Component component = AdventureMetadata.deserialize(json);
							SignReflectionHelper.setLine(sign, i, component);
						}
					}
				}

				sign.update(true, false);
			}

			// Skull
			if (state instanceof Skull skull && tileData.contains("owner")) {
				final UUID ownerId = UUID.fromString(tileData.getString("owner"));
				skull.setOwningPlayer(Bukkit.getOfflinePlayer(ownerId));
				skull.update(true, false);
			}

			// Banner
			if (state instanceof Banner banner) {
				if (tileData.contains("baseColor")) {
					banner.setBaseColor(DyeColor.valueOf(tileData.getString("baseColor")));
				}
				if (tileData.get("patterns") instanceof ListBinaryTag patternList) {
					final List<Pattern> patterns = patternList.stream().map(tag -> ((StringBinaryTag) tag).value())
							.map(entry -> entry.split("\\|")).filter(parts -> parts.length == 2).map(parts -> {
								try {
									final DyeColor color = DyeColor.valueOf(parts[0]);
									final PatternType type = VariantRegistry.getBannerPattern(parts[1]);
									return type != null ? new Pattern(color, type) : null;
								} catch (IllegalArgumentException e) {
									return null;
								}
							}).filter(Objects::nonNull).collect(Collectors.toList());
					banner.setPatterns(patterns);
				}
				banner.update(true, false);
			}

			// Lectern
			if (state instanceof Lectern lectern) {
				if (tileData.get("book") instanceof CompoundBinaryTag bookTag) {
					lectern.getInventory().setItem(0, itemStackFromNBT(bookTag));
				}
				if (tileData.get("page") instanceof IntBinaryTag pageTag) {
					lectern.setPage(pageTag.intValue());
				}
				lectern.update(true, false);
			}

			// Beehive
			if (state instanceof Beehive beehive && tileData.get("flower") instanceof CompoundBinaryTag flowerTag) {
				final World w = Bukkit.getWorld(flowerTag.getString("world"));
				if (w != null) {
					final Location loc = new Location(w, flowerTag.getDouble("x"), flowerTag.getDouble("y"),
							flowerTag.getDouble("z"), flowerTag.getFloat("yaw"), flowerTag.getFloat("pitch"));
					beehive.setFlower(loc);
				}
				beehive.update(true, false);
			}

			// Jukebox
			if (state instanceof Jukebox jukebox && tileData.get("jukeboxRecord") instanceof CompoundBinaryTag recTag) {
				jukebox.setRecord(itemStackFromNBT(recTag));
				jukebox.update(true, false);
			}

			// Brewing Stand
			if (state instanceof BrewingStand brewing) {
				if (tileData.get("brewingInv") instanceof CompoundBinaryTag invTag) {
					brewing.getInventory().clear();
					inventoryFromNBT(invTag, brewing.getInventory());
				}
				if (tileData.get("brewTime") instanceof IntBinaryTag brewTime) {
					brewing.setBrewingTime(brewTime.intValue());
				}
				if (tileData.get("fuelLevel") instanceof IntBinaryTag fuel) {
					brewing.setFuelLevel(fuel.intValue());
				}
				brewing.update(true, false);
			}

			// Furnace
			if (state instanceof Furnace furnace) {
				if (tileData.get("furnaceInv") instanceof CompoundBinaryTag invTag) {
					furnace.getInventory().clear();
					inventoryFromNBT(invTag, furnace.getInventory());
				}
				if (tileData.get("burnTime") instanceof ShortBinaryTag burnTime) {
					furnace.setBurnTime(burnTime.shortValue());
				}
				if (tileData.get("cookTime") instanceof ShortBinaryTag cookTime) {
					furnace.setCookTime(cookTime.shortValue());
				}
				if (tileData.get("cookTimeTotal") instanceof ShortBinaryTag totalCook) {
					furnace.setCookTimeTotal(totalCook.shortValue());
				}
				furnace.update(true, false);
			}

			// Beacon
			if (state instanceof Beacon beacon) {
				if (tileData.contains("primaryEffect")) {
					PotionEffectType effect = PotionEffectResolver.resolve(tileData.getString("primaryEffect"));
					if (effect != null) {
						beacon.setPrimaryEffect(effect);
					}
				}
				if (tileData.contains("secondaryEffect")) {
					PotionEffectType effect = PotionEffectResolver.resolve(tileData.getString("secondaryEffect"));
					if (effect != null) {
						beacon.setSecondaryEffect(effect);
					}
				}
				beacon.update(true, false);
			}

			// Enchanting Table
			if (state instanceof EnchantingTable table && tileData.contains("customName")) {
				Component name = AdventureMetadata.deserialize(tileData.getString("customName"));
				AdventureMetadata.setEnchantingTableName(table, name);
				table.update(true, false);
			}

			// Command Block
			if (state instanceof CommandBlock cmd) {
				if (tileData.contains("command")) {
					cmd.setCommand(tileData.getString("command"));
				}
				if (tileData.contains("name")) {
					Component name = AdventureMetadata.deserialize(tileData.getString("name"));
					AdventureMetadata.setCommandBlockName(cmd, name);
				}
				if (VersionHelper.isPaperFork() && tileData.get("successCount") instanceof IntBinaryTag success) {
					PaperReflection.setCommandBlockSuccessCount(cmd, success.intValue());
				}
				cmd.update(true, false);
			}

			// Structure Block
			if (state instanceof Structure structure) {
				if (tileData.contains("structureData")) {
					structure.setStructureName(tileData.getString("structureData"));
				}
				if (tileData.contains("usageMode")) {
					structure.setUsageMode(UsageMode.valueOf(tileData.getString("usageMode")));
				}
				if (tileData.contains("author")) {
					structure.setAuthor(tileData.getString("author"));
				}
				if (tileData.get("integrity") instanceof FloatBinaryTag integrity) {
					structure.setIntegrity(integrity.floatValue());
				}
				if (tileData.get("seed") instanceof LongBinaryTag seed) {
					structure.setSeed(seed.longValue());
				}
				structure.update(true, false);
			}

			// Spawner
			if (state instanceof CreatureSpawner spawner && tileData.contains("spawnedType")) {
				spawner.setSpawnedType(EntityType.valueOf(tileData.getString("spawnedType")));
				spawner.setDelay(tileData.getInt("spawnDelay"));
				spawner.setMinSpawnDelay(tileData.getInt("minSpawnDelay"));
				spawner.setMaxSpawnDelay(tileData.getInt("maxSpawnDelay"));
				spawner.setSpawnCount(tileData.getInt("spawnCount"));
				spawner.setMaxNearbyEntities(tileData.getInt("maxNearbyEntities"));
				spawner.setRequiredPlayerRange(tileData.getInt("requiredPlayerRange"));
				spawner.setSpawnRange(tileData.getInt("spawnRange"));
				spawner.update(true, false);
			}

			// Campfire
			if (state instanceof Campfire campfire && tileData.get("campfireInv") instanceof CompoundBinaryTag invTag) {
				invTag.keySet().forEach(key -> {
					final int i = Integer.parseInt(key);
					campfire.setItem(i, itemStackFromNBT(invTag.getCompound(key)));
				});
				if (tileData.get("campfireCookTimes") instanceof CompoundBinaryTag cookTag) {
					cookTag.keySet().forEach(key -> campfire.setCookTime(Integer.parseInt(key), cookTag.getInt(key)));
				}
				if (tileData.get("campfireCookTimesTotal") instanceof CompoundBinaryTag totalTag) {
					totalTag.keySet()
							.forEach(key -> campfire.setCookTimeTotal(Integer.parseInt(key), totalTag.getInt(key)));
				}
				campfire.update(true, false);
			}

			// Chiseled Bookshelf
			if (VersionHelper.isVersionNewerThan1_19_3() && PaperReflection.isChiseledBookshelf(state)
					&& tileData.get("bookshelfInv") instanceof CompoundBinaryTag booksTag) {
				Inventory bookshelfInv = PaperReflection.getChiseledBookshelfInventory(state);
				if (bookshelfInv != null) {
					bookshelfInv.clear();
					inventoryFromNBT(booksTag, bookshelfInv);
					PaperReflection.updateChiseledBookshelf(state);
				}
			}

			// End Gateway
			if (state instanceof EndGateway gateway
					&& tileData.get("exitLocation") instanceof CompoundBinaryTag exitTag) {
				final World w = Bukkit.getWorld(exitTag.getString("world"));
				if (w != null) {
					final Location loc = new Location(w, exitTag.getDouble("x"), exitTag.getDouble("y"),
							exitTag.getDouble("z"), exitTag.getFloat("yaw"), exitTag.getFloat("pitch"));
					gateway.setExitLocation(loc);
				}
				if (tileData.get("exactTeleport") instanceof ByteBinaryTag boolTag) {
					gateway.setExactTeleport(boolTag.byteValue() != 0);
				}
				gateway.update(true, false);
			}
		}

		if (tileData != null && tileData.contains("customBlockId")) {
			String id = tileData.getString("customBlockId");
			Key key = Key.key(id);
			if (key != null) {
				CustomBlock customBlock = CustomBlockTypes.registry().get(key);
				if (customBlock != null) {
					CustomBlockState blockState = customBlock.createBlockState();
					HellblockPlugin.getInstance().getWorldManager().getWorld(world)
							.ifPresent(hbWorld -> hbWorld.updateBlockState(Pos3.from(block.getLocation()), blockState));
				}
			}
		}

		// Restore entities
		if (entities != null && !entities.isEmpty()) {
			entities.forEach(snapshot -> snapshot.spawn(world));
		}

		return state;
	}

	/**
	 * Create a deep clone of an ItemStack, including all metadata and nested
	 * inventories (e.g. shulker boxes). This ensures that modifications to the
	 * cloned item do not affect the original item.
	 * 
	 * @param item The ItemStack to clone
	 * @return A deep clone of the ItemStack
	 */
	@SuppressWarnings("deprecation")
	private static ItemStack deepCloneItem(ItemStack item) {
		if (item == null || item.getType().isAir()) {
			return null;
		}

		// Full serialize/deserialize ensures persistent data & plugin NBT are preserved
		final Map<String, Object> serialized = item.serialize();
		final ItemStack clone = ItemStack.deserialize(serialized);
		clone.setAmount(item.getAmount());

		// Extra handling for BlockStateMeta (container-like items)
		if (clone.getItemMeta() instanceof BlockStateMeta meta) {
			final BlockState state = meta.getBlockState();

			// --- ShulkerBox ---
			if (state instanceof ShulkerBox shulker) {
				final ItemStack[] contents = shulker.getInventory().getContents();
				for (int i = 0; i < contents.length; i++) {
					if (contents[i] != null) {
						shulker.getInventory().setItem(i, deepCloneItem(contents[i]));
					}
				}
				meta.setBlockState(shulker);
				clone.setItemMeta(meta);
			}

			// --- Banner ---
			else if (state instanceof Banner banner) {
				banner.setPatterns(new ArrayList<>(banner.getPatterns())); // defensive copy
				meta.setBlockState(banner);
				clone.setItemMeta(meta);
			}

			// --- Lectern ---
			else if (state instanceof Lectern lectern) {
				final ItemStack book = lectern.getInventory().getItem(0);
				if (book != null) {
					lectern.getInventory().setItem(0, deepCloneItem(book));
				}
				meta.setBlockState(lectern);
				clone.setItemMeta(meta);
			}

			// --- Skull ---
			else if (state instanceof Skull skull) {
				if (skull.getOwningPlayer() != null) {
					skull.setOwningPlayer(skull.getOwningPlayer());
				}
				meta.setBlockState(skull);
				clone.setItemMeta(meta);
			}

			// --- Sign (Adventure components, both sides) ---
			else if (state instanceof Sign sign) {
				if (SignReflectionHelper.isDualSided()) {
					try {
						for (Object side : SignReflectionHelper.getSideEnumConstants()) {
							Object signSide = SignReflectionHelper.invokeGetSide(sign, side);
							for (int i = 0; i < 4; i++) {
								Component line = SignReflectionHelper.invokeGetLine(signSide, i);
								SignReflectionHelper.invokeSetLine(signSide, i, line); // rewrite line for consistency
							}
						}
					} catch (Exception ex) {
						HellblockPlugin.getInstance().getPluginLogger()
								.warn("Failed to clone sign metadata: " + ex.getMessage());
					}
				} else {
					for (int i = 0; i < 4; i++) {
						Component line = SignReflectionHelper.getLine(sign, i);
						SignReflectionHelper.setLine(sign, i, line); // rewrite for consistency
					}
				}

				meta.setBlockState(sign);
				clone.setItemMeta(meta);
			}

			// Enchanting Table: preserve custom name
			else if (state instanceof EnchantingTable table) {
				Component name = AdventureMetadata.getEnchantingTableName(table);
				if (name != null) {
					AdventureMetadata.setEnchantingTableName(table, name); // ensures cross-compat
				}
				meta.setBlockState(table);
				clone.setItemMeta(meta);
			}

			// --- Command Block (Adventure component name + command) ---
			else if (state instanceof CommandBlock cmd) {
				Component name = AdventureMetadata.getCommandBlockName(cmd);
				if (name != null) {
					AdventureMetadata.setCommandBlockName(cmd, name); // Paper/Spigot safe
				}
				cmd.setCommand(cmd.getCommand()); // preserve command
				if (VersionHelper.isPaperFork()) {
					PaperReflection.setCommandBlockSuccessCount(cmd, PaperReflection.getCommandBlockSuccessCount(cmd));
				}
				meta.setBlockState(cmd);
				clone.setItemMeta(meta);
			}

			// --- Furnace / Smoker / BlastFurnace ---
			else if (state instanceof Furnace furnace) {
				final ItemStack[] contents = furnace.getInventory().getContents();
				for (int i = 0; i < contents.length; i++) {
					if (contents[i] != null) {
						furnace.getInventory().setItem(i, deepCloneItem(contents[i]));
					}
				}
				furnace.setBurnTime(furnace.getBurnTime());
				furnace.setCookTime(furnace.getCookTime());
				furnace.setCookTimeTotal(furnace.getCookTimeTotal());
				meta.setBlockState(furnace);
				clone.setItemMeta(meta);
			}

			// --- Brewing Stand ---
			else if (state instanceof BrewingStand brewing) {
				final ItemStack[] contents = brewing.getInventory().getContents();
				for (int i = 0; i < contents.length; i++) {
					if (contents[i] != null) {
						brewing.getInventory().setItem(i, deepCloneItem(contents[i]));
					}
				}
				brewing.setBrewingTime(brewing.getBrewingTime());
				brewing.setFuelLevel(brewing.getFuelLevel());
				meta.setBlockState(brewing);
				clone.setItemMeta(meta);
			}

			// --- Jukebox ---
			else if (state instanceof Jukebox jukebox && JukeboxReflectionHelper.hasRecord(jukebox)) {
				jukebox.setRecord(deepCloneItem(jukebox.getRecord()));
				meta.setBlockState(jukebox);
				clone.setItemMeta(meta);
			}

			// --- Campfire ---
			else if (state instanceof Campfire campfire) {
				for (int i = 0; i < campfire.getSize(); i++) {
					final ItemStack slotItem = campfire.getItem(i);
					if (slotItem != null) {
						campfire.setItem(i, deepCloneItem(slotItem));
					}
					campfire.setCookTime(i, campfire.getCookTime(i));
					campfire.setCookTimeTotal(i, campfire.getCookTimeTotal(i));
				}
				meta.setBlockState(campfire);
				clone.setItemMeta(meta);
			}

			// --- Chiseled Bookshelf (1.19.3+) ---
			else if (VersionHelper.isVersionNewerThan1_19_3() && PaperReflection.isChiseledBookshelf(state)) {
				Inventory bookshelfInv = PaperReflection.getChiseledBookshelfInventory(state);
				if (bookshelfInv != null) {
					final ItemStack[] contents = bookshelfInv.getContents();
					for (int i = 0; i < contents.length; i++) {
						if (contents[i] != null) {
							bookshelfInv.setItem(i, deepCloneItem(contents[i]));
						}
					}
				}
				meta.setBlockState(state); // applies the state (inventory) to item meta
				clone.setItemMeta(meta); // finalizes the clone
			}

			// --- Fallback ---
			else {
				meta.setBlockState(state);
				clone.setItemMeta(meta);
			}
		}

		return clone;
	}

	/**
	 * Create a deep clone of an array of ItemStacks, ensuring that modifications to
	 * the cloned items do not affect the original items.
	 * 
	 * @param source   The source array of ItemStacks to clone
	 * @param consumer A Consumer to receive the cloned array
	 */
	private static void deepCloneInventory(ItemStack[] source, Consumer<ItemStack[]> consumer) {
		final ItemStack[] cloned = new ItemStack[source.length];
		for (int i = 0; i < source.length; i++) {
			cloned[i] = deepCloneItem(source[i]);
		}
		consumer.accept(cloned);
	}

	/**
	 * Find entities that are "attached" to the given block location, such as
	 * paintings, item frames, armor stands, and shulkers. These entities are
	 * treated as part of the block for snapshot purposes.
	 * 
	 * @param loc The block location to check
	 * @return A list of EntitySnapshots representing the attached entities
	 */
	private static List<EntitySnapshot> findAttachedEntities(Location loc) {
		final World world = loc.getWorld();
		if (world == null) {
			return List.of();
		}

		final Block block = loc.getBlock();
		final BoundingBox box = block.getBoundingBox();

		// treat shulkers as block-attached
		return world.getNearbyEntities(box).stream()
				.filter(entity -> entity instanceof Hanging || entity instanceof Shulker
						|| (entity instanceof ArmorStand stand && stand.isVisible() && stand.isSmall()))
				.map(EntitySnapshot::fromEntity).toList();
	}

	/**
	 * Convert a Location to a CompoundBinaryTag for NBT storage.
	 * 
	 * @param loc The location to convert
	 * @return The CompoundBinaryTag representing the location
	 */
	private static CompoundBinaryTag locationToNBT(Location loc) {
		return CompoundBinaryTag.builder().putString("world", loc.getWorld().getName()).putDouble("x", loc.getX())
				.putDouble("y", loc.getY()).putDouble("z", loc.getZ()).putFloat("yaw", loc.getYaw())
				.putFloat("pitch", loc.getPitch()).build();
	}

	/**
	 * Convert an ItemStack to a CompoundBinaryTag for NBT storage.
	 * 
	 * @param item The ItemStack to convert
	 * @return The CompoundBinaryTag representing the ItemStack
	 */
	private static CompoundBinaryTag itemStackToNBT(ItemStack item) {
		if (item == null) {
			return CompoundBinaryTag.empty();
		}

		// Serialize Bukkit ItemStack to a Map
		final Map<String, Object> serialized = item.serialize();

		// Recursively build CompoundBinaryTag from Map
		return mapToNBT(serialized);
	}

	/**
	 * Convert an Inventory to a CompoundBinaryTag for NBT storage.
	 * 
	 * @param inventory The Inventory to convert
	 * @return The CompoundBinaryTag representing the Inventory
	 */
	private static CompoundBinaryTag inventoryToNBT(Inventory inventory) {
		final CompoundBinaryTag.Builder builder = CompoundBinaryTag.builder();
		for (int i = 0; i < inventory.getSize(); i++) {
			final ItemStack item = inventory.getItem(i);
			if (item != null) {
				builder.put(String.valueOf(i), itemStackToNBT(item));
			}
		}
		return builder.build();
	}

	/**
	 * Recursively convert a Map<String, Object> to a CompoundBinaryTag. Supports
	 * nested maps, lists, and primitive types.
	 * 
	 * @param map The map to convert
	 * @return The corresponding CompoundBinaryTag
	 */
	private static CompoundBinaryTag mapToNBT(Map<String, Object> map) {
		final CompoundBinaryTag.Builder tagBuilder = CompoundBinaryTag.builder();
		map.forEach((key, value) -> tagBuilder.put(key, objectToTag(value)));
		return tagBuilder.build();
	}

	/**
	 * Convert a generic Object to a BinaryTag for NBT storage. Supports primitives,
	 * strings, maps, lists, and enums.
	 * 
	 * @param value The object to convert
	 * @return The corresponding BinaryTag
	 */
	private static BinaryTag objectToTag(Object value) {
		if (value == null) {
			return StringBinaryTag.stringBinaryTag("null");
		}

		// --- Primitives ---
		if (value instanceof String str) {
			return StringBinaryTag.stringBinaryTag(str);
		}
		if (value instanceof Integer i) {
			return IntBinaryTag.intBinaryTag(i);
		}
		if (value instanceof Short s) {
			return ShortBinaryTag.shortBinaryTag(s);
		}
		if (value instanceof Byte b) {
			return ByteBinaryTag.byteBinaryTag(b);
		}
		if (value instanceof Boolean b) {
			return ByteBinaryTag.byteBinaryTag((byte) (b ? 1 : 0));
		}
		if (value instanceof Long l) {
			return LongBinaryTag.longBinaryTag(l);
		}
		if (value instanceof Float f) {
			return FloatBinaryTag.floatBinaryTag(f);
		}
		if (value instanceof Double d) {
			return DoubleBinaryTag.doubleBinaryTag(d);
		}

		// --- Map (nested compound tags) ---
		if (value instanceof Map<?, ?> nestedMap) {
			final Map<String, Object> stringKeyedMap = new HashMap<>();
			nestedMap.forEach((k, v) -> stringKeyedMap.put(String.valueOf(k), v));
			return mapToNBT(stringKeyedMap);
		}

		// --- Lists ---
		if (value instanceof List<?> list) {
			final Builder<BinaryTag> listBuilder = ListBinaryTag.builder();
			list.forEach((Object element) -> listBuilder.add(objectToTag(element)));
			return listBuilder.build();
		}

		// --- Enums ---
		if (value instanceof Enum<?> e) {
			return StringBinaryTag.stringBinaryTag(e.name());
		}

		// Fallback: toString()
		return StringBinaryTag.stringBinaryTag(value.toString());
	}

	/**
	 * Convert a CompoundBinaryTag back into an ItemStack.
	 * 
	 * @param tag The CompoundBinaryTag to convert
	 * @return The reconstructed ItemStack
	 */
	private static ItemStack itemStackFromNBT(CompoundBinaryTag tag) {
		// Deserialize ItemStack from Bukkit ConfigurationSerializable map stored in NBT
		final Map<String, Object> map = new HashMap<>();
		tag.keySet().forEach(key -> map.put(key, tagToObject(tag.get(key))));
		return ItemStack.deserialize(map);
	}

	/**
	 * Populate an Inventory from a CompoundBinaryTag representation.
	 * 
	 * @param tag       The CompoundBinaryTag containing inventory data
	 * @param inventory The Inventory to populate
	 */
	private static void inventoryFromNBT(CompoundBinaryTag tag, Inventory inventory) {
		tag.keySet().forEach(key -> {
			try {
				final int slot = Integer.parseInt(key);
				final BinaryTag itemTag = tag.get(key);
				if (itemTag instanceof CompoundBinaryTag compound) {
					inventory.setItem(slot, itemStackFromNBT(compound));
				}
			} catch (NumberFormatException ignored) {
				// Skip non-integer keys
			}
		});
	}

	/**
	 * Convert an Adventure BinaryTag to a standard Java Object.
	 * 
	 * @param tag The BinaryTag to convert
	 * @return The corresponding Java Object, or null if unsupported
	 */
	private static Object tagToObject(BinaryTag tag) {
		final BinaryTagType<?> type = tag.type();

		if (type == BinaryTagTypes.STRING) {
			return ((StringBinaryTag) tag).value();
		}
		if (type == BinaryTagTypes.INT) {
			return ((IntBinaryTag) tag).intValue();
		}
		if (type == BinaryTagTypes.SHORT) {
			return ((ShortBinaryTag) tag).shortValue();
		}
		if (type == BinaryTagTypes.BYTE) {
			return ((ByteBinaryTag) tag).byteValue();
		}
		if (type == BinaryTagTypes.LONG) {
			return ((LongBinaryTag) tag).longValue();
		}
		if (type == BinaryTagTypes.FLOAT) {
			return ((FloatBinaryTag) tag).floatValue();
		}
		if (type == BinaryTagTypes.DOUBLE) {
			return ((DoubleBinaryTag) tag).doubleValue();
		}

		if (type == BinaryTagTypes.COMPOUND) {
			final CompoundBinaryTag compound = (CompoundBinaryTag) tag;
			return compound.keySet().stream().collect(Collectors.toMap(k -> k, k -> tagToObject(compound.get(k))));
		}

		if (type != BinaryTagTypes.LIST) {
			return null;
		}

		final ListBinaryTag list = (ListBinaryTag) tag;
		return list.stream().map(IslandSnapshotBlock::tagToObject).collect(Collectors.toList());
	}
}