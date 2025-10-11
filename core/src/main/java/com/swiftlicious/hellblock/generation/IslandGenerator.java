package com.swiftlicious.hellblock.generation;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.saicone.rtag.RtagBlock;
import com.saicone.rtag.RtagItem;
import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.api.Reloadable;
import com.swiftlicious.hellblock.context.Context;
import com.swiftlicious.hellblock.handlers.AdventureHelper;
import com.swiftlicious.hellblock.handlers.VersionHelper;
import com.swiftlicious.hellblock.player.UserData;
import com.swiftlicious.hellblock.player.mailbox.MailboxFlag;
import com.swiftlicious.hellblock.scheduler.SchedulerTask;
import com.swiftlicious.hellblock.schematic.SchematicMetadata;
import com.swiftlicious.hellblock.utils.extras.TextValue;

import net.kyori.adventure.text.Component;

public class IslandGenerator implements Listener, Reloadable {

	protected final HellblockPlugin instance;

	protected final Set<UUID> animatingPlayers = ConcurrentHashMap.newKeySet();
	protected final Map<UUID, ArmorStand> cameraAnchors = new ConcurrentHashMap<>();
	protected final Map<UUID, SchedulerTask> runningAnimations = new ConcurrentHashMap<>();

	public IslandGenerator(HellblockPlugin plugin) {
		this.instance = plugin;
	}

	@Override
	public void load() {
		Bukkit.getPluginManager().registerEvents(this, instance);
	}

	@Override
	public void unload() {
		HandlerList.unregisterAll(this);
		animatingPlayers.clear();
		cameraAnchors.values().forEach(Entity::remove);
		cameraAnchors.clear();
		runningAnimations.values().stream().filter(Objects::nonNull).forEach(SchedulerTask::cancel);
		runningAnimations.clear();
	}

	/**
	 * Generates a hellblock island from a schematic at the specified location.
	 * Searches for the first chest in the schematic to place items.
	 * 
	 * @param world     The world where the island will be generated.
	 * @param location  The base location for the island (bottom center).
	 * @param player    The player for chest orientation and sound effects.
	 * @param schematic The name of the schematic to paste.
	 * @param animated  Whether the animation should play for schematic placement.
	 * @return A CompletableFuture that completes when the island generation is
	 *         done.
	 */
	public CompletableFuture<Location> generateHellblockSchematic(@NotNull World world, @NotNull Location location,
			@NotNull Player player, @NotNull String schematic, boolean ignoreAirBlock, boolean animated) {

		Optional<UserData> onlineUser = instance.getStorageManager().getOnlineUser(player.getUniqueId());
		if (onlineUser.isEmpty()) {
			return CompletableFuture.completedFuture(null);
		}

		BoundingBox boundingBox = onlineUser.get().getHellblockData().getBoundingBox();
		if (boundingBox == null) {
			instance.getPluginLogger().warn("Null bounding box for player " + player.getName());
			return CompletableFuture.completedFuture(null);
		}

		// === Load schematic metadata ===
		SchematicMetadata metadata = instance.getSchematicManager().loadSchematicMetadata(schematic);

		// === Calculate paste origin ===
		Location pasteLocation = boundingBox.getCenter().toLocation(world).add(0, instance.getConfigManager().height(),
				0);

		// === Paste schematic ===
		return instance.getSchematicManager().pasteSchematic(player.getUniqueId(), world, schematic, pasteLocation,
				metadata, ignoreAirBlock, animated).thenApplyAsync(spawnLocation -> {
					if (spawnLocation == null) {
						return null;
					}

					Location chestLocation = null;

					// === Handle chest location (from metadata or fallback) ===
					if (metadata.getChest() != null) {
						chestLocation = pasteLocation.clone().add(metadata.getChest());
					} else {
						chestLocation = findNearestChestNear(world, pasteLocation, 8);
					}

					if (chestLocation != null) {
						Block chestBlock = chestLocation.getBlock();
						if (chestBlock.getType() == Material.CHEST || chestBlock.getType() == Material.TRAPPED_CHEST) {
							Location finalChestLoc = chestLocation;
							instance.getScheduler().executeSync(() -> {
								generateHellblockChest(world, finalChestLoc, player, animated);
								cleanupAnimation(player);
							}, finalChestLoc);
						} else {
							instance.getPluginLogger().warn("Expected chest not found at: " + chestLocation
									+ " for schematic: " + schematic + ". Maybe the chest vector is outdated?");
							// === Restore player and remove camera ===
							if (animated) {
								cleanupAnimation(player);
							}
						}
					} else {
						instance.getPluginLogger()
								.warn("No chest vector or valid fallback chest found for schematic: " + schematic);
						// === Restore player and remove camera ===
						if (animated) {
							cleanupAnimation(player);
						}
					}

					return spawnLocation;
				});
	}

