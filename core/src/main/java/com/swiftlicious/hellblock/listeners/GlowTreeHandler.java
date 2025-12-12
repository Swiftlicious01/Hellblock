package com.swiftlicious.hellblock.listeners;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.TreeType;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.Levelled;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Result;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.StructureGrowEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.api.Reloadable;
import com.swiftlicious.hellblock.challenges.HellblockChallenge.ActionType;
import com.swiftlicious.hellblock.challenges.requirement.GrowRequirement;
import com.swiftlicious.hellblock.config.locale.MessageConstants;
import com.swiftlicious.hellblock.handlers.AdventureHelper;
import com.swiftlicious.hellblock.handlers.VersionHelper;
import com.swiftlicious.hellblock.nms.inventory.HandSlot;
import com.swiftlicious.hellblock.utils.ParticleUtils;
import com.swiftlicious.hellblock.utils.PlayerUtils;
import com.swiftlicious.hellblock.utils.RandomUtils;
import com.swiftlicious.hellblock.world.CustomBlock;
import com.swiftlicious.hellblock.world.CustomBlockState;
import com.swiftlicious.hellblock.world.HellblockWorld;
import com.swiftlicious.hellblock.world.Pos3;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.sound.Sound.Source;

/**
 * Handles the logic for custom glowstone tree growth in the Hellblock world.
 *
 * <p>
 * This includes controlling natural and forced tree growth via saplings on soul
 * sand, converting naturally spawned trees into glowstone variants, and
 * managing physics behavior such as preventing gravel from falling after tree
 * generation.
 * </p>
 *
 * <p>
 * It uses {@link StructureGrowEvent} and {@link PlayerInteractEvent} to
 * intercept tree growth behavior, and converts resulting blocks to
 * glowstone-themed versions. It also tracks recent gravel placements from glow
 * trees to prevent unintended physics interactions.
 * </p>
 *
 * <p>
 * This class relies on the Hellblock-specific world model and uses {@link Pos3}
 * as an abstract world position type to decouple from Bukkit's {@link Location}
 * class.
 * </p>
 */
public class GlowTreeHandler implements Listener, Reloadable {

	protected final HellblockPlugin instance;

	private final Cache<Pos3, Boolean> validComposters = Caffeine.newBuilder().expireAfterWrite(Duration.ofMinutes(10)) // auto-expire
			.weakKeys() // allows GC for unused locations
			.build();

	/**
	 * A set of tree types supported by glowstone tree transformations and natural
	 * growth control.
	 *
	 * <p>
	 * This includes all vanilla and newer tree types (where available), with
	 * compatibility checks for newer versions like mangrove or cherry.
	 * </p>
	 */
	private static final Set<TreeType> TREE_TYPES = buildTreeTypes();

	private final Set<Pos3> recentGlowTreeGravel = ConcurrentHashMap.newKeySet();

	@NotNull
	private static Set<TreeType> buildTreeTypes() {
		Set<TreeType> types = EnumSet.noneOf(TreeType.class);

		// Always safe in 1.17+
		types.addAll(Set.of(TreeType.TREE, TreeType.BIG_TREE, TreeType.REDWOOD, TreeType.TALL_REDWOOD, TreeType.BIRCH,
				TreeType.TALL_BIRCH, TreeType.JUNGLE, TreeType.SMALL_JUNGLE, TreeType.DARK_OAK, TreeType.ACACIA,
				TreeType.SWAMP, TreeType.MEGA_REDWOOD));

		// Conditionally add newer types
		try {
			types.add(TreeType.valueOf("MEGA_PINE"));
		} catch (NoSuchFieldError | IllegalArgumentException ignored) {
		}
		try {
			types.add(TreeType.valueOf("MANGROVE"));
		} catch (NoSuchFieldError | IllegalArgumentException ignored) {
		}
		try {
			types.add(TreeType.valueOf("TALL_MANGROVE"));
		} catch (NoSuchFieldError | IllegalArgumentException ignored) {
		}
		try {
			types.add(TreeType.valueOf("CHERRY"));
		} catch (NoSuchFieldError | IllegalArgumentException ignored) {
		}
		try {
			types.add(TreeType.valueOf("AZALEA"));
		} catch (NoSuchFieldError | IllegalArgumentException ignored) {
		}
		try {
			types.add(TreeType.valueOf("PALE_OAK"));
		} catch (NoSuchFieldError | IllegalArgumentException ignored) {
		}

		return types;
	}

