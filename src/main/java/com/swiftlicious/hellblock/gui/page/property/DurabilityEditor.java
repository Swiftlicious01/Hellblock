package com.swiftlicious.hellblock.gui.page.property;

import org.bukkit.Material;
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

import dev.dejvokep.boostedyaml.block.implementation.Section;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.gui.PagedGui;
import xyz.xenondevs.invui.item.Item;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.impl.AbstractItem;
import xyz.xenondevs.invui.item.impl.SimpleItem;
import xyz.xenondevs.invui.window.AnvilWindow;

public class DurabilityEditor {

	private final SectionPage parentPage;
	private String dur;
	private final Section section;

	public DurabilityEditor(Player player, SectionPage parentPage) {
		this.parentPage = parentPage;
		this.section = parentPage.getSection();

		Item border = new SimpleItem(new ItemBuilder(Material.AIR));
		var confirm = new ConfirmIcon();
		Gui upperGui = Gui.normal().setStructure("a # b")
				.addIngredient('a',
						new ItemBuilder(Material.NETHERITE_PICKAXE)
								.setDisplayName(String.valueOf(section.getInt("max-durability", 64))))
				.addIngredient('#', border).addIngredient('b', confirm).build();

		var gui = PagedGui.items()
				.setStructure("x x x x x x x x x", "x x x x x x x x x", "x x x x x x x x x", "# # # # c # # # #")
				.addIngredient('x', new ItemStack(Material.AIR)).addIngredient('c', parentPage.getBackItem())
				.addIngredient('#', new BackGroundItem()).build();

		var window = AnvilWindow
				.split().setViewer(player).setTitle(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
						.getAdventureManager().getComponentFromMiniMessage(HBLocale.GUI_TITLE_CUSTOM_DURABILITY)))
				.addRenameHandler(s -> {
					dur = s;
					confirm.notifyWindows();
				}).setUpperGui(upperGui).setLowerGui(gui).build();

		window.open();
	}

	public class ConfirmIcon extends AbstractItem {

		@Override
		public ItemProvider getItemProvider() {
			if (dur == null || dur.isEmpty()) {
				return new ItemBuilder(Material.STRUCTURE_VOID)
						.setDisplayName(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
								.getAdventureManager().getComponentFromMiniMessage(HBLocale.GUI_DELETE_PROPERTY)));
			} else {
				try {
					int m = Integer.parseInt(dur);
					if (m >= 1) {
						return new ItemBuilder(Material.NETHERITE_PICKAXE)
								.setDisplayName(new ShadedAdventureComponentWrapper(
										HellblockPlugin.getInstance().getAdventureManager()
												.getComponentFromMiniMessage(HBLocale.GUI_NEW_VALUE + dur)))
								.addLoreLines(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
										.getAdventureManager().getComponentFromMiniMessage(HBLocale.GUI_CLICK_CONFIRM)))
								.setDamage(Math.max(0, Material.NETHERITE_PICKAXE.getMaxDurability() - m));
					} else {
						return new ItemBuilder(Material.BARRIER).setDisplayName(
								new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
										.getComponentFromMiniMessage(HBLocale.GUI_INVALID_NUMBER)));
					}
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
			if (dur == null || dur.isEmpty()) {
				section.set("max-durability", null);
			} else {
				try {
					int value = Integer.parseInt(dur);
					if (value >= 1) {
						section.set("max-durability", value);
					} else {
						return;
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
