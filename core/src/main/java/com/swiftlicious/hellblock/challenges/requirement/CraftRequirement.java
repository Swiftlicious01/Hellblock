package com.swiftlicious.hellblock.challenges.requirement;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import com.swiftlicious.hellblock.challenges.AbstractItemRequirement;
import com.swiftlicious.hellblock.challenges.ItemResolver;
import com.swiftlicious.hellblock.context.Context;

import dev.dejvokep.boostedyaml.block.implementation.Section;

public class CraftRequirement extends AbstractItemRequirement {
	
	public CraftRequirement(Section data, ItemResolver resolver) {
		super(data, resolver, "CRAFT");
	}

	@Override
	public boolean matchesWithContext(Object context, @Nullable Context<Player> ctx) {
		if (!(context instanceof ItemStack crafted))
			return false;
		ItemStack expected = resolveExpected(ctx);
		return expected != null && expected.isSimilar(crafted);
	}
}