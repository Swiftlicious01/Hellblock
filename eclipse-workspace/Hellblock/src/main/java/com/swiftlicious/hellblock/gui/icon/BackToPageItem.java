package com.swiftlicious.hellblock.gui.icon;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.config.HBLocale;
import com.swiftlicious.hellblock.gui.ParentPage;
import com.swiftlicious.hellblock.utils.wrappers.ShadedAdventureComponentWrapper;

import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.impl.AbstractItem;

public class BackToPageItem extends AbstractItem {

	private final ParentPage parentPage;

	public BackToPageItem(ParentPage parentPage) {
		this.parentPage = parentPage;
	}

	@Override
	public ItemProvider getItemProvider() {
		return new ItemBuilder(Material.ORANGE_STAINED_GLASS_PANE)
				.setDisplayName(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
						.getComponentFromMiniMessage(HBLocale.GUI_BACK_TO_PARENT_PAGE)));
	}

	@Override
	public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
		parentPage.reOpen();
	}
}