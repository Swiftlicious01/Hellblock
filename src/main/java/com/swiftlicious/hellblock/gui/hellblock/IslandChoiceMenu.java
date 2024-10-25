package com.swiftlicious.hellblock.gui.hellblock;

import java.time.Duration;

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

	public IslandChoiceMenu(Player player, boolean isReset) {

		Gui gui = Gui.normal().setStructure("# d c s #").addIngredient('d', new DefaultIslandChoiceItem(isReset))
				.addIngredient('c', new ClassicIslandChoiceItem(isReset))
				.addIngredient('s', new SchematicIslandChoiceItem(isReset)).addIngredient('#', new BackGroundItem())
				.build();

		Window window = Window.single().setViewer(player)
				.setTitle(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
						.getComponentFromMiniMessage("<red>Hellblock Island Options")))
				.setGui(gui).setCloseable(HellblockPlugin.getInstance().getHellblockHandler()
						.getActivePlayer(player.getUniqueId()).hasHellblock())
				.build();

		window.open();
	}

	public class DefaultIslandChoiceItem extends AbstractItem {

		boolean isReset = false;

		public DefaultIslandChoiceItem(boolean isReset) {
			this.isReset = isReset;
		}

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
			if (isReset && HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(player)
					.getResetCooldown() > 0) {
				HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
						String.format("<red>You have recently reset your hellblock already, you must wait for %s!",
								HellblockPlugin.getInstance().getFormattedCooldown(HellblockPlugin.getInstance()
										.getHellblockHandler().getActivePlayer(player).getResetCooldown())));
				return;
			}
			HellblockPlugin.getInstance().getHellblockHandler().createHellblock(player, IslandOptions.DEFAULT);
			if (isReset) {
				HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(player.getUniqueId())
						.setResetCooldown(Duration.ofDays(1).toHours());
				HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(player.getUniqueId())
						.saveHellblockPlayer();
			}
			new HellblockMenu(player);
		}
	}

	public class ClassicIslandChoiceItem extends AbstractItem {

		boolean isReset = false;

		public ClassicIslandChoiceItem(boolean isReset) {
			this.isReset = isReset;
		}

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
			if (isReset && HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(player)
					.getResetCooldown() > 0) {
				HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
						String.format("<red>You have recently reset your hellblock already, you must wait for %s!",
								HellblockPlugin.getInstance().getFormattedCooldown(HellblockPlugin.getInstance()
										.getHellblockHandler().getActivePlayer(player).getResetCooldown())));
				return;
			}
			HellblockPlugin.getInstance().getHellblockHandler().createHellblock(player, IslandOptions.CLASSIC);
			if (isReset) {
				HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(player.getUniqueId())
						.setResetCooldown(Duration.ofDays(1).toHours());
				HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(player.getUniqueId())
						.saveHellblockPlayer();
			}
			new HellblockMenu(player);
		}
	}

	public class SchematicIslandChoiceItem extends AbstractItem {

		boolean isReset = false;

		public SchematicIslandChoiceItem(boolean isReset) {
			this.isReset = isReset;
		}

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
			if (isReset && HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(player)
					.getResetCooldown() > 0) {
				HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
						String.format("<red>You have recently reset your hellblock already, you must wait for %s!",
								HellblockPlugin.getInstance().getFormattedCooldown(HellblockPlugin.getInstance()
										.getHellblockHandler().getActivePlayer(player).getResetCooldown())));
				return;
			}
			boolean schematicsAvailable = false;
			for (String list : HellblockPlugin.getInstance().getHellblockHandler().getIslandOptions()) {
				if (list.equalsIgnoreCase("classic") || list.equalsIgnoreCase("default"))
					continue;
				if (!HellblockPlugin.getInstance().getSchematicManager().schematicFiles.containsKey(list))
					continue;

				schematicsAvailable = true;
				break;
			}
			if (schematicsAvailable) {
				new SchematicMenu(player, isReset);
			} else {
				HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
						"<red>There are no hellblock schematics for you to choose from!");
			}
		}
	}
}
