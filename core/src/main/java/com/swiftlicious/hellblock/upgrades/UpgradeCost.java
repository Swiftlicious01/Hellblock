package com.swiftlicious.hellblock.upgrades;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a single cost requirement for an upgrade.
 * <p>
 * Each cost has a type (e.g., MONEY, EXP, ITEM), a numeric amount, and
 * optionally an item name if the cost type is {@code ITEM}.
 */
public class UpgradeCost {

	private final UpgradeCostType type; // MONEY, EXP, ITEM, TOKENS, etc.
	private final double amount;
	private final String item; // only used if type == ITEM

	/**
	 * Constructs a new {@code UpgradeCost}.
	 *
	 * @param type   the type of cost (e.g., MONEY, EXP, ITEM)
	 * @param amount the numeric amount required
	 * @param item   the item name (only applicable for ITEM cost types, may be
	 *               {@code null})
	 */
	public UpgradeCost(@NotNull UpgradeCostType type, double amount, @Nullable String item) {
		this.type = type;
		this.amount = amount;
		this.item = item;
	}

	/**
	 * @return the cost type (e.g., MONEY, ITEM)
	 */
	@NotNull
	public UpgradeCostType getType() {
		return type;
	}

	/**
	 * @return the amount required for this cost
	 */
	public double getAmount() {
		return amount;
	}

	/**
	 * @return the item name if the type is ITEM, otherwise {@code null}
	 */
	public String getItem() {
		return item;
	}

	/**
	 * @return {@code true} if this cost is item-based; {@code false} otherwise
	 */
	public boolean isItemCost() {
		return type == UpgradeCostType.ITEM;
	}
}