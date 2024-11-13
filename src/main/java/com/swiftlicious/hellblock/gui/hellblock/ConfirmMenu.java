package com.swiftlicious.hellblock.gui.hellblock;

import java.util.Iterator;
import java.util.Optional;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.config.HBLocale;
import com.swiftlicious.hellblock.gui.icon.BackGroundItem;
import com.swiftlicious.hellblock.player.UserData;
import com.swiftlicious.hellblock.utils.wrappers.ShadedAdventureComponentWrapper;

import lombok.NonNull;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.impl.AbstractItem;
import xyz.xenondevs.invui.window.Window;

public class ConfirmMenu {

	public ConfirmMenu(Player player, String action) {

		Optional<UserData> onlineUser = HellblockPlugin.getInstance().getStorageManager()
				.getOnlineUser(player.getUniqueId());
		if (onlineUser.isEmpty()) {
			HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
					"<red>Still loading your player data... please try again in a few seconds.");
			return;
		}

		Gui gui = Gui.normal().setStructure(" # c # d # ").addIngredient('c', new ConfirmItem(action))
				.addIngredient('d', new DenyItem(action)).addIngredient('#', new BackGroundItem()).build();

		Window window = Window
				.single().setViewer(player).setTitle(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
						.getAdventureManager().getComponentFromMiniMessage("<red>Hellblock Confirm Menu")))
				.setGui(gui).build();

		window.open();
	}

	public class ConfirmItem extends AbstractItem {

		private final String action;

		public ConfirmItem(@NonNull String action) {
			this.action = action;
		}

		@Override
		public ItemProvider getItemProvider() {
			return new ItemBuilder(Material.LIME_WOOL).addAllItemFlags()
					.setDisplayName(
							new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
									.getComponentFromMiniMessage(String.format("<green>Confirm %s Action", action))))
					.addLoreLines(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
							.getAdventureManager().getComponentFromMiniMessage("<dark_green>Reset your Hellblock!")));
		}

		@Override
		public void handleClick(@NotNull ClickType clickType, @NotNull Player player,
				@NotNull InventoryClickEvent event) {
			Optional<UserData> onlineUser = HellblockPlugin.getInstance().getStorageManager()
					.getOnlineUser(player.getUniqueId());
			if (onlineUser.isEmpty())
				return;
			if (!onlineUser.get().getHellblockData().hasHellblock()) {
				HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
						HBLocale.MSG_Hellblock_Not_Found);
				HellblockPlugin.getInstance().getAdventureManager().sendSound(player,
						net.kyori.adventure.sound.Sound.Source.PLAYER,
						net.kyori.adventure.key.Key.key("minecraft:entity.villager.no"), 1, 1);
				return;
			}
			if (onlineUser.get().getHellblockData().getOwnerUUID() == null) {
				throw new NullPointerException("Owner reference returned null, please report this to the developer.");
			}
			if (onlineUser.get().getHellblockData().getOwnerUUID() != null
					&& !onlineUser.get().getHellblockData().getOwnerUUID().equals(player.getUniqueId())) {
				HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
						HBLocale.MSG_Not_Owner_Of_Hellblock);
				HellblockPlugin.getInstance().getAdventureManager().sendSound(player,
						net.kyori.adventure.sound.Sound.Source.PLAYER,
						net.kyori.adventure.key.Key.key("minecraft:entity.villager.no"), 1, 1);
				return;
			}
			if (onlineUser.get().getHellblockData().isAbandoned()) {
				HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
						HBLocale.MSG_Hellblock_Is_Abandoned);
				HellblockPlugin.getInstance().getAdventureManager().sendSound(player,
						net.kyori.adventure.sound.Sound.Source.PLAYER,
						net.kyori.adventure.key.Key.key("minecraft:entity.villager.no"), 1, 1);
				return;
			}
			if (onlineUser.get().getHellblockData().getResetCooldown() > 0) {
				HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
						String.format("<red>You've recently reset your hellblock already, you must wait for %s!",
								HellblockPlugin.getInstance()
										.getFormattedCooldown(onlineUser.get().getHellblockData().getResetCooldown())));
				HellblockPlugin.getInstance().getAdventureManager().sendSound(player,
						net.kyori.adventure.sound.Sound.Source.PLAYER,
						net.kyori.adventure.key.Key.key("minecraft:entity.villager.no"), 1, 1);
				return;
			}

			HellblockPlugin.getInstance().getScheduler().sync().runLater(() -> {
				for (Iterator<Window> windows = getWindows().iterator(); windows.hasNext();) {
					Window window = windows.next();
					if (window.getViewerUUID().equals(player.getUniqueId())) {
						window.close();
					}
				}
			}, 1 * 20L, player.getLocation());
			HellblockPlugin.getInstance().getHellblockHandler().resetHellblock(player.getUniqueId(), false);
			HellblockPlugin.getInstance().getAdventureManager().sendSound(player,
					net.kyori.adventure.sound.Sound.Source.PLAYER,
					net.kyori.adventure.key.Key.key("minecraft:ui.button.click"), 1, 1);
		}
	}

	public class DenyItem extends AbstractItem {

		private final String action;

		public DenyItem(String action) {
			this.action = action;
		}

		@Override
		public ItemProvider getItemProvider() {
			return new ItemBuilder(Material.RED_WOOL).addAllItemFlags()
					.setDisplayName(
							new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
									.getComponentFromMiniMessage(String.format("<red>Deny %s Action", action))))
					.addLoreLines(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
							.getAdventureManager().getComponentFromMiniMessage("<dark_red>Return to Main Menu!")));
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
