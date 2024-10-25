package com.swiftlicious.hellblock.gui.hellblock;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.playerdata.HellblockPlayer;
import com.swiftlicious.hellblock.utils.LogUtils;
import com.swiftlicious.hellblock.utils.wrappers.ShadedAdventureComponentWrapper;

import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.builder.SkullBuilder;
import xyz.xenondevs.invui.item.builder.SkullBuilder.HeadTexture;
import xyz.xenondevs.invui.item.impl.AbstractItem;
import xyz.xenondevs.invui.util.MojangApiUtils.MojangApiException;
import xyz.xenondevs.invui.window.Window;

public class CoopMenu {

	public CoopMenu(Player player) {

		Gui gui = Gui.normal().setStructure(" o m m m m ").addIngredient('o', new OwnerItem(player.getUniqueId()))
				.addIngredient('m', new MemberItem(player.getUniqueId())).build();

		Window window = Window
				.single().setViewer(player).setTitle(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
						.getAdventureManager().getComponentFromMiniMessage("<red>Hellblock Party Members")))
				.setGui(gui).build();

		window.open();
	}

	public class OwnerItem extends AbstractItem {

		private UUID playerUUID;

		public OwnerItem(UUID playerUUID) {
			this.playerUUID = playerUUID;
		}

		@Override
		public ItemProvider getItemProvider() {
			HellblockPlayer hbPlayer = HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(playerUUID);
			UUID owner = hbPlayer.getHellblockOwner();
			try {
				return new SkullBuilder(owner)
						.setDisplayName(
								new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
										.getComponentFromMiniMessage(String.format("<aqua>%s",
												(Bukkit.getOfflinePlayer(owner).hasPlayedBefore()
														&& Bukkit.getOfflinePlayer(owner).getName() != null
																? Bukkit.getOfflinePlayer(owner).getName()
																: "Unknown")))))
						.addLoreLines(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
								.getAdventureManager().getComponentFromMiniMessage("<yellow>Role: <gold>Owner")));
			} catch (MojangApiException | IOException e) {
				LogUtils.severe("Failed to create owner player head!", e);
				return new ItemBuilder(Material.BARRIER).setDisplayName(
						new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
								.getComponentFromMiniMessage("<dark_red>Broken, please report this.")));
			}
		}

		@Override
		public void handleClick(@NotNull ClickType clickType, @NotNull Player player,
				@NotNull InventoryClickEvent event) {
		}
	}

	public class MemberItem extends AbstractItem {

		private UUID playerUUID;
		private NamespacedKey inviteKey;

		public MemberItem(UUID playerUUID) {
			this.playerUUID = playerUUID;
			this.inviteKey = new NamespacedKey(HellblockPlugin.getInstance(), "invite-allowed");
		}

		@Override
		public ItemProvider getItemProvider() {
			HellblockPlayer hbPlayer = HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(playerUUID);
			List<UUID> party = hbPlayer.getHellblockParty();
			for (UUID uuid : party) {
				try {
					return new SkullBuilder(uuid)
							.setDisplayName(new ShadedAdventureComponentWrapper(
									HellblockPlugin.getInstance().getAdventureManager()
											.getComponentFromMiniMessage(String.format("<aqua>%s",
													(Bukkit.getOfflinePlayer(uuid).hasPlayedBefore()
															&& Bukkit.getOfflinePlayer(uuid).getName() != null
																	? Bukkit.getOfflinePlayer(uuid).getName()
																	: "Unknown")))))
							.addLoreLines(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
									.getAdventureManager().getComponentFromMiniMessage("<yellow>Role: <gold>Member")));
				} catch (MojangApiException | IOException e) {
					LogUtils.severe("Failed to create party member player heads!", e);
					return new ItemBuilder(Material.BARRIER).setDisplayName(
							new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
									.getComponentFromMiniMessage("<dark_red>Broken, please report this.")));
				}
			}
			try {
				SkullBuilder item = new SkullBuilder(HeadTexture.of("MHF_QUESTION"))
						.setDisplayName(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
								.getAdventureManager().getComponentFromMiniMessage("<dark_green>Empty Slot")))
						.addLoreLines(
								new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
										.getComponentFromMiniMessage("<green>Click to invite a new member!")));
				item.get().getItemMeta().getPersistentDataContainer().set(inviteKey, PersistentDataType.BOOLEAN, true);
				item.get().setItemMeta(item.get().getItemMeta());
				ItemBuilder update = new ItemBuilder(item.get());
				return update;
			} catch (MojangApiException | IOException e) {
				LogUtils.severe("Failed to create question mark player heads!", e);
				return new ItemBuilder(Material.BARRIER).setDisplayName(
						new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
								.getComponentFromMiniMessage("<dark_red>Broken, please report this.")));
			}
		}

		@Override
		public void handleClick(@NotNull ClickType clickType, @NotNull Player player,
				@NotNull InventoryClickEvent event) {
			if (event.getCurrentItem() != null
					&& event.getCurrentItem().getPersistentDataContainer().has(inviteKey, PersistentDataType.BOOLEAN)) {
				new InvitationMenu(player);
			}
		}
	}
}