	/**
	 * Generates a defined hellblock island variant at the specified location with
	 * animation, particle effects, and sound effects.
	 * 
	 * @param variant  The variant of the hellblock to generate.
	 * @param world    The world where the island will be generated.
	 * @param location The base location for the island (bottom center).
	 * @param player   The player for chest orientation and sound effects.
	 * @return A CompletableFuture that completes when the island generation is
	 *         done.
	 */
	public CompletableFuture<Void> generateAnimatedHellblockIsland(@NotNull IslandVariant variant, @NotNull World world,
			@NotNull Location location, @NotNull Player player) {

		final int x = location.getBlockX();
		final int y = location.getBlockY();
		final int z = location.getBlockZ();

		List<Map<Block, Material>> stages = buildHellblockStructure(world, x, y, z, variant.getOptions());

		ArmorStand camera = createCameraAnchor(player, location);
		animateCamera(camera, location, 10 + (stages.size() / 10));

		return animateBlockPlacementWithInterrupt(player, stages, location, 10L)
				.thenCompose(
						v -> generateHellblockGlowstoneTree(world, location.clone().add(variant.getTreeOffset()), true))
				.thenRun(() -> instance.getScheduler().executeSync(() -> {
					generateHellblockChest(world, location.clone().add(variant.getChestOffset()), player, true);
					cleanupAnimation(player);
				}, location)).exceptionally(ex -> {
					cleanupAnimation(player);
					instance.getPluginLogger()
							.warn("Generation failed (" + variant.name() + " Hellblock): " + ex.getMessage());
					return null;
				});
	}

	/**
	 * Instantly generates a defined hellblock island variant at the specified
	 * location.
	 * 
	 * @param variant  The variant of the hellblock to generate.
	 * @param world    The world where the island will be generated.
	 * @param location The base location for the island (bottom center).
	 * @param player   The player for chest orientation and sound effects.
	 */
	public void generateInstantHellblockIsland(@NotNull IslandVariant variant, @NotNull World world,
			@NotNull Location location, @NotNull Player player) {

		final int x = location.getBlockX();
		final int y = location.getBlockY();
		final int z = location.getBlockZ();

		List<Map<Block, Material>> layers = buildHellblockStructure(world, x, y, z, variant.getOptions());

		instance.getScheduler().executeSync(() -> {
			layers.forEach(layer -> layer.forEach(Block::setType));
			generateHellblockGlowstoneTree(world, location.clone().add(variant.getTreeOffset()), false).thenRun(
					() -> generateHellblockChest(world, location.clone().add(variant.getChestOffset()), player, false));
		}, location);
	}

