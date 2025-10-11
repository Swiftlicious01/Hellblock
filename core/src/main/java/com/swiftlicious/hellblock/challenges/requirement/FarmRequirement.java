package com.swiftlicious.hellblock.challenges.requirement;

import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.type.Farmland;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.challenges.ChallengeRequirement;
import com.swiftlicious.hellblock.listeners.FarmingHandler;

import dev.dejvokep.boostedyaml.block.implementation.Section;

public class FarmRequirement implements ChallengeRequirement {
	public static final Set<Material> SUPPORTED_CROPS = EnumSet.of(Material.WHEAT, Material.POTATOES,
			Material.BEETROOTS, Material.CARROTS, Material.MELON, Material.PUMPKIN, Material.SUGAR_CANE,
			Material.NETHER_WART, Material.RED_MUSHROOM, Material.BROWN_MUSHROOM, Material.CACTUS,
			Material.SWEET_BERRY_BUSH, Material.BAMBOO);

	private final Material cropType;

	public FarmRequirement(Section data) {
		String crop = data.getString("crop");
		if (crop == null) {
			throw new IllegalArgumentException("FARM requires 'crop' in data");
		}

		Material m = Material.matchMaterial(crop.toUpperCase(Locale.ROOT));
		if (m == null || !SUPPORTED_CROPS.contains(m)) {
			throw new IllegalArgumentException("Invalid or unsupported crop material: " + crop);
		}

		this.cropType = m;
	}

	@Override
	public boolean matches(Object context) {
		if (!(context instanceof Block block)) {
			return false;
		}
		if (block.getType() != cropType) {
			return false;
		}

		// Nether wart: fully grown check
		if (cropType == Material.NETHER_WART) {
			if (block.getBlockData() instanceof Ageable ageable) {
				return ageable.getAge() == ageable.getMaximumAge();
			}
			return false;
		}

		// Sugar cane: must be >= 3 tall, breakable at base or 2nd block
		if (cropType == Material.SUGAR_CANE) {
			Block base = getBase(block, Material.SUGAR_CANE);

			// count height
			int height = 1;
			Block current = base;
			while (current.getRelative(BlockFace.UP).getType() == Material.SUGAR_CANE) {
				current = current.getRelative(BlockFace.UP);
				height++;
			}

			if (height < 3) {
				return false;
			}

			// must be breaking base or 2nd cane block
			if (!(block.equals(base) || block.equals(base.getRelative(BlockFace.UP)))) {
				return false;
			}

			// lava check (use FarmingHandler for consistency)
			FarmingHandler farming = HellblockPlugin.getInstance().getFarmingManager();
			return farming.checkForLavaAroundSugarCane(base);
		}

		// Melons & Pumpkins: must have fully-grown stem + hydrated farmland
		if (cropType == Material.MELON || cropType == Material.PUMPKIN) {
			FarmingHandler farming = HellblockPlugin.getInstance().getFarmingManager();

			for (BlockFace face : new BlockFace[] { BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST,
					BlockFace.WEST }) {
				Block adjacent = block.getRelative(face);

				// Check for stem
				if (adjacent.getType() == Material.MELON_STEM || adjacent.getType() == Material.PUMPKIN_STEM) {
					if (adjacent.getBlockData() instanceof Ageable stem) {
						// Stem must be fully grown
						if (stem.getAge() != stem.getMaximumAge()) {
							continue;
						}

						// Stem must be above hydrated farmland
						Block belowStem = adjacent.getRelative(BlockFace.DOWN);
						if (belowStem.getBlockData() instanceof Farmland) {
							if (farming.checkForLavaAroundFarm(belowStem)) {
								return true; // fully-grown + hydrated stem found
							}
						}
					}
				}
			}
			return false; // no valid stems
		}

		// Sweet Berries: fully grown bush
		if (cropType == Material.SWEET_BERRY_BUSH) {
			if (block.getBlockData() instanceof Ageable ageable) {
				return ageable.getAge() == ageable.getMaximumAge() && HellblockPlugin.getInstance().getFarmingManager()
						.checkForLavaAroundBerryBush(block.getRelative(BlockFace.DOWN));
			}
			return false;
		}

		// Bamboo: must be >= 3 tall, breakable at base or 2nd block
		if (cropType == Material.BAMBOO) {
			Block base = getBase(block, Material.BAMBOO);

			// count height
			int height = 1;
			Block current = base;
			while (current.getRelative(BlockFace.UP).getType() == Material.BAMBOO) {
				current = current.getRelative(BlockFace.UP);
				height++;
			}

			if (height < 3) {
				return false;
			}

			// must be breaking base or 2nd block
			if (!(block.equals(base) || block.equals(base.getRelative(BlockFace.UP)))) {
				return false;
			}

			FarmingHandler farming = HellblockPlugin.getInstance().getFarmingManager();
			return farming.checkForLavaAroundBamboo(base);
		}

		// Cactus: must be >= 3 tall, breakable at base or 2nd block
		if (cropType == Material.CACTUS) {
			Block base = getBase(block, Material.CACTUS);

			// count height
			int height = 1;
			Block current = base;
			while (current.getRelative(BlockFace.UP).getType() == Material.CACTUS) {
				current = current.getRelative(BlockFace.UP);
				height++;
			}

			if (height < 3) {
				return false;
			}

			// must be breaking base or 2nd block
			if (!(block.equals(base) || block.equals(base.getRelative(BlockFace.UP)))) {
				return false;
			}

			FarmingHandler farming = HellblockPlugin.getInstance().getFarmingManager();
			return farming.checkForLavaAroundCactus(base);
		}

		// Mushrooms: only count if they were spread via lava system
		if (cropType == Material.RED_MUSHROOM || cropType == Material.BROWN_MUSHROOM) {
			FarmingHandler farming = HellblockPlugin.getInstance().getFarmingManager();
			return farming.isLavaGrownMushroom(block);
		}

		// Normal crops: must be fully grown
		if (block.getBlockData() instanceof Ageable ageable) {
			if (ageable.getAge() != ageable.getMaximumAge()) {
				return false;
			}
		} else {
			return false;
		}

		// Check farmland hydration
		Block below = block.getRelative(BlockFace.DOWN);
		if (below.getBlockData() instanceof Farmland) {
			return HellblockPlugin.getInstance().getFarmingManager().checkForLavaAroundFarm(below);
		}

		return false;
	}

	/**
	 * Traverses downwards to find the base block.
	 */
	private Block getBase(Block start, Material type) {
		Block current = start;
		while (current.getRelative(BlockFace.DOWN).getType() == type) {
			current = current.getRelative(BlockFace.DOWN);
		}
		return current;
	}
}