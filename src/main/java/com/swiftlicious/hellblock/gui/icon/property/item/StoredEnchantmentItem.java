package com.swiftlicious.hellblock.gui.icon.property.item;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.jetbrains.annotations.NotNull;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.config.HBLocale;
import com.swiftlicious.hellblock.gui.SectionPage;
import com.swiftlicious.hellblock.gui.page.property.EnchantmentEditor;
import com.swiftlicious.hellblock.utils.wrappers.ShadedAdventureComponentWrapper;

import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.impl.AbstractItem;

import java.util.Map;

public class StoredEnchantmentItem extends AbstractItem {

	private final SectionPage itemPage;

	public StoredEnchantmentItem(SectionPage itemPage) {
		this.itemPage = itemPage;
	}

	@Override
	public ItemProvider getItemProvider() {
		ItemBuilder itemBuilder = new ItemBuilder(Material.ENCHANTED_BOOK)
				.setDisplayName(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
						.getComponentFromMiniMessage(HBLocale.GUI_ITEM_STORED_ENCHANTMENT)))
				.addEnchantment(Enchantment.FLAME, 1, true).addItemFlags(ItemFlag.HIDE_ENCHANTS);
		if (itemPage.getSection().contains("stored-enchantments")) {
			itemBuilder.addLoreLines(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
					.getAdventureManager().getComponentFromMiniMessage(HBLocale.GUI_CURRENT_VALUE)));
			for (Map.Entry<String, Object> entry : itemPage.getSection().getSection("stored-enchantments")
					.getStringRouteMappedValues(false).entrySet()) {
				itemBuilder.addLoreLines(new ShadedAdventureComponentWrapper(
						HellblockPlugin.getInstance().getAdventureManager().getComponentFromMiniMessage(
								" <gray>- <white>" + entry.getKey() + ":" + entry.getValue())));
			}
			itemBuilder.addLoreLines("")
					.addLoreLines(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
							.getAdventureManager().getComponentFromMiniMessage(HBLocale.GUI_LEFT_CLICK_EDIT)))
					.addLoreLines(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
							.getAdventureManager().getComponentFromMiniMessage(HBLocale.GUI_RIGHT_CLICK_RESET)));
		} else {
			itemBuilder.addLoreLines(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
					.getAdventureManager().getComponentFromMiniMessage(HBLocale.GUI_LEFT_CLICK_EDIT)));
		}
		return itemBuilder;
	}

	@Override
	public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
		if (clickType.isLeftClick()) {
			new EnchantmentEditor(player, itemPage, true);
		} else if (clickType.isRightClick()) {
			itemPage.getSection().set("stored-enchantments", null);
			itemPage.save();
			itemPage.reOpen();
		}
	}
}
