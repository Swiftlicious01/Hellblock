package com.swiftlicious.hellblock.challenges.requirement;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.bukkit.Material;
import org.bukkit.TreeType;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.challenges.ChallengeRequirement;
import com.swiftlicious.hellblock.handlers.VersionHelper;
import com.swiftlicious.hellblock.listeners.GlowTreeHandler.CustomTreeGrowContext;
import com.swiftlicious.hellblock.world.HellblockWorld;
import com.swiftlicious.hellblock.world.Pos3;

import dev.dejvokep.boostedyaml.block.implementation.Section;

/**
 * Represents a challenge requirement where a player must successfully grow a
 * specific type of tree, fungus, or custom tree (e.g. Glowstone Tree).
 * <p>
 * Supports all vanilla growable trees and fungi, automatically normalizing
 * family variants — for example, <code>JUNGLE</code> matches both
 * {@link TreeType#JUNGLE} and {@link TreeType#SMALL_JUNGLE}.
 * </p>
 *
 * <p>
 * <b>Example configuration (in challenges.yml):</b>
 * </p>
 * 
 * <pre>
 *   GROW_JUNGLE_TREES:
 *     needed-amount: 10
 *     action: GROW
 *     data:
 *       family: JUNGLE
 *     rewards:
 *       item_action:
 *         type: 'give-vanilla-item'
 *         value:
 *           material: JUNGLE_SAPLING
 * </pre>
 * <p>
 * In this example, the player must grow <b>10 jungle trees of any size</b> —
 * both {@link TreeType#JUNGLE} and {@link TreeType#SMALL_JUNGLE} count.
 * </p>
 *
 * <p>
 * <b>Custom example:</b>
 * </p>
 * 
 * <pre>
 *   GROW_GLOWSTONE_TREE:
 *     needed-amount: 1
 *     action: GROW
 *     data:
 *       tree: GLOWSTONE_TREE
 * </pre>
 * <p>
 * Glowstone Trees are recognized by saplings planted on
 * {@link Material#SOUL_SAND}.
 * </p>
 */
public class GrowRequirement implements ChallengeRequirement {

	private final TreeType vanillaTree;
	private final Material vanillaBlockType;
	private final String customId;
	private final String familyName;

	/** Maps canonical family names to all growable tree variants in that family. */
	private static final Map<String, Set<TreeType>> TREE_FAMILIES = new HashMap<>();

	/** List of growable tree types allowed in challenges. */
	private static final Set<TreeType> ALLOWED_TREES = EnumSet.noneOf(TreeType.class);

	static {
		// === Base growable trees (universal) ===
		addAll(ALLOWED_TREES, TreeType.TREE, TreeType.BIG_TREE, // Oak
				TreeType.REDWOOD, TreeType.TALL_REDWOOD, TreeType.MEGA_REDWOOD, // Spruce
				TreeType.BIRCH, TreeType.TALL_BIRCH, TreeType.JUNGLE, TreeType.SMALL_JUNGLE, TreeType.ACACIA,
				TreeType.DARK_OAK, TreeType.RED_MUSHROOM, TreeType.BROWN_MUSHROOM, TreeType.CRIMSON_FUNGUS,
				TreeType.WARPED_FUNGUS, TreeType.AZALEA // 1.17+
		);

		// === 1.19.3+ ===
		if (VersionHelper.isVersionNewerThan1_19_3()) {
			addAll(ALLOWED_TREES, TreeType.valueOf("MANGROVE"));
		}

		// === 1.20.1+ ===
		if (VersionHelper.isVersionNewerThan1_20_1()) {
			addAll(ALLOWED_TREES, TreeType.valueOf("CHERRY"));
		}

		// === 1.21+ ===
		if (VersionHelper.isVersionNewerThan1_21()) {
			addAll(ALLOWED_TREES, TreeType.valueOf("PALE_OAK"), TreeType.valueOf("PALE_OAK_CREAKING"));
		}

		// === Family groupings ===
		TREE_FAMILIES.put("OAK", EnumSet.of(TreeType.TREE, TreeType.BIG_TREE));
		TREE_FAMILIES.put("SPRUCE", EnumSet.of(TreeType.REDWOOD, TreeType.TALL_REDWOOD, TreeType.MEGA_REDWOOD));
		TREE_FAMILIES.put("REDWOOD", TREE_FAMILIES.get("SPRUCE")); // alias
		TREE_FAMILIES.put("BIRCH", EnumSet.of(TreeType.BIRCH, TreeType.TALL_BIRCH));
		TREE_FAMILIES.put("JUNGLE", EnumSet.of(TreeType.JUNGLE, TreeType.SMALL_JUNGLE));
		TREE_FAMILIES.put("ACACIA", EnumSet.of(TreeType.ACACIA));
		TREE_FAMILIES.put("DARK_OAK", EnumSet.of(TreeType.DARK_OAK));
		TREE_FAMILIES.put("AZALEA", EnumSet.of(TreeType.AZALEA));
		TREE_FAMILIES.put("RED_MUSHROOM", EnumSet.of(TreeType.RED_MUSHROOM));
		TREE_FAMILIES.put("BROWN_MUSHROOM", EnumSet.of(TreeType.BROWN_MUSHROOM));
		TREE_FAMILIES.put("CRIMSON_FUNGUS", EnumSet.of(TreeType.CRIMSON_FUNGUS));
		TREE_FAMILIES.put("WARPED_FUNGUS", EnumSet.of(TreeType.WARPED_FUNGUS));
		// === Mushroom family (shared Overworld mushrooms) ===
		TREE_FAMILIES.put("MUSHROOM", EnumSet.of(TreeType.RED_MUSHROOM, TreeType.BROWN_MUSHROOM));
		// === Fungus family (shared Nether fungi) ===
		TREE_FAMILIES.put("FUNGUS", EnumSet.of(TreeType.CRIMSON_FUNGUS, TreeType.WARPED_FUNGUS));

		if (VersionHelper.isVersionNewerThan1_19_3()) {
			TREE_FAMILIES.put("MANGROVE", EnumSet.of(TreeType.valueOf("MANGROVE")));
		}
		if (VersionHelper.isVersionNewerThan1_20_1()) {
			TREE_FAMILIES.put("CHERRY", EnumSet.of(TreeType.valueOf("CHERRY")));
		}
		if (VersionHelper.isVersionNewerThan1_21()) {
			TREE_FAMILIES.put("PALE_OAK",
					EnumSet.of(TreeType.valueOf("PALE_OAK"), TreeType.valueOf("PALE_OAK_CREAKING")));
		}
	}

