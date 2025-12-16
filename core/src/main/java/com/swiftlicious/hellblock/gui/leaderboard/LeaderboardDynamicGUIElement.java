package com.swiftlicious.hellblock.gui.leaderboard;

import java.util.UUID;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public class LeaderboardDynamicGUIElement extends LeaderboardGUIElement {

	@Nullable
	private String skullTexture;

	public LeaderboardDynamicGUIElement(char symbol, ItemStack itemStack, UUID uuid) {
		super(symbol, itemStack, uuid);
	}

	public LeaderboardDynamicGUIElement(char symbol, ItemStack itemStack) {
		super(symbol, itemStack);
	}

	public void setItemStack(ItemStack itemStack) {
		super.itemStack = itemStack;
	}

	public void setUUID(UUID uuid) {
		super.uuid = uuid;
	}

	public void setSkullTexture(@Nullable String texture) {
		this.skullTexture = texture;
	}

	@Nullable
	public String getSkullTexture() {
		return this.skullTexture;
	}
}