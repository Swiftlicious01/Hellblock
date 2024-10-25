package com.swiftlicious.hellblock.gui.hellblock;

import java.util.UUID;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.generation.HellBiome;
import com.swiftlicious.hellblock.gui.icon.BackGroundItem;
import com.swiftlicious.hellblock.playerdata.HellblockPlayer;
import com.swiftlicious.hellblock.utils.wrappers.ShadedAdventureComponentWrapper;

import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.impl.AbstractItem;
import xyz.xenondevs.invui.window.Window;

public class BiomeMenu {

	public BiomeMenu(Player player) {

		Gui gui = Gui.normal().setStructure(" # # s w c n b # x ")
				.addIngredient('s', new SoulSandValleyBiomeItem(player.getUniqueId()))
				.addIngredient('w', new WarpedForestBiomeItem(player.getUniqueId()))
				.addIngredient('c', new CrimsonForestBiomeItem(player.getUniqueId()))
				.addIngredient('n', new NetherWastesBiomeItem(player.getUniqueId()))
				.addIngredient('b', new BasaltDeltasBiomeItem(player.getUniqueId()))
				.addIngredient('#', new BackGroundItem()).addIngredient('x', new BackToMainMenuItem()).build();

		Window window = Window
				.single().setViewer(player).setTitle(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
						.getAdventureManager().getComponentFromMiniMessage("<red>Hellblock Biome Options")))
				.setGui(gui).build();

		window.open();
	}

	public class SoulSandValleyBiomeItem extends AbstractItem {

		private UUID playerUUID;

		public SoulSandValleyBiomeItem(UUID playerUUID) {
			this.playerUUID = playerUUID;
		}

		@Override
		public ItemProvider getItemProvider() {
			return new ItemBuilder(Material.SOUL_SOIL)
					.setDisplayName(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
							.getAdventureManager().getComponentFromMiniMessage("<aqua>Soul Sand Valley")))
					.addLoreLines(new ShadedAdventureComponentWrapper(
							HellblockPlugin.getInstance().getAdventureManager().getComponentFromMiniMessage(
									"<blue>Click to change your biome to Soul Sand Valley!")));
		}

		@Override
		public void handleClick(@NotNull ClickType clickType, @NotNull Player player,
				@NotNull InventoryClickEvent event) {
			HellblockPlayer hbPlayer = HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(playerUUID);
			if (HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(player).getBiomeCooldown() > 0) {
				HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
						String.format("<red>You have recently changed your hellbiome biome, you must wait for %s!",
								HellblockPlugin.getInstance().getFormattedCooldown(HellblockPlugin.getInstance()
										.getHellblockHandler().getActivePlayer(player).getBiomeCooldown())));
				return;
			}
			HellblockPlugin.getInstance().getBiomeHandler().changeHellblockBiome(hbPlayer, HellBiome.SOUL_SAND_VALLEY,
					false, false);
		}
	}

	public class CrimsonForestBiomeItem extends AbstractItem {

		private UUID playerUUID;

		public CrimsonForestBiomeItem(UUID playerUUID) {
			this.playerUUID = playerUUID;
		}

		@Override
		public ItemProvider getItemProvider() {
			return new ItemBuilder(Material.CRIMSON_STEM)
					.setDisplayName(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
							.getAdventureManager().getComponentFromMiniMessage("<aqua>Crimson Forest")))
					.addLoreLines(new ShadedAdventureComponentWrapper(
							HellblockPlugin.getInstance().getAdventureManager().getComponentFromMiniMessage(
									"<blue>Click to change your biome to Crimson Forest!")));
		}

		@Override
		public void handleClick(@NotNull ClickType clickType, @NotNull Player player,
				@NotNull InventoryClickEvent event) {
			HellblockPlayer hbPlayer = HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(playerUUID);
			if (HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(player).getBiomeCooldown() > 0) {
				HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
						String.format("<red>You have recently changed your hellbiome biome, you must wait for %s!",
								HellblockPlugin.getInstance().getFormattedCooldown(HellblockPlugin.getInstance()
										.getHellblockHandler().getActivePlayer(player).getBiomeCooldown())));
				return;
			}
			HellblockPlugin.getInstance().getBiomeHandler().changeHellblockBiome(hbPlayer, HellBiome.CRIMSON_FOREST,
					false, false);
		}
	}

	public class WarpedForestBiomeItem extends AbstractItem {

		private UUID playerUUID;

		public WarpedForestBiomeItem(UUID playerUUID) {
			this.playerUUID = playerUUID;
		}

		@Override
		public ItemProvider getItemProvider() {
			return new ItemBuilder(Material.WARPED_STEM)
					.setDisplayName(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
							.getAdventureManager().getComponentFromMiniMessage("<aqua>Warped Forest")))
					.addLoreLines(new ShadedAdventureComponentWrapper(
							HellblockPlugin.getInstance().getAdventureManager().getComponentFromMiniMessage(
									"<blue>Click to change your biome to Warped Forest!")));

		}

		@Override
		public void handleClick(@NotNull ClickType clickType, @NotNull Player player,
				@NotNull InventoryClickEvent event) {
			HellblockPlayer hbPlayer = HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(playerUUID);
			if (HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(player).getBiomeCooldown() > 0) {
				HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
						String.format("<red>You have recently changed your hellbiome biome, you must wait for %s!",
								HellblockPlugin.getInstance().getFormattedCooldown(HellblockPlugin.getInstance()
										.getHellblockHandler().getActivePlayer(player).getBiomeCooldown())));
				return;
			}
			HellblockPlugin.getInstance().getBiomeHandler().changeHellblockBiome(hbPlayer, HellBiome.WARPED_FOREST,
					false, false);
		}
	}

	public class NetherWastesBiomeItem extends AbstractItem {

		private UUID playerUUID;

		public NetherWastesBiomeItem(UUID playerUUID) {
			this.playerUUID = playerUUID;
		}

		@Override
		public ItemProvider getItemProvider() {
			return new ItemBuilder(Material.NETHERRACK)
					.setDisplayName(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
							.getAdventureManager().getComponentFromMiniMessage("<aqua>Nether Wastes")))
					.addLoreLines(new ShadedAdventureComponentWrapper(
							HellblockPlugin.getInstance().getAdventureManager().getComponentFromMiniMessage(
									"<blue>Click to change your biome to Nether Wastes!")));
		}

		@Override
		public void handleClick(@NotNull ClickType clickType, @NotNull Player player,
				@NotNull InventoryClickEvent event) {
			HellblockPlayer hbPlayer = HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(playerUUID);
			if (HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(player).getBiomeCooldown() > 0) {
				HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
						String.format("<red>You have recently changed your hellbiome biome, you must wait for %s!",
								HellblockPlugin.getInstance().getFormattedCooldown(HellblockPlugin.getInstance()
										.getHellblockHandler().getActivePlayer(player).getBiomeCooldown())));
				return;
			}
			HellblockPlugin.getInstance().getBiomeHandler().changeHellblockBiome(hbPlayer, HellBiome.NETHER_WASTES,
					false, false);
		}
	}

	public class BasaltDeltasBiomeItem extends AbstractItem {

		private UUID playerUUID;

		public BasaltDeltasBiomeItem(UUID playerUUID) {
			this.playerUUID = playerUUID;
		}

		@Override
		public ItemProvider getItemProvider() {
			return new ItemBuilder(Material.NETHERITE_BLOCK)
					.setDisplayName(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
							.getAdventureManager().getComponentFromMiniMessage("<aqua>Basalt Deltas")))
					.addLoreLines(new ShadedAdventureComponentWrapper(
							HellblockPlugin.getInstance().getAdventureManager().getComponentFromMiniMessage(
									"<blue>Click to change your biome to Basalt Deltas!")));
		}

		@Override
		public void handleClick(@NotNull ClickType clickType, @NotNull Player player,
				@NotNull InventoryClickEvent event) {
			HellblockPlayer hbPlayer = HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(playerUUID);
			if (HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(player).getBiomeCooldown() > 0) {
				HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
						String.format("<red>You have recently changed your hellbiome biome, you must wait for %s!",
								HellblockPlugin.getInstance().getFormattedCooldown(HellblockPlugin.getInstance()
										.getHellblockHandler().getActivePlayer(player).getBiomeCooldown())));
				return;
			}
			HellblockPlugin.getInstance().getBiomeHandler().changeHellblockBiome(hbPlayer, HellBiome.BASALT_DELTAS,
					false, false);
		}
	}

	public class BackToMainMenuItem extends AbstractItem {

		@Override
		public ItemProvider getItemProvider() {
			return new ItemBuilder(Material.ORANGE_STAINED_GLASS_PANE)
					.setDisplayName(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
							.getAdventureManager().getComponentFromMiniMessage("<gold>Return to Hellblock Menu")));
		}

		@Override
		public void handleClick(@NotNull ClickType clickType, @NotNull Player player,
				@NotNull InventoryClickEvent event) {
			new HellblockMenu(player);
		}
	}
}
