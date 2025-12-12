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
 * Represents a challenge requirement where a player must obtain a specific item
 * from Piglin bartering.
 * <p>
 * This requirement supports multiple item definition sources:
 * </p>
 * <ul>
 * <li><b>Numeric barter key</b> — resolves from the registry section
 * {@code piglin-bartering.items.#}</li>
 * <li><b>Vanilla or custom item</b> — defined using {@code item}, {@code id},
 * or inline definitions ({@code material}, {@code components}, etc.)</li>
 * </ul>
 *
 * <p>
 * <b>Example configuration:</b>
 * </p>
 * 
 * <pre>
 *   BARTER_ENDER_PEARLS:
 *     needed-amount: 16
 *     action: BARTER
 *     data:
 *       barter-item: 4
 *     rewards:
 *       item_action:
 *         type: 'give-vanilla-item'
 *         value:
 *           material: GOLD_INGOT
 * </pre>
 *
 * <p>
 * In this example, Piglin barter item #4 from the registry is required to
 * complete the challenge.
 * </p>
 */
public class BarterRequirement extends AbstractItemRequirement {

	public BarterRequirement(@NotNull Section data, @NotNull ItemResolver resolver) {
		super(normalizeBarterData(data), resolver, "BARTER");
	}

	/**
	 * Checks whether the item obtained from Piglin bartering matches the expected
	 * configuration. Supports registry lookups (numeric or named barter items) and
	 * vanilla definitions.
	 *
	 * @param context The bartered item (expected to be an {@link ItemStack}).
	 * @param ctx     Optional player context for placeholder resolution.
	 * @return {@code true} if the item matches the expected barter reward.
	 */
	@Override
	public boolean matchesWithContext(@NotNull Object context, @Nullable Context<Player> ctx) {
		if (!(context instanceof ItemStack bartered))
			return false;

		ItemStack expected = null;

		// --- Case 1: Numeric or named registry key ---
		if (isNumericBarter(data)) {
			String numberKey = resolveNumericKey(data);
			Section itemDef = this.resolver.getPlugin().getConfigManager().getRegistry()
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

	/**
	 * Determines whether this barter definition uses a numeric or named key.
	 */
	private boolean isNumericBarter(@NotNull Section data) {
		Object root = data.get("");
		return (root instanceof Number) || data.contains("barter-item") || data.contains("number");
	}

	/**
	 * Resolves the barter key (e.g., "4" or "gold_sword").
	 */
	@NotNull
	private String resolveNumericKey(@NotNull Section data) {
		Object root = data.get("");
		if (root instanceof Number n) {
			return String.valueOf(n.intValue());
		} else if (data.contains("barter-item")) {
			return data.getString("barter-item").trim();
		} else {
			return data.getString("number").trim();
		}
	}

	@NotNull
	private static Section normalizeBarterData(@NotNull Section data) {
		// Handle case where 'data' itself is a scalar (like "data: 1")
		Object rootValue = data.get("");
		if (rootValue instanceof Number || rootValue instanceof String) {
			Section wrapper = data.createSection("wrapped");
			if (rootValue instanceof Number n) {
				wrapper.set("number", n.intValue());
			} else {
				wrapper.set("barter-item", rootValue.toString());
			}
			return wrapper;
		}

		// If no usable item hints exist, insert dummy
		if (!data.contains("item") && !data.contains("id") && !data.contains("barter-item")
				&& !data.contains("number")) {
			data.set("item", "PIGLIN_BARTER_DUMMY");
		}

		return data;
	}
}