package com.swiftlicious.hellblock.schematic;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Rotation;
import org.bukkit.World;
import org.bukkit.block.Banner;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.BrewingStand;
import org.bukkit.block.Campfire;
import org.bukkit.block.Container;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.block.Furnace;
import org.bukkit.block.Jukebox;
import org.bukkit.block.Lectern;
import org.bukkit.block.ShulkerBox;
import org.bukkit.block.Sign;
import org.bukkit.block.Skull;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.banner.PatternType;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Painting;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.LecternInventory;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.config.locale.MessageConstants;
import com.swiftlicious.hellblock.handlers.AdventureHelper;
import com.swiftlicious.hellblock.handlers.VersionHelper;
import com.swiftlicious.hellblock.nms.entity.armorstand.FakeArmorStand;
import com.swiftlicious.hellblock.player.GameProfileBuilder;
import com.swiftlicious.hellblock.scheduler.SchedulerTask;
import com.swiftlicious.hellblock.schematic.SchematicManager.SpawnSearchMode;
import com.swiftlicious.hellblock.utils.ParticleUtils;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.ByteBinaryTag;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.IntArrayBinaryTag;
import net.kyori.adventure.nbt.IntBinaryTag;
import net.kyori.adventure.nbt.ListBinaryTag;
import net.kyori.adventure.nbt.ShortBinaryTag;
import net.kyori.adventure.nbt.StringBinaryTag;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.sound.Sound.Source;
import net.kyori.adventure.text.Component;

public class SchematicAsync implements SchematicPaster {

	private static final Map<File, SchematicData> schematicCache = new ConcurrentHashMap<>();
	private static final int pasterDelay = 1;
	private static final int pasterLimit = 10;
	private static final int AIR_BLOCK_THRESHOLD = 20; // hardcoded air block threshold
	private static final long PASTE_TIMEOUT_TICKS = 200L; // 10 seconds (20 ticks/second)

	private final Map<UUID, SchedulerTask> runningPastes = new ConcurrentHashMap<>();
	private final Map<UUID, Integer> pasteProgress = new ConcurrentHashMap<>(); // percentage (0–100)
	private final Map<UUID, Integer> totalBlocks = new ConcurrentHashMap<>(); // total block count
	private final Map<UUID, SchedulerTask> pasteTimeouts = new ConcurrentHashMap<>();

	protected final HellblockPlugin instance;

	public SchematicAsync(HellblockPlugin plugin) {
		instance = plugin;
	}

	@Override
	public CompletableFuture<Location> pasteHellblock(UUID playerId, File file, Location location,
			boolean ignoreAirBlock, SchematicMetadata metadata, boolean animated) {
		CompletableFuture<Location> future = new CompletableFuture<>();

		instance.getScheduler().executeAsync(() -> {
			final SchematicData schematicData = getSchematicData(file);
			final Set<Coordinate> coordinates = getCoordinates(schematicData);

			short length = schematicData.length;
			short width = schematicData.width;

			if (ignoreAirBlock) {
				coordinates.removeIf(coord -> {
					int index = coord.y() * width * length + coord.z() * width + coord.x();
					for (String blockData : schematicData.palette.keySet()) {
						int val = ((IntBinaryTag) schematicData.palette.get(blockData)).value();
						if (schematicData.blockdata[index] == val) {
							return blockData.contains("air");
						}
					}
					return true;
				});
			}

			if (coordinates.size() < AIR_BLOCK_THRESHOLD) {
				instance.getPluginLogger()
						.warn("Skipped schematic '" + file.getName() + "' (below air block threshold)");
				future.complete(null);
				return;
			}

			// Async thread -> schedule sync paste
			instance.getScheduler().executeSync(() -> pasteBlocks(playerId, file, schematicData, coordinates,
					location.clone(), ignoreAirBlock, metadata, animated, future));
		});

		return future;
	}

