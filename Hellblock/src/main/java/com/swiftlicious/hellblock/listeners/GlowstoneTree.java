package com.swiftlicious.hellblock.listeners;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.TreeType;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.StructureGrowEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import com.jeff_media.customblockdata.CustomBlockData;
import com.jeff_media.customblockdata.events.CustomBlockDataEvent;
import com.jeff_media.customblockdata.events.CustomBlockDataEvent.Reason;
import com.jeff_media.customblockdata.events.CustomBlockDataMoveEvent;
import com.jeff_media.customblockdata.events.CustomBlockDataRemoveEvent;
import com.swiftlicious.hellblock.HellblockPlugin;

import lombok.Getter;

public class GlowstoneTree implements Listener {

	private final HellblockPlugin instance;

	private static final Set<TreeType> TREE_TYPES = new HashSet<>(Arrays.asList(TreeType.TREE, TreeType.BIRCH, TreeType.ACACIA,
			TreeType.JUNGLE, TreeType.DARK_OAK, TreeType.BIG_TREE, TreeType.CHERRY, TreeType.REDWOOD, TreeType.MANGROVE,
			TreeType.TALL_REDWOOD, TreeType.SWAMP, TreeType.SMALL_JUNGLE, TreeType.TALL_BIRCH, TreeType.TALL_REDWOOD,
			TreeType.MEGA_PINE, TreeType.MEGA_REDWOOD, TreeType.TALL_MANGROVE, TreeType.AZALEA));

	@Getter
	private final NamespacedKey glowTreeKey;

