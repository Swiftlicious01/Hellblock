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

public class ScrollDownItem extends ScrollItem implements Icon {

	public ScrollDownItem() {
		super(1);
	}

	@Override
	public ItemProvider getItemProvider(ScrollGui<?> gui) {
		ItemBuilder builder = new ItemBuilder(Material.GREEN_STAINED_GLASS_PANE);
		builder.setDisplayName(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
				.getComponentFromMiniMessage(HBLocale.GUI_SCROLL_DOWN)));
		if (!gui.canScroll(1))
			builder.addLoreLines(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
					.getComponentFromMiniMessage(HBLocale.GUI_CANNOT_SCROLL_DOWN)));
		return builder;
	}
}