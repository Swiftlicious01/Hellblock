package com.swiftlicious.hellblock.listeners;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.TreeType;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.type.Sapling;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.StructureGrowEvent;
import org.bukkit.inventory.ItemStack;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.challenges.HellblockChallenge.ChallengeType;
import com.swiftlicious.hellblock.player.OnlineUser;
import com.swiftlicious.hellblock.utils.RandomUtils;

import lombok.NonNull;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound.Source;

public class GlowstoneTree implements Listener {

	private final HellblockPlugin instance;

	private boolean growNaturalTrees;

	private static final Set<TreeType> TREE_TYPES = Set.of(TreeType.TREE, TreeType.BIRCH, TreeType.ACACIA,
			TreeType.JUNGLE, TreeType.DARK_OAK, TreeType.BIG_TREE, TreeType.CHERRY, TreeType.REDWOOD, TreeType.MANGROVE,
			TreeType.TALL_REDWOOD, TreeType.SWAMP, TreeType.SMALL_JUNGLE, TreeType.TALL_BIRCH, TreeType.MEGA_PINE,
			TreeType.MEGA_REDWOOD, TreeType.TALL_MANGROVE, TreeType.AZALEA);

	public GlowstoneTree(HellblockPlugin plugin) {
		instance = plugin;
		this.growNaturalTrees = instance.getConfig("config.yml").getBoolean("hellblock.grow-natural-trees", false);
		Bukkit.getPluginManager().registerEvents(this, instance);
	}

	@EventHandler
	public void onGlowTreeCreation(StructureGrowEvent event) {
		if (!event.getWorld().getName().equalsIgnoreCase(instance.getHellblockHandler().getWorldName()))
			return;

		Material growing = event.getLocation().getBlock().getRelative(BlockFace.DOWN).getType();
		if (growing == Material.SOUL_SAND) {
			final Player player = event.getPlayer();
			if (player != null && event.isFromBonemeal() && !event.getBlocks().isEmpty()
					&& !event.getBlocks().stream().anyMatch(state -> state.getBlockData() instanceof Sapling)) {
				OnlineUser onlineUser = instance.getStorageManager().getOnlineUser(player.getUniqueId());
				if (onlineUser == null)
					return;
				if (!onlineUser.getHellblockData().isChallengeActive(ChallengeType.GLOWSTONE_TREE_CHALLENGE)
						&& !onlineUser.getHellblockData()
								.isChallengeCompleted(ChallengeType.GLOWSTONE_TREE_CHALLENGE)) {
					onlineUser.getHellblockData().beginChallengeProgression(onlineUser.getPlayer(),
							ChallengeType.GLOWSTONE_TREE_CHALLENGE);
				} else {
					onlineUser.getHellblockData().updateChallengeProgression(onlineUser.getPlayer(),
							ChallengeType.GLOWSTONE_TREE_CHALLENGE, 1);
					if (onlineUser.getHellblockData().isChallengeCompleted(ChallengeType.GLOWSTONE_TREE_CHALLENGE)) {
						onlineUser.getHellblockData().completeChallenge(onlineUser.getPlayer(),
								ChallengeType.GLOWSTONE_TREE_CHALLENGE);
					}
				}
			}

			event.setCancelled(true);
			instance.getIslandGenerator().generateGlowstoneTree(event.getLocation());
		} else {
			if (!this.growNaturalTrees) {
				if (TREE_TYPES.contains(event.getSpecies())) {
					final Player player = event.getPlayer();
					// have to check this to prevent sapling bonemeal over a block and see it's not
					// growing.
					if (player != null && event.isFromBonemeal() && !event.getBlocks().isEmpty()
							&& !event.getBlocks().stream().anyMatch(state -> state.getBlockData() instanceof Sapling)) {
						OnlineUser onlineUser = instance.getStorageManager().getOnlineUser(player.getUniqueId());
						if (onlineUser == null)
							return;
						if (!onlineUser.getHellblockData().isChallengeActive(ChallengeType.GLOWSTONE_TREE_CHALLENGE)
								&& !onlineUser.getHellblockData()
										.isChallengeCompleted(ChallengeType.GLOWSTONE_TREE_CHALLENGE)) {
							onlineUser.getHellblockData().beginChallengeProgression(onlineUser.getPlayer(),
									ChallengeType.GLOWSTONE_TREE_CHALLENGE);
						} else {
							onlineUser.getHellblockData().updateChallengeProgression(onlineUser.getPlayer(),
									ChallengeType.GLOWSTONE_TREE_CHALLENGE, 1);
							if (onlineUser.getHellblockData()
									.isChallengeCompleted(ChallengeType.GLOWSTONE_TREE_CHALLENGE)) {
								onlineUser.getHellblockData().completeChallenge(onlineUser.getPlayer(),
										ChallengeType.GLOWSTONE_TREE_CHALLENGE);
							}
						}
					}
					Iterator<BlockState> blocks = event.getBlocks().iterator();

					while (true) {
						BlockState block;
						do {
							if (!blocks.hasNext()) {
								return;
							}

							block = (BlockState) blocks.next();
							if (Tag.LOGS_THAT_BURN.isTagged(block.getType())
									|| Tag.SAPLINGS.isTagged(block.getType())) {
								block.setType(Material.GRAVEL);
							}
							if (Tag.DIRT.isTagged(block.getType())) {
								block.setType(growing);
							}
						} while (Tag.LEAVES.isTagged(block.getType()));

						block.setType(Material.GLOWSTONE);
					}
				}
			}
		}
	}

