package com.swiftlicious.hellblock.gui.icon;

import org.bukkit.Material;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.config.locale.MessageConstants;
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
		builder.setDisplayName(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
				.getComponentFromMiniMessage(HellblockPlugin.getInstance().getTranslationManager()
						.miniMessageTranslation(MessageConstants.GUI_PREVIOUS_PAGE.build().key()))))
				.addLoreLines(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
						.getComponentFromMiniMessage(gui.hasPreviousPage()
								? HellblockPlugin.getInstance().getTranslationManager()
										.miniMessageTranslation(MessageConstants.GUI_GOTO_PREVIOUS_PAGE.build().key()
												.replace("{0}", String.valueOf(gui.getCurrentPage()))
												.replace("{1}", String.valueOf(gui.getPageAmount())))
								: HellblockPlugin.getInstance().getTranslationManager().miniMessageTranslation(
										MessageConstants.GUI_CANNOT_GOTO_PREVIOUS_PAGE.build().key()))));
		return builder;
	}
}