	public GrowRequirement(@NotNull Section data) {
		String treeStr = data.getString("tree");
		String familyStr = data.getString("family");

		if (treeStr == null && familyStr == null)
			throw new IllegalArgumentException("GROW requires 'tree' or 'family' in data");

		String key = (treeStr != null ? treeStr : familyStr).toUpperCase(Locale.ROOT);

		// Normalize common aliases (SPRUCE <-> REDWOOD)
		if ("SPRUCE".equals(key))
			key = "REDWOOD";
		if ("REDWOOD".equals(key))
			key = "SPRUCE";

		this.familyName = TREE_FAMILIES.containsKey(key) ? key : null;

		TreeType t = null;
		try {
			t = TreeType.valueOf(key);
		} catch (NoSuchFieldError | IllegalArgumentException ignored) {
		}

		Material m = (t == null) ? Material.matchMaterial(key) : null;

		// Ensure tree type is valid and can actually be grown by a player
		if (t != null && !ALLOWED_TREES.contains(t)) {
			throw new IllegalArgumentException("Tree type " + t + " cannot be grown by players in this version.");
		}

		this.vanillaTree = t;
		this.vanillaBlockType = m;
		this.customId = (t == null && m == null && familyName == null) ? key : null;
	}

	/**
	 * Checks whether the provided context represents a successful growth event that
	 * matches this requirement.
	 *
	 * @param context A {@link Block} for block-based growth, or a
	 *                {@link CustomTreeGrowContext} for saplings/trees.
	 * @return {@code true} if the grown entity matches the expected tree, family,
	 *         or block type.
	 */
	@Override
	public boolean matches(@NotNull Object context) {
		// Case 1: block-based growth (vanilla material)
		if (context instanceof Block block && vanillaBlockType != null) {
			return block.getType() == vanillaBlockType;
		}

		// Case 2: Tree growth context
		if (context instanceof CustomTreeGrowContext tg) {
			TreeType grown = tg.treeType();

			// Family-based check
			if (familyName != null && TREE_FAMILIES.containsKey(familyName)) {
				return TREE_FAMILIES.get(familyName).contains(grown);
			}

			// Exact or related variant match
			if (vanillaTree != null && isExactOrFamilyMatch(vanillaTree, grown)) {
				return true;
			}

			// Custom-defined trees (e.g., Glowstone Tree)
			if ("GLOWSTONE_TREE".equals(customId)) {
				Pos3 saplingPos = tg.position();
				Pos3 belowPos = saplingPos.down();

				Optional<HellblockWorld<?>> worldOpt = HellblockPlugin.getInstance().getWorldManager()
						.getWorld(tg.worldName());
				if (worldOpt.isEmpty() || worldOpt.get().bukkitWorld() == null)
					return false;

				HellblockWorld<?> world = worldOpt.get();

				Block below = belowPos.toLocation(world.bukkitWorld()).getBlock();
				return below.getType() == Material.SOUL_SAND;
			}
		}

		return false;
	}

	/**
	 * Determines whether the grown tree matches the configured tree exactly or
	 * belongs to the same family (e.g., JUNGLE vs SMALL_JUNGLE).
	 */
	private boolean isExactOrFamilyMatch(@NotNull TreeType configured, @NotNull TreeType grown) {
		if (configured == grown)
			return true;

		// Family-based match (used for multi-variant trees like JUNGLE or OAK)
		return TREE_FAMILIES.values().stream()
				.anyMatch(family -> family.contains(configured) && family.contains(grown));
	}

	/**
	 * Checks if the provided tree type is a valid player-growable variant.
	 */
	public static boolean isGrowableTree(@NotNull TreeType type) {
		return ALLOWED_TREES.contains(type);
	}

	/**
	 * Resolves the canonical family name for a given tree type (e.g., TREE → OAK).
	 * Returns {@code null} if not part of any family.
	 */
	@Nullable
	public static String getFamilyName(@NotNull TreeType type) {
		return TREE_FAMILIES.entrySet().stream().filter(entry -> entry.getValue().contains(type)).map(Map.Entry::getKey)
				.findFirst().orElse(null);
	}

	/**
	 * Adds one or more {@link TreeType}s to a target {@link Set}, ignoring
	 * duplicates.
	 * <p>
	 * Used during static initialization to populate {@link #ALLOWED_TREES}.
	 * </p>
	 *
	 * @param set   The target set to add into.
	 * @param types The tree types to include.
	 */
	private static void addAll(@NotNull Set<TreeType> set, @NotNull TreeType... types) {
		Collections.addAll(set, types);
	}
}