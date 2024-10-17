package com.swiftlicious.hellblock.gui.page.property;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.config.HBLocale;
import com.swiftlicious.hellblock.gui.SectionPage;
import com.swiftlicious.hellblock.gui.icon.BackGroundItem;
import com.swiftlicious.hellblock.utils.wrappers.ShadedAdventureComponentWrapper;

import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.gui.PagedGui;
import xyz.xenondevs.invui.gui.structure.Markers;
import xyz.xenondevs.invui.item.Item;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.impl.AbstractItem;
import xyz.xenondevs.invui.item.impl.SimpleItem;
import xyz.xenondevs.invui.window.AnvilWindow;

import java.util.ArrayList;
import java.util.List;

public class LoreEditor {

	private final Player player;
	private final SectionPage parentPage;
	private final ArrayList<String> lore;
	private final ConfigurationSection section;
	private int index;

	public LoreEditor(Player player, SectionPage parentPage) {
		this.player = player;
		this.parentPage = parentPage;
		this.section = parentPage.getSection();
		this.index = 0;
		this.lore = new ArrayList<>(section.getStringList("display.lore"));
		this.lore.add(0, HBLocale.GUI_SELECT_ONE_LORE);
		reOpen(0);
	}

	public void reOpen(int idx) {
		Item border = new SimpleItem(new ItemBuilder(Material.AIR));
		var confirm = new ConfirmIcon();
		Gui upperGui = Gui.normal().setStructure("a # b")
				.addIngredient('a', new ItemBuilder(Material.NAME_TAG).setDisplayName(lore.get(idx)))
				.addIngredient('#', border).addIngredient('b', confirm).build();

		var gui = PagedGui.items()
				.setStructure("x x x x x x x x x", "x x x x x x x x x", "x x x x x x x x x", "# # # # c # # # #")
				.addIngredient('x', Markers.CONTENT_LIST_SLOT_HORIZONTAL).addIngredient('c', parentPage.getBackItem())
				.addIngredient('#', new BackGroundItem()).setContent(getContents()).build();

		var window = AnvilWindow
				.split().setViewer(player).setTitle(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
						.getAdventureManager().getComponentFromMiniMessage(HBLocale.GUI_TITLE_LORE)))
				.addRenameHandler(s -> {
					if (index == 0)
						return;
					lore.set(index, s);
					confirm.notifyWindows();
				}).setUpperGui(upperGui).setLowerGui(gui).build();

		window.open();
	}

	public List<Item> getContents() {
		ArrayList<Item> items = new ArrayList<>();
		int i = 1;
		List<String> subList = lore.subList(1, lore.size());
		for (String lore : subList) {
			items.add(new LoreElement(lore, i++));
		}
		items.add(new AddLore());
		return items;
	}

	public class AddLore extends AbstractItem {

		@Override
		public ItemProvider getItemProvider() {
			return new ItemBuilder(Material.ANVIL).setDisplayName(new ShadedAdventureComponentWrapper(HellblockPlugin
					.getInstance().getAdventureManager().getComponentFromMiniMessage(HBLocale.GUI_ADD_NEW_LORE)));
		}

		@Override
		public void handleClick(@NotNull ClickType clickType, @NotNull Player player,
				@NotNull InventoryClickEvent event) {
			lore.add("Text");
			index = lore.size() - 1;
			reOpen(index);
		}
	}

	public class LoreElement extends AbstractItem {

		private final String line;
		private final int idx;

		public LoreElement(String line, int idx) {
			this.line = line;
			this.idx = idx;
		}

		@Override
		public ItemProvider getItemProvider() {
			return new ItemBuilder(Material.PAPER)
					.setDisplayName(new ShadedAdventureComponentWrapper(
							HellblockPlugin.getInstance().getAdventureManager().getComponentFromMiniMessage(line)))
					.addLoreLines("")
					.addLoreLines(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
							.getAdventureManager().getComponentFromMiniMessage(HBLocale.GUI_LEFT_CLICK_EDIT)))
					.addLoreLines(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
							.getAdventureManager().getComponentFromMiniMessage(HBLocale.GUI_RIGHT_CLICK_DELETE)));
		}

		@Override
		public void handleClick(@NotNull ClickType clickType, @NotNull Player player,
				@NotNull InventoryClickEvent event) {
			if (clickType == ClickType.LEFT) {
				index = idx;
				reOpen(idx);
			} else if (clickType == ClickType.RIGHT) {
				lore.remove(idx);
				index = Math.min(index, lore.size() - 1);
				reOpen(index);
			}
		}
	}

	public class ConfirmIcon extends AbstractItem {

		@Override
		public ItemProvider getItemProvider() {
			List<String> subList = lore.subList(1, lore.size());
			if (subList.isEmpty()) {
				return new ItemBuilder(Material.STRUCTURE_VOID)
						.setDisplayName(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
								.getAdventureManager().getComponentFromMiniMessage(HBLocale.GUI_DELETE_PROPERTY)));
			} else {
				var builder = new ItemBuilder(Material.NAME_TAG)
						.setDisplayName(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
								.getAdventureManager().getComponentFromMiniMessage(HBLocale.GUI_CLICK_CONFIRM)));
				for (String lore : subList) {
					builder.addLoreLines(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
							.getAdventureManager().getComponentFromMiniMessage(" <gray>-</gray> " + lore)));
				}
				return builder;
			}
		}

		@Override
		public void handleClick(@NotNull ClickType clickType, @NotNull Player player,
				@NotNull InventoryClickEvent event) {
			List<String> subList = lore.subList(1, lore.size());
			if (lore.isEmpty()) {
				section.set("display.lore", null);
			} else {
				section.set("display.lore", subList);
			}
			parentPage.reOpen();
			parentPage.save();
		}
	}
}
