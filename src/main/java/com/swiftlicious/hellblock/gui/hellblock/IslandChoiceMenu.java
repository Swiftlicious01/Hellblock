package com.swiftlicious.hellblock.gui.hellblock;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.generation.IslandOptions;
import com.swiftlicious.hellblock.gui.icon.BackGroundItem;
import com.swiftlicious.hellblock.utils.wrappers.ShadedAdventureComponentWrapper;

import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.impl.AbstractItem;
import xyz.xenondevs.invui.window.Window;

public class IslandChoiceMenu {

	public IslandChoiceMenu(Player player) {

		Gui gui = Gui.normal().setStructure("# d c s #").addIngredient('d', new DefaultIslandChoiceItem())
				.addIngredient('c', new ClassicIslandChoiceItem()).addIngredient('s', new SchematicIslandChoiceItem())
				.addIngredient('#', new BackGroundItem()).build();

		Window window = Window.single().setViewer(player)
				.setTitle(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
						.getComponentFromMiniMessage("<red>Hellblock Island Options")))
				.setGui(gui).setCloseable(HellblockPlugin.getInstance().getHellblockHandler()
						.getActivePlayer(player.getUniqueId()).hasHellblock())
				.build();

		window.open();
	}

	public static class DefaultIslandChoiceItem extends AbstractItem {

		@Override
		public ItemProvider getItemProvider() {
			return new ItemBuilder(Material.NETHERRACK)
					.setDisplayName(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
							.getAdventureManager().getComponentFromMiniMessage("<red>Default Hellblock Island")))
					.addLoreLines(new ShadedAdventureComponentWrapper(
							HellblockPlugin.getInstance().getAdventureManager().getComponentFromMiniMessage(
									"<gold>Click to generate the default hellblock island type!")));
		}

		@Override
		public void handleClick(@NotNull ClickType clickType, @NotNull Player player,
				@NotNull InventoryClickEvent event) {
			HellblockPlugin.getInstance().getHellblockHandler().createHellblock(player, IslandOptions.DEFAULT);
			player.closeInventory();
		}
	}

	public static class ClassicIslandChoiceItem extends AbstractItem {

		@Override
		public ItemProvider getItemProvider() {
			return new ItemBuilder(Material.SOUL_SAND)
					.setDisplayName(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
							.getAdventureManager().getComponentFromMiniMessage("<red>Classic Hellblock Island")))
					.addLoreLines(new ShadedAdventureComponentWrapper(
							HellblockPlugin.getInstance().getAdventureManager().getComponentFromMiniMessage(
									"<gold>Click to generate the classic hellblock island type!")));
		}

		@Override
		public void handleClick(@NotNull ClickType clickType, @NotNull Player player,
				@NotNull InventoryClickEvent event) {
			HellblockPlugin.getInstance().getHellblockHandler().createHellblock(player, IslandOptions.CLASSIC);
			player.closeInventory();
		}
	}

	public static class SchematicIslandChoiceItem extends AbstractItem {

		@Override
		public ItemProvider getItemProvider() {
			return new ItemBuilder(Material.MAP)
					.setDisplayName(
							new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
									.getComponentFromMiniMessage("<red>Choose a schematic for your Hellblock Island!")))
					.addLoreLines(new ShadedAdventureComponentWrapper(
							HellblockPlugin.getInstance().getAdventureManager().getComponentFromMiniMessage(
									"<gold>Click to view the different schematic options you have available!")));
		}

		@Override
		public void handleClick(@NotNull ClickType clickType, @NotNull Player player,
				@NotNull InventoryClickEvent event) {
			new SchematicMenu(player);
		}
	}
}
