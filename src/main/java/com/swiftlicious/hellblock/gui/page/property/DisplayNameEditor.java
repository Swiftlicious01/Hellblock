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

public class DisplayNameEditor {

	private final SectionPage parentPage;
	private String name;
	private final Section section;

	public DisplayNameEditor(Player player, SectionPage parentPage) {
		this.parentPage = parentPage;
		this.section = parentPage.getSection();

		Item border = new SimpleItem(new ItemBuilder(Material.AIR));
		var confirm = new ConfirmIcon();
		Gui upperGui = Gui.normal().setStructure("a # b")
				.addIngredient('a',
						new ItemBuilder(Material.NAME_TAG)
								.setDisplayName(section.getString("display.name", HBLocale.GUI_NEW_DISPLAY_NAME)))
				.addIngredient('#', border).addIngredient('b', confirm).build();

		var gui = PagedGui.items()
				.setStructure("x x x x x x x x x", "x x x x x x x x x", "x x x x x x x x x", "# # # # c # # # #")
				.addIngredient('x', new ItemStack(Material.AIR)).addIngredient('c', parentPage.getBackItem())
				.addIngredient('#', new BackGroundItem()).build();

		var window = AnvilWindow
				.split().setViewer(player).setTitle(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
						.getAdventureManager().getComponentFromMiniMessage(HBLocale.GUI_TITLE_DISPLAY_NAME)))
				.addRenameHandler(s -> {
					name = s;
					confirm.notifyWindows();
				}).setUpperGui(upperGui).setLowerGui(gui).build();

		window.open();
	}

	public class ConfirmIcon extends AbstractItem {

		@Override
		public ItemProvider getItemProvider() {
			if (name == null || name.isEmpty()) {
				return new ItemBuilder(Material.STRUCTURE_VOID)
						.setDisplayName(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
								.getAdventureManager().getComponentFromMiniMessage(HBLocale.GUI_DELETE_PROPERTY)));
			} else {
				return new ItemBuilder(Material.NAME_TAG)
						.setDisplayName(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
								.getAdventureManager().getComponentFromMiniMessage("<!i>" + name)))
						.addLoreLines(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
								.getAdventureManager().getComponentFromMiniMessage(HBLocale.GUI_CLICK_CONFIRM)));
			}
		}

		@Override
		public void handleClick(@NotNull ClickType clickType, @NotNull Player player,
				@NotNull InventoryClickEvent event) {
			if (name == null || name.isEmpty()) {
				section.set("display.name", null);
			} else {
				section.set("display.name", name);
			}
			parentPage.reOpen();
			parentPage.save();
		}
	}
}
