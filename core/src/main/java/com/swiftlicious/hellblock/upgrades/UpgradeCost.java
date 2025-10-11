package com.swiftlicious.hellblock.upgrades;

public class UpgradeCost {

	private final String type; // MONEY, EXP, ITEM, TOKENS, etc.
	private final double amount;
	private final String item; // only used if type == ITEM

	public UpgradeCost(String type, double amount, String item) {
		this.type = type.toUpperCase();
		this.amount = amount;
		this.item = item;
	}

	public String getType() {
		return type;
	}

	public double getAmount() {
		return amount;
	}

	public String getItem() {
		return item;
	}

	public boolean isItemCost() {
		return "ITEM".equalsIgnoreCase(type);
	}
}