	private void pasteBlocks(UUID playerId, File file, SchematicData schematicData, Set<Coordinate> coordinates,
			Location location, boolean ignoreAirBlock, SchematicMetadata metadata, boolean animated,
			CompletableFuture<Location> future) {

		final short length = schematicData.length;
		final short width = schematicData.width;
		final short height = schematicData.height;

		location.subtract(width / 2.0, height / 2.0, length / 2.0); // center paste

		final Iterator<Coordinate> iterator = coordinates.iterator();
		final int total = coordinates.size();
		final int[] placedBlocks = { 0 };
		final long startTime = System.currentTimeMillis();

		totalBlocks.put(playerId, total);
		pasteProgress.put(playerId, 0);

		Player player = Bukkit.getPlayer(playerId);

		if (animated && player != null && player.isOnline()) {
			long durationTicks = (total / (long) pasterLimit) + 20L;

			instance.getStorageManager().getOnlineUser(playerId).ifPresent(userData -> {
				FakeArmorStand camera = instance.getIslandGenerator().createCameraAnchor(userData, location);
				instance.getIslandGenerator().animateCamera(camera, location, player, durationTicks);
			});
		}

		final SchedulerTask[] actionBarTaskRef = new SchedulerTask[1];
		if (animated && player != null && player.isOnline()) {
			actionBarTaskRef[0] = instance.getScheduler().sync().runRepeating(new Runnable() {
				int secondsElapsed = 0;

				@Override
				public void run() {
					if (future.isDone()) {
						if (actionBarTaskRef[0] != null && !actionBarTaskRef[0].isCancelled())
							actionBarTaskRef[0].cancel();
						return;
					}

					int percent = (int) ((placedBlocks[0] / (double) total) * 100);
					VersionHelper.getNMSManager().sendActionBar(player, AdventureHelper.componentToJson(instance
							.getTranslationManager()
							.render(MessageConstants.MSG_HELLBLOCK_SCHEMATIC_PROGRESS_BAR
									.arguments(AdventureHelper.miniMessageToComponent(String.format("%d%%", percent)),
											AdventureHelper.miniMessageToComponent(String.valueOf(secondsElapsed)))
									.build())));
					secondsElapsed++;
				}
			}, 20L, 20L, location);
		}

		Runnable runnable = () -> {
			int remaining = pasterLimit;

			while (remaining > 0 && iterator.hasNext()) {
				Coordinate coord = iterator.next();
				int x = coord.x(), y = coord.y(), z = coord.z();
				int index = y * width * length + z * width + x;

				Location blockLoc = location.clone().add(x, y, z);
				Block block = blockLoc.getBlock();

				for (String blockData : schematicData.palette.keySet()) {
					int i = SchematicData.getChildTag(schematicData.palette, blockData, IntBinaryTag.class).value();
					if (schematicData.blockdata[index] == i) {
						BlockData data = Bukkit.createBlockData(blockData);
						if (data.getMaterial() == Material.AIR && ignoreAirBlock)
							continue;

						block.setBlockData(data, false);

						if (animated && player != null && player.isOnline()) {
							player.spawnParticle(ParticleUtils.getParticle("BLOCK_CRACK"), blockLoc, 5, data);
							AdventureHelper.playPositionalSound(player.getWorld(), blockLoc,
									Sound.sound(Key.key("minecraft:block.stone.place"), Source.BLOCK, 0.5f, 1.2f));
						}

						placedBlocks[0]++;
						remaining--;
					}
				}
			}

			if (!iterator.hasNext()) {
				SchedulerTask running = runningPastes.remove(playerId);
				if (running != null && !running.isCancelled())
					running.cancel();

				SchedulerTask timeout = pasteTimeouts.remove(playerId);
				if (timeout != null && !timeout.isCancelled())
					timeout.cancel();

				if (actionBarTaskRef[0] != null && !actionBarTaskRef[0].isCancelled())
					actionBarTaskRef[0].cancel();

				schematicData.tileEntities.forEach(tag -> {
					final int[] pos = ((IntArrayBinaryTag) tag.get("Pos")).value();
					final Location blockLocation = location.clone().add(pos[0], pos[1], pos[2]);
					BlockState state = blockLocation.getBlock().getState();

					restoreInventory(state, tag);
					restoreTileEntity(state, tag, blockLocation,
							player != null ? player.getName()
									: instance.getTranslationManager()
											.miniMessageTranslation(MessageConstants.FORMAT_UNKNOWN.build().key()));
				});

				restoreEntitiesFromSchematic(schematicData, location);

				Vector treeVec = metadata.getTree();
				Location treeLoc = treeVec != null ? treeVec.toLocation(location.getWorld())
						: location.clone().add(width / 2.0, 0, length / 2.0); // fallback center

				Optional<TreeAnimationData> treeDataOpt = scanGlowstoneTree(treeLoc, 5);
				treeDataOpt.ifPresentOrElse(treeData -> {
					totalBlocks.put(playerId, total + treeData.getStagedBlocks().size());
					animateTreeFromScanned(playerId, treeData, future, location, metadata, width, height, length);
				}, () -> {
					Location safeSpawn = (metadata.getHome() != null) ? location.clone().add(metadata.getHome())
							: instance.getSchematicManager().findSafeSpawn(location.getWorld(), location.clone(), width,
									height, length, SpawnSearchMode.CENTER);

					long duration = System.currentTimeMillis() - startTime;
					instance.getPluginLogger().info("Pasted schematic '" + file.getName() + "' with " + placedBlocks[0]
							+ " blocks in " + (duration / 1000.0) + "s.");

					pasteProgress.remove(playerId);
					totalBlocks.remove(playerId);

					future.complete(safeSpawn);
				});
			}
		};

		SchedulerTask scheduledTask = instance.getScheduler().sync().runRepeating(runnable, pasterDelay, pasterDelay,
				location);
		runningPastes.put(playerId, scheduledTask);

		SchedulerTask timeoutTask = instance.getScheduler().sync().runLater(() -> {
			if (future.isDone())
				return;

			pasteProgress.remove(playerId);
			totalBlocks.remove(playerId);

			SchedulerTask running = runningPastes.remove(playerId);
			if (running != null && !running.isCancelled())
				running.cancel();

			if (actionBarTaskRef[0] != null && !actionBarTaskRef[0].isCancelled()) {
				actionBarTaskRef[0].cancel();
			}

			instance.getStorageManager().getCachedUserData(playerId)
					.ifPresent(userData -> instance.getIslandGenerator().cleanupAnimation(userData));
			future.completeExceptionally(new TimeoutException("Schematic paste timed out after 10 seconds."));
			instance.getPluginLogger().warn("InternalAsync schematic paste for player " + playerId + " timed out.");
		}, PASTE_TIMEOUT_TICKS, location);

		pasteTimeouts.put(playerId, timeoutTask);
	}

