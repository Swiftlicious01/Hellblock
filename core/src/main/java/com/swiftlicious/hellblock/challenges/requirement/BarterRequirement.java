package com.swiftlicious.hellblock.challenges.requirement;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.challenges.AbstractItemRequirement;
import com.swiftlicious.hellblock.challenges.ItemResolver;
import com.swiftlicious.hellblock.context.Context;

import dev.dejvokep.boostedyaml.block.implementation.Section;

public class BarterRequirement extends AbstractItemRequirement {

	public BarterRequirement(Section data, ItemResolver resolver) {
		super(data, resolver, "BARTER");
	}

	@Override
	public boolean matchesWithContext(Object context, @Nullable Context<Player> ctx) {
		if (!(context instanceof ItemStack bartered))
			return false;

		ItemStack expected = null;

		// --- Case 1: Friendly numeric key (preferred for bartering) ---
		if (isNumericBarter(data)) {
			String numberKey = resolveNumericKey(data);
			Section itemDef = HellblockPlugin.getInstance().getConfigManager().getRegistry()
					.getSection("piglin-bartering.items." + numberKey);

			if (itemDef != null) {
				expected = resolver.resolveItemStack(itemDef, ctx);
			}
		}

		// --- Case 2: Vanilla or inline fallback ---
		if (expected == null) {
			expected = resolveExpected(ctx);
		}

		return expected != null && expected.isSimilar(bartered);
	}

	private boolean isNumericBarter(Section data) {
		return data.isInt("") || data.contains("barter-item") || data.contains("number");
	}

	private String resolveNumericKey(Section data) {
		if (data.isInt("")) {
			return String.valueOf(data.getInt(""));
		} else if (data.contains("barter-item")) {
			return data.getString("barter-item").trim();
		} else {
			return data.getString("number").trim();
		}
	}
}