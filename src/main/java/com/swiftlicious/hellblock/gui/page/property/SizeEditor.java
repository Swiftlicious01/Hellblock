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

public class SizeEditor {

	private final Player player;
	private final SectionPage parentPage;
	private final String[] size;
	private int index;
	private final Section section;

	public SizeEditor(Player player, SectionPage parentPage) {
		this.player = player;
		this.parentPage = parentPage;
		this.section = parentPage.getSection();
		this.index = 0;
		this.size = section.contains("size") ? section.getString("size").split("~") : new String[] { "0", "0" };
		reOpen();
	}

	public void reOpen() {
		Item border = new SimpleItem(new ItemBuilder(Material.AIR));
		var confirm = new ConfirmIcon();
		Gui upperGui = Gui.normal().setStructure("a # b")
				.addIngredient('a', new ItemBuilder(Material.PUFFERFISH).setDisplayName(size[index]))
				.addIngredient('#', border).addIngredient('b', confirm).build();

		var gui = PagedGui.items()
				.setStructure("a b x x x x x x x", "x x x x x x x x x", "x x x x x x x x x", "# # # # c # # # #")
				.addIngredient('x', new ItemStack(Material.AIR)).addIngredient('c', parentPage.getBackItem())
				.addIngredient('#', new BackGroundItem()).addIngredient('a', new MinItem())
				.addIngredient('b', new MaxItem()).build();

		var window = AnvilWindow
				.split().setViewer(player).setTitle(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
						.getAdventureManager().getComponentFromMiniMessage(HBLocale.GUI_SIZE_TITLE)))
				.addRenameHandler(s -> {
					if (s == null || s.equals("")) {
						size[index] = "0";
						return;
					}
					size[index] = s;
					confirm.notifyWindows();
				}).setUpperGui(upperGui).setLowerGui(gui).build();

		window.open();
	}

	public class MinItem extends AbstractItem {

		@Override
		public ItemProvider getItemProvider() {
			return new ItemBuilder(Material.IRON_INGOT)
					.setDisplayName(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
							.getAdventureManager().getComponentFromMiniMessage(HBLocale.GUI_SIZE_MIN)));
		}

		@Override
		public void handleClick(@NotNull ClickType clickType, @NotNull Player player,
				@NotNull InventoryClickEvent event) {
			index = 0;
			reOpen();
		}
	}

	public class MaxItem extends AbstractItem {

		@Override
		public ItemProvider getItemProvider() {
			return new ItemBuilder(Material.IRON_BLOCK)
					.setDisplayName(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
							.getAdventureManager().getComponentFromMiniMessage(HBLocale.GUI_SIZE_MAX)));
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
			if (size[0].equals("0") && size[1].equals("0")) {
				return new ItemBuilder(Material.STRUCTURE_VOID)
						.setDisplayName(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
								.getAdventureManager().getComponentFromMiniMessage(HBLocale.GUI_DELETE_PROPERTY)));
			} else {
				try {
					double min = Double.parseDouble(size[0]);
					double max = Double.parseDouble(size[1]);

					if (min <= max) {
						return new ItemBuilder(Material.PUFFERFISH)
								.setDisplayName(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
										.getAdventureManager().getComponentFromMiniMessage(HBLocale.GUI_NEW_VALUE)))
								.addLoreLines(new ShadedAdventureComponentWrapper(
										HellblockPlugin.getInstance().getAdventureManager().getComponentFromMiniMessage(
												"<gray> - <white>" + size[0] + "~" + size[1])))
								.addLoreLines(new ShadedAdventureComponentWrapper(
										HellblockPlugin.getInstance().getAdventureManager()
												.getComponentFromMiniMessage(HBLocale.GUI_CLICK_CONFIRM)));
					} else {
						return new ItemBuilder(Material.BARRIER).setDisplayName(
								new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
										.getComponentFromMiniMessage(HBLocale.GUI_SIZE_MAX_NO_LESS)));
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
			if (size[0].equals("0") && size[1].equals("0")) {
				section.set("size", null);
			} else {
				try {
					double min = Double.parseDouble(size[0]);
					double max = Double.parseDouble(size[1]);
					if (min <= max) {
						section.set("size", min + "~" + max);
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
