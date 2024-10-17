package com.swiftlicious.hellblock.gui.icon;

import org.bukkit.Material;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.config.HBLocale;
import com.swiftlicious.hellblock.gui.Icon;
import com.swiftlicious.hellblock.utils.wrappers.ShadedAdventureComponentWrapper;

import xyz.xenondevs.invui.gui.PagedGui;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.impl.controlitem.PageItem;

public class PreviousPageItem extends PageItem implements Icon {

	public PreviousPageItem() {
		super(false);
	}

	@Override
	public ItemProvider getItemProvider(PagedGui<?> gui) {
		ItemBuilder builder = new ItemBuilder(Material.RED_STAINED_GLASS_PANE);
		builder.setDisplayName(
				new ShadedAdventureComponentWrapper(
						HellblockPlugin
								.getInstance().getAdventureManager().getComponentFromMiniMessage(
										HBLocale.GUI_PREVIOUS_PAGE)))
				.addLoreLines(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
						.getComponentFromMiniMessage(gui.hasPreviousPage()
								? HBLocale.GUI_GOTO_PREVIOUS_PAGE.replace("{0}", String.valueOf(gui.getCurrentPage()))
										.replace("{1}", String.valueOf(gui.getPageAmount()))
								: HBLocale.GUI_CANNOT_GOTO_PREVIOUS_PAGE)));
		return builder;
	}
}