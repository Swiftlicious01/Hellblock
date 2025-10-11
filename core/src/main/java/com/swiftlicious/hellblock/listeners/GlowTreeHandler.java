package com.swiftlicious.hellblock.listeners;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

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
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Result;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.StructureGrowEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.api.Reloadable;
import com.swiftlicious.hellblock.challenges.HellblockChallenge.ActionType;
import com.swiftlicious.hellblock.config.locale.MessageConstants;
import com.swiftlicious.hellblock.handlers.AdventureHelper;
import com.swiftlicious.hellblock.handlers.VersionHelper;
import com.swiftlicious.hellblock.nms.inventory.HandSlot;
import com.swiftlicious.hellblock.utils.LocationUtils;
import com.swiftlicious.hellblock.utils.RandomUtils;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.sound.Sound.Source;

public class GlowTreeHandler implements Listener, Reloadable {

	protected final HellblockPlugin instance;

	private static final Set<TreeType> TREE_TYPES = buildTreeTypes();

	private final Set<Location> recentGlowTreeGravel = ConcurrentHashMap.newKeySet();

	private static Set<TreeType> buildTreeTypes() {
		Set<TreeType> types = EnumSet.noneOf(TreeType.class);

		// Always safe in 1.17+
		types.addAll(Set.of(TreeType.TREE, TreeType.BIG_TREE, TreeType.REDWOOD, TreeType.TALL_REDWOOD, TreeType.BIRCH,
				TreeType.TALL_BIRCH, TreeType.JUNGLE, TreeType.SMALL_JUNGLE, TreeType.DARK_OAK, TreeType.ACACIA,
				TreeType.SWAMP, TreeType.MEGA_REDWOOD, TreeType.MEGA_PINE));

		// Conditionally add newer types
		try {
			types.add(TreeType.MANGROVE);
		} catch (IllegalArgumentException ignored) {
		}
		try {
			types.add(TreeType.TALL_MANGROVE);
		} catch (IllegalArgumentException ignored) {
		}
		try {
			types.add(TreeType.CHERRY);
		} catch (IllegalArgumentException ignored) {
		}
		try {
			types.add(TreeType.AZALEA);
		} catch (IllegalArgumentException ignored) {
		}

		return types;
	}

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

	public void updateGlowstoneTrees(World world, Player player) {
		if (!instance.getHellblockHandler().isInCorrectWorld(world)) {
			return;
		}

		// Skip if no lava rain
		if (!instance.getFarmingManager().lavaRainCheck(world.getSpawnLocation().getBlock())) {
			return;
		}

		instance.getProtectionManager().getHellblockBlocks(world, player.getUniqueId()).thenAccept(blocks -> {
			if (blocks == null || blocks.isEmpty()) {
				return;
			}

			for (Block block : blocks) {
				if (!Tag.SAPLINGS.isTagged(block.getType()))
					continue;
				if (block.getRelative(BlockFace.DOWN).getType() != Material.SOUL_SAND)
					continue;

				// Wrap bonus logic
				instance.getFarmingManager().withCropGrowthBonusIfValid(block, bonus -> {
					int baseChance = 5;
					int finalChance = (int) Math.min(100, baseChance + bonus);

					if (instance.getFarmingManager().rollChance(finalChance) && canGrow(block)) {
						TreeGrowContext context = new TreeGrowContext(getTreeTypeForSapling(block.getType()), block);

						instance.getIslandGenerator().generateHellblockGlowstoneTree(world, block.getLocation(), false)
								.thenRun(() -> instance.getStorageManager().getOnlineUser(player.getUniqueId())
										.ifPresent(user -> instance.getChallengeManager()
												.handleChallengeProgression(player, ActionType.GROW, context)));
					}
				});
			}
		});
	}