	public Optional<TreeAnimationData> scanGlowstoneTree(@NotNull Location center, int radius) {
		Set<Block> visited = new HashSet<>();
		Queue<Block> queue = new ArrayDeque<>();

		Block base = center.getBlock();
		if (!isGlowstoneTreeBlock(base))
			return Optional.empty();

		queue.add(base);

		while (!queue.isEmpty()) {
			Block current = queue.poll();
			if (!visited.add(current))
				continue;

			for (int dx = -1; dx <= 1; dx++) {
				for (int dy = -1; dy <= 1; dy++) {
					for (int dz = -1; dz <= 1; dz++) {
						if (Math.abs(dx) + Math.abs(dy) + Math.abs(dz) > 1)
							continue;

						Block neighbor = current.getRelative(dx, dy, dz);
						if (neighbor.getLocation().distance(center) > radius)
							continue;

						if (!visited.contains(neighbor) && isGlowstoneTreeBlock(neighbor)) {
							queue.add(neighbor);
						}
					}
				}
			}
		}

		if (visited.isEmpty())
			return Optional.empty();
		return Optional.of(new TreeAnimationData(visited));
	}

	public void animateTreeFromScanned(@NotNull UUID playerId, @NotNull TreeAnimationData treeData,
			@NotNull CompletableFuture<Location> future, @NotNull Location location,
			@NotNull SchematicMetadata metadata, int width, int height, int length) {

		List<List<Block>> stages = new ArrayList<>(treeData.getStagedBlocks().values());
		AtomicInteger step = new AtomicInteger();
		Block top = treeData.getTopBlock();
		Location topLocation = top.getLocation().clone().add(0.5, 0.5, 0.5);

		final SchedulerTask[] taskRef = new SchedulerTask[1]; // Mutable reference

		taskRef[0] = instance.getScheduler().sync().runRepeating(() -> {
			int currentStep = step.getAndIncrement();

			if (currentStep >= stages.size()) {
				World world = top.getWorld();
				world.spawnParticle(Particle.CLOUD, topLocation, 30, 0.4, 0.3, 0.4, 0.02);
				world.spawnParticle(Particle.END_ROD, topLocation, 20, 0.2, 0.3, 0.2, 0.01);
				world.strikeLightningEffect(topLocation);
				AdventureHelper.playPositionalSound(world, topLocation,
						Sound.sound(Key.key("minecraft:entity.lightning_bolt.thunder"), Source.BLOCK, 0.8f, 1.0f));

				Location spawn = (metadata.getHome() != null) ? location.clone().add(metadata.getHome())
						: instance.getSchematicManager().findSafeSpawn(location.getWorld(), location, width, height,
								length, SpawnSearchMode.CENTER);

				pasteProgress.remove(playerId);
				totalBlocks.remove(playerId);
				runningPastes.remove(playerId);

				future.complete(spawn);

				if (taskRef[0] != null && !taskRef[0].isCancelled()) {
					taskRef[0].cancel();
				}
				return;
			}

			List<Block> layer = stages.get(currentStep);
			layer.forEach(b -> {
				Location loc = b.getLocation().clone().add(0.5, 0.5, 0.5);
				b.getWorld().spawnParticle(Particle.END_ROD, loc, 8, 0.1, 0.1, 0.1, 0.01);
				AdventureHelper.playPositionalSound(b.getWorld(), loc,
						Sound.sound(Key.key("minecraft:block.amethyst_block.hit"), Source.BLOCK, 0.6f, 1.4f));
			});

			pasteProgress.compute(playerId, (k, v) -> {
				int progress = (int) (((double) (currentStep + 1) / stages.size()) * 100);
				return Math.min(progress, 100);
			});
		}, 0L, 3L, topLocation);
	}

	private boolean isGlowstoneTreeBlock(Block block) {
		Material mat = block.getType();
		return mat == Material.GLOWSTONE || mat == Material.GRAVEL;
	}

