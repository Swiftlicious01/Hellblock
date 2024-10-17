package com.swiftlicious.hellblock.gui.icon.property.item;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.config.HBLocale;
import com.swiftlicious.hellblock.gui.SectionPage;
import com.swiftlicious.hellblock.utils.wrappers.ShadedAdventureComponentWrapper;

import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.impl.AbstractItem;

public class TagItem extends AbstractItem {

	private final SectionPage itemPage;

	public TagItem(SectionPage itemPage) {
		this.itemPage = itemPage;
	}

	@Override
	public ItemProvider getItemProvider() {
		ItemBuilder itemBuilder = new ItemBuilder(Material.TOTEM_OF_UNDYING)
				.setDisplayName(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
						.getComponentFromMiniMessage(HBLocale.GUI_ITEM_TAG)));
		itemBuilder
				.addLoreLines(new ShadedAdventureComponentWrapper(
						HellblockPlugin.getInstance().getAdventureManager().getComponentFromMiniMessage(
								HBLocale.GUI_CURRENT_VALUE + itemPage.getSection().getBoolean("tag", true))))
				.addLoreLines("").addLoreLines(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
						.getAdventureManager().getComponentFromMiniMessage(HBLocale.GUI_CLICK_TO_TOGGLE)));
		return itemBuilder;
	}

	@Override
	public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
		itemPage.getSection().set("tag", !itemPage.getSection().getBoolean("tag", true));
		itemPage.save();
		itemPage.reOpen();
	}
}