	/**
	 * Generates a glowstone tree with a gravel trunk at the specified location. The
	 * tree is built in stages with optional animation, particle effects, and sound
	 * effects.
	 * 
	 * @param world    The world where the tree will be generated.
	 * @param location The base location for the tree (bottom center of the trunk).
	 * @param animated If true, the tree is built in stages with visual and sound
	 *                 effects.
	 * @return A CompletableFuture that completes when the tree generation is done.
	 */
	public CompletableFuture<Void> generateHellblockGlowstoneTree(@NotNull World world, @NotNull Location location,
			boolean animated) {
		return CompletableFuture.runAsync(() -> {
			final int x = location.getBlockX();
			final int y = location.getBlockY();
			final int z = location.getBlockZ();

			Map<Block, Material> allBlocks = new LinkedHashMap<>();

			for (int i = 0; i < 3; i++) {
				allBlocks.put(world.getBlockAt(x, y + i, z), Material.GRAVEL);
			}

			fillPattern(allBlocks, world, x, y + 3, z, 2, Material.GLOWSTONE);
			allBlocks.put(world.getBlockAt(x + 2, y + 3, z + 2), Material.AIR);
			allBlocks.put(world.getBlockAt(x + 2, y + 3, z - 2), Material.AIR);
			allBlocks.put(world.getBlockAt(x - 2, y + 3, z + 2), Material.AIR);
			allBlocks.put(world.getBlockAt(x - 2, y + 3, z - 2), Material.AIR);
			allBlocks.put(world.getBlockAt(x, y + 3, z), Material.GRAVEL);

			fillPattern(allBlocks, world, x, y + 4, z, 1, Material.GLOWSTONE);
			allBlocks.put(world.getBlockAt(x, y + 4, z), Material.GRAVEL);
			allBlocks.put(world.getBlockAt(x - 2, y + 4, z), Material.GLOWSTONE);
			allBlocks.put(world.getBlockAt(x + 2, y + 4, z), Material.GLOWSTONE);
			allBlocks.put(world.getBlockAt(x, y + 4, z - 2), Material.GLOWSTONE);
			allBlocks.put(world.getBlockAt(x, y + 4, z + 2), Material.GLOWSTONE);

			allBlocks.put(world.getBlockAt(x - 1, y + 5, z), Material.GLOWSTONE);
			allBlocks.put(world.getBlockAt(x + 1, y + 5, z), Material.GLOWSTONE);
			allBlocks.put(world.getBlockAt(x, y + 5, z - 1), Material.GLOWSTONE);
			allBlocks.put(world.getBlockAt(x, y + 5, z + 1), Material.GLOWSTONE);
			allBlocks.put(world.getBlockAt(x, y + 5, z), Material.GRAVEL);

			allBlocks.put(world.getBlockAt(x, y + 6, z), Material.GLOWSTONE);

			Map<Block, Material> gravelStage = new LinkedHashMap<>();
			Map<Integer, Map<Block, Material>> glowstoneStages = new TreeMap<>();

			allBlocks.forEach((block, mat) -> {
				int blockY = block.getY();
				if (mat == Material.GRAVEL) {
					gravelStage.put(block, mat);
				} else {
					glowstoneStages.computeIfAbsent(blockY, __ -> new LinkedHashMap<>()).put(block, mat);
				}
			});

			if (!animated) {
				instance.getScheduler().executeSync(() -> {
					gravelStage.forEach(Block::setType);
					glowstoneStages.values().forEach(stage -> stage.forEach(Block::setType));
				}, location);
				return;
			}

			List<Map<Block, Material>> stages = new ArrayList<>();
			stages.add(gravelStage);
			stages.addAll(glowstoneStages.values());

			AtomicInteger step = new AtomicInteger();
			BukkitRunnable animationTask = new BukkitRunnable() {
				@Override
				public void run() {
					if (step.get() >= stages.size()) {
						Location top = location.clone().add(0.5, 6.5, 0.5);
						world.spawnParticle(Particle.CLOUD, top, 30, 0.4, 0.3, 0.4, 0.02);
						world.spawnParticle(Particle.END_ROD, top, 20, 0.2, 0.3, 0.2, 0.01);
						world.strikeLightningEffect(top);
						AdventureHelper.playPositionalSound(world, top, "minecraft:entity.lightning_bolt.thunder", 0.8f,
								1.0f);
						cancel();
						return;
					}

					Map<Block, Material> stage = stages.get(step.getAndIncrement());
					stage.forEach((block, mat) -> {
						block.setType(mat, false);
						block.getWorld().spawnParticle(Particle.END_ROD, block.getLocation().add(0.5, 0.5, 0.5), 8, 0.1,
								0.1, 0.1, 0.01);
						AdventureHelper.playPositionalSound(block.getWorld(), block.getLocation(),
								"minecraft:block.amethyst_block.hit", 0.6f, 1.4f);
					});
				}
			};

			// Use dummy player UUID to track (since no Player is passed here)
			UUID treeAnimationId = UUID
					.nameUUIDFromBytes(("tree-" + location.toString()).getBytes(StandardCharsets.UTF_8));

			SchedulerTask task = instance.getScheduler().sync().runRepeating(animationTask, 0L, 3L, location);
			trackAnimation(treeAnimationId, task); // Track the task for cleanup
		});
	}