	/**
	 * Cardinal directions used to check adjacent blocks for sapling protection or
	 * flood-fill operations.
	 */
	private static final BlockFace[] FACES = { BlockFace.DOWN, BlockFace.UP, BlockFace.NORTH, BlockFace.EAST,
			BlockFace.SOUTH, BlockFace.WEST };

	public GlowTreeHandler(HellblockPlugin plugin) {
		instance = plugin;
	}

	@Override
	public void load() {
		Bukkit.getPluginManager().registerEvents(this, instance);
	}

	@Override
	public void unload() {
		HandlerList.unregisterAll(this);
	}

	@EventHandler
	public void onGlowTreeCreation(StructureGrowEvent event) {
		if (!instance.getHellblockHandler().isInCorrectWorld(event.getWorld())) {
			return;
		}

		final Player player = event.getPlayer();
		final World world = event.getWorld();
		final CustomTreeGrowContext context = new CustomTreeGrowContext(event.getSpecies(),
				Pos3.from(event.getLocation()), world.getName());
		final Material growing = event.getLocation().getBlock().getRelative(BlockFace.DOWN).getType();

		// Special glowstone tree growth on soul sand
		if (growing == Material.SOUL_SAND) {
			event.setCancelled(true);
			// Track glowtree gravel
			instance.getWorldManager().getWorld(world).ifPresent(hellWorld -> instance.getIslandGenerator()
					.generateHellblockGlowstoneTree(hellWorld, event.getLocation(), false).thenRun(() -> {
						if (player != null) {
							instance.getStorageManager().getOnlineUser(player.getUniqueId()).ifPresent(userData -> {
								if (instance.getCooldownManager().shouldUpdateActivity(player.getUniqueId(), 5000)) {
									userData.getHellblockData().updateLastIslandActivity();
								}
								instance.getChallengeManager().handleChallengeProgression(userData, ActionType.GROW,
										context);
							});
						}
					}));
			return;
		}

		// Replace natural tree growth if disabled
		final boolean naturalCondition = !instance.getConfigManager().growNaturalTrees()
				&& TREE_TYPES.contains(event.getSpecies());

		if (!naturalCondition) {
			if (player != null) {
				instance.getStorageManager().getOnlineUser(player.getUniqueId()).ifPresent(userData -> {
					if (instance.getCooldownManager().shouldUpdateActivity(player.getUniqueId(), 5000)) {
						userData.getHellblockData().updateLastIslandActivity();
					}
					instance.getChallengeManager().handleChallengeProgression(userData, ActionType.GROW, context);
				});
			}
			return;
		}

		event.getBlocks().forEach(block -> {
			final Material type = block.getType();

			// Skip Nether saplings and nether tree components
			if (isNetherTreeComponent(type)) {
				return;
			}

			if (Tag.LOGS_THAT_BURN.isTagged(type) || Tag.SAPLINGS.isTagged(type)) {
				block.setType(Material.GRAVEL);
			} else if (Tag.DIRT.isTagged(type)) {
				block.setType(growing);
			} else if (Tag.LEAVES.isTagged(type)) {
				block.setType(Material.GLOWSTONE);
			} else if (type == Material.VINE) {
				final Material newVine = (Math.random() < 0.5) ? Material.WEEPING_VINES : Material.TWISTING_VINES;
				block.setType(newVine);

				// Set random growth length if vine supports it
				if (block.getBlockData() instanceof Ageable ageable) {
					ageable.setAge(RandomUtils.generateRandomInt(1, ageable.getMaximumAge() + 1));
					block.setBlockData(ageable);
				}
			}
		});

		instance.getWorldManager().getWorld(world).ifPresent(hellWorld -> {
			List<Pos3> gravelPositions = event.getBlocks().stream().map(BlockState::getBlock)
					.map(b -> new Pos3(b.getX(), b.getY(), b.getZ())).toList();

			instance.getGlowstoneTreeHandler().markGlowTreeGravel(hellWorld, gravelPositions);
		});
	}

	private boolean isNetherTreeComponent(@NotNull Material type) {
		return switch (type) {
		case CRIMSON_FUNGUS, WARPED_FUNGUS, CRIMSON_STEM, WARPED_STEM, NETHER_WART_BLOCK, WARPED_WART_BLOCK -> true;
		default -> false;
		};
	}

