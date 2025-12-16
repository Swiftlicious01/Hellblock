package com.swiftlicious.hellblock.gui.leaderboard;

import java.util.UUID;

import org.bukkit.inventory.ItemStack;

import com.swiftlicious.hellblock.gui.BaseGUIElement;

public class LeaderboardGUIElement extends BaseGUIElement {

	protected UUID uuid;

	public LeaderboardGUIElement(char symbol, ItemStack itemStack, UUID uuid) {
		super(symbol, itemStack);
		this.uuid = uuid;
	}

	public LeaderboardGUIElement(char symbol, ItemStack itemStack) {
		this(symbol, itemStack, null);
	}

	// Getter method to retrieve uuid from leaderboard owner associated with this
	// element
	public UUID getUUID() {
		return uuid;
	}
}