	private void restoreEntitiesFromSchematic(SchematicData schematicData, Location origin) {
		World world = origin.getWorld();
		if (world == null || schematicData.entities.isEmpty())
			return;

		Map<UUID, UUID> leashMap = new HashMap<>(); // store entity->leashHolder temporarily
		Map<UUID, UUID> vehicleMap = new HashMap<>(); // store entity->vehicle temporarily
		Map<UUID, Entity> spawnedEntities = new HashMap<>();

		// === First pass: spawn all entities ===
		for (CompoundBinaryTag entityTag : schematicData.entities) {
			try {
				// === Position ===
				List<Double> pos = ((ListBinaryTag) entityTag.get("Pos")).stream()
						.map(t -> Double.parseDouble(((StringBinaryTag) t).value())).toList();

				double x = pos.get(0) + origin.getX();
				double y = pos.get(1) + origin.getY();
				double z = pos.get(2) + origin.getZ();

				String entityId = ((StringBinaryTag) entityTag.get("id")).value().toLowerCase().replace("minecraft:",
						"");
				EntityType type = EntityType.valueOf(entityId.toUpperCase());

				if (type == null) {
					instance.getPluginLogger().warn("Unknown entity type: " + entityId);
					continue;
				}

				Location spawnLoc = new Location(world, x, y, z);
				Entity entity = world.spawnEntity(spawnLoc, type);
				spawnedEntities.put(entity.getUniqueId(), entity);

				// === Custom Name ===
				if (entityTag.get("CustomName") instanceof StringBinaryTag customName) {
					Component name = AdventureMetadata.deserialize(customName.value());
					if (name != null) {
						AdventureMetadata.setEntityCustomName(entity, name);
					}
				}

				// === Rotation ===
				if (entityTag.get("Rotation") instanceof ListBinaryTag rotationTag) {
					try {
						float yaw = Float.parseFloat(((StringBinaryTag) rotationTag.get(0)).value());
						float pitch = Float.parseFloat(((StringBinaryTag) rotationTag.get(1)).value());
						entity.teleport(new Location(world, x, y, z, yaw, pitch));
					} catch (Exception ignored) {
					}
				}

				// === ItemFrame ===
				if (entity instanceof ItemFrame frame) {
					if (entityTag.get("Item") instanceof CompoundBinaryTag itemTag) {
						frame.setItem(deserializeItem(itemTag));
					}
					if (entityTag.get("ItemRotation") instanceof ByteBinaryTag rotTag) {
						int rotation = rotTag.value();
						frame.setRotation(Rotation.values()[rotation % Rotation.values().length]);
					}
					if (entityTag.get("Fixed") instanceof ByteBinaryTag fixedTag) {
						frame.setFixed(fixedTag.value() != 0);
					}
				}

				// === Painting ===
				if (entity instanceof Painting painting) {
					if (entityTag.get("Motive") instanceof StringBinaryTag motiveTag) {
						String motive = motiveTag.value();
						painting.setArt(VariantRegistry.getPaintingVariant(motive)); // Your registry
					}
				}

				// === Passengers ===
				if (entityTag.get("Passengers") instanceof ListBinaryTag passengers) {
					for (BinaryTag passengerTag : passengers) {
						if (passengerTag instanceof CompoundBinaryTag passengerCompound) {
							Entity passenger = spawnEntityFromNBT(passengerCompound, origin);
							if (passenger != null) {
								entity.addPassenger(passenger);
								spawnedEntities.put(passenger.getUniqueId(), passenger);
							}
						}
					}
				}

				// === Equipment ===
				if (entity instanceof LivingEntity living) {
					try {
						if (entityTag.get("ArmorItems") instanceof ListBinaryTag armorTags) {
							ItemStack[] armor = new ItemStack[armorTags.size()];
							for (int i = 0; i < armorTags.size(); i++) {
								BinaryTag armorTag = armorTags.get(i);
								armor[i] = armorTag instanceof CompoundBinaryTag compound ? deserializeItem(compound)
										: new ItemStack(Material.AIR);
							}
							living.getEquipment().setArmorContents(armor);
						}

						if (entityTag.get("HandItems") instanceof ListBinaryTag handTags) {
							if (handTags.size() >= 1 && handTags.get(0) instanceof CompoundBinaryTag main) {
								living.getEquipment().setItemInMainHand(deserializeItem(main));
							}
							if (handTags.size() >= 2 && handTags.get(1) instanceof CompoundBinaryTag off) {
								living.getEquipment().setItemInOffHand(deserializeItem(off));
							}
						}
					} catch (Exception ex) {
						instance.getPluginLogger().warn("Failed to apply equipment to entity: " + ex.getMessage());
					}
				}

				// === Store leash target (to resolve after spawn) ===
				if (entityTag.get("Leash") instanceof StringBinaryTag leashTag) {
					try {
						UUID holderUUID = UUID.fromString(leashTag.value());
						leashMap.put(entity.getUniqueId(), holderUUID);
					} catch (Exception ex) {
						instance.getPluginLogger().warn("Invalid leash UUID: " + leashTag.value());
					}
				}

				// === Store vehicle (entity riding another entity) ===
				if (entityTag.get("Vehicle") instanceof StringBinaryTag vehicleTag) {
					try {
						UUID vehicleUUID = UUID.fromString(vehicleTag.value());
						vehicleMap.put(entity.getUniqueId(), vehicleUUID);
					} catch (Exception ex) {
						instance.getPluginLogger().warn("Invalid vehicle UUID: " + vehicleTag.value());
					}
				}

			} catch (Exception ex) {
				instance.getPluginLogger().warn("Failed to restore entity from schematic: " + ex.getMessage());
			}
		}

		// === Second pass: resolve leashes and vehicles after all entities are spawned
		// ===
		leashMap.entrySet().forEach(leashEntry -> {
			Entity leashed = spawnedEntities.get(leashEntry.getKey());
			Entity holder = spawnedEntities.get(leashEntry.getValue());
			if (leashed instanceof LivingEntity living && holder instanceof LivingEntity) {
				living.setLeashHolder(holder);
			}
		});

		vehicleMap.entrySet().forEach(vehicleEntry -> {
			Entity passenger = spawnedEntities.get(vehicleEntry.getKey());
			Entity vehicle = spawnedEntities.get(vehicleEntry.getValue());
			if (passenger != null && vehicle != null) {
				vehicle.addPassenger(passenger);
			}
		});
	}

