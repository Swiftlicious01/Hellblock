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

public class CustomModelDataEditor {

	private final Player player;
	private final SectionPage parentPage;
	private String cmd;
	private final Section section;
	private final String material;

	public CustomModelDataEditor(Player player, SectionPage parentPage) {
		this.player = player;
		this.parentPage = parentPage;
		this.section = parentPage.getSection();
		this.material = section.getString("material");

		Item border = new SimpleItem(new ItemBuilder(Material.AIR));
		var confirm = new ConfirmIcon();
		Gui upperGui = Gui.normal().setStructure("a # b").addIngredient('a',
				new ItemBuilder(HellblockPlugin.getInstance().getItemManager().getItemStackAppearance(player, material))
						.setCustomModelData(section.getInt("custom-model-data", 0))
						.setDisplayName(String.valueOf(section.getInt("custom-model-data", 0))))
				.addIngredient('#', border).addIngredient('b', confirm).build();

		var gui = PagedGui.items()
				.setStructure("x x x x x x x x x", "x x x x x x x x x", "x x x x x x x x x", "# # # # c # # # #")
				.addIngredient('x', new ItemStack(Material.AIR)).addIngredient('c', parentPage.getBackItem())
				.addIngredient('#', new BackGroundItem()).build();

		var window = AnvilWindow
				.split().setViewer(player).setTitle(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
						.getAdventureManager().getComponentFromMiniMessage(HBLocale.GUI_TITLE_MODEL_DATA)))
				.addRenameHandler(s -> {
					cmd = s;
					confirm.notifyWindows();
				}).setUpperGui(upperGui).setLowerGui(gui).build();

		window.open();
	}

	public class ConfirmIcon extends AbstractItem {

		@Override
		public ItemProvider getItemProvider() {
			if (cmd == null || cmd.isEmpty()) {
				return new ItemBuilder(Material.STRUCTURE_VOID)
						.setDisplayName(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
								.getAdventureManager().getComponentFromMiniMessage(HBLocale.GUI_DELETE_PROPERTY)));
			} else {
				try {
					int value = Integer.parseInt(cmd);
					if (value >= 0) {
						return new ItemBuilder(
								HellblockPlugin.getInstance().getItemManager().getItemStackAppearance(player, material))
								.setCustomModelData(value).setDisplayName(HBLocale.GUI_NEW_VALUE + value)
								.addLoreLines(new ShadedAdventureComponentWrapper(
										HellblockPlugin.getInstance().getAdventureManager()
												.getComponentFromMiniMessage(HBLocale.GUI_CLICK_CONFIRM)));
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
			if (cmd == null || cmd.isEmpty()) {
				section.set("custom-model-data", null);
			} else {
				try {
					int value = Integer.parseInt(cmd);
					if (value >= 0) {
						section.set("custom-model-data", value);
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