	@EventHandler
	public void onPlaceNextToSapling(BlockPhysicsEvent event) {
		final Block block = event.getSourceBlock();
		if (!block.getWorld().getName().equalsIgnoreCase(instance.getHellblockHandler().getWorldName()))
			return;

		if (block.getType() == Material.OAK_SAPLING) {
			if (block.getRelative(BlockFace.DOWN).getType() == Material.SOUL_SAND) {
				event.setCancelled(true);
			}
		}
		for (BlockFace face : FACES) {
			if (block.getRelative(face).getType() == Material.OAK_SAPLING) {
				if (block.getRelative(face).getRelative(BlockFace.DOWN).getType() == Material.SOUL_SAND) {
					event.setCancelled(true);
				}
			}
		}
	}

	@EventHandler
	public void onGlowstoneTreeGrowth(PlayerInteractEvent event) {
		final Player player = event.getPlayer();
		if (!player.getWorld().getName().equalsIgnoreCase(instance.getHellblockHandler().getWorldName()))
			return;

		final UUID id = player.getUniqueId();
		Block block = event.getClickedBlock();
		ItemStack inHand = event.getItem();
		int randomChance = RandomUtils.generateRandomInt(1, 3);
		if (inHand != null && block != null) {
			if (event.getBlockFace() == BlockFace.UP && block.getType() == Material.SOUL_SAND
					&& block.getRelative(BlockFace.UP).getType().isAir()) {
				if (inHand.getType() == Material.GLOWSTONE_DUST) {
					player.swingMainHand();
					block.getWorld().playEffect(block.getLocation(), Effect.SHOOT_WHITE_SMOKE, event.getBlockFace());
					inHand.setAmount(inHand.getAmount() > 1 ? inHand.getAmount() - 1 : 0);
					if (randomChance == RandomUtils.generateRandomInt(2, 3)) {
						block.getRelative(BlockFace.UP).setType(Material.OAK_SAPLING);
						instance.getAdventureManager().sendMessage(player, "<red>Soul Sand has been fertilized!");
						instance.getAdventureManager().sendMessage(player,
								"<red>Right click with <dark_red>flint <red>to grow the <gold>glowstone tree<red>!");
						instance.getAdventureManager().sendSound(player, Source.PLAYER,
								Key.key("minecraft:block.grass.place"), 1.0F, 1.0F);
					}
				}
			}

			if (block.getRelative(BlockFace.DOWN).getType() == Material.SOUL_SAND
					&& block.getType() == Material.OAK_SAPLING) {
				if (inHand.getType() == Material.FLINT) {
					player.swingMainHand();
					inHand.setAmount(inHand.getAmount() > 1 ? inHand.getAmount() - 1 : 0);
					block.getWorld().playEffect(block.getLocation(), Effect.BONE_MEAL_USE,
							RandomUtils.generateRandomInt(2, 5));
					if (canGrow(block)) {
						if (randomChance == RandomUtils.generateRandomInt(2, 4)) {
							OnlineUser onlineUser = instance.getStorageManager().getOnlineUser(id);
							if (onlineUser == null)
								return;
							instance.getIslandGenerator().generateGlowstoneTree(block.getLocation()).thenRun(() -> {
								if (!onlineUser.getHellblockData()
										.isChallengeActive(ChallengeType.GLOWSTONE_TREE_CHALLENGE)
										&& !onlineUser.getHellblockData()
												.isChallengeCompleted(ChallengeType.GLOWSTONE_TREE_CHALLENGE)) {
									onlineUser.getHellblockData().beginChallengeProgression(onlineUser.getPlayer(),
											ChallengeType.GLOWSTONE_TREE_CHALLENGE);
								} else {
									onlineUser.getHellblockData().updateChallengeProgression(onlineUser.getPlayer(),
											ChallengeType.GLOWSTONE_TREE_CHALLENGE, 1);
									if (onlineUser.getHellblockData()
											.isChallengeCompleted(ChallengeType.GLOWSTONE_TREE_CHALLENGE)) {
										onlineUser.getHellblockData().completeChallenge(onlineUser.getPlayer(),
												ChallengeType.GLOWSTONE_TREE_CHALLENGE);
									}
								}
							});
						}
					}
				}
			}
		}
	}

