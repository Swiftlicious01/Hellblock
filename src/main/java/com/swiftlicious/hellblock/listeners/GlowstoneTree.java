package com.swiftlicious.hellblock.listeners;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.TreeType;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.world.StructureGrowEvent;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.challenges.HellblockChallenge.ChallengeType;
import com.swiftlicious.hellblock.playerdata.HellblockPlayer;
import com.swiftlicious.hellblock.utils.LocationCache;

import lombok.AllArgsConstructor;
import lombok.Getter;

public class GlowstoneTree implements Listener {

	private final HellblockPlugin instance;

	@Getter
	public final Map<Location, GlowTree> glowTreeCache;

	private static final Set<TreeType> TREE_TYPES = new HashSet<>(Arrays.asList(TreeType.TREE, TreeType.BIRCH,
			TreeType.ACACIA, TreeType.JUNGLE, TreeType.DARK_OAK, TreeType.BIG_TREE, TreeType.CHERRY, TreeType.REDWOOD,
			TreeType.MANGROVE, TreeType.TALL_REDWOOD, TreeType.SWAMP, TreeType.SMALL_JUNGLE, TreeType.TALL_BIRCH,
			TreeType.TALL_REDWOOD, TreeType.MEGA_PINE, TreeType.MEGA_REDWOOD, TreeType.TALL_MANGROVE, TreeType.AZALEA));

	public GlowstoneTree(HellblockPlugin plugin) {
		instance = plugin;
		this.glowTreeCache = new HashMap<>();
		Bukkit.getPluginManager().registerEvents(this, instance);
	}

	@EventHandler
	public void onGlowTreeCreation(StructureGrowEvent event) {
		if (!event.getWorld().getName().equalsIgnoreCase(instance.getHellblockHandler().getWorldName()))
			return;

		if (TREE_TYPES.contains(event.getSpecies())) {
			final Player player = event.getPlayer();
			// have to check this to prevent sapling bonemeal over a block and see it's not
			// growing.
			if (player != null && event.isFromBonemeal() && !event.getBlocks().isEmpty()
					&& event.getBlocks().size() > 4) {
				HellblockPlayer pi = instance.getHellblockHandler().getActivePlayer(player);
				if (!pi.isChallengeActive(ChallengeType.GLOWSTONE_TREE_CHALLENGE)
						&& !pi.isChallengeCompleted(ChallengeType.GLOWSTONE_TREE_CHALLENGE)) {
					pi.beginChallengeProgression(ChallengeType.GLOWSTONE_TREE_CHALLENGE);
				} else {
					pi.updateChallengeProgression(ChallengeType.GLOWSTONE_TREE_CHALLENGE, 1);
					if (pi.isChallengeCompleted(ChallengeType.GLOWSTONE_TREE_CHALLENGE)) {
						pi.completeChallenge(ChallengeType.GLOWSTONE_TREE_CHALLENGE);
					}
				}
			}

			Iterator<BlockState> blocks = event.getBlocks().iterator();
			GlowTree glowTree = new GlowTree(event.getSpecies(), event.getBlocks());

			while (true) {
				BlockState block;
				do {
					if (!blocks.hasNext()) {
						return;
					}
					block = (BlockState) blocks.next();
					if (Tag.LOGS_THAT_BURN.isTagged(block.getType())) {
						block.setType(Material.GRAVEL);
						block.update();
					}
				} while (!Tag.LEAVES.isTagged(block.getType()));

				block.setType(Material.GLOWSTONE);
				block.update();
				this.glowTreeCache.putIfAbsent(LocationCache.getCachedLocation(event.getLocation()), glowTree);
			}
		}
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onGravelChangeBlock(EntityChangeBlockEvent event) {
		if (!event.getBlock().getWorld().getName().equalsIgnoreCase(instance.getHellblockHandler().getWorldName()))
			return;

		if (event.getBlock().getType() != Material.GRAVEL) {
			return;
		}

		if (this.glowTreeCache.isEmpty()) {
			return;
		}

		if (this.glowTreeCache.containsKey(event.getBlock().getLocation())) {
			event.setCancelled(true);
			GlowTree glowTree = this.glowTreeCache.get(event.getBlock().getLocation());
			for (BlockState state : glowTree.getTreeBlocks()) {
				Block block = state.getBlock();
				if (block.getType() == Material.GRAVEL) {
					event.setCancelled(true);
				}
			}
		} else {
			Set<Block> blocks = getConnectedBlocks(event.getBlock());
			for (Block block : blocks) {
				if (this.glowTreeCache.containsKey(block.getLocation())) {
					this.glowTreeCache.remove(block.getLocation());
				}
			}
		}
	}

	@EventHandler
	public void onGravelExplode(BlockExplodeEvent event) {
		if (!event.getBlock().getWorld().getName().equalsIgnoreCase(instance.getHellblockHandler().getWorldName()))
			return;

		if (event.getBlock().getType() != Material.GRAVEL) {
			return;
		}

		if (this.glowTreeCache.isEmpty()) {
			return;
		}

		if (this.glowTreeCache.containsKey(event.getBlock().getLocation())) {
			this.glowTreeCache.remove(event.getBlock().getLocation());
		} else {
			Set<Block> blocks = getConnectedBlocks(event.getBlock());
			for (Block block : blocks) {
				if (this.glowTreeCache.containsKey(block.getLocation())) {
					this.glowTreeCache.remove(block.getLocation());
				}
			}
		}
	}

	@EventHandler
	public void onGravelBreak(BlockBreakEvent event) {
		if (!event.getBlock().getWorld().getName().equalsIgnoreCase(instance.getHellblockHandler().getWorldName()))
			return;

		if (event.getBlock().getType() != Material.GRAVEL) {
			return;
		}

		if (this.glowTreeCache.isEmpty()) {
			return;
		}

		if (this.glowTreeCache.containsKey(event.getBlock().getLocation())) {
			this.glowTreeCache.remove(event.getBlock().getLocation());
		} else {
			Set<Block> blocks = getConnectedBlocks(event.getBlock());
			for (Block block : blocks) {
				if (this.glowTreeCache.containsKey(block.getLocation())) {
					this.glowTreeCache.remove(block.getLocation());
				}
			}
		}
	}

	@EventHandler
	public void onGravelMove(BlockPistonExtendEvent event) {
		if (!event.getBlock().getWorld().getName().equalsIgnoreCase(instance.getHellblockHandler().getWorldName()))
			return;

		if (event.getBlock().getType() != Material.GRAVEL) {
			return;
		}

		if (this.glowTreeCache.isEmpty()) {
			return;
		}

		if (this.glowTreeCache.containsKey(event.getBlock().getLocation())) {
			this.glowTreeCache.remove(event.getBlock().getLocation());
		} else {
			Set<Block> blocks = getConnectedBlocks(event.getBlock());
			for (Block block : blocks) {
				if (this.glowTreeCache.containsKey(block.getLocation())) {
					this.glowTreeCache.remove(block.getLocation());
				}
			}
		}
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

	@AllArgsConstructor
	@Getter
	public class GlowTree {

		private TreeType originalTreeType;
		private List<BlockState> treeBlocks;
	}
}