	/**
	 * Generates a chest at the specified location with configured items and name.
	 * The chest is oriented to face the player and includes particle and sound
	 * effects.
	 * 
	 * @param world    The world where the chest will be placed.
	 * @param location The location to place the chest.
	 * @param player   The player for orientation and sound effects.
	 * @param animated If true, plays particle and sound effects on spawn.
	 */
	public void generateHellblockChest(@NotNull World world, @NotNull Location location, @NotNull Player player,
			boolean animated) {
		instance.getScheduler().executeSync(() -> {
			final Block block = world.getBlockAt(location);
			block.setType(Material.CHEST);

			final Directional directional = (Directional) block.getBlockData();
			directional.setFacing(getPlayerFacing(location, player.getLocation()));
			block.setBlockData(directional);

			final Context<Player> context = Context.player(player);
			final String chestName = instance.getConfigManager().chestName();

			final RtagBlock chestTag = new RtagBlock(block);
			if (chestName != null && !chestName.isEmpty() && chestName.length() <= 35) {
				final TextValue<Player> text = TextValue.auto(chestName);
				final String rendered = text.render(context);
				final Component centered = AdventureHelper.parseCenteredTitleMultiline(rendered);
				chestTag.setCustomName(AdventureHelper.componentToJson(centered));
			}

			final Chest chest = (Chest) chestTag.load().getState();
			chest.update();

			Location center = location.clone().add(0.5, 0.5, 0.5);

			// === NON-ANIMATED ===
			if (!animated) {
				chest.getInventory().clear();
				instance.getConfigManager().chestItems().values().forEach(entry -> {
					final ItemStack item = setChestData(entry.right().build(context), true);
					int slot = entry.left();
					if (slot >= 0 && slot <= 26) {
						chest.getInventory().setItem(slot, item);
					} else if (chest.getInventory().firstEmpty() != -1) {
						chest.getInventory().addItem(item);
					}
				});
				chest.update();
				return; // No particles, no sounds, no animations
			}

			// === ANIMATED SEQUENCE ===
			center.getWorld().spawnParticle(Particle.ENCHANT, center, 40, 0.5, 0.5, 0.5, 0.2);
			AdventureHelper.playPositionalSound(world, center, "minecraft:block.chest.open", 0.7f, 1.0f);

			playChestOpenAnimation(chest.getLocation());

			// Spawn items floating above chest
			List<Item> fallingItems = new ArrayList<>();
			instance.getConfigManager().chestItems().values().forEach(entry -> {
				ItemStack displayItem = setChestData(entry.right().build(context), false);
				Location dropLoc = center.clone().add((Math.random() - 0.5) * 0.6, 1.3, (Math.random() - 0.5) * 0.6);
				Item item = world.dropItem(dropLoc, displayItem);
				item.setGravity(false);
				item.setPickupDelay(Integer.MAX_VALUE);
				item.setVelocity(new Vector(0, 0, 0));
				fallingItems.add(item);
			});

			// Suction animation
			instance.getScheduler().sync().runRepeating(new BukkitRunnable() {
				int ticks = 0;

				@Override
				public void run() {
					if (ticks++ >= 20 || fallingItems.isEmpty()) {
						this.cancel();
						return;
					}
					for (Item item : fallingItems) {
						if (!item.isValid())
							continue;
						Location current = item.getLocation();
						Vector toChest = center.clone().subtract(current).toVector().multiply(0.25);
						item.setVelocity(toChest);
						current.getWorld().spawnParticle(Particle.WITCH, current.clone().add(0, 0.2, 0), 3, 0.05, 0.05,
								0.05, 0.01);
					}
				}
			}, 10L, 2L, location);

			// After animation: close chest + insert items
			instance.getScheduler().sync().runLater(() -> {
				playChestCloseAnimation(chest.getLocation());
				AdventureHelper.playPositionalSound(world, center, "minecraft:block.chest.close", 0.7f, 0.9f);
				world.spawnParticle(Particle.CRIT, center.clone().add(0, 0.8, 0), 10, 0.3, 0.2, 0.3, 0.02);

				chest.getInventory().clear();
				instance.getConfigManager().chestItems().values().forEach(entry -> {
					final ItemStack item = setChestData(entry.right().build(context), true);
					int slot = entry.left();
					if (slot >= 0 && slot <= 26) {
						chest.getInventory().setItem(slot, item);
					} else if (chest.getInventory().firstEmpty() != -1) {
						chest.getInventory().addItem(item);
					}
				});
				chest.update();

				// Remove visuals
				fallingItems.forEach(Entity::remove);
			}, 50L, location); // ~2.5 seconds total animation
		}, location);
	}

	public @Nullable Location findNearestChestNear(World world, Location center, int radius) {
		Location nearest = null;
		double nearestDistanceSq = Double.MAX_VALUE;

		int cx = center.getBlockX();
		int cy = center.getBlockY();
		int cz = center.getBlockZ();

		for (int y = cy - radius; y <= cy + radius; y++) {
			for (int x = cx - radius; x <= cx + radius; x++) {
				for (int z = cz - radius; z <= cz + radius; z++) {
					Block block = world.getBlockAt(x, y, z);
					if (block.getType() == Material.CHEST || block.getType() == Material.TRAPPED_CHEST) {
						Location candidate = block.getLocation();
						double distanceSq = candidate.distanceSquared(center);

						if (distanceSq < nearestDistanceSq) {
							nearestDistanceSq = distanceSq;
							nearest = candidate;
						}
					}
				}
			}
		}
		return nearest;
	}

	public void playChestOpenAnimation(@NotNull Location location) {
		playChestAnimation(location, true);
	}

	public void playChestCloseAnimation(@NotNull Location location) {
		playChestAnimation(location, false);
	}

