package com.swiftlicious.hellblock.gui.visit;

import java.util.UUID;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public class VisitDynamicGUIElement extends VisitGUIElement {

	private @Nullable String skullTexture;
	private long featuredUntil;

	public VisitDynamicGUIElement(char symbol, ItemStack itemStack, UUID uuid) {
		super(symbol, itemStack, uuid);
	}

	public VisitDynamicGUIElement(char symbol, ItemStack itemStack) {
		super(symbol, itemStack);
	}

	public void setItemStack(ItemStack itemStack) {
		super.itemStack = itemStack;
	}

	public void setUUID(UUID uuid) {
		super.uuid = uuid;
	}

	public long getFeaturedUntil() {
		return featuredUntil;
	}

	public void setFeaturedUntil(long until) {
		this.featuredUntil = until;
	}

	public void setSkullTexture(@Nullable String texture) {
		this.skullTexture = texture;
	}

	public @Nullable String getSkullTexture() {
		return this.skullTexture;
	}

}