	@EventHandler
	public void onBlockFall(EntityChangeBlockEvent event) {
		if (!(event.getEntity() instanceof FallingBlock))
			return;

		Block block = event.getBlock();
		if (block.getType() != Material.GRAVEL)
			return;

		// Convert Block location to Pos3
		Pos3 pos = new Pos3(block.getX(), block.getY(), block.getZ());
		if (recentGlowTreeGravel.contains(pos)) {
			event.setCancelled(true);
		}
	}

	/**
	 * Adds glow tree gravel blocks to a temporary set to prevent physics updates
	 * (e.g., falling).
	 *
	 * <p>
	 * This is used after a glowstone tree is generated to mark any gravel that may
	 * be unstable (e.g., replacing logs). These entries are automatically removed
	 * after 5 minutes.
	 * </p>
	 *
	 * @param world     the world that this is taking place in
	 * @param positions the block positions involved in the tree generation
	 */
	public CompletableFuture<Void> markGlowTreeGravel(@NotNull HellblockWorld<?> world, @NotNull List<Pos3> positions) {
		final long expireAfter = 5 * 60L * 20L; // 5 minutes in ticks

		List<CompletableFuture<Void>> tasks = new ArrayList<>();

		for (Pos3 pos : positions) {
			CompletableFuture<Void> task = world.getBlockState(pos).thenAccept(stateOpt -> {
				if (stateOpt.isEmpty())
					return;

				CustomBlockState state = stateOpt.get();
				Material fallback = instance.getIslandGenerator().resolveFallbackMaterial(state);

				if (fallback != Material.GRAVEL)
					return;

				recentGlowTreeGravel.add(pos);

				// Safe scheduler context using actual world location
				Location context = pos.toLocation(world.bukkitWorld());
				instance.getScheduler().sync().runLater(() -> recentGlowTreeGravel.remove(pos), expireAfter, context);
			});

			tasks.add(task);
		}

		// Return a single future that completes when all tasks are done
		return CompletableFuture.allOf(tasks.toArray(CompletableFuture[]::new));
	}

	@EventHandler
	public void onPlaceNextToSapling(BlockPhysicsEvent event) {
		final Block block = event.getSourceBlock();
		if (!instance.getHellblockHandler().isInCorrectWorld(block.getWorld())) {
			return;
		}

		// Prevent saplings on soul sand from being updated/removed
		if (Tag.SAPLINGS.isTagged(block.getType())
				&& block.getRelative(BlockFace.DOWN).getType() == Material.SOUL_SAND) {
			event.setCancelled(true);
			return;
		}

		// Prevent surrounding saplings from being updated/removed
		for (BlockFace face : FACES) {
			if (Tag.SAPLINGS.isTagged(block.getRelative(face).getType())
					&& block.getRelative(face).getRelative(BlockFace.DOWN).getType() == Material.SOUL_SAND) {
				event.setCancelled(true);
				break;
			}
		}
	}

	@EventHandler
	public void onGlowstoneTreeGrowth(PlayerInteractEvent event) {
		final Player player = event.getPlayer();
		if (!instance.getHellblockHandler().isInCorrectWorld(player.getWorld())) {
			return;
		}

		final Block clickedBlock = event.getClickedBlock();
		final ItemStack inHand = event.getItem();
		if (clickedBlock == null || inHand == null) {
			return;
		}

		// Wrap Bukkit world into custom world
		final Optional<HellblockWorld<?>> worldOpt = instance.getWorldManager().getWorld(clickedBlock.getWorld());
		if (worldOpt.isEmpty() || worldOpt.get().bukkitWorld() == null) {
			return;
		}

		if (Tag.NYLIUM.isTagged(clickedBlock.getRelative(BlockFace.DOWN).getType())
				|| clickedBlock.getType() == Material.CRIMSON_FUNGUS
				|| clickedBlock.getType() == Material.WARPED_FUNGUS) {
			return;
		}

		final HellblockWorld<?> world = worldOpt.get();

		final Pos3 clickedPos = new Pos3(clickedBlock.getX(), clickedBlock.getY(), clickedBlock.getZ());
		final int randomChance = RandomUtils.generateRandomInt(1, 3);

		// --- Grow sapling using glowstone dust ---
		if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getBlockFace() == BlockFace.UP
				&& clickedBlock.getType() == Material.SOUL_SAND
				&& clickedBlock.getRelative(BlockFace.UP).getType().isAir()
				&& inHand.getType() == Material.GLOWSTONE_DUST) {

			useItemAndAnimateHand(event, player, inHand);
			clickedBlock.getWorld().playEffect(clickedBlock.getLocation(), Effect.SMOKE, event.getBlockFace());

			if (randomChance == 2) {
				clickedBlock.getRelative(BlockFace.UP).setType(RandomUtils.pickRandomSapling());
				CustomBlock custom = instance.getFarmingManager().getPlacedSaplingMapping()
						.get(clickedBlock.getRelative(BlockFace.UP).getType());
				if (custom != null) {
					if (instance.getFarmingManager().isFarmSapling(custom)) {
						world.updateBlockState(clickedPos.up(), custom.createBlockState());
						instance.debug("Glow tree sapling placed at " + clickedPos + " in world '" + world.worldName()
								+ "' using " + (event.getItem() != null ? event.getItem().getType() : "null") + " on "
								+ clickedBlock.getType() + " → " + custom.type().key().asString());
					}
				}
				instance.getSenderFactory().wrap(player).sendMessage(instance.getTranslationManager()
						.render(MessageConstants.MSG_HELLBLOCK_GROWING_GLOWSTONE_TREE.build()));
				AdventureHelper.playSound(instance.getSenderFactory().getAudience(player),
						Sound.sound(Key.key("minecraft:block.grass.place"), Source.PLAYER, 1.0F, 1.0F));
			}
			return;
		}