	private void restoreInventory(BlockState state, CompoundBinaryTag tags) {
		if (!(state instanceof Container container)) {
			return;
		}

		final ListBinaryTag items;
		try {
			items = SchematicData.getChildTag(tags, "Items", ListBinaryTag.class);
		} catch (IllegalArgumentException e) {
			return; // No items present or wrong type
		}

		for (BinaryTag item : items) {
			if (!(item instanceof CompoundBinaryTag itemTag)) {
				continue;
			}

			try {
				final byte slot = SchematicData.getChildTag(itemTag, "Slot", ByteBinaryTag.class).value();
				final String name = SchematicData.getChildTag(itemTag, "id", StringBinaryTag.class).value()
						.toLowerCase().replace("minecraft:", "").replace("reeds", "sugar_cane");

				final byte amount = SchematicData.getChildTag(itemTag, "Count", ByteBinaryTag.class).value();
				final Material material = Material.matchMaterial(name.toUpperCase(Locale.ROOT));

				if (material == null || material == Material.AIR) {
					continue;
				}

				final ItemStack itemStack = new ItemStack(material, amount);

				if (state instanceof ShulkerBox shulker) {
					shulker.getInventory().setItem(slot, itemStack);
				} else {
					container.getInventory().setItem(slot, itemStack);
				}
			} catch (NoSuchFieldError | IllegalArgumentException ignored) {
				// Skip items with missing/invalid tags
			}
		}

		if (state instanceof ShulkerBox shulker) {
			shulker.update();
		} else {
			container.update();
		}
	}

