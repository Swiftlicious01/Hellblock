package com.swiftlicious.hellblock.gui.hellblock;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.jetbrains.annotations.NotNull;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.gui.icon.BackGroundItem;
import com.swiftlicious.hellblock.playerdata.HellblockPlayer;
import com.swiftlicious.hellblock.playerdata.UUIDFetcher;
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

		int maxPartySize = HellblockPlugin.getInstance().getCoopManager().getPartySizeLimit();
		StringBuilder partyLayout = new StringBuilder();
		partyLayout.append(" o");
		for (int i = 0; i < maxPartySize; i++) {
			partyLayout.append(" m");
		}
		int layout = 0;
		for (char item : partyLayout.toString().toCharArray()) {
			if (item == ' ')
				continue;
			layout++;
		}
		if (layout < 8) {
			for (int i = layout; i < 8; i++) {
				partyLayout.append(" #");
			}
		} else {
			partyLayout.append(",");
			for (int i = layout; i < 17; i++) {
				partyLayout.append(" #");
			}
		}
		partyLayout.append(" x ");

		String[] party = partyLayout.toString().split(",");

		Gui gui = Gui.normal().setStructure(party).addIngredient('o', new OwnerItem(player.getUniqueId()))
				.addIngredient('m', new MemberItem(player.getUniqueId())).addIngredient('#', new BackGroundItem())
				.addIngredient('x', new BackToMainMenuItem()).build();

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
			if (owner != null) {
				try {
					return new SkullBuilder(owner)
							.setDisplayName(new ShadedAdventureComponentWrapper(
									HellblockPlugin.getInstance().getAdventureManager()
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
			return new ItemBuilder(Material.BARRIER).setDisplayName(
					new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
							.getComponentFromMiniMessage("<dark_red>Broken, please report this.")));
		}

		@Override
		public void handleClick(@NotNull ClickType clickType, @NotNull Player player,
				@NotNull InventoryClickEvent event) {
		}
	}

	public class MemberItem extends AbstractItem {

		private UUID playerUUID;
		private String input;

		public MemberItem(UUID playerUUID) {
			this.playerUUID = playerUUID;
		}

		@Override
		public ItemProvider getItemProvider() {
			HellblockPlayer hbPlayer = HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(playerUUID);
			UUID owner = hbPlayer.getHellblockOwner();
			Set<UUID> party = hbPlayer.getHellblockParty();
			if (!party.isEmpty()) {
				for (UUID uuid : party) {
					try {
						input = Bukkit.getPlayer(uuid) != null ? Bukkit.getPlayer(uuid).getName()
								: Bukkit.getOfflinePlayer(uuid).hasPlayedBefore()
										&& Bukkit.getOfflinePlayer(uuid).getName() != null
												? Bukkit.getOfflinePlayer(uuid).getName()
												: "Unknown";
						SkullBuilder currentMember = new SkullBuilder(uuid)
								.setDisplayName(new ShadedAdventureComponentWrapper(
										HellblockPlugin.getInstance().getAdventureManager()
												.getComponentFromMiniMessage(String.format("<aqua>%s", input))))
								.addLoreLines(new ShadedAdventureComponentWrapper(
										HellblockPlugin.getInstance().getAdventureManager()
												.getComponentFromMiniMessage("<yellow>Role: <gold>Member")));
						if (owner != null && owner.equals(playerUUID)) {
							currentMember.addLoreLines(
									new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
											.getAdventureManager().getComponentFromMiniMessage(" ")),
									new ShadedAdventureComponentWrapper(
											HellblockPlugin.getInstance().getAdventureManager()
													.getComponentFromMiniMessage("<red>Click to kick them!")));
						}
						return currentMember;
					} catch (MojangApiException | IOException e) {
						LogUtils.severe("Failed to create party member player heads!", e);
						return new ItemBuilder(Material.BARRIER).setDisplayName(
								new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
										.getComponentFromMiniMessage("<dark_red>Broken, please report this.")));
					}
				}
			}
			try {
				SkullBuilder newMember = new SkullBuilder(HeadTexture.of("MHF_QUESTION")).setUnbreakable(true)
						.addItemFlags(ItemFlag.HIDE_UNBREAKABLE)
						.setDisplayName(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
								.getAdventureManager().getComponentFromMiniMessage("<dark_green>Empty Slot")));
				if (owner != null && owner.equals(playerUUID)) {
					newMember.addLoreLines(
							new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
									.getComponentFromMiniMessage("<green>Click to invite a new member!")));
				}
				return newMember;
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
			HellblockPlayer hbPlayer = HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(player);
			UUID owner = hbPlayer.getHellblockOwner();
			if (owner != null && owner.equals(player.getUniqueId())) {
				if (event.getCurrentItem() != null && event.getCurrentItem().getType() == Material.PLAYER_HEAD) {
					if (event.getCurrentItem().hasItemMeta() && event.getCurrentItem().getItemMeta().isUnbreakable()) {
						new InvitationMenu(player);
					} else {
						if (!input.equals("Unknown")) {
							HellblockPlugin.getInstance().getCoopManager().removeMemberFromHellblock(hbPlayer, input,
									UUIDFetcher.getUUID(input));
							new CoopMenu(player);
						}
					}
					HellblockPlugin.getInstance().getAdventureManager().sendSound(player,
							net.kyori.adventure.sound.Sound.Source.PLAYER,
							net.kyori.adventure.key.Key.key("minecraft:ui.button.click"), 1, 1);
				}
			}
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
