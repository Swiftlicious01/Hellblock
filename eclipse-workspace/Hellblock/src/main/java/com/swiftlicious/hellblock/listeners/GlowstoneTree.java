package com.swiftlicious.hellblock.listeners;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.TreeType;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.StructureGrowEvent;

import com.swiftlicious.hellblock.HellblockPlugin;

import lombok.AllArgsConstructor;
import lombok.Getter;

public class GlowstoneTree implements Listener {

	private final HellblockPlugin instance;

	private static final Set<TreeType> TREE_TYPES = new HashSet<>(Arrays.asList(TreeType.TREE, TreeType.BIRCH,
			TreeType.ACACIA, TreeType.JUNGLE, TreeType.DARK_OAK, TreeType.BIG_TREE, TreeType.CHERRY, TreeType.REDWOOD,
			TreeType.MANGROVE, TreeType.TALL_REDWOOD, TreeType.SWAMP, TreeType.SMALL_JUNGLE, TreeType.TALL_BIRCH,
			TreeType.TALL_REDWOOD, TreeType.MEGA_PINE, TreeType.MEGA_REDWOOD, TreeType.TALL_MANGROVE, TreeType.AZALEA));

	public GlowstoneTree(HellblockPlugin plugin) {
		instance = plugin;
		Bukkit.getPluginManager().registerEvents(this, instance);
	}

	@EventHandler
	public void onGlowTreeCreation(StructureGrowEvent event) {
		if (!event.getWorld().getName().equalsIgnoreCase(instance.getHellblockHandler().getWorldName()))
			return;

		if (TREE_TYPES.contains(event.getSpecies())) {

			Iterator<BlockState> blocks = event.getBlocks().iterator();

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
			}
		}
	}

	// TODO: make gravel not fall if spawned from a tree?
//	@EventHandler(priority = EventPriority.HIGHEST)
//	public void onGravelChangeBlock(BlockPhysicsEvent event) {
//		if (!event.getBlock().getWorld().getName().equalsIgnoreCase(instance.getHellblockHandler().getWorldName()))
//			return;
//
//		if (event.getSourceBlock().getType() == Material.GRAVEL) {
//			event.setCancelled(true);
//		}
//	}

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
		private Location spawnLocation;
		private List<BlockState> treeBlocks;
	}
}