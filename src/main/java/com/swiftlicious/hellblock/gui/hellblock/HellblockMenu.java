package com.swiftlicious.hellblock.gui.hellblock;

import java.util.UUID;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.gui.icon.BackGroundItem;
import com.swiftlicious.hellblock.playerdata.HellblockPlayer;
import com.swiftlicious.hellblock.utils.LocationUtils;
import com.swiftlicious.hellblock.utils.wrappers.ShadedAdventureComponentWrapper;

import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.impl.AbstractItem;
import xyz.xenondevs.invui.window.Window;

public class HellblockMenu {

	public HellblockMenu(Player player) {

		if (HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(player).hasHellblock()) {
			Gui gui = Gui.normal().setStructure("# c t p b l f r #").addIngredient('c', new CreateIslandItem())
					.addIngredient('t', new TeleportIslandItem()).addIngredient('p', new ViewPartyMembersItem())
					.addIngredient('b', new BiomeItem(player.getUniqueId()))
					.addIngredient('l', new LockIslandItem(player.getUniqueId()))
					.addIngredient('r', new ResetIslandItem(player.getUniqueId()))
					.addIngredient('f', new ProtectionFlagItem()).addIngredient('#', new BackGroundItem()).build();

			Window window = Window
					.single().setViewer(player).setTitle(new ShadedAdventureComponentWrapper(HellblockPlugin
							.getInstance().getAdventureManager().getComponentFromMiniMessage("<red>Hellblock Menu")))
					.setGui(gui).build();

			window.open();
		} else {
			new IslandChoiceMenu(player, false);
		}
	}

	public class ProtectionFlagItem extends AbstractItem {

		@Override
		public ItemProvider getItemProvider() {
			return new ItemBuilder(Material.REPEATER).addAllItemFlags()
					.setDisplayName(
							new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
									.getComponentFromMiniMessage("<green>Change your Hellblock Protection Flags!")))
					.addLoreLines(new ShadedAdventureComponentWrapper(
							HellblockPlugin.getInstance().getAdventureManager().getComponentFromMiniMessage(
									"<aqua>Click to change your hellblock protection flags!")));
		}

