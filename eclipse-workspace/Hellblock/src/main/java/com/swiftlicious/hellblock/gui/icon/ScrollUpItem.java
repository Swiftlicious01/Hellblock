package com.swiftlicious.hellblock.gui.icon;

import org.bukkit.Material;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.config.HBLocale;
import com.swiftlicious.hellblock.gui.Icon;
import com.swiftlicious.hellblock.utils.wrappers.ShadedAdventureComponentWrapper;

import xyz.xenondevs.invui.gui.ScrollGui;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.impl.controlitem.ScrollItem;

public class ScrollUpItem extends ScrollItem implements Icon {

	public ScrollUpItem() {
		super(-1);
	}

	@Override
	public ItemProvider getItemProvider(ScrollGui<?> gui) {
		ItemBuilder builder = new ItemBuilder(Material.RED_STAINED_GLASS_PANE);
		builder.setDisplayName(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
				.getComponentFromMiniMessage(HBLocale.GUI_SCROLL_UP)));
		if (!gui.canScroll(-1))
			builder.addLoreLines(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
					.getComponentFromMiniMessage(HBLocale.GUI_CANNOT_SCROLL_UP)));
		return builder;
	}
}