	private boolean canGrow(@NonNull Block block) {
		int centerX = block.getLocation().getBlockX();
		int centerY = block.getLocation().getBlockY();
		int centerZ = block.getLocation().getBlockZ();
		for (int x = centerX - 2; x <= centerX + 2; x++)
			for (int y = centerY + 1; y <= centerY + 5; y++)
				for (int z = centerZ - 2; z <= centerZ + 2; z++)
					if (!canGrowIn(block.getWorld().getBlockAt(x, y, z).getType()))
						return false;

		return true;
	}

	private boolean canGrowIn(Material material) {
		return material == Material.GLOWSTONE || material.isAir() || material == Material.SNOW
				|| material == Material.TALL_GRASS || material == Material.VINE;
	}

	// These are all the sides of the block
	private static final BlockFace[] FACES = { BlockFace.DOWN, BlockFace.UP, BlockFace.NORTH, BlockFace.EAST,
			BlockFace.SOUTH, BlockFace.WEST };

	private void getConnectedBlocks(Block block, Set<Block> results, List<Block> todo) {
		// Here I collect all blocks that are directly connected to variable 'block'.
		// (Shouldn't be more than 6, because a block has 6 sides)
		Set<Block> result = results;

		// Loop through all block faces (All 6 sides around the block)
		for (BlockFace face : FACES) {
			Block b = block.getRelative(face);
			// Check if they're both of the same type
			if (b.getType() == block.getType()) {
				// Add the block if it wasn't added already
				if (result.add(b)) {

					// Add this block to the list of blocks that are yet to be done.
					todo.add(b);
				}
			}
		}
	}

	public Set<Block> getConnectedBlocks(Block block) {
		Set<Block> set = new HashSet<>();
		LinkedList<Block> list = new LinkedList<>();

		// Add the current block to the list of blocks that are yet to be done
		list.add(block);

		// Execute this method for each block in the 'todo' list
		while ((block = list.poll()) != null) {
			getConnectedBlocks(block, set, list);
		}
		return set;
	}
}