		@Override
		public void handleClick(@NotNull ClickType clickType, @NotNull Player player,
				@NotNull InventoryClickEvent event) {
			new FlagMenu(player);
		}
	}

	public class CreateIslandItem extends AbstractItem {

		@Override
		public ItemProvider getItemProvider() {
			return new ItemBuilder(Material.SOUL_SAND).addAllItemFlags()
					.setDisplayName(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
							.getAdventureManager().getComponentFromMiniMessage("<green>Create your Hellblock!")))
					.addLoreLines(new ShadedAdventureComponentWrapper(
							HellblockPlugin.getInstance().getAdventureManager().getComponentFromMiniMessage(
									"<aqua>Click to view the options to create your very own hellblock!")));
		}

		@Override
		public void handleClick(@NotNull ClickType clickType, @NotNull Player player,
				@NotNull InventoryClickEvent event) {
			HellblockPlayer pi = HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(player);
			if (!pi.hasHellblock()) {
				if (pi.getResetCooldown() > 0) {
					HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
							String.format("<red>You have recently reset your hellblock already, you must wait for %s!",
									HellblockPlugin.getInstance().getFormattedCooldown(pi.getResetCooldown())));
					return;
				}
				new IslandChoiceMenu(player, false);
			} else {
				HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
						"<red>You already have a hellblock!");
			}
		}
	}

	public class LockIslandItem extends AbstractItem {

		private UUID playerUUID;

		public LockIslandItem(UUID playerUUID) {
			this.playerUUID = playerUUID;
		}

		@Override
		public ItemProvider getItemProvider() {
			return new ItemBuilder(Material.TRAPPED_CHEST).addAllItemFlags()
					.setDisplayName(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
							.getAdventureManager()
							.getComponentFromMiniMessage(String.format("<green>%s your Hellblock!",
									(HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(playerUUID)
											.getLockedStatus() ? "Unlock" : "Lock")))))
					.addLoreLines(new ShadedAdventureComponentWrapper(
							HellblockPlugin.getInstance().getAdventureManager().getComponentFromMiniMessage(
									"<aqua>Click to change the visitor status of your hellblock!")));
		}

		@Override
		public void handleClick(@NotNull ClickType clickType, @NotNull Player player,
				@NotNull InventoryClickEvent event) {
			HellblockPlayer pi = HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(player);
			if (pi.hasHellblock()) {
				if (pi.getHellblockOwner() != null && !pi.getHellblockOwner().equals(player.getUniqueId())) {
					HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
							"<red>Only the owner of the hellblock island can change this!");
					return;
				}
				pi.setLockedStatus(!pi.getLockedStatus());
				HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
						String.format("<red>You have just <dark_red>%s <red>your hellblock island!",
								(pi.getLockedStatus() ? "locked" : "unlocked")));
				if (pi.getLockedStatus()) {
					HellblockPlugin.getInstance().getCoopManager().kickVisitorsIfLocked(player.getUniqueId());
					HellblockPlugin.getInstance().getCoopManager().changeLockStatus(player);
				}
				new HellblockMenu(player);
			} else {
				HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
						"<red>You don't have a hellblock!");
			}
		}
	}

	public class ViewPartyMembersItem extends AbstractItem {

		@Override
		public ItemProvider getItemProvider() {
			return new ItemBuilder(Material.BEACON).addAllItemFlags()
					.setDisplayName(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
							.getAdventureManager().getComponentFromMiniMessage("<green>View your party members!")))
					.addLoreLines(
							new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
									.getComponentFromMiniMessage("<aqua>Click to see a list of your party members!")));
		}

		@Override
		public void handleClick(@NotNull ClickType clickType, @NotNull Player player,
				@NotNull InventoryClickEvent event) {
			HellblockPlayer pi = HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(player);
			if (pi.hasHellblock()) {
				new CoopMenu(player);
			} else {
				HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
						"<red>You don't have a hellblock!");
			}
		}
	}

	public class BiomeItem extends AbstractItem {

		private UUID playerUUID;

		public BiomeItem(UUID playerUUID) {
			this.playerUUID = playerUUID;
		}

		@Override
		public ItemProvider getItemProvider() {
			HellblockPlayer hbPlayer = HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(playerUUID);
			if (hbPlayer.getBiomeCooldown() == 0) {
				return new ItemBuilder(Material.NETHER_WART).addAllItemFlags()
						.setDisplayName(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
								.getAdventureManager().getComponentFromMiniMessage("<green>Change island biome!")))
						.addLoreLines(
								new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
										.getComponentFromMiniMessage("<aqua>Click to view all of the biome options!")));
			} else {
				return new ItemBuilder(Material.BARRIER).addAllItemFlags()
						.setDisplayName(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
								.getAdventureManager().getComponentFromMiniMessage("<red>Reset on Cooldown!")))
						.addLoreLines(new ShadedAdventureComponentWrapper(
								HellblockPlugin.getInstance().getAdventureManager().getComponentFromMiniMessage(
										"<dark_red>Your ability to change your biome is on cooldown!")));
			}
		}

		@Override
		public void handleClick(@NotNull ClickType clickType, @NotNull Player player,
				@NotNull InventoryClickEvent event) {
			HellblockPlayer pi = HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(player);
			if (event.getCurrentItem() != null && event.getCurrentItem().getType() == Material.NETHER_WART) {
				if (pi.hasHellblock()) {
					new BiomeMenu(player);
				} else {
					HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
							"<red>You don't have a hellblock!");
				}
			} else {
				if (pi.getBiomeCooldown() > 0) {
					HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
							String.format("<red>You have recently changed your biome already, you must wait for %s!",
									HellblockPlugin.getInstance().getFormattedCooldown(pi.getBiomeCooldown())));
					return;
				}
			}
		}
	}

	public class TeleportIslandItem extends AbstractItem {

		@Override
		public ItemProvider getItemProvider() {
			return new ItemBuilder(Material.FIRE_CHARGE).addAllItemFlags()
					.setDisplayName(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
							.getAdventureManager().getComponentFromMiniMessage("<green>Teleport to your Hellblock!")))
					.addLoreLines(
							new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
									.getComponentFromMiniMessage("<aqua>Click to go to your home island!")));
		}

		@Override
		public void handleClick(@NotNull ClickType clickType, @NotNull Player player,
				@NotNull InventoryClickEvent event) {
			HellblockPlayer pi = HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(player);
			if (!pi.hasHellblock()) {
				HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
						"<red>You don't have a hellblock!");
			} else {
				if (pi.getHomeLocation() != null) {
					if (!LocationUtils.isSafeLocation(pi.getHomeLocation())) {
						HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
								"<red>This hellblock home location was deemed not safe, resetting to bedrock location!");
						pi.setHome(HellblockPlugin.getInstance().getHellblockHandler()
								.locateBedrock(player.getUniqueId()));
						HellblockPlugin.getInstance().getCoopManager().updateParty(player.getUniqueId(), "home",
								pi.getHomeLocation());
					}
					HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
							"<red>Teleporting you to your hellblock!");
					player.teleportAsync(pi.getHomeLocation());
					// if raining give player a bit of protection
					if (HellblockPlugin.getInstance().getLavaRain().getLavaRainTask() != null
							&& HellblockPlugin.getInstance().getLavaRain().getLavaRainTask().isLavaRaining()
							&& HellblockPlugin.getInstance().getLavaRain().getHighestBlock(player.getLocation()) != null
							&& !HellblockPlugin.getInstance().getLavaRain().getHighestBlock(player.getLocation())
									.isEmpty()) {
						player.setNoDamageTicks(5 * 20);
					}
				} else {
					HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
							"<red>Error teleporting you to your hellblock!");
				}
			}
		}
	}

	public class ResetIslandItem extends AbstractItem {

		private UUID playerUUID;

		public ResetIslandItem(UUID playerUUID) {
			this.playerUUID = playerUUID;
		}

		@Override
		public ItemProvider getItemProvider() {
			HellblockPlayer hbPlayer = HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(playerUUID);
			if (hbPlayer.getResetCooldown() == 0) {
				return new ItemBuilder(Material.NETHER_BRICKS).addAllItemFlags()
						.setDisplayName(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
								.getAdventureManager().getComponentFromMiniMessage("<green>Reset your Hellblock!")))
						.addLoreLines(
								new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
										.getComponentFromMiniMessage("<aqua>Click to reset your hellblock island!")));
			} else {
				return new ItemBuilder(Material.BARRIER).addAllItemFlags()
						.setDisplayName(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
								.getAdventureManager().getComponentFromMiniMessage("<red>Reset on Cooldown!")))
						.addLoreLines(new ShadedAdventureComponentWrapper(
								HellblockPlugin.getInstance().getAdventureManager().getComponentFromMiniMessage(
										"<dark_red>Your ability to reset your hellblock is on cooldown!")));
			}
		}

		@Override
		public void handleClick(@NotNull ClickType clickType, @NotNull Player player,
				@NotNull InventoryClickEvent event) {
			HellblockPlayer pi = HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(player);
			if (event.getCurrentItem() != null && event.getCurrentItem().getType() == Material.NETHER_BRICKS) {
				new ConfirmMenu(player, "Reset");
			} else {
				if (pi.getResetCooldown() > 0) {
					HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
							String.format("<red>You have recently reset your hellblock already, you must wait for %s!",
									HellblockPlugin.getInstance().getFormattedCooldown(pi.getResetCooldown())));
					return;
				}
			}
		}
	}
}