	@EventHandler
	public void onGlowTreeCreation(StructureGrowEvent event) {
		if (!instance.getHellblockHandler().isInCorrectWorld(event.getWorld())) {
			return;
		}

		final Player player = event.getPlayer();
		final TreeGrowContext context = new TreeGrowContext(event.getSpecies(), event.getLocation().getBlock());
		final Material growing = event.getLocation().getBlock().getRelative(BlockFace.DOWN).getType();

		// Special glowstone tree growth on soul sand
		if (growing == Material.SOUL_SAND) {
			event.setCancelled(true);
			instance.getIslandGenerator().generateHellblockGlowstoneTree(event.getWorld(), event.getLocation(), false)
					.thenRun(() -> {
						// Track glowtree gravel
						markGlowTreeGravel(event.getBlocks());

						if (player != null) {
							instance.getStorageManager().getOnlineUser(player.getUniqueId())
									.ifPresent(user -> instance.getChallengeManager().handleChallengeProgression(player,
											ActionType.GROW, context));
						}
					});
			return;
		}

		// Replace natural tree growth if disabled
		final boolean naturalCondition = !instance.getConfigManager().growNaturalTrees()
				&& TREE_TYPES.contains(event.getSpecies());

		if (!naturalCondition) {
			instance.getStorageManager().getOnlineUser(player.getUniqueId()).ifPresent(user -> instance
					.getChallengeManager().handleChallengeProgression(player, ActionType.GROW, context));
			return;
		}

		event.getBlocks().forEach(block -> {
			if (Tag.LOGS_THAT_BURN.isTagged(block.getType()) || Tag.SAPLINGS.isTagged(block.getType())) {
				block.setType(Material.GRAVEL);
			} else if (Tag.DIRT.isTagged(block.getType())) {
				block.setType(growing);
			} else if (Tag.LEAVES.isTagged(block.getType())) {
				block.setType(Material.GLOWSTONE);
			} else if (block.getType() == Material.VINE) {
				final Material newVine = (Math.random() < 0.5) ? Material.WEEPING_VINES : Material.TWISTING_VINES;
				block.setType(newVine);

				// Set random growth length if vine supports it
				if (block.getBlockData() instanceof Ageable ageable) {
					ageable.setAge(ThreadLocalRandom.current().nextInt(1, ageable.getMaximumAge() + 1));
					block.setBlockData(ageable);
				}
			}
		});
	}

	@EventHandler
	public void onBlockFall(EntityChangeBlockEvent event) {
		if (!(event.getEntity() instanceof FallingBlock))
			return;

		Block block = event.getBlock();
		if (block.getType() != Material.GRAVEL)
			return;

		Location loc = block.getLocation().getBlock().getLocation(); // Normalize
		if (recentGlowTreeGravel.contains(loc)) {
			event.setCancelled(true);
		}
	}

	private void markGlowTreeGravel(List<BlockState> blocks) {
		final long expireAfter = 5 * 60L * 20L; // 5 minutes in ticks

		// Normalize location
		blocks.stream().map(BlockState::getBlock).filter(block -> block.getType() == Material.GRAVEL)
				.map(block -> block.getLocation().getBlock().getLocation()).forEach(loc -> {
					recentGlowTreeGravel.add(loc);
					// Schedule removal from cache
					instance.getScheduler().sync().runLater(() -> recentGlowTreeGravel.remove(loc), expireAfter,
							LocationUtils.getAnyLocationInstance());
				});
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

		final Block block = event.getClickedBlock();
		final ItemStack inHand = event.getItem();
		if (block == null || inHand == null) {
			return;
		}

		final int randomChance = RandomUtils.generateRandomInt(1, 3);

		// Grow sapling using glowstone dust
		if (event.getBlockFace() == BlockFace.UP && block.getType() == Material.SOUL_SAND
				&& block.getRelative(BlockFace.UP).getType().isAir() && inHand.getType() == Material.GLOWSTONE_DUST) {

			useItemAndAnimateHand(event, player, inHand);
			block.getWorld().playEffect(block.getLocation(), Effect.SMOKE, event.getBlockFace());

			if (randomChance == 2) {
				block.getRelative(BlockFace.UP).setType(RandomUtils.pickRandomSapling());
				instance.getSenderFactory().wrap(player).sendMessage(instance.getTranslationManager()
						.render(MessageConstants.MSG_HELLBLOCK_GROWING_GLOWSTONE_TREE.build()));
				AdventureHelper.playSound(instance.getSenderFactory().getAudience(player),
						Sound.sound(Key.key("minecraft:block.grass.place"), Source.PLAYER, 1.0F, 1.0F));
			}
			return;
		}

		// Grow glowstone tree using flint
		if (!(block.getRelative(BlockFace.DOWN).getType() == Material.SOUL_SAND
				&& Tag.SAPLINGS.isTagged(block.getType()) && inHand.getType() == Material.FLINT)) {
			return;
		}

		useItemAndAnimateHand(event, player, inHand);
		block.getWorld().playEffect(block.getLocation(), Effect.BONE_MEAL_USE, RandomUtils.generateRandomInt(2, 5));
		if (canGrow(block) && randomChance == 2) {
			instance.getIslandGenerator().generateHellblockGlowstoneTree(block.getWorld(), block.getLocation(), false)
					.thenRun(() -> {
						final TreeGrowContext context = new TreeGrowContext(getTreeTypeForSapling(block.getType()),
								block);
						instance.getStorageManager().getOnlineUser(player.getUniqueId()).ifPresent(user -> instance
								.getChallengeManager().handleChallengeProgression(player, ActionType.GROW, context));
					});
		}
	}

