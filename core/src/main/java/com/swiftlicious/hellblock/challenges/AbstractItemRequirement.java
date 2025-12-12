package com.swiftlicious.hellblock.challenges;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.swiftlicious.hellblock.challenges.requirement.BarterRequirement;
import com.swiftlicious.hellblock.challenges.requirement.CraftRequirement;
import com.swiftlicious.hellblock.challenges.requirement.FishRequirement;
import com.swiftlicious.hellblock.context.Context;

import dev.dejvokep.boostedyaml.block.implementation.Section;

/**
 * Abstract base class for all {@link ChallengeRequirement} types that depend on
 * specific items.
 * <p>
 * Subclasses such as {@link CraftRequirement}, {@link FishRequirement}, or
 * {@link BarterRequirement} define the concrete logic to match player actions
 * against an expected item definition.
 * </p>
 * <p>
 * This class provides automatic validation, shared item resolution via
 * {@link ItemResolver}, and a unified matching API.
 * </p>
 */
public abstract class AbstractItemRequirement implements ChallengeRequirement {

	/** Raw YAML configuration for this requirement. */
	protected final Section data;

	/** Resolves item definitions from config into real {@link ItemStack}s. */
	protected final ItemResolver resolver;

	/**
	 * Constructs a new AbstractItemRequirement.
	 *
	 * @param data       The YAML section defining the item requirement (material,
	 *                   id, etc.).
	 * @param resolver   The resolver used to create {@link ItemStack}s from config
	 *                   data.
	 * @param actionName The name of the challenge action (for error reporting).
	 * @throws IllegalArgumentException If neither a valid item reference nor inline
	 *                                  data is found.
	 */
	public AbstractItemRequirement(@NotNull Section data, ItemResolver resolver, @NotNull String actionName) {
		this.data = data;
		this.resolver = resolver;

		// Only enforce item/id check for non-barter actions
		if (!actionName.equalsIgnoreCase("BARTER") && !data.contains("item") && !data.contains("id")
				&& !isInlineItem(data)) {
			throw new IllegalArgumentException(actionName + " requires either 'item' (vanilla) or 'id' (custom)");
		}
	}

	/**
	 * Determines if this requirement is defined as an inline item (e.g. with
	 * <code>material:</code>, <code>components:</code>, or <code>display:</code>
	 * tags) instead of a referenced ID or pre-registered item key.
	 *
	 * @param data The configuration section to check.
	 * @return True if inline item data exists.
	 */
	private boolean isInlineItem(@NotNull Section data) {
		return data.contains("material") || data.contains("components") || data.contains("display");
	}

	/**
	 * Resolves the expected {@link ItemStack} this requirement is testing against.
	 * This uses the internal {@link ItemResolver}, optionally applying context
	 * (e.g. player placeholders or conditions).
	 *
	 * @param ctx Optional player context for dynamic resolution.
	 * @return The resolved expected {@link ItemStack}.
	 */
	@NotNull
	protected ItemStack resolveExpected(@Nullable Context<Player> ctx) {
		return resolver.resolveItemStack(data, ctx);
	}

	/**
	 * Default match check without context — subclasses may override or rely on
	 * {@link #matchesWithContext(Object, Context)} for more advanced logic.
	 *
	 * @param context The event or object being compared (e.g. caught fish, crafted
	 *                item).
	 * @return True if the context satisfies the requirement.
	 */
	@Override
	public boolean matches(@NotNull Object context) {
		return matchesWithContext(context, Context.playerEmpty());
	}

	/**
	 * Subclasses must implement this to define how the requirement is fulfilled.
	 * Called whenever an event (like fishing, crafting, or bartering) occurs.
	 *
	 * @param context The event object or data.
	 * @param ctx     Optional player context, used for placeholder or condition
	 *                resolution.
	 * @return True if the requirement is satisfied by the given context.
	 */
	public abstract boolean matchesWithContext(@NotNull Object context, @Nullable Context<Player> ctx);
}