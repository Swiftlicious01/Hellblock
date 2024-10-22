package com.swiftlicious.hellblock.gui.page.file;

import java.io.File;
import java.util.ArrayDeque;
import java.util.Deque;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.config.HBLocale;
import com.swiftlicious.hellblock.gui.Icon;
import com.swiftlicious.hellblock.gui.icon.BackGroundItem;
import com.swiftlicious.hellblock.gui.icon.BackToFolderItem;
import com.swiftlicious.hellblock.gui.icon.ScrollDownItem;
import com.swiftlicious.hellblock.gui.icon.ScrollUpItem;
import com.swiftlicious.hellblock.gui.page.item.ItemSelector;
import com.swiftlicious.hellblock.utils.wrappers.ShadedAdventureComponentWrapper;

import xyz.xenondevs.invui.animation.impl.SequentialAnimation;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.gui.ScrollGui;
import xyz.xenondevs.invui.gui.SlotElement;
import xyz.xenondevs.invui.gui.structure.Markers;
import xyz.xenondevs.invui.item.Item;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.impl.AbstractItem;
import xyz.xenondevs.invui.window.Window;

public class FileSelector {

	public FileSelector(Player player, File folder) {
		File[] files = folder.listFiles();
		Deque<Item> items = new ArrayDeque<>();
		if (files != null) {
			for (File file : files) {
				if (file.isFile() && file.getName().endsWith(".yml")) {
					items.addLast(new FileItem(file));
				} else if (file.isDirectory()) {
					String path = file.getPath().replace("/", "\\");
					String[] split = path.split("\\\\");
					String type = split[3];
					switch (type) {
					case "item", "rod", "bait", "util", "hook" -> items.addFirst(new FolderItem(file));
					}
				}
			}
		}

		Gui gui = ScrollGui.items()
				.setStructure("x x x x x x x x u", "x x x x x x x x #", "x x x x x x x x b", "x x x x x x x x #",
						"x x x x x x x x d")
				.addIngredient('x', Markers.CONTENT_LIST_SLOT_HORIZONTAL).addIngredient('#', new BackGroundItem())
				.addIngredient('u', new ScrollUpItem()).addIngredient('d', new ScrollDownItem())
				.addIngredient('b', new BackToFolderItem(folder.getParentFile())).setContent(items.stream().toList())
				.build();

		Window window = Window
				.single().setViewer(player).setTitle(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
						.getAdventureManager().getComponentFromMiniMessage(HBLocale.GUI_SELECT_FILE)))
				.setGui(gui).build();

		gui.playAnimation(new SequentialAnimation(1, true), slotElement -> {
			if (slotElement instanceof SlotElement.ItemSlotElement itemSlotElement) {
				return !(itemSlotElement.getItem() instanceof Icon);
			}
			return true;
		});

		window.open();
	}

	public static class FileItem extends AbstractItem {

		private final File file;

		public FileItem(File file) {
			this.file = file;
		}

		@Override
		public ItemProvider getItemProvider() {
			return new ItemBuilder(Material.PAPER).setDisplayName(new ShadedAdventureComponentWrapper(HellblockPlugin
					.getInstance().getAdventureManager().getComponentFromMiniMessage("<#FDF5E6>" + file.getName())));
		}

		@Override
		public void handleClick(@NotNull ClickType clickType, @NotNull Player player,
				@NotNull InventoryClickEvent event) {
			String path = file.getPath().replace("/", "\\");
			String[] split = path.split("\\\\");
			String type = split[3];
			switch (type) {
			case "item", "rod", "bait", "util", "hook" -> {
				new ItemSelector(player, file, type);
			}
			}
		}
	}

	public static class FolderItem extends AbstractItem {

		private final File file;

		public FolderItem(File file) {
			this.file = file;
		}

		@Override
		public ItemProvider getItemProvider() {
			return new ItemBuilder(Material.BOOK).setDisplayName(new ShadedAdventureComponentWrapper(HellblockPlugin
					.getInstance().getAdventureManager().getComponentFromMiniMessage("<#D2B48C><b>" + file.getName())));
		}

		@Override
		public void handleClick(@NotNull ClickType clickType, @NotNull Player player,
				@NotNull InventoryClickEvent event) {
			new FileSelector(player, file);
		}
	}
}