	private void useItemAndAnimateHand(PlayerInteractEvent event, Player player, ItemStack inHand) {
		event.setUseItemInHand(Result.ALLOW);
		VersionHelper.getNMSManager().swingHand(player,
				event.getHand() == EquipmentSlot.HAND ? HandSlot.MAIN : HandSlot.OFF);
		if (player.getGameMode() != GameMode.CREATIVE) {
			inHand.setAmount(Math.max(inHand.getAmount() - 1, 0));
		}
	}

	private boolean canGrow(@NotNull Block block) {
		final int centerX = block.getX();
		final int centerY = block.getY();
		final int centerZ = block.getZ();

		for (int x = centerX - 2; x <= centerX + 2; x++) {
			for (int y = centerY + 1; y <= centerY + 5; y++) {
				for (int z = centerZ - 2; z <= centerZ + 2; z++) {
					if (!canGrowIn(block.getWorld().getBlockAt(x, y, z).getType())) {
						return false;
					}
				}
			}
		}
		return true;
	}

	private boolean canGrowIn(Material material) {
		return material == Material.GLOWSTONE || material.isAir() || material == Material.SNOW
				|| material == Material.TALL_GRASS || material == Material.VINE;
	}

	private void getConnectedBlocks(Block block, Set<Block> results, Deque<Block> convert) {
		for (BlockFace face : FACES) {
			final Block b = block.getRelative(face);
			if (b.getType() == block.getType() && results.add(b)) {
				convert.add(b);
			}
		}
	}

	private TreeType getTreeTypeForSapling(Material sapling) {
		return switch (sapling) {
		case OAK_SAPLING -> TreeType.TREE;
		case SPRUCE_SAPLING -> TreeType.REDWOOD;
		case BIRCH_SAPLING -> TreeType.BIRCH;
		case JUNGLE_SAPLING -> TreeType.SMALL_JUNGLE;
		case ACACIA_SAPLING -> TreeType.ACACIA;
		case DARK_OAK_SAPLING -> TreeType.DARK_OAK;
		case MANGROVE_PROPAGULE -> TreeType.MANGROVE;
		case CHERRY_SAPLING -> TreeType.CHERRY;
		case AZALEA -> TreeType.AZALEA;
		default -> null; // not a sapling we care about
		};
	}

	public Set<Block> getConnectedBlocks(Block block) {
		final Set<Block> set = new HashSet<>();
		final Deque<Block> queue = new ArrayDeque<>();

		queue.add(block);

		while ((block = queue.poll()) != null) {
			getConnectedBlocks(block, set, queue);
		}
		return set;
	}

	// small record used for tree grow context (sapling + tree type) if you want
	public record TreeGrowContext(TreeType treeType, Block sapling) {
	}
}