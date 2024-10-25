package com.swiftlicious.hellblock.gui.hellblock;

import java.io.File;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;

import com.google.common.io.Files;
import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.generation.IslandOptions;
import com.swiftlicious.hellblock.gui.icon.BackGroundItem;
import com.swiftlicious.hellblock.gui.icon.ScrollUpItem;
import com.swiftlicious.hellblock.gui.icon.ScrollDownItem;
import com.swiftlicious.hellblock.utils.wrappers.ShadedAdventureComponentWrapper;

import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.gui.ScrollGui;
import xyz.xenondevs.invui.gui.structure.Markers;
import xyz.xenondevs.invui.item.Item;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.impl.AbstractItem;
import xyz.xenondevs.invui.window.Window;

public class SchematicMenu {

	public SchematicMenu(Player player, boolean isReset) {
		File[] files = HellblockPlugin.getInstance().getHellblockHandler().getSchematics();
		Deque<Item> items = new ArrayDeque<>();
		if (files != null) {
			for (File file : files) {
				if (file.isFile()
						&& HellblockPlugin.getInstance().getHellblockHandler().getIslandOptions()
								.contains(Files.getNameWithoutExtension(file.getName()))
						&& (player.hasPermission("hellblock.schematic.*") || player
								.hasPermission("hellblock.schematic." + Files.getNameWithoutExtension(file.getName())))
						&& (file.getName().endsWith(".schematic") || file.getName().endsWith(".schem"))) {
					items.addFirst(new SchematicItem(file, isReset));
				}
			}
		}

		Gui gui = ScrollGui.items()
				.setStructure("x x x x x x x x u", "x x x x x x x x #", "x x x x x x x x b", "x x x x x x x x #",
						"x x x x x x x x d")
				.addIngredient('x', Markers.CONTENT_LIST_SLOT_VERTICAL).addIngredient('#', new BackGroundItem())
				.addIngredient('u', new ScrollUpItem()).addIngredient('d', new ScrollDownItem())
				.addIngredient('b', new BackToChoicesItem()).setContent(items.stream().toList()).build();

		Window window = Window.single().setViewer(player)
				.setTitle(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
						.getComponentFromMiniMessage("<red>Hellblock Island Schematics")))
				.setGui(gui).setCloseable(HellblockPlugin.getInstance().getHellblockHandler()
						.getActivePlayer(player.getUniqueId()).hasHellblock())
				.build();

		window.open();
	}

	public class SchematicItem extends AbstractItem {

		private final File file;
		private boolean isReset;

		public SchematicItem(File file, boolean isReset) {
			this.file = file;
			this.isReset = isReset;
		}

		@Override
		public ItemProvider getItemProvider() {
			return new ItemBuilder(Material.PAPER)
					.setDisplayName(
							new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
									.getComponentFromMiniMessage(String.format("<yellow>%s",
											Files.getNameWithoutExtension(file.getName())))))
					.addLoreLines(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
							.getAdventureManager()
							.getComponentFromMiniMessage(String.format(
									"<gold>Click to generate your hellblock island as the <yellow>%s <gold>schematic!",
									Files.getNameWithoutExtension(file.getName())))));
		}

		@Override
		public void handleClick(@NotNull ClickType clickType, @NotNull Player player,
				@NotNull InventoryClickEvent event) {
			if (player.hasPermission("hellblock.schematic.*")
					|| player.hasPermission("hellblock.schematic." + Files.getNameWithoutExtension(file.getName()))) {
				if (HellblockPlugin.getInstance().getHellblockHandler().getIslandOptions()
						.contains(Files.getNameWithoutExtension(file.getName()))) {
					if (isReset && HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(player)
							.getResetCooldown() > 0) {
						HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
								String.format(
										"<red>You have recently reset your hellblock already, you must wait for %s!",
										HellblockPlugin.getInstance().getFormattedCooldown(HellblockPlugin.getInstance()
												.getHellblockHandler().getActivePlayer(player).getResetCooldown())));
						return;
					}
					HellblockPlugin.getInstance().getHellblockHandler().createHellblock(player, IslandOptions.SCHEMATIC,
							Files.getNameWithoutExtension(file.getName()));
					HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(player.getUniqueId())
							.setResetCooldown(Duration.ofDays(1).toHours());
					HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(player.getUniqueId())
							.saveHellblockPlayer();
					new HellblockMenu(player);
				} else {
					HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player, String.format(
							"<red>The schematic <dark_red>%s <red>hellblock island type is not available to generate!",
							Files.getNameWithoutExtension(file.getName())));
				}
			} else {
				HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
						String.format(
								"<red>You don't have permission to generate the hellblock schematic <dark_red>%s<red>!",
								Files.getNameWithoutExtension(file.getName())));
			}
		}
	}

	public class BackToChoicesItem extends AbstractItem {
		
		@Override
		public ItemProvider getItemProvider() {
			return new ItemBuilder(Material.ORANGE_STAINED_GLASS_PANE)
					.setDisplayName(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
							.getAdventureManager().getComponentFromMiniMessage("<gold>Return to Choices Menu")));
		}

		@Override
		public void handleClick(@NotNull ClickType clickType, @NotNull Player player,
				@NotNull InventoryClickEvent event) {
			new IslandChoiceMenu(player, false);
		}
	}
}
