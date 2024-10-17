package com.swiftlicious.hellblock.gui.page.property;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.config.HBLocale;
import com.swiftlicious.hellblock.gui.SectionPage;
import com.swiftlicious.hellblock.gui.icon.BackGroundItem;
import com.swiftlicious.hellblock.utils.wrappers.ShadedAdventureComponentWrapper;

import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.gui.PagedGui;
import xyz.xenondevs.invui.item.Item;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.impl.AbstractItem;
import xyz.xenondevs.invui.item.impl.SimpleItem;
import xyz.xenondevs.invui.window.AnvilWindow;

public class PriceEditor {

	private final Player player;
	private final SectionPage parentPage;
	private final String[] price;
	private int index;
	private final ConfigurationSection section;

	public PriceEditor(Player player, SectionPage parentPage) {
		this.player = player;
		this.parentPage = parentPage;
		this.section = parentPage.getSection();
		this.index = 0;
		this.price = new String[] { section.getString("price.base", "0"), section.getString("price.bonus", "0") };
		reOpen();
	}

	public void reOpen() {
		Item border = new SimpleItem(new ItemBuilder(Material.AIR));
		var confirm = new ConfirmIcon();
		Gui upperGui = Gui.normal().setStructure("a # b")
				.addIngredient('a', new ItemBuilder(Material.GOLD_INGOT).setDisplayName(price[index]))
				.addIngredient('#', border).addIngredient('b', confirm).build();

		var gui = PagedGui.items()
				.setStructure("a b x x x x x x x", "x x x x x x x x x", "x x x x x x x x x", "# # # # c # # # #")
				.addIngredient('x', new ItemStack(Material.AIR)).addIngredient('c', parentPage.getBackItem())
				.addIngredient('#', new BackGroundItem()).addIngredient('a', new BaseItem())
				.addIngredient('b', new BonusItem()).build();

		var window = AnvilWindow
				.split().setViewer(player).setTitle(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
						.getAdventureManager().getComponentFromMiniMessage(HBLocale.GUI_PRICE_TITLE)))
				.addRenameHandler(s -> {
					if (s == null || s.equals("")) {
						price[index] = "0";
						return;
					}
					price[index] = s;
					confirm.notifyWindows();
				}).setUpperGui(upperGui).setLowerGui(gui).build();

		window.open();
	}

	public class BaseItem extends AbstractItem {

		@Override
		public ItemProvider getItemProvider() {
			return new ItemBuilder(Material.GOLD_BLOCK)
					.setDisplayName(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
							.getAdventureManager().getComponentFromMiniMessage(HBLocale.GUI_PRICE_BASE)));
		}

		@Override
		public void handleClick(@NotNull ClickType clickType, @NotNull Player player,
				@NotNull InventoryClickEvent event) {
			index = 0;
			reOpen();
		}
	}

	public class BonusItem extends AbstractItem {

		@Override
		public ItemProvider getItemProvider() {
			return new ItemBuilder(Material.GOLD_NUGGET)
					.setDisplayName(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
							.getAdventureManager().getComponentFromMiniMessage(HBLocale.GUI_PRICE_BONUS)));
		}

		@Override
		public void handleClick(@NotNull ClickType clickType, @NotNull Player player,
				@NotNull InventoryClickEvent event) {
			index = 1;
			reOpen();
		}
	}

	public class ConfirmIcon extends AbstractItem {

		@Override
		public ItemProvider getItemProvider() {
			if (price[0].equals("0") && price[1].equals("0")) {
				return new ItemBuilder(Material.STRUCTURE_VOID)
						.setDisplayName(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
								.getAdventureManager().getComponentFromMiniMessage(HBLocale.GUI_DELETE_PROPERTY)));
			} else {
				try {
					Double.parseDouble(price[0]);
					Double.parseDouble(price[1]);
					return new ItemBuilder(Material.GOLD_INGOT)
							.setDisplayName(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
									.getAdventureManager().getComponentFromMiniMessage(HBLocale.GUI_NEW_VALUE)))
							.addLoreLines(new ShadedAdventureComponentWrapper(
									HellblockPlugin.getInstance().getAdventureManager()
											.getComponentFromMiniMessage(HBLocale.GUI_ITEM_PRICE_BASE + price[0])))
							.addLoreLines(new ShadedAdventureComponentWrapper(
									HellblockPlugin.getInstance().getAdventureManager()
											.getComponentFromMiniMessage(HBLocale.GUI_ITEM_PRICE_BONUS + price[1])))
							.addLoreLines(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
									.getAdventureManager().getComponentFromMiniMessage(HBLocale.GUI_CLICK_CONFIRM)));
				} catch (NumberFormatException e) {
					return new ItemBuilder(Material.BARRIER)
							.setDisplayName(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
									.getAdventureManager().getComponentFromMiniMessage(HBLocale.GUI_INVALID_NUMBER)));
				}
			}
		}

		@Override
		public void handleClick(@NotNull ClickType clickType, @NotNull Player player,
				@NotNull InventoryClickEvent event) {
			if (price[0].equals("0") && price[1].equals("0")) {
				section.set("price", null);
			} else {
				try {
					double base = Double.parseDouble(price[0]);
					double bonus = Double.parseDouble(price[1]);
					if (base != 0) {
						section.set("price.base", base);
					}
					if (bonus != 0) {
						section.set("price.bonus", bonus);
					}
				} catch (NumberFormatException e) {
					return;
				}
			}
			parentPage.reOpen();
			parentPage.save();
		}
	}
}