	private void playChestAnimation(@NotNull Location location, boolean open) {
		try {
			World world = location.getWorld();
			if (world == null)
				return;

			Block block = location.getBlock();
			Material type = block.getType();

			if (type != Material.CHEST && type != Material.TRAPPED_CHEST) {
				return; // Only support chest types
			}

			// Reflect: CraftWorld
			Object craftWorld = world.getClass().cast(world);

			// Get handle (WorldServer / net.minecraft.world.level.World)
			Method getHandle = craftWorld.getClass().getMethod("getHandle");
			Object nmsWorld = getHandle.invoke(craftWorld);

			// Get BlockPosition (net.minecraft.core.BlockPosition)
			Class<?> blockPosClass = Class.forName("net.minecraft.core.BlockPosition");
			Constructor<?> blockPosCtor = blockPosClass.getConstructor(int.class, int.class, int.class);
			Object blockPos = blockPosCtor.newInstance(location.getBlockX(), location.getBlockY(),
					location.getBlockZ());

			// Get block (net.minecraft.world.level.block.Block)
			Method getBlockHandle = block.getClass().getMethod("getBlock");
			Object nmsBlock = getBlockHandle.invoke(block);

			// playBlockAction(BlockPos, Block, int, int)
			Method playBlockAction = nmsWorld.getClass().getMethod("playBlockAction", blockPosClass,
					nmsBlock.getClass(), int.class, int.class);
			playBlockAction.invoke(nmsWorld, blockPos, nmsBlock, 1, open ? 1 : 0);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Builds the block structure for the specified island type at the given
	 * coordinates. Returns a list of stages, where each stage is a map of blocks to
	 * their target materials.
	 * 
	 * @param world The world where the structure will be built.
	 * @param x     The x-coordinate of the center of the structure.
	 * @param y     The y-coordinate of the base of the structure.
	 * @param z     The z-coordinate of the center of the structure.
	 * @param type  The type of island structure to build.
	 * @return A list of stages for animated block placement.
	 */
	public List<Map<Block, Material>> buildHellblockStructure(@NotNull World world, int x, int y, int z,
			IslandOptions type) {
		List<Map<Block, Material>> stages = new ArrayList<>();

		switch (type) {
		case DEFAULT -> {
			// Layer 0: Bedrock base
			stages.add(Map.of(world.getBlockAt(x, y, z), Material.BEDROCK));

			// Layer 4
			Map<Block, Material> layer4 = new LinkedHashMap<>();
			fillLayer(layer4, world, x - 3, y + 4, z - 3, x + 3, z + 3, Material.SOUL_SAND);
			putBlock(layer4, world, x - 3, y + 4, z - 3, Material.AIR);
			putBlock(layer4, world, x - 3, y + 4, z + 3, Material.AIR);
			putBlock(layer4, world, x + 3, y + 4, z - 3, Material.AIR);
			putBlock(layer4, world, x + 3, y + 4, z + 3, Material.AIR);
			stages.add(layer4);

			// Layer 3
			Map<Block, Material> layer3 = new LinkedHashMap<>();
			fillLayer(layer3, world, x - 2, y + 3, z - 2, x + 2, z + 2, Material.SOUL_SAND);
			putBlock(layer3, world, x, y + 3, z, Material.GRASS_BLOCK);
			putBlock(layer3, world, x - 3, y + 3, z, Material.SOUL_SAND);
			putBlock(layer3, world, x + 3, y + 3, z, Material.SOUL_SAND);
			putBlock(layer3, world, x, y + 3, z - 3, Material.SOUL_SAND);
			putBlock(layer3, world, x, y + 3, z + 3, Material.SOUL_SAND);
			stages.add(layer3);

			// Layer 2
			Map<Block, Material> layer2 = new LinkedHashMap<>();
			fillLayer(layer2, world, x - 1, y + 2, z - 1, x + 1, z + 1, Material.SOUL_SAND);
			putBlock(layer2, world, x, y + 2, z, Material.DIRT);
			putBlock(layer2, world, x - 2, y + 2, z, Material.SOUL_SAND);
			putBlock(layer2, world, x + 2, y + 2, z, Material.SOUL_SAND);
			putBlock(layer2, world, x, y + 2, z - 2, Material.SOUL_SAND);
			putBlock(layer2, world, x, y + 2, z + 2, Material.SOUL_SAND);
			stages.add(layer2);

			// Layer 1
			Map<Block, Material> layer1 = new LinkedHashMap<>();
			putBlock(layer1, world, x, y + 1, z, Material.DIRT);
			putBlock(layer1, world, x - 1, y + 1, z, Material.SOUL_SAND);
			putBlock(layer1, world, x + 1, y + 1, z, Material.SOUL_SAND);
			putBlock(layer1, world, x, y + 1, z - 1, Material.SOUL_SAND);
			putBlock(layer1, world, x, y + 1, z + 1, Material.SOUL_SAND);
			stages.add(layer1);
		}

		case CLASSIC -> {
			// Base layer
			Map<Block, Material> base = new LinkedHashMap<>();
			fillLayer(base, world, x - 5, y, z - 2, x, z, Material.SOUL_SAND);
			fillLayer(base, world, x - 2, y, z - 5, x, z, Material.SOUL_SAND);
			putBlock(base, world, x, y, z, Material.SOUL_SAND);
			stages.add(base);

			// Layer 1
			Map<Block, Material> l1 = new LinkedHashMap<>();
			fillLayer(l1, world, x - 5, y + 1, z - 2, x, z, Material.SOUL_SAND);
			fillLayer(l1, world, x - 2, y + 1, z - 5, x, z, Material.SOUL_SAND);
			stages.add(l1);

			// Layer 2
			Map<Block, Material> l2 = new LinkedHashMap<>();
			fillLayer(l2, world, x - 5, y + 2, z - 2, x, z, Material.SOUL_SAND);
			fillLayer(l2, world, x - 2, y + 2, z - 5, x, z, Material.SOUL_SAND);
			stages.add(l2);

			// Decorations
			Map<Block, Material> deco = new LinkedHashMap<>();
			putBlock(deco, world, x - 1, y + 1, z - 1, Material.GRASS_BLOCK);
			putBlock(deco, world, x - 3, y + 1, z - 1, Material.SOUL_SAND);
			putBlock(deco, world, x - 4, y + 1, z - 1, Material.DIRT);
			putBlock(deco, world, x - 1, y + 1, z - 3, Material.SOUL_SAND);
			putBlock(deco, world, x - 1, y + 1, z - 4, Material.DIRT);
			putBlock(deco, world, x - 1, y, z - 1, Material.BEDROCK);
			stages.add(deco);
		}

		default -> throw new IllegalArgumentException("Unsupported island type: " + type);
		}

		return stages;
	}

	/**
	 * Animates block placements in stages with a delay between each stage. If the
	 * player disconnects during the animation, it stops and completes
	 * exceptionally.
	 * 
	 * @param player     The player to check for disconnection.
	 * @param stages     A list of block changes to apply in sequence.
	 * @param reference  A location used to schedule the task in the correct world
	 *                   context.
	 * @param ticksDelay Delay in ticks between each stage.
	 * @return A CompletableFuture that completes when the animation is done or
	 *         exceptionally if interrupted.
	 */
	public CompletableFuture<Void> animateBlockPlacementWithInterrupt(Player player, List<Map<Block, Material>> stages,
			Location reference, long ticksDelay) {
		CompletableFuture<Void> future = new CompletableFuture<>();
		AtomicInteger index = new AtomicInteger(0);

		BukkitRunnable task = new BukkitRunnable() {
			@Override
			public void run() {
				if (!player.isOnline()) {
					cancel();
					future.completeExceptionally(
							new IllegalStateException("Player disconnected during island generation."));
					return;
				}

				if (index.get() >= stages.size()) {
					cancel();
					future.complete(null);
					return;
				}

				Map<Block, Material> stage = stages.get(index.getAndIncrement());
				stage.entrySet().forEach(entry -> {
					Block block = entry.getKey();
					block.setType(entry.getValue(), false);
					block.getWorld().spawnParticle(Particle.BLOCK_CRUMBLE, block.getLocation().add(0.5, 0.5, 0.5), 5,
							0.25, 0.25, 0.25, block.getBlockData());
				});

				AdventureHelper.playPositionalSound(player.getWorld(), player.getLocation(),
						"minecraft:block.stone.place", 0.4f, 1.2f);
			}
		};

		SchedulerTask scheduledTask = instance.getScheduler().sync().runRepeating(task, 0L, ticksDelay, reference);
		trackAnimation(player.getUniqueId(), scheduledTask); // Track animation safely

		return future;
	}

	/**
	 * Creates an invisible, non-collidable armor stand to serve as a camera anchor
	 * for the player. The player is set to spectator mode and forced to spectate
	 * the armor stand.
	 * 
	 * @param player     The player who will spectate the armor stand.
	 * @param focusPoint The point the camera should focus on.
	 * @return The created armor stand used as the camera anchor.
	 */
	public ArmorStand createCameraAnchor(Player player, Location focusPoint) {
		Location anchorLocation = focusPoint.clone().add(-8, 6, -8);
		anchorLocation.setDirection(focusPoint.clone().subtract(anchorLocation).toVector());

		World world = focusPoint.getWorld();
		if (world == null)
			return null;

		ArmorStand camera = world.spawn(anchorLocation, ArmorStand.class, stand -> {
			stand.setVisible(false);
			stand.setGravity(false);
			stand.setMarker(true);
			stand.setInvulnerable(true);
			stand.setCollidable(false);
			stand.setSilent(true);
			stand.setCustomNameVisible(false);
			stand.setAI(false);
			stand.setInvisible(true);
			stand.setBasePlate(false);
		});

		// Store reference
		startAnimationFor(player);
		cameraAnchors.put(player.getUniqueId(), camera);

		player.setGameMode(GameMode.SPECTATOR);
		player.setSpectatorTarget(camera);

		return camera;
	}

	public void animateCamera(ArmorStand stand, Location target, long durationTicks) {
		instance.getScheduler().sync().runRepeating(new BukkitRunnable() {
			long elapsed = 0;

			@Override
			public void run() {
				if (elapsed >= durationTicks || stand.isDead()) {
					this.cancel();
					return;
				}

				Location current = stand.getLocation();
				Vector direction = target.clone().subtract(current).toVector().normalize();
				current.setDirection(direction);

				stand.teleport(current);
				elapsed++;
			}
		}, 1L, 1L, target);
	}

	public void startSchematicCameraAnimation(@NotNull Player player, @NotNull Location focus, long durationTicks,
			boolean panCamera) {
		fadeScreen(player, 10, 30, 10);

		ArmorStand camera = createCameraAnchor(player, focus);
		if (camera == null) {
			HellblockPlugin.getInstance().getPluginLogger()
					.warn("Failed to create camera anchor for: " + player.getName());
			return;
		}

		if (panCamera) {
			animateCamera(camera, focus.clone().add(0, 2, 0), durationTicks);
		}
	}

	public void fadeScreen(Player player, int fadeIn, int stay, int fadeOut) {
		VersionHelper.getNMSManager().sendTitle(player, "", "", fadeIn, stay, fadeOut);
	}

	public void startAnimationFor(Player player) {
		animatingPlayers.add(player.getUniqueId());
	}

	public void stopAnimationFor(Player player) {
		animatingPlayers.remove(player.getUniqueId());
	}

	public boolean isAnimating(Player player) {
		return animatingPlayers.contains(player.getUniqueId());
	}

	public void cleanupAnimation(@NotNull Player player) {
		UUID uuid = player.getUniqueId();

		// Stop tracking
		stopAnimationFor(player);

		// Remove camera anchor if it exists
		ArmorStand anchor = cameraAnchors.remove(uuid);
		if (anchor != null && !anchor.isDead()) {
			anchor.remove();
		}

		// Cancel any running animation task if you track them
		SchedulerTask task = runningAnimations.remove(uuid);
		if (task != null) {
			task.cancel();
		}

		// Optional: teleport player back to spawn or fallback
		if (!player.isOnline()) {
			instance.getMailboxManager().queueMailbox(uuid, MailboxFlag.RESET_ANIMATION);
			return; // prevent async issues
		}

		// Restore gamemode if needed
		if (player.getGameMode() == GameMode.SPECTATOR) {
			player.setGameMode(GameMode.SURVIVAL);
		}

		// Unset spectator target
		player.setSpectatorTarget(null);

		instance.getHellblockHandler().teleportToSpawn(player, true); // if you have a fallback method
	}

	public void trackAnimation(UUID uuid, SchedulerTask task) {
		SchedulerTask previous = runningAnimations.remove(uuid);
		if (previous != null) {
			previous.cancel();
		}
		runningAnimations.put(uuid, task);
	}

	@EventHandler
	public void onQuit(PlayerQuitEvent event) {
		Player player = event.getPlayer();
		if (isAnimating(player)) {
			cleanupAnimation(player);
		}
	}

	private final Set<String> commandWhitelist = Set.of("/msg", "/tell", "/me", "/whisper", "/message", "/help",
			"/mail", "/?", "/seen", "/reply", "/rules", "/ignore", "/msgtoggle", "/pay", "/paytoggle", "/shout",
			"/socialspy", "/bal", "/balance", "/ptime", "/list", "/broadcast", "/r", "/feed", "/nick", "/heal");

	@EventHandler(priority = EventPriority.HIGH)
	public void onCommand(PlayerCommandPreprocessEvent event) {
		Player player = event.getPlayer();
		if (!isAnimating(player))
			return;

		String msg = event.getMessage().toLowerCase(Locale.ROOT);
		boolean allowed = commandWhitelist.stream().anyMatch(msg::startsWith);
		if (!allowed) {
			event.setCancelled(true);
		}
	}

	@EventHandler
	public void onMove(PlayerMoveEvent event) {
		Player p = event.getPlayer();
		if (!isAnimating(p))
			return;

		// allow looking around but cancel position changes
		if (event.getFrom().getX() != event.getTo().getX() || event.getFrom().getY() != event.getTo().getY()
				|| event.getFrom().getZ() != event.getTo().getZ()) {
			event.setTo(event.getFrom());
		}
	}

	@EventHandler
	public void onTeleport(PlayerTeleportEvent event) {
		if (isAnimating(event.getPlayer())) {
			event.setCancelled(true);
		}
	}

	@EventHandler
	public void onInteract(PlayerInteractEvent event) {
		if (isAnimating(event.getPlayer()))
			event.setCancelled(true);
	}

	@EventHandler
	public void onInteractEntity(PlayerInteractEntityEvent event) {
		if (isAnimating(event.getPlayer()))
			event.setCancelled(true);
	}

	@EventHandler
	public void onInvOpen(InventoryOpenEvent event) {
		if (event.getPlayer() instanceof Player player && isAnimating(player))
			event.setCancelled(true);
	}

	@EventHandler
	public void onInvClick(InventoryClickEvent event) {
		if (event.getWhoClicked() instanceof Player player && isAnimating(player))
			event.setCancelled(true);
	}

	@EventHandler
	public void onDrop(PlayerDropItemEvent event) {
		if (isAnimating(event.getPlayer()))
			event.setCancelled(true);
	}

	@EventHandler
	public void onPickup(EntityPickupItemEvent event) {
		if (event.getEntity() instanceof Player player && isAnimating(player))
			event.setCancelled(true);
	}

	@EventHandler
	public void onInteractAtEntity(PlayerInteractAtEntityEvent event) {
		if (isAnimating(event.getPlayer()))
			event.setCancelled(true);
	}

	@EventHandler
	public void onToggleFlight(PlayerToggleFlightEvent event) {
		if (isAnimating(event.getPlayer()))
			event.setCancelled(true);
	}

	@EventHandler
	public void onDamage(EntityDamageEvent event) {
		if (event.getEntity() instanceof Player player && isAnimating(player))
			event.setCancelled(true);
	}

	@EventHandler
	public void onWorldChange(PlayerChangedWorldEvent event) {
		Player player = event.getPlayer();
		if (isAnimating(player)) {
			cleanupAnimation(player);
		}
	}

	/**
	 * Helper to put a block change into the map.
	 * 
	 * @param changes
	 * @param world
	 * @param x
	 * @param y
	 * @param z
	 * @param type
	 */
	private void putBlock(Map<Block, Material> changes, World world, int x, int y, int z, Material type) {
		changes.put(world.getBlockAt(x, y, z), type);
	}

	/**
	 * Fills a rectangular layer from (minX, minZ) to (maxX, maxZ) at height y with
	 * the given material type.
	 * 
	 * @param changes
	 * @param world
	 * @param y
	 * @param minX
	 * @param maxX
	 * @param minZ
	 * @param maxZ
	 * @param type
	 */
	private void fillLayer(Map<Block, Material> changes, World world, int y, int minX, int maxX, int minZ, int maxZ,
			Material type) {
		for (int x = minX; x <= maxX; x++) {
			for (int z = minZ; z <= maxZ; z++) {
				putBlock(changes, world, x, y, z, type);
			}
		}
	}

	/**
	 * Fills a square pattern centered at (centerX, centerZ) with the given radius
	 * at height y,
	 * 
	 * @param changes
	 * @param world
	 * @param centerX
	 * @param y
	 * @param centerZ
	 * @param radius
	 * @param type
	 */
	private void fillPattern(Map<Block, Material> changes, World world, int centerX, int y, int centerZ, int radius,
			Material type) {
		for (int dx = -radius; dx <= radius; dx++) {
			for (int dz = -radius; dz <= radius; dz++) {
				final Block block = world.getBlockAt(centerX + dx, y, centerZ + dz);
				if (block.getType().isAir()) {
					changes.put(block, type);
				}
			}
		}
	}

	/**
	 * Determines the BlockFace direction from the chest to the player.
	 * 
	 * @param chestLocation  The location of the chest.
	 * @param playerLocation The location of the player.
	 * @return The BlockFace direction the chest should face.
	 */
	private BlockFace getPlayerFacing(@NotNull Location chestLocation, @NotNull Location playerLocation) {
		// Vector from chest to player
		final Vector direction = playerLocation.toVector().subtract(chestLocation.toVector()).normalize();

		final double dx = direction.getX();
		final double dz = direction.getZ();

		// Pick the strongest axis (X or Z) to decide facing
		if (Math.abs(dx) > Math.abs(dz)) {
			return dx > 0 ? BlockFace.EAST : BlockFace.WEST;
		} else {
			return dz > 0 ? BlockFace.SOUTH : BlockFace.NORTH;
		}
	}

	/**
	 * Checks if the given ItemStack has the "isStarterChestItem" tag.
	 * 
	 * @param item The ItemStack to check.
	 * @return True if the tag is present and true, false otherwise.
	 */
	public boolean checkChestData(@Nullable ItemStack item) {
		if (item == null || item.getType() == Material.AIR) {
			return false;
		}

		return new RtagItem(item).hasTag("HellblockChest", "isStarterChestItem");
	}

	/**
	 * Retrieves the "isStarterChestItem" tag from the given ItemStack.
	 * 
	 * @param item The ItemStack to check.
	 * @return True if the tag is present and true, false otherwise.
	 */
	public boolean getChestData(@Nullable ItemStack item) {
		if (item == null || item.getType() == Material.AIR) {
			return false;
		}

		return new RtagItem(item).getOptional("HellblockChest", "isStarterChestItem").asBoolean();
	}

	/**
	 * Sets or removes the "isStarterChestItem" tag on the given ItemStack.
	 * 
	 * @param item The ItemStack to modify.
	 * @param data If true, sets the tag; if false, removes it.
	 * @return The modified ItemStack, or null if the input was null or AIR.
	 */
	public @Nullable ItemStack setChestData(@Nullable ItemStack item, boolean data) {
		if (item == null || item.getType() == Material.AIR) {
			return null;
		}

		final Consumer<RtagItem> rtag = tag -> tag.set(data, "HellblockChest", "isStarterChestItem");
		return RtagItem.edit(item, rtag);
	}
}