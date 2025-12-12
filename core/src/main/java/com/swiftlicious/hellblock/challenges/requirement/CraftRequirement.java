package com.swiftlicious.hellblock.challenges.requirement;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.swiftlicious.hellblock.challenges.AbstractItemRequirement;
import com.swiftlicious.hellblock.challenges.ItemResolver;
import com.swiftlicious.hellblock.context.Context;

import dev.dejvokep.boostedyaml.block.implementation.Section;

/**
 * Represents a challenge requirement where a player must craft a specific item.
 * <p>
 * This requirement is triggered when a player successfully crafts an item that
 * matches the configured {@link ItemStack}. Matching is determined using
 * {@link ItemStack#isSimilar(ItemStack)}, meaning material, display name, and
 * item meta must align.
 * </p>
 *
 * <p>
 * <b>Example configuration (vanilla item):</b>
 * </p>
 * 
 * <pre>
 *   CRAFT_NETHERITE_SWORD:
 *     needed-amount: 1
 *     action: CRAFT
 *     data:
 *       item: NETHERITE_SWORD
 *     rewards:
 *       item_action:
 *         type: 'give-vanilla-item'
 *         value:
 *           material: DIAMOND
 * </pre>
 *
 * <p>
 * In this example, crafting one <b>Netherite Sword</b> completes the challenge.
 * </p>
 *
 * <p>
 * <b>Example configuration (custom item from registry):</b>
 * </p>
 * 
 * <pre>
 *   CRAFT_NETHERSTAR_CHESTPLATE:
 *     needed-amount: 1
 *     action: CRAFT
 *     data:
 *       id: armor.netherstar.chestplate
 *     rewards:
 *       item_action:
 *         type: 'give-vanilla-item'
 *         value:
 *           material: DIAMOND
 * </pre>
 *
 * <p>
 * In this example, crafting the custom-registered item
 * <code>armor.netherstar.chestplate</code> — as defined in your item registry —
 * completes the challenge. Both vanilla <code>item</code> and custom
 * <code>id</code> keys are supported.
 * </p>
 */
public class CraftRequirement extends AbstractItemRequirement {

	public CraftRequirement(@NotNull Section data, @NotNull ItemResolver resolver) {
		super(data, resolver, "CRAFT");
	}

	/**
	 * Checks whether the crafted {@link ItemStack} matches the expected
	 * configuration.
	 *
	 * @param context The crafted item (expected to be an {@link ItemStack}).
	 * @param ctx     Optional player context for placeholder resolution.
	 * @return {@code true} if the crafted item matches the expected one.
	 */
	@Override
	public boolean matchesWithContext(@NotNull Object context, @Nullable Context<Player> ctx) {
		if (!(context instanceof ItemStack crafted))
			return false;
		ItemStack expected = resolveExpected(ctx);
		return expected != null && expected.isSimilar(crafted);
	}
}