	public GlowstoneTree(HellblockPlugin plugin) {
		instance = plugin;
		this.glowTreeKey = new NamespacedKey(instance, "glowtree");
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
						Block gravel = event.getWorld().getBlockAt(block.getLocation());
						PersistentDataContainer customBlockData = new CustomBlockData(gravel, instance);
						customBlockData.set(getGlowTreeKey(), PersistentDataType.BOOLEAN, true);
						gravel.setType(Material.GRAVEL);
						block.setType(gravel.getType());
					}
				} while (!Tag.LEAVES.isTagged(block.getType()));

				block.setType(Material.GLOWSTONE);
			}
		}
	}

	@EventHandler
	public void onGravelPhysics(CustomBlockDataEvent event) {
		if (!event.getBlock().getWorld().getName().equalsIgnoreCase(instance.getHellblockHandler().getWorldName()))
			return;

		PersistentDataContainer customBlockData = event.getCustomBlockData();
		if (event.getReason() == Reason.ENTITY_CHANGE_BLOCK) {
			if (customBlockData.has(getGlowTreeKey(), PersistentDataType.BOOLEAN)) {
				event.setCancelled(true);
				instance.getAdventureManager().sendConsoleMessage("1 TEST SUCCESS 3");
			}
		} else if (event.getReason() == Reason.BLOCK_PLACE) {
			if (customBlockData.has(getGlowTreeKey(), PersistentDataType.BOOLEAN)) {
				customBlockData.remove(getGlowTreeKey());
				Set<Block> blocks = getConnectedBlocks(event.getCustomBlockData().getBlock());
				for (Block surroundingBlock : blocks) {
					if (surroundingBlock.getType() != Material.GRAVEL)
						continue;

					PersistentDataContainer customSurroundingBlockData = new CustomBlockData(surroundingBlock, instance);
					if (customSurroundingBlockData.has(getGlowTreeKey())) {
						customSurroundingBlockData.remove(getGlowTreeKey());
					}
				}
			}
		} else if (event.getReason() == Reason.EXPLOSION) {
			if (customBlockData.has(getGlowTreeKey(), PersistentDataType.BOOLEAN)) {
				customBlockData.remove(getGlowTreeKey());
			}
		}
	}

	@EventHandler
	public void onGravelBreak(CustomBlockDataRemoveEvent event) {
		if (!event.getBlock().getWorld().getName().equalsIgnoreCase(instance.getHellblockHandler().getWorldName()))
			return;

		Block block = event.getBlock();
		PersistentDataContainer customBlockData = event.getCustomBlockData();
		if (customBlockData.has(getGlowTreeKey(), PersistentDataType.BOOLEAN)) {
			customBlockData.remove(getGlowTreeKey());
			Set<Block> blocks = getConnectedBlocks(block);
			for (Block surroundingBlock : blocks) {
				if (surroundingBlock.getType() != Material.GRAVEL)
					continue;

				PersistentDataContainer customSurroundingBlockData = new CustomBlockData(surroundingBlock, instance);
				if (customSurroundingBlockData.has(getGlowTreeKey(), PersistentDataType.BOOLEAN)) {
					customSurroundingBlockData.remove(getGlowTreeKey());
				}
			}
			instance.getAdventureManager().sendConsoleMessage("2 TEST SUCCESS 4");
		} else {
			Block topBlock = block.getRelative(BlockFace.UP);
			PersistentDataContainer customTopBlockData = new CustomBlockData(topBlock, instance);
			if (customTopBlockData.has(getGlowTreeKey(), PersistentDataType.BOOLEAN)) {
				customTopBlockData.remove(getGlowTreeKey());
				Set<Block> blocks = getConnectedBlocks(topBlock);
				for (Block surroundingBlock : blocks) {
					if (surroundingBlock.getType() != Material.GRAVEL)
						continue;

					PersistentDataContainer customSurroundingBlockData = new CustomBlockData(surroundingBlock,
							instance);
					if (customSurroundingBlockData.has(getGlowTreeKey(), PersistentDataType.BOOLEAN)) {
						customSurroundingBlockData.remove(getGlowTreeKey());
					}
				}
				instance.getAdventureManager().sendConsoleMessage("2 TEST SUCCESS 5");
			}
		}
	}

	@EventHandler
	public void onGravelMove(CustomBlockDataMoveEvent event) {
		if (!event.getBlock().getWorld().getName().equalsIgnoreCase(instance.getHellblockHandler().getWorldName()))
			return;

		Block block = event.getBlock();
		PersistentDataContainer customBlockData = event.getCustomBlockData();
		if (customBlockData.has(getGlowTreeKey(), PersistentDataType.BOOLEAN)) {
			customBlockData.remove(getGlowTreeKey());
			Set<Block> blocks = getConnectedBlocks(block);
			for (Block surroundingBlock : blocks) {
				if (surroundingBlock.getType() != Material.GRAVEL)
					continue;

				PersistentDataContainer customSurroundingBlockData = new CustomBlockData(surroundingBlock, instance);
				if (customSurroundingBlockData.has(getGlowTreeKey(), PersistentDataType.BOOLEAN)) {
					customSurroundingBlockData.remove(getGlowTreeKey());
				}
			}
		} else {
			Block topBlock = block.getRelative(BlockFace.UP);
			PersistentDataContainer customTopBlockData = new CustomBlockData(topBlock, instance);
			if (customTopBlockData.has(getGlowTreeKey(), PersistentDataType.BOOLEAN)) {
				customTopBlockData.remove(getGlowTreeKey());
				Set<Block> blocks = getConnectedBlocks(topBlock);
				for (Block surroundingBlock : blocks) {
					if (surroundingBlock.getType() != Material.GRAVEL)
						continue;

					PersistentDataContainer customSurroundingBlockData = new CustomBlockData(surroundingBlock,
							instance);
					if (customSurroundingBlockData.has(getGlowTreeKey(), PersistentDataType.BOOLEAN)) {
						customSurroundingBlockData.remove(getGlowTreeKey());
					}
				}
			}
		}
	}

	// These are all the sides of the block
	private static final BlockFace[] faces = { BlockFace.DOWN, BlockFace.UP, BlockFace.NORTH, BlockFace.EAST,
			BlockFace.SOUTH, BlockFace.WEST };

	private void getConnectedBlocks(Block block, Set<Block> results, List<Block> todo) {
		// Here I collect all blocks that are directly connected to variable 'block'.
		// (Shouldn't be more than 6, because a block has 6 sides)
		Set<Block> result = results;

		// Loop through all block faces (All 6 sides around the block)
		for (BlockFace face : faces) {
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