	private void restoreTileEntity(BlockState state, CompoundBinaryTag tag, Location blockLocation, String ownerName) {
		// Furnace
		if (state instanceof Furnace furnace) {
			try {
				furnace.setBurnTime(SchematicData.getChildTag(tag, "BurnTime", ShortBinaryTag.class).value());
				furnace.setCookTime(SchematicData.getChildTag(tag, "CookTime", ShortBinaryTag.class).value());
				furnace.setCookTimeTotal(SchematicData.getChildTag(tag, "CookTimeTotal", ShortBinaryTag.class).value());

				if (tag.get("Items") instanceof ListBinaryTag items) {
					for (BinaryTag itemTag : items) {
						if (!(itemTag instanceof CompoundBinaryTag itemCompound)) {
							continue;
						}

						final byte slot = SchematicData.getChildTag(itemCompound, "Slot", ByteBinaryTag.class).value();
						final String name = SchematicData.getChildTag(itemCompound, "id", StringBinaryTag.class).value()
								.toLowerCase().replace("minecraft:", "");
						final byte amount = SchematicData.getChildTag(itemCompound, "Count", ByteBinaryTag.class)
								.value();
						final Material material = Material.matchMaterial(name.toUpperCase(Locale.ROOT));

						if (material != null && slot >= 0 && slot <= 2) {
							furnace.getInventory().setItem(slot, new ItemStack(material, amount));
						}
					}
				}
				furnace.update();
			} catch (Exception ex) {
				instance.getPluginLogger()
						.warn("Failed to restore furnace metadata at " + blockLocation + ": " + ex.getMessage());
			}
		}

		// Brewing Stand
		if (state instanceof BrewingStand brewingStand) {
			try {
				brewingStand.setBrewingTime(SchematicData.getChildTag(tag, "BrewTime", ShortBinaryTag.class).value());

				if (tag.get("Items") instanceof ListBinaryTag items) {
					for (BinaryTag itemTag : items) {
						if (!(itemTag instanceof CompoundBinaryTag itemCompound)) {
							continue;
						}

						final byte slot = SchematicData.getChildTag(itemCompound, "Slot", ByteBinaryTag.class).value();
						final String name = SchematicData.getChildTag(itemCompound, "id", StringBinaryTag.class).value()
								.toLowerCase().replace("minecraft:", "");
						final byte amount = SchematicData.getChildTag(itemCompound, "Count", ByteBinaryTag.class)
								.value();
						final Material material = Material.matchMaterial(name.toUpperCase(Locale.ROOT));

						if (material != null && slot >= 0 && slot <= 4) {
							brewingStand.getInventory().setItem(slot, new ItemStack(material, amount));
						}
					}
				}
				brewingStand.update();
			} catch (Exception ex) {
				instance.getPluginLogger()
						.warn("Failed to restore brewing stand metadata at " + blockLocation + ": " + ex.getMessage());
			}
		}

		// Lectern
		if (state instanceof Lectern lectern) {
			try {
				if (tag.get("Book") instanceof CompoundBinaryTag bookTag) {
					final String titleRaw = SchematicData.getChildTag(bookTag, "title", StringBinaryTag.class).value();
					final String authorRaw = SchematicData.getChildTag(bookTag, "author", StringBinaryTag.class)
							.value();
					final ListBinaryTag pagesTag = SchematicData.getChildTag(bookTag, "pages", ListBinaryTag.class);

					final ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
					final BookMeta meta = (BookMeta) book.getItemMeta();

					if (titleRaw != null) {
						String replacedTitle = titleRaw.replace("{player}", ownerName);
						meta.setTitle(replacedTitle);
					}

					if (authorRaw != null) {
						String replacedAuthor = authorRaw.replace("{player}", ownerName);
						meta.setAuthor(replacedAuthor);
					}

					final List<Component> pages = new ArrayList<>();
					for (BinaryTag page : pagesTag) {
						if (page instanceof StringBinaryTag sbt) {
							String rawPage = sbt.value();
							String replacedPage = rawPage.replace("{player}", ownerName);

							Component pageComponent;
							try {
								pageComponent = AdventureHelper.jsonToComponent(replacedPage);
							} catch (Exception e) {
								pageComponent = AdventureHelper
										.miniMessageToComponent(AdventureHelper.legacyToMiniMessage(replacedPage));
							}

							pages.add(pageComponent);
						}
					}

					AdventureMetadata.setBookPages(meta, pages);
					book.setItemMeta(meta);

					((LecternInventory) lectern.getInventory()).setItem(0, book);
					lectern.setPage(0);
					lectern.update();
				}
			} catch (Exception ex) {
				instance.getPluginLogger()
						.warn("Failed to restore lectern book metadata at " + blockLocation + ": " + ex.getMessage());
			}
		}

		// Banner
		if (state instanceof Banner banner) {
			try {
				final int baseIndex = SchematicData.getChildTag(tag, "Base", IntBinaryTag.class).value();
				banner.setBaseColor(DyeColor.values()[baseIndex]);

				if (tag.get("Patterns") instanceof ListBinaryTag patterns) {
					for (BinaryTag p : patterns) {
						if (!(p instanceof CompoundBinaryTag compound)) {
							continue;
						}

						final int colorIndex = SchematicData.getChildTag(compound, "Color", IntBinaryTag.class).value();
						final String patternId = SchematicData.getChildTag(compound, "Pattern", StringBinaryTag.class)
								.value();
						final DyeColor color = DyeColor.values()[colorIndex];

						final PatternType type = VariantRegistry.getBannerPattern(patternId);

						if (type != null) {
							banner.addPattern(new Pattern(color, type));
						}
					}
				}
				banner.update();
			} catch (Exception ex) {
				instance.getPluginLogger()
						.warn("Failed to restore banner metadata at " + blockLocation + ": " + ex.getMessage());
			}
		}

		// Sign
		if (state instanceof Sign sign) {
			try {
				List<Component> processedLines = new ArrayList<>(4);

				// Deserialize the raw JSON lines and replace placeholders
				for (int i = 0; i < 4; i++) {
					final String key = "Text" + (i + 1);
					final String rawJson = tag.get(key) instanceof StringBinaryTag sbt ? sbt.value() : "";

					Component line;
					try {
						// Replace {player} BEFORE parsing into a Component
						String replacedJson = rawJson.replace("{player}", ownerName);
						line = AdventureHelper.jsonToComponent(replacedJson);
					} catch (Exception e) {
						// Fallback to legacy → MiniMessage
						String fallback = AdventureHelper.legacyToMiniMessage(rawJson.replace("{player}", ownerName));
						line = AdventureHelper.miniMessageToComponent(fallback);
					}

					processedLines.add(line);
				}

				// Set lines using dual-version reflection helper
				if (SignReflectionHelper.isDualSided()) {
					// In 1.20+, write to both FRONT and BACK sides
					for (Object sideEnum : SignReflectionHelper.getSideEnumConstants()) {
						Object signSide = SignReflectionHelper.invokeGetSide(sign, sideEnum);
						for (int i = 0; i < 4; i++) {
							SignReflectionHelper.invokeSetLine(signSide, i, processedLines.get(i));
						}
					}
				} else {
					// In 1.19.4 or earlier: single side only
					for (int i = 0; i < 4; i++) {
						SignReflectionHelper.setLine(sign, i, processedLines.get(i));
					}
				}

				sign.update();

			} catch (Exception ex) {
				instance.getPluginLogger()
						.warn("Failed to restore sign metadata at " + blockLocation + ": " + ex.getMessage());
			}
		}

		// Beehive
		if (state.getBlockData() instanceof org.bukkit.block.data.type.Beehive beehiveData) {
			try {
				int honeyLevel = SchematicData.getChildTag(tag, "honey_level", IntBinaryTag.class).value();
				honeyLevel = Math.min(5, Math.max(0, honeyLevel));
				beehiveData.setHoneyLevel(honeyLevel);
				state.setBlockData(beehiveData);
			} catch (Exception ex) {
				instance.getPluginLogger()
						.warn("Failed to restore beehive metadata at " + blockLocation + ": " + ex.getMessage());
			}
		}

		// Skull
		if (state instanceof Skull skull) {
			try {
				if (tag.get("SkullOwner") instanceof CompoundBinaryTag skullOwner) {
					if (skullOwner.get("Properties") instanceof CompoundBinaryTag properties) {
						if (properties.get("textures") instanceof ListBinaryTag textureList) {
							for (BinaryTag textureTag : textureList) {
								if (!(textureTag instanceof CompoundBinaryTag textureData)) {
									continue;
								}
								final String base64 = SchematicData
										.getChildTag(textureData, "Value", StringBinaryTag.class).value();
								applySkullTexture(skull, base64);
							}
						}
					}
				}
			} catch (Exception ex) {
				instance.getPluginLogger().warn("Failed to restore skull texture: " + ex.getMessage());
			}
		}

		// Spawner
		if (state instanceof CreatureSpawner spawner) {
			try {
				if (tag.get("SpawnData") instanceof CompoundBinaryTag spawnData) {
					final String entityId = SchematicData.getChildTag(spawnData, "id", StringBinaryTag.class).value()
							.toLowerCase().replace("minecraft:", "");
					final EntityType type = EntityType.valueOf(entityId.toUpperCase());
					spawner.setSpawnedType(type);
				}
				if (tag.get("SpawnCount") instanceof IntBinaryTag countTag) {
					spawner.setSpawnCount(countTag.value());
				}
				if (tag.get("SpawnRange") instanceof IntBinaryTag rangeTag) {
					spawner.setSpawnRange(rangeTag.value());
				}
				if (tag.get("Delay") instanceof IntBinaryTag delayTag) {
					spawner.setDelay(delayTag.value());
				}
				spawner.update();
			} catch (Exception ex) {
				instance.getPluginLogger()
						.warn("Failed to restore spawner metadata at " + blockLocation + ": " + ex.getMessage());
			}
		}

		// Campfire
		if (state instanceof Campfire campfire) {
			try {
				if (tag.get("Items") instanceof ListBinaryTag items) {
					for (BinaryTag item : items) {
						if (!(item instanceof CompoundBinaryTag itemCompound)) {
							continue;
						}

						final int slot = SchematicData.getChildTag(itemCompound, "Slot", ByteBinaryTag.class).value();
						final String itemId = SchematicData.getChildTag(itemCompound, "id", StringBinaryTag.class)
								.value().toLowerCase().replace("minecraft:", "");
						final int count = SchematicData.getChildTag(itemCompound, "Count", ByteBinaryTag.class).value();
						final Material material = Material.matchMaterial(itemId.toUpperCase(Locale.ROOT));
						if (material != null && slot >= 0 && slot < campfire.getSize()) {
							campfire.setItem(slot, new ItemStack(material, count));
						}
					}
				}

				if (tag.get("Lit") instanceof ByteBinaryTag litTag
						&& state.getBlockData() instanceof org.bukkit.block.data.type.Campfire campfireData) {
					campfireData.setLit(litTag.value() != 0);
					state.setBlockData(campfireData);
				}

				campfire.update();
			} catch (Exception ex) {
				instance.getPluginLogger()
						.warn("Failed to restore campfire metadata at " + blockLocation + ": " + ex.getMessage());
			}
		}

		// Jukebox
		if (state instanceof Jukebox jukebox) {
			try {
				if (tag.get("RecordItem") instanceof CompoundBinaryTag record) {
					final String itemId = SchematicData.getChildTag(record, "id", StringBinaryTag.class).value()
							.toLowerCase().replace("minecraft:", "");
					final Material mat = Material.matchMaterial(itemId.toUpperCase(Locale.ROOT));
					if (mat != null && mat.isRecord()) {
						jukebox.setRecord(new ItemStack(mat));
					}
				}
				jukebox.update();
			} catch (Exception ex) {
				instance.getPluginLogger()
						.warn("Failed to restore jukebox metadata at " + blockLocation + ": " + ex.getMessage());
			}
		}
	}

