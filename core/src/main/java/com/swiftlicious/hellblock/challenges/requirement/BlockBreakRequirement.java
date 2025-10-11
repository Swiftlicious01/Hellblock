package com.swiftlicious.hellblock.challenges.requirement;

import java.util.Locale;

import org.bukkit.Material;
import org.bukkit.block.Block;

import com.swiftlicious.hellblock.challenges.ChallengeRequirement;

import dev.dejvokep.boostedyaml.block.implementation.Section;

public class BlockBreakRequirement implements ChallengeRequirement {
	private final Material requiredBlock;

	public BlockBreakRequirement(Section data) {
		String blockName = data.getString("block");
		if (blockName == null)
			throw new IllegalArgumentException("BREAK requires 'block' in data");

		Material m = Material.matchMaterial(blockName.toUpperCase(Locale.ROOT));
		if (m == null)
			throw new IllegalArgumentException("Invalid block material: " + blockName);

		this.requiredBlock = m;
	}

	@Override
	public boolean matches(Object context) {
		return context instanceof Block block && block.getType() == requiredBlock;
	}
}