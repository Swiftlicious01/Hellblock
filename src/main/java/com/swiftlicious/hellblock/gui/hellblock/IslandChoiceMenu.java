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

		if (!HellblockPlugin.getInstance().getHellblockHandler().getIslandOptions().isEmpty()) {
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
		} else {
			HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
					"<red>The island options list is empty, creating classic hellblock!");
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

	public class DefaultIslandChoiceItem extends AbstractItem {

		boolean isReset = false;

		public DefaultIslandChoiceItem(boolean isReset) {
			this.isReset = isReset;
		}

		@Override
		public ItemProvider getItemProvider() {
			if (HellblockPlugin.getInstance().getHellblockHandler().getIslandOptions()
					.contains(IslandOptions.DEFAULT.getName())) {
				return new ItemBuilder(Material.NETHERRACK)
						.setDisplayName(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
								.getAdventureManager().getComponentFromMiniMessage("<red>Default Hellblock Island")))
						.addLoreLines(new ShadedAdventureComponentWrapper(
								HellblockPlugin.getInstance().getAdventureManager().getComponentFromMiniMessage(
										"<gold>Click to generate the default hellblock island type!")));
			} else {
				return new ItemBuilder(Material.BARRIER)
						.setDisplayName(
								new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
										.getComponentFromMiniMessage("<red>Default Hellblock Unavailable!")))
						.addLoreLines(new ShadedAdventureComponentWrapper(
								HellblockPlugin.getInstance().getAdventureManager().getComponentFromMiniMessage(
										"<gold>This type of hellblock isn't available to choose!")));
			}
		}

		@Override
		public void handleClick(@NotNull ClickType clickType, @NotNull Player player,
				@NotNull InventoryClickEvent event) {
			if (event.getCurrentItem() != null && event.getCurrentItem().getType() == Material.NETHERRACK) {
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
			} else {
				HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
						"<red>The default hellblock type isn't available to generate!");
			}
		}
	}

	public class ClassicIslandChoiceItem extends AbstractItem {

		boolean isReset = false;

		public ClassicIslandChoiceItem(boolean isReset) {
			this.isReset = isReset;
		}

		@Override
		public ItemProvider getItemProvider() {
			if (HellblockPlugin.getInstance().getHellblockHandler().getIslandOptions()
					.contains(IslandOptions.CLASSIC.getName())) {
				return new ItemBuilder(Material.SOUL_SAND)
						.setDisplayName(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
								.getAdventureManager().getComponentFromMiniMessage("<red>Classic Hellblock Island")))
						.addLoreLines(new ShadedAdventureComponentWrapper(
								HellblockPlugin.getInstance().getAdventureManager().getComponentFromMiniMessage(
										"<gold>Click to generate the classic hellblock island type!")));
			} else {
				return new ItemBuilder(Material.BARRIER)
						.setDisplayName(
								new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
										.getComponentFromMiniMessage("<red>Classic Hellblock Unavailable!")))
						.addLoreLines(new ShadedAdventureComponentWrapper(
								HellblockPlugin.getInstance().getAdventureManager().getComponentFromMiniMessage(
										"<gold>This type of hellblock isn't available to choose!")));
			}
		}

		@Override
		public void handleClick(@NotNull ClickType clickType, @NotNull Player player,
				@NotNull InventoryClickEvent event) {
			if (event.getCurrentItem() != null && event.getCurrentItem().getType() == Material.SOUL_SAND) {
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
			} else {
				HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
						"<red>The classic hellblock type isn't available to generate!");
			}
		}
	}

	public class SchematicIslandChoiceItem extends AbstractItem {

		boolean isReset = false;

		public SchematicIslandChoiceItem(boolean isReset) {
			this.isReset = isReset;
		}

		@Override
		public ItemProvider getItemProvider() {
			boolean schematicsAvailable = false;
			for (String list : HellblockPlugin.getInstance().getHellblockHandler().getIslandOptions()) {
				if (list.equalsIgnoreCase(IslandOptions.CLASSIC.getName())
						|| list.equalsIgnoreCase(IslandOptions.DEFAULT.getName()))
					continue;
				if (!HellblockPlugin.getInstance().getSchematicManager().schematicFiles.containsKey(list))
					continue;

				schematicsAvailable = true;
				break;
			}
			if (schematicsAvailable) {
				return new ItemBuilder(Material.MAP)
						.setDisplayName(new ShadedAdventureComponentWrapper(
								HellblockPlugin.getInstance().getAdventureManager().getComponentFromMiniMessage(
										"<red>Choose a schematic for your Hellblock Island!")))
						.addLoreLines(new ShadedAdventureComponentWrapper(
								HellblockPlugin.getInstance().getAdventureManager().getComponentFromMiniMessage(
										"<gold>Click to view the different schematic options you have available!")));
			} else {
				return new ItemBuilder(Material.BARRIER)
						.setDisplayName(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
								.getAdventureManager().getComponentFromMiniMessage("<red>No schematics available!")))
						.addLoreLines(new ShadedAdventureComponentWrapper(
								HellblockPlugin.getInstance().getAdventureManager().getComponentFromMiniMessage(
										"<gold>There are no options to choose from here!")));
			}
		}

		@Override
		public void handleClick(@NotNull ClickType clickType, @NotNull Player player,
				@NotNull InventoryClickEvent event) {
			if (event.getCurrentItem() != null && event.getCurrentItem().getType() == Material.MAP) {
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
					if (list.equalsIgnoreCase(IslandOptions.CLASSIC.getName())
							|| list.equalsIgnoreCase(IslandOptions.DEFAULT.getName()))
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
			} else {
				HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
						"<red>There are no hellblock schematics for you to choose from!");
			}
		}
	}
}