	private SchematicData getSchematicData(File file) {
		try {
			SchematicData schematicData = schematicCache.getOrDefault(file, null);
			if (schematicData == null) {
				schematicData = SchematicData.loadSchematic(file);
			}
			schematicCache.put(file, schematicData);
			return schematicData;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	private Set<Coordinate> getCoordinates(SchematicData schematicData) {
		final short length = schematicData.length;
		final short width = schematicData.width;
		final short height = schematicData.height;
		final Set<Coordinate> coordinates = new HashSet<>();
		for (int y = 0; y < height; ++y) {
			for (int x = 0; x < width; ++x) {
				for (int z = 0; z < length; ++z) {
					coordinates.add(new Coordinate(x, y, z));
				}
			}
		}
		return coordinates;
	}

	private ItemStack deserializeItem(CompoundBinaryTag tag) {
		try {
			final String name = SchematicData.getChildTag(tag, "id", StringBinaryTag.class).value()
					.replace("minecraft:", "").toUpperCase();
			final Material material = Material.matchMaterial(name.toUpperCase(Locale.ROOT));
			if (material == null) {
				return new ItemStack(Material.AIR);
			}

			final int count = SchematicData.getChildTag(tag, "Count", ByteBinaryTag.class).value();
			return new ItemStack(material, count);
		} catch (Exception ex) {
			return new ItemStack(Material.AIR);
		}
	}

	private void applySkullTexture(Skull skull, String base64) {
		try {
			final GameProfile profile = GameProfileBuilder.fetch(UUID.randomUUID());
			profile.getProperties().put("textures", new Property("textures", base64));

			final Field profileField = skull.getClass().getDeclaredField("profile");
			profileField.setAccessible(true);
			profileField.set(skull, profile);

			skull.update();
		} catch (Exception e) {
			instance.getPluginLogger().warn("Failed to apply custom skull texture: " + e.getMessage());
		}
	}

	private Entity spawnEntityFromNBT(CompoundBinaryTag tag, Location baseLoc) {
		try {
			final ListBinaryTag posTag = (ListBinaryTag) tag.get("Pos");
			final List<Double> pos = posTag.stream().map(tag1 -> Double.parseDouble(((StringBinaryTag) tag1).value()))
					.toList();

			final double x = pos.get(0) + baseLoc.getX();
			final double y = pos.get(1) + baseLoc.getY();
			final double z = pos.get(2) + baseLoc.getZ();

			final String id = ((StringBinaryTag) tag.get("id")).value().replace("minecraft:", "");
			final EntityType type = EntityType.valueOf(id.toUpperCase());

			return baseLoc.getWorld().spawnEntity(new Location(baseLoc.getWorld(), x, y, z), type);
		} catch (Exception ex) {
			instance.getPluginLogger().warn("Failed to spawn passenger entity: " + ex.getMessage());
			return null;
		}
	}

	@Override
	public boolean cancelPaste(UUID playerId) {
		SchedulerTask task = runningPastes.remove(playerId);
		SchedulerTask timeout = pasteTimeouts.remove(playerId);
		pasteProgress.remove(playerId);
		totalBlocks.remove(playerId);

		if (timeout != null && !timeout.isCancelled())
			timeout.cancel();
		if (task != null && !task.isCancelled()) {
			task.cancel();
			return true;
		}
		return false;
	}

	@Override
	public int getPasteProgress(UUID playerId) {
		return pasteProgress.getOrDefault(playerId, 0);
	}

	@Override
	public void clearCache() {
		schematicCache.clear();
	}

	public class TreeAnimationData {
		private final Map<Integer, List<Block>> stagedBlocks = new TreeMap<>();
		private final Block topBlock;

		public TreeAnimationData(Set<Block> treeBlocks) {
			Block highest = null;
			for (Block b : treeBlocks) {
				int y = b.getY();
				stagedBlocks.computeIfAbsent(y, __ -> new ArrayList<>()).add(b);
				if (highest == null || y > highest.getY())
					highest = b;
			}
			this.topBlock = highest;
		}

		public Map<Integer, List<Block>> getStagedBlocks() {
			return stagedBlocks;
		}

		public Block getTopBlock() {
			return topBlock;
		}
	}
}