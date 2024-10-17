package com.swiftlicious.hellblock.gui.page.property;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.config.HBLocale;
import com.swiftlicious.hellblock.creation.item.ItemManager;
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

public class MaterialEditor {

	private final Player player;
	private final SectionPage parentPage;
	private String material;
	private final ConfigurationSection section;

	public MaterialEditor(Player player, SectionPage parentPage) {
		this.player = player;
		this.parentPage = parentPage;
		this.section = parentPage.getSection();
		this.material = section.getString("material");

		Item border = new SimpleItem(new ItemBuilder(Material.AIR));
		var confirm = new ConfirmIcon();
		var itemBuilder = new ItemBuilder(
				HellblockPlugin.getInstance().getItemManager().getItemStackAppearance(player, material))
				.setDisplayName(section.getString("material", ""));

		if (section.contains("custom-model-data"))
			itemBuilder.setCustomModelData(section.getInt("custom-model-data", 0));

		Gui upperGui = Gui.normal().setStructure("a # b").addIngredient('a', itemBuilder).addIngredient('#', border)
				.addIngredient('b', confirm).build();

		var gui = PagedGui.items()
				.setStructure("x x x x x x x x x", "x x x x x x x x x", "x x x x x x x x x", "# # # # c # # # #")
				.addIngredient('x', Markers.CONTENT_LIST_SLOT_HORIZONTAL).addIngredient('c', parentPage.getBackItem())
				.addIngredient('#', new BackGroundItem()).setContent(getCompatibilityItemList()).build();

		var window = AnvilWindow
				.split().setViewer(player).setTitle(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
						.getAdventureManager().getComponentFromMiniMessage(HBLocale.GUI_TITLE_MATERIAL)))
				.addRenameHandler(s -> {
					material = s;
					confirm.notifyWindows();
				}).setUpperGui(upperGui).setLowerGui(gui).build();

		window.open();
	}

	public List<Item> getCompatibilityItemList() {
		ArrayList<Item> items = new ArrayList<>();
		for (String lib : ((ItemManager) HellblockPlugin.getInstance().getItemManager()).getItemLibraries()) {
			switch (lib) {
			case "MMOItems" ->
				items.add(new SimpleItem(new ItemBuilder(Material.BELL).setDisplayName(lib + ":TYPE:ID")));
			case "ItemsAdder" ->
				items.add(new SimpleItem(new ItemBuilder(Material.BELL).setDisplayName(lib + ":namespace:id")));
			case "vanilla", "CustomFishing" -> {
			}
			default -> items.add(new SimpleItem(new ItemBuilder(Material.BELL).setDisplayName(lib + ":ID")));
			}
		}
		return items;
	}

	public class ConfirmIcon extends AbstractItem {

		@Override
		public ItemProvider getItemProvider() {
			if (material == null || material.isEmpty()) {
				return new ItemBuilder(Material.STRUCTURE_VOID)
						.setDisplayName(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
								.getAdventureManager().getComponentFromMiniMessage(HBLocale.GUI_DELETE_PROPERTY)));
			} else {
				var builder = new ItemBuilder(
						HellblockPlugin.getInstance().getItemManager().getItemStackAppearance(player, material))
						.setDisplayName(HBLocale.GUI_NEW_VALUE + material)
						.addLoreLines(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
								.getAdventureManager().getComponentFromMiniMessage(HBLocale.GUI_CLICK_CONFIRM)));
				if (section.contains("custom-model-data"))
					builder.setCustomModelData(section.getInt("custom-model-data"));
				return builder;
			}
		}

		@Override
		public void handleClick(@NotNull ClickType clickType, @NotNull Player player,
				@NotNull InventoryClickEvent event) {
			if (material == null || material.isEmpty()) {
				section.set("material", null);
			} else if (HellblockPlugin.getInstance().getItemManager().getItemStackAppearance(player, material)
					.getType() == Material.BARRIER) {
				return;
			} else {
				section.set("material", material);
			}
			parentPage.reOpen();
			parentPage.save();
		}
	}
}