		// --- Grow glowstone tree using flint ---
		if (event.getAction() == Action.RIGHT_CLICK_BLOCK
				&& clickedBlock.getRelative(BlockFace.DOWN).getType() == Material.SOUL_SAND
				&& Tag.SAPLINGS.isTagged(clickedBlock.getType()) && inHand.getType() == Material.FLINT) {

			useItemAndAnimateHand(event, player, inHand);
			instance.getFarmingManager().playBoneMealUseEffect(clickedBlock.getWorld(), clickedBlock.getLocation());

			// Try to get state from Pos3
			if (canGrow(clickedPos, world) && randomChance == 2) {
				TreeType type = getTreeTypeForSapling(clickedPos, world.bukkitWorld());
				if (type == null)
					return;

				CustomTreeGrowContext context = new CustomTreeGrowContext(type, clickedPos, world.worldName());

				instance.getIslandGenerator()
						.generateHellblockGlowstoneTree(world, clickedPos.toLocation(world.bukkitWorld()), false)
						.thenRun(() -> instance.getStorageManager().getOnlineUser(player.getUniqueId())
								.ifPresent(userData -> {
									if (instance.getCooldownManager().shouldUpdateActivity(player.getUniqueId(),
											5000)) {
										userData.getHellblockData().updateLastIslandActivity();
									}
									instance.getChallengeManager().handleChallengeProgression(userData, ActionType.GROW,
											context);
								}));
			}
		}
	}

	@EventHandler
	public void onCompostInsert(PlayerInteractEvent event) {
		if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getHand() == null
				|| event.getHand() != EquipmentSlot.HAND)
			return;

		Block block = event.getClickedBlock();
		if (block == null || block.getType() != Material.COMPOSTER)
			return;
		if (!instance.getHellblockHandler().isInCorrectWorld(block.getWorld())) {
			return;
		}

		ItemStack item = event.getItem();
		if (item == null)
			return;

		Pos3 pos = Pos3.from(block.getLocation());
		Material type = item.getType();

		if (type == Material.FLINT || type == Material.GLOWSTONE_DUST) {
			validComposters.asMap().putIfAbsent(pos, true);
		} else {
			validComposters.put(pos, false);
		}
	}

	@EventHandler
	public void onComposterFullClick(PlayerInteractEvent event) {
		if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getHand() == null
				|| event.getHand() != EquipmentSlot.HAND)
			return;

		Block block = event.getClickedBlock();
		if (block == null || block.getType() != Material.COMPOSTER)
			return;
		if (!instance.getHellblockHandler().isInCorrectWorld(block.getWorld())) {
			return;
		}

		BlockState state = block.getState();
		if (!(state.getBlockData() instanceof Levelled levelled))
			return;

		if (levelled.getLevel() < 8)
			return;

		Pos3 pos = Pos3.from(block.getLocation());
		Boolean isValid = validComposters.getIfPresent(pos);

		if (Boolean.TRUE.equals(isValid)) {
			Player player = event.getPlayer();

			// Reset composter level
			levelled.setLevel(0);
			block.setBlockData(levelled);
			state.update();

			// Remove from cache
			validComposters.invalidate(pos);

			// Give sapling
			Material sapling = RandomUtils.generateRandomBoolean() ? Material.CRIMSON_FUNGUS : Material.WARPED_FUNGUS;
			ItemStack netherSapling = new ItemStack(sapling);
			if (player.getInventory().firstEmpty() != -1) {
				PlayerUtils.giveItem(player, netherSapling, 1);
			} else {
				PlayerUtils.dropItem(player, netherSapling, true, true, true);
			}

			AdventureHelper.playSound(instance.getSenderFactory().getAudience(player),
					Sound.sound(Key.key("minecraft:entity.item.pickup"), Source.PLAYER, 1.0F, 1.2F));
			block.getWorld().spawnParticle(ParticleUtils.getParticle("VILLAGER_HAPPY"),
					block.getLocation().clone().add(0.5, 1, 0.5), 10);

			event.setCancelled(true);
		} else {
			validComposters.invalidate(pos); // cleanup even if invalid
		}
	}

	@EventHandler
	public void onBlockBreak(BlockBreakEvent event) {
		Block block = event.getBlock();
		if (block.getType() == Material.COMPOSTER) {
			validComposters.invalidate(Pos3.from(block.getLocation()));
		}
	}

	@EventHandler
	public void onBlockExplode(BlockExplodeEvent event) {
		List<Block> blocks = event.blockList();
		blocks.stream().filter(block -> block.getType() == Material.COMPOSTER)
				.forEach(block -> validComposters.invalidate(Pos3.from(block.getLocation())));
	}

	/**
	 * Consumes one item from the player's hand (unless in creative) and triggers a
	 * hand swing animation.
	 *
	 * <p>
	 * Ensures proper item usage handling during sapling/flint interactions and
	 * provides a visual swing animation via the appropriate NMS method. Prevents
	 * item desync in multiplayer contexts.
	 * </p>
	 *
	 * @param event  the interact event triggering the action
	 * @param player the player using the item
	 * @param inHand the item being used
	 */
	private void useItemAndAnimateHand(@NotNull PlayerInteractEvent event, @NotNull Player player,
			@NotNull ItemStack inHand) {
		event.setUseItemInHand(Result.ALLOW);
		if (event.getHand() != null) {
			VersionHelper.getNMSManager().swingHand(player,
					event.getHand() == EquipmentSlot.HAND ? HandSlot.MAIN : HandSlot.OFF);
		}
		if (player.getGameMode() != GameMode.CREATIVE) {
			inHand.setAmount(Math.max(inHand.getAmount() - 1, 0));
		}
	}

	/**
	 * Determines whether a glowstone tree can grow at the given position.
	 *
	 * <p>
	 * A tree can grow if the 5x5x5 space above the sapling is entirely empty or
	 * filled with replaceable blocks like air, glowstone, vines, etc.
	 * </p>
	 *
	 * @param pos   the position of the sapling block
	 * @param world the custom world instance
	 * @return {@code true} if a tree can grow in the space, {@code false} otherwise
	 */
	public boolean canGrow(@NotNull Pos3 pos, @NotNull HellblockWorld<?> world) {
		int centerX = pos.x();
		int centerY = pos.y();
		int centerZ = pos.z();

		for (int x = centerX - 2; x <= centerX + 2; x++) {
			for (int y = centerY + 1; y <= centerY + 5; y++) {
				for (int z = centerZ - 2; z <= centerZ + 2; z++) {
					Location loc = new Location(world.bukkitWorld(), x, y, z);
					Material material = loc.getBlock().getType();

					if (!canGrowIn(material)) {
						return false;
					}
				}
			}
		}
		return true;
	}

	/**
	 * Determines whether a material can be replaced during glowstone tree growth.
	 *
	 * <p>
	 * Valid materials are glowstone, air, snow, tall grass, or vines — matching
	 * those considered replaceable in vanilla generation logic.
	 * </p>
	 *
	 * @param material the material to check
	 * @return {@code true} if the block is replaceable, {@code false} otherwise
	 */
	private boolean canGrowIn(@NotNull Material material) {
		return material == Material.GLOWSTONE || material.isAir() || material == Material.SNOW
				|| material == Material.TALL_GRASS || material == Material.VINE;
	}

	/**
	 * Infers the {@link TreeType} that should grow from the sapling at a given
	 * position.
	 *
	 * <p>
	 * This method is version-safe and supports all current sapling types. If a
	 * sapling type is unavailable in the current Minecraft version (e.g.,
	 * {@code MANGROVE_PROPAGULE}, {@code CHERRY_SAPLING},
	 * {@code PALE_OAK_SAPLING}), it will be safely ignored.
	 * </p>
	 *
	 * <p>
	 * Glow saplings visually mimic a random vanilla sapling but always grow into
	 * custom Glowstone Trees. These are handled externally via
	 * {@link CustomTreeGrowContext}.
	 * </p>
	 *
	 * @param pos   the {@link Pos3} location of the sapling
	 * @param world the Bukkit {@link World} the sapling resides in
	 * @return the matching {@link TreeType}, or {@code null} if none is recognized
	 */
	@Nullable
	public TreeType getTreeTypeForSapling(@NotNull Pos3 pos, @NotNull World world) {
		Block block = pos.toLocation(world).getBlock();
		Material mat = block.getType();
		String name = mat.name();

		// Handle all known sapling types dynamically
		return switch (name) {
		case "OAK_SAPLING" -> TreeType.TREE;
		case "SPRUCE_SAPLING" -> TreeType.REDWOOD;
		case "BIRCH_SAPLING" -> TreeType.BIRCH;
		case "JUNGLE_SAPLING" -> TreeType.SMALL_JUNGLE;
		case "ACACIA_SAPLING" -> TreeType.ACACIA;
		case "DARK_OAK_SAPLING" -> TreeType.DARK_OAK;

		// Conditional types handled by name string — no crash on older versions
		case "MANGROVE_PROPAGULE" -> safeTreeType("MANGROVE");
		case "CHERRY_SAPLING" -> safeTreeType("CHERRY");
		case "PALE_OAK_SAPLING" -> safeTreeType("PALE_OAK");

		default -> null;
		};
	}

	/**
	 * Helper for safely resolving a {@link TreeType} by name across different
	 * versions.
	 */
	@Nullable
	private static TreeType safeTreeType(@NotNull String typeName) {
		try {
			return TreeType.valueOf(typeName);
		} catch (NoSuchFieldError | IllegalArgumentException ignored) {
			return null;
		}
	}

	/**
	 * Recursively finds all adjacent blocks of the same type connected to the given
	 * block.
	 *
	 * <p>
	 * This is a helper for the flood-fill algorithm used in
	 * {@link #getConnectedBlocks(Block)}. It checks all 6 directions from the
	 * current block and adds any connected blocks of the same type to the result
	 * set.
	 * </p>
	 *
	 * @param block   the origin block
	 * @param results the set collecting all connected blocks
	 * @param convert the queue used for flood fill traversal
	 */
	private void getConnectedBlocks(@NotNull Block block, @NotNull Set<Block> results, @NotNull Deque<Block> convert) {
		for (BlockFace face : FACES) {
			final Block b = block.getRelative(face);
			if (b.getType() == block.getType() && results.add(b)) {
				convert.add(b);
			}
		}
	}

	/**
	 * Finds all contiguous blocks of the same type connected to the given origin
	 * block.
	 *
	 * <p>
	 * Uses a flood-fill search to collect all touching blocks of the same type,
	 * checking in all 6 cardinal directions. Useful for detecting tree structures
	 * or preventing circular growth issues.
	 * </p>
	 *
	 * @param block the starting block
	 * @return a set of all connected blocks of the same type
	 */
	@NotNull
	public Set<Block> getConnectedBlocks(@NotNull Block block) {
		final Set<Block> set = new HashSet<>();
		final Deque<Block> queue = new ArrayDeque<>();

		queue.add(block);

		while ((block = queue.poll()) != null) {
			getConnectedBlocks(block, set, queue);
		}
		return set;
	}

	/**
	 * Represents the context for a tree growth event during a {@code GROW}
	 * challenge.
	 * <p>
	 * Used to describe both the type of tree that was grown and the specific
	 * sapling block involved. This record is passed to {@link GrowRequirement}
	 * during tree-related progression checks.
	 * </p>
	 *
	 * <p>
	 * <b>Example use:</b> If a player plants a sapling and it grows into a tree,
	 * the plugin creates a {@code TreeGrowContext} containing the {@link TreeType}
	 * and the original {@link Pos3} where the sapling was placed.
	 * </p>
	 *
	 * @param treeType  The Bukkit {@link TreeType} that was generated (e.g.,
	 *                  {@code TREE}, {@code WARPED_FUNGUS}).
	 * @param position  The {@link Pos3} representing the sapling that initiated the
	 *                  tree growth.
	 * @param worldName The world that this growth is taking place in.
	 */
	public record CustomTreeGrowContext(@NotNull TreeType treeType, @NotNull Pos3 position, @NotNull String worldName) {
	}
}