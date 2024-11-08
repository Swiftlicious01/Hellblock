package com.swiftlicious.hellblock.gui.hellblock;

import java.util.UUID;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.jetbrains.annotations.NotNull;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.generation.HellBiome;
import com.swiftlicious.hellblock.gui.icon.BackGroundItem;
import com.swiftlicious.hellblock.player.OnlineUser;
import com.swiftlicious.hellblock.utils.wrappers.ShadedAdventureComponentWrapper;

import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.impl.AbstractItem;
import xyz.xenondevs.invui.window.Window;

public class BiomeMenu {

	public BiomeMenu(Player player) {

		OnlineUser onlineUser = HellblockPlugin.getInstance().getStorageManager().getOnlineUser(player.getUniqueId());
		if (onlineUser == null) {
			HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
					"<red>Still loading your player data... please try again in a few seconds.");
			return;
		}

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
			OnlineUser onlineUser = HellblockPlugin.getInstance().getStorageManager().getOnlineUser(playerUUID);
			if (onlineUser.getHellblockData().getBiome() == HellBiome.SOUL_SAND_VALLEY) {
				return new ItemBuilder(Material.SOUL_SOIL).addEnchantment(Enchantment.UNBREAKING, 1, false)
						.addItemFlags(ItemFlag.HIDE_ENCHANTS)
						.setDisplayName(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
								.getAdventureManager().getComponentFromMiniMessage("<aqua>Soul Sand Valley")))
						.addLoreLines(
								new ShadedAdventureComponentWrapper(
										HellblockPlugin.getInstance().getAdventureManager().getComponentFromMiniMessage(
												"<blue>Click to change your biome to Soul Sand Valley!")),
								new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
										.getComponentFromMiniMessage(" ")),
								new ShadedAdventureComponentWrapper(
										HellblockPlugin.getInstance().getAdventureManager().getComponentFromMiniMessage(
												"<light_purple>This is your currently selected biome!")));
			} else {
				return new ItemBuilder(Material.SOUL_SOIL)
						.setDisplayName(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
								.getAdventureManager().getComponentFromMiniMessage("<aqua>Soul Sand Valley")))
						.addLoreLines(new ShadedAdventureComponentWrapper(
								HellblockPlugin.getInstance().getAdventureManager().getComponentFromMiniMessage(
										"<blue>Click to change your biome to Soul Sand Valley!")));
			}
		}

		@Override
		public void handleClick(@NotNull ClickType clickType, @NotNull Player player,
				@NotNull InventoryClickEvent event) {
			OnlineUser onlineUser = HellblockPlugin.getInstance().getStorageManager()
					.getOnlineUser(player.getUniqueId());
			if (onlineUser == null)
				return;
			if (onlineUser.getHellblockData().getBiome() == HellBiome.SOUL_SAND_VALLEY) {
				HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
						String.format("<red>Your hellblock biome is already set to <dark_red>%s<red>!",
								HellBiome.SOUL_SAND_VALLEY.getName()));
				HellblockPlugin.getInstance().getAdventureManager().sendSound(player,
						net.kyori.adventure.sound.Sound.Source.PLAYER,
						net.kyori.adventure.key.Key.key("minecraft:entity.villager.no"), 1, 1);
				return;
			}
			if (onlineUser.getHellblockData().getBiomeCooldown() > 0) {
				HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
						String.format("<red>You've recently changed your hellbiome biome, you must wait for %s!",
								HellblockPlugin.getInstance()
										.getFormattedCooldown(onlineUser.getHellblockData().getBiomeCooldown())));
				HellblockPlugin.getInstance().getAdventureManager().sendSound(player,
						net.kyori.adventure.sound.Sound.Source.PLAYER,
						net.kyori.adventure.key.Key.key("minecraft:entity.villager.no"), 1, 1);
				return;
			}
			HellblockPlugin.getInstance().getBiomeHandler().changeHellblockBiome(onlineUser,
					HellBiome.SOUL_SAND_VALLEY);
			HellblockPlugin.getInstance().getAdventureManager().sendSound(player,
					net.kyori.adventure.sound.Sound.Source.PLAYER,
					net.kyori.adventure.key.Key.key("minecraft:ui.button.click"), 1, 1);
			new BiomeMenu(player);
		}
	}

	public class CrimsonForestBiomeItem extends AbstractItem {

		private UUID playerUUID;

		public CrimsonForestBiomeItem(UUID playerUUID) {
			this.playerUUID = playerUUID;
		}

		@Override
		public ItemProvider getItemProvider() {
			OnlineUser onlineUser = HellblockPlugin.getInstance().getStorageManager().getOnlineUser(playerUUID);
			if (onlineUser.getHellblockData().getBiome() == HellBiome.CRIMSON_FOREST) {
				return new ItemBuilder(Material.CRIMSON_STEM).addEnchantment(Enchantment.UNBREAKING, 1, false)
						.addItemFlags(ItemFlag.HIDE_ENCHANTS)
						.setDisplayName(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
								.getAdventureManager().getComponentFromMiniMessage("<aqua>Crimson Forest")))
						.addLoreLines(
								new ShadedAdventureComponentWrapper(
										HellblockPlugin.getInstance().getAdventureManager().getComponentFromMiniMessage(
												"<blue>Click to change your biome to Crimson Forest!")),
								new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
										.getComponentFromMiniMessage(" ")),
								new ShadedAdventureComponentWrapper(
										HellblockPlugin.getInstance().getAdventureManager().getComponentFromMiniMessage(
												"<light_purple>This is your currently selected biome!")));
			} else {
				return new ItemBuilder(Material.CRIMSON_STEM)
						.setDisplayName(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
								.getAdventureManager().getComponentFromMiniMessage("<aqua>Crimson Forest")))
						.addLoreLines(new ShadedAdventureComponentWrapper(
								HellblockPlugin.getInstance().getAdventureManager().getComponentFromMiniMessage(
										"<blue>Click to change your biome to Crimson Forest!")));
			}
		}

		@Override
		public void handleClick(@NotNull ClickType clickType, @NotNull Player player,
				@NotNull InventoryClickEvent event) {
			OnlineUser onlineUser = HellblockPlugin.getInstance().getStorageManager()
					.getOnlineUser(player.getUniqueId());
			if (onlineUser == null)
				return;
			if (onlineUser.getHellblockData().getBiome() == HellBiome.CRIMSON_FOREST) {
				HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
						String.format("<red>Your hellblock biome is already set to <dark_red>%s<red>!",
								HellBiome.CRIMSON_FOREST.getName()));
				HellblockPlugin.getInstance().getAdventureManager().sendSound(player,
						net.kyori.adventure.sound.Sound.Source.PLAYER,
						net.kyori.adventure.key.Key.key("minecraft:entity.villager.no"), 1, 1);
				return;
			}
			if (onlineUser.getHellblockData().getBiomeCooldown() > 0) {
				HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
						String.format("<red>You've recently changed your hellbiome biome, you must wait for %s!",
								HellblockPlugin.getInstance()
										.getFormattedCooldown(onlineUser.getHellblockData().getBiomeCooldown())));
				HellblockPlugin.getInstance().getAdventureManager().sendSound(player,
						net.kyori.adventure.sound.Sound.Source.PLAYER,
						net.kyori.adventure.key.Key.key("minecraft:entity.villager.no"), 1, 1);
				return;
			}
			HellblockPlugin.getInstance().getBiomeHandler().changeHellblockBiome(onlineUser, HellBiome.CRIMSON_FOREST);
			HellblockPlugin.getInstance().getAdventureManager().sendSound(player,
					net.kyori.adventure.sound.Sound.Source.PLAYER,
					net.kyori.adventure.key.Key.key("minecraft:ui.button.click"), 1, 1);
			new BiomeMenu(player);
		}
	}

	public class WarpedForestBiomeItem extends AbstractItem {

		private UUID playerUUID;

		public WarpedForestBiomeItem(UUID playerUUID) {
			this.playerUUID = playerUUID;
		}

		@Override
		public ItemProvider getItemProvider() {
			OnlineUser onlineUser = HellblockPlugin.getInstance().getStorageManager().getOnlineUser(playerUUID);
			if (onlineUser.getHellblockData().getBiome() == HellBiome.WARPED_FOREST) {
				return new ItemBuilder(Material.WARPED_STEM).addEnchantment(Enchantment.UNBREAKING, 1, false)
						.addItemFlags(ItemFlag.HIDE_ENCHANTS)
						.setDisplayName(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
								.getAdventureManager().getComponentFromMiniMessage("<aqua>Warped Forest")))
						.addLoreLines(
								new ShadedAdventureComponentWrapper(
										HellblockPlugin.getInstance().getAdventureManager().getComponentFromMiniMessage(
												"<blue>Click to change your biome to Warped Forest!")),
								new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
										.getComponentFromMiniMessage(" ")),
								new ShadedAdventureComponentWrapper(
										HellblockPlugin.getInstance().getAdventureManager().getComponentFromMiniMessage(
												"<light_purple>This is your currently selected biome!")));
			} else {
				return new ItemBuilder(Material.WARPED_STEM)
						.setDisplayName(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
								.getAdventureManager().getComponentFromMiniMessage("<aqua>Warped Forest")))
						.addLoreLines(new ShadedAdventureComponentWrapper(
								HellblockPlugin.getInstance().getAdventureManager().getComponentFromMiniMessage(
										"<blue>Click to change your biome to Warped Forest!")));
			}

		}

		@Override
		public void handleClick(@NotNull ClickType clickType, @NotNull Player player,
				@NotNull InventoryClickEvent event) {
			OnlineUser onlineUser = HellblockPlugin.getInstance().getStorageManager()
					.getOnlineUser(player.getUniqueId());
			if (onlineUser == null)
				return;
			if (onlineUser.getHellblockData().getBiome() == HellBiome.WARPED_FOREST) {
				HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
						String.format("<red>Your hellblock biome is already set to <dark_red>%s<red>!",
								HellBiome.WARPED_FOREST.getName()));
				HellblockPlugin.getInstance().getAdventureManager().sendSound(player,
						net.kyori.adventure.sound.Sound.Source.PLAYER,
						net.kyori.adventure.key.Key.key("minecraft:entity.villager.no"), 1, 1);
				return;
			}
			if (onlineUser.getHellblockData().getBiomeCooldown() > 0) {
				HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
						String.format("<red>You've recently changed your hellbiome biome, you must wait for %s!",
								HellblockPlugin.getInstance()
										.getFormattedCooldown(onlineUser.getHellblockData().getBiomeCooldown())));
				HellblockPlugin.getInstance().getAdventureManager().sendSound(player,
						net.kyori.adventure.sound.Sound.Source.PLAYER,
						net.kyori.adventure.key.Key.key("minecraft:entity.villager.no"), 1, 1);
				return;
			}
			HellblockPlugin.getInstance().getBiomeHandler().changeHellblockBiome(onlineUser, HellBiome.WARPED_FOREST);
			HellblockPlugin.getInstance().getAdventureManager().sendSound(player,
					net.kyori.adventure.sound.Sound.Source.PLAYER,
					net.kyori.adventure.key.Key.key("minecraft:ui.button.click"), 1, 1);
			new BiomeMenu(player);
		}
	}

	public class NetherWastesBiomeItem extends AbstractItem {

		private UUID playerUUID;

		public NetherWastesBiomeItem(UUID playerUUID) {
			this.playerUUID = playerUUID;
		}

		@Override
		public ItemProvider getItemProvider() {
			OnlineUser onlineUser = HellblockPlugin.getInstance().getStorageManager().getOnlineUser(playerUUID);
			if (onlineUser.getHellblockData().getBiome() == HellBiome.NETHER_WASTES) {
				return new ItemBuilder(Material.NETHERRACK).addEnchantment(Enchantment.UNBREAKING, 1, false)
						.addItemFlags(ItemFlag.HIDE_ENCHANTS)
						.setDisplayName(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
								.getAdventureManager().getComponentFromMiniMessage("<aqua>Nether Wastes")))
						.addLoreLines(
								new ShadedAdventureComponentWrapper(
										HellblockPlugin.getInstance().getAdventureManager().getComponentFromMiniMessage(
												"<blue>Click to change your biome to Nether Wastes!")),
								new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
										.getComponentFromMiniMessage(" ")),
								new ShadedAdventureComponentWrapper(
										HellblockPlugin.getInstance().getAdventureManager().getComponentFromMiniMessage(
												"<light_purple>This is your currently selected biome!")));
			} else {
				return new ItemBuilder(Material.NETHERRACK)
						.setDisplayName(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
								.getAdventureManager().getComponentFromMiniMessage("<aqua>Nether Wastes")))
						.addLoreLines(new ShadedAdventureComponentWrapper(
								HellblockPlugin.getInstance().getAdventureManager().getComponentFromMiniMessage(
										"<blue>Click to change your biome to Nether Wastes!")));
			}
		}

		@Override
		public void handleClick(@NotNull ClickType clickType, @NotNull Player player,
				@NotNull InventoryClickEvent event) {
			OnlineUser onlineUser = HellblockPlugin.getInstance().getStorageManager()
					.getOnlineUser(player.getUniqueId());
			if (onlineUser == null)
				return;
			if (onlineUser.getHellblockData().getBiome() == HellBiome.NETHER_WASTES) {
				HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
						String.format("<red>Your hellblock biome is already set to <dark_red>%s<red>!",
								HellBiome.NETHER_WASTES.getName()));
				HellblockPlugin.getInstance().getAdventureManager().sendSound(player,
						net.kyori.adventure.sound.Sound.Source.PLAYER,
						net.kyori.adventure.key.Key.key("minecraft:entity.villager.no"), 1, 1);
				return;
			}
			if (onlineUser.getHellblockData().getBiomeCooldown() > 0) {
				HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
						String.format("<red>You've recently changed your hellbiome biome, you must wait for %s!",
								HellblockPlugin.getInstance()
										.getFormattedCooldown(onlineUser.getHellblockData().getBiomeCooldown())));
				HellblockPlugin.getInstance().getAdventureManager().sendSound(player,
						net.kyori.adventure.sound.Sound.Source.PLAYER,
						net.kyori.adventure.key.Key.key("minecraft:entity.villager.no"), 1, 1);
				return;
			}
			HellblockPlugin.getInstance().getBiomeHandler().changeHellblockBiome(onlineUser, HellBiome.NETHER_WASTES);
			HellblockPlugin.getInstance().getAdventureManager().sendSound(player,
					net.kyori.adventure.sound.Sound.Source.PLAYER,
					net.kyori.adventure.key.Key.key("minecraft:ui.button.click"), 1, 1);
			new BiomeMenu(player);
		}
	}

	public class BasaltDeltasBiomeItem extends AbstractItem {

		private UUID playerUUID;

		public BasaltDeltasBiomeItem(UUID playerUUID) {
			this.playerUUID = playerUUID;
		}

		@Override
		public ItemProvider getItemProvider() {
			OnlineUser onlineUser = HellblockPlugin.getInstance().getStorageManager().getOnlineUser(playerUUID);
			if (onlineUser.getHellblockData().getBiome() == HellBiome.BASALT_DELTAS) {
				return new ItemBuilder(Material.NETHERITE_BLOCK).addEnchantment(Enchantment.UNBREAKING, 1, false)
						.addItemFlags(ItemFlag.HIDE_ENCHANTS)
						.setDisplayName(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
								.getAdventureManager().getComponentFromMiniMessage("<aqua>NBasalt Deltas")))
						.addLoreLines(
								new ShadedAdventureComponentWrapper(
										HellblockPlugin.getInstance().getAdventureManager().getComponentFromMiniMessage(
												"<blue>Click to change your biome to Basalt Deltas!")),
								new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
										.getComponentFromMiniMessage(" ")),
								new ShadedAdventureComponentWrapper(
										HellblockPlugin.getInstance().getAdventureManager().getComponentFromMiniMessage(
												"<light_purple>This is your currently selected biome!")));
			} else {
				return new ItemBuilder(Material.NETHERITE_BLOCK)
						.setDisplayName(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
								.getAdventureManager().getComponentFromMiniMessage("<aqua>Basalt Deltas")))
						.addLoreLines(new ShadedAdventureComponentWrapper(
								HellblockPlugin.getInstance().getAdventureManager().getComponentFromMiniMessage(
										"<blue>Click to change your biome to Basalt Deltas!")));
			}
		}

		@Override
		public void handleClick(@NotNull ClickType clickType, @NotNull Player player,
				@NotNull InventoryClickEvent event) {
			OnlineUser onlineUser = HellblockPlugin.getInstance().getStorageManager()
					.getOnlineUser(player.getUniqueId());
			if (onlineUser == null)
				return;
			if (onlineUser.getHellblockData().getBiome() == HellBiome.BASALT_DELTAS) {
				HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
						String.format("<red>Your hellblock biome is already set to <dark_red>%s<red>!",
								HellBiome.BASALT_DELTAS.getName()));
				HellblockPlugin.getInstance().getAdventureManager().sendSound(player,
						net.kyori.adventure.sound.Sound.Source.PLAYER,
						net.kyori.adventure.key.Key.key("minecraft:entity.villager.no"), 1, 1);
				return;
			}
			if (onlineUser.getHellblockData().getBiomeCooldown() > 0) {
				HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
						String.format("<red>You've recently changed your hellbiome biome, you must wait for %s!",
								HellblockPlugin.getInstance()
										.getFormattedCooldown(onlineUser.getHellblockData().getBiomeCooldown())));
				HellblockPlugin.getInstance().getAdventureManager().sendSound(player,
						net.kyori.adventure.sound.Sound.Source.PLAYER,
						net.kyori.adventure.key.Key.key("minecraft:entity.villager.no"), 1, 1);
				return;
			}
			HellblockPlugin.getInstance().getBiomeHandler().changeHellblockBiome(onlineUser, HellBiome.BASALT_DELTAS);
			HellblockPlugin.getInstance().getAdventureManager().sendSound(player,
					net.kyori.adventure.sound.Sound.Source.PLAYER,
					net.kyori.adventure.key.Key.key("minecraft:ui.button.click"), 1, 1);
			new BiomeMenu(player);
		}
	}

	public class BackToMainMenuItem extends AbstractItem {

		@Override
		public ItemProvider getItemProvider() {
			return new ItemBuilder(Material.ORANGE_STAINED_GLASS_PANE).addAllItemFlags()
					.setDisplayName(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
							.getAdventureManager().getComponentFromMiniMessage("<gold>Return to Hellblock Menu")));
		}

		@Override
		public void handleClick(@NotNull ClickType clickType, @NotNull Player player,
				@NotNull InventoryClickEvent event) {
			new HellblockMenu(player);
			HellblockPlugin.getInstance().getAdventureManager().sendSound(player,
					net.kyori.adventure.sound.Sound.Source.PLAYER,
					net.kyori.adventure.key.Key.key("minecraft:ui.button.click"), 1, 1);
		}
	}
}
