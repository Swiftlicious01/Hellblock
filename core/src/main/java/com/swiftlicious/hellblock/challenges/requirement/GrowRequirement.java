package com.swiftlicious.hellblock.challenges.requirement;

import java.util.Locale;

import org.bukkit.Material;
import org.bukkit.TreeType;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

import com.swiftlicious.hellblock.challenges.ChallengeRequirement;
import com.swiftlicious.hellblock.listeners.GlowTreeHandler.TreeGrowContext;

import dev.dejvokep.boostedyaml.block.implementation.Section;

public class GrowRequirement implements ChallengeRequirement {
	private final TreeType vanillaTree;
	private final Material vanillaBlockType;
	private final String customId;

	public GrowRequirement(Section data) {
		String treeStr = data.getString("tree");
		if (treeStr == null)
			throw new IllegalArgumentException("GROW requires 'tree' in data");

		TreeType t = null;
		try {
			t = TreeType.valueOf(treeStr.toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException ignored) {
		}

		Material m = (t == null) ? Material.matchMaterial(treeStr.toUpperCase(Locale.ROOT)) : null;

		this.vanillaTree = t;
		this.vanillaBlockType = m;
		this.customId = (t == null && m == null) ? treeStr.toUpperCase(Locale.ROOT) : null;
	}

	@Override
	public boolean matches(Object context) {
		if (context instanceof Block block && vanillaBlockType != null) {
			return block.getType() == vanillaBlockType;
		}

		if (context instanceof TreeGrowContext tg) {
			if (vanillaTree != null)
				return tg.treeType() == vanillaTree;

			if ("GLOWSTONE_TREE".equals(customId)) {
				Block below = tg.sapling().getRelative(BlockFace.DOWN);
				return below.getType() == Material.SOUL_SAND;
			}
		}

		return false;
	}
}