package com.swiftlicious.hellblock.challenges;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import com.swiftlicious.hellblock.context.Context;

import dev.dejvokep.boostedyaml.block.implementation.Section;

public abstract class AbstractItemRequirement implements ChallengeRequirement {
	protected final Section data;
	protected final ItemResolver resolver;

	public AbstractItemRequirement(Section data, ItemResolver resolver, String actionName) {
		this.data = data;
		this.resolver = resolver;

		if (!data.contains("item") && !data.contains("id") && !isInlineItem(data)) {
			throw new IllegalArgumentException(actionName + " requires either 'item' (vanilla) or 'id' (custom)");
		}
	}

	private boolean isInlineItem(Section data) {
		return data.contains("material") || data.contains("components") || data.contains("display");
	}

	protected ItemStack resolveExpected(@Nullable Context<Player> ctx) {
		return resolver.resolveItemStack(data, ctx);
	}

	@Override
	public boolean matches(Object context) {
		return matchesWithContext(context, Context.empty());
	}

	public abstract boolean matchesWithContext(Object context, @Nullable Context<Player> ctx);
}
