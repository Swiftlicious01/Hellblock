package com.swiftlicious.hellblock.gui.hellblock;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.gui.icon.BackGroundItem;
import com.swiftlicious.hellblock.gui.icon.NextPageItem;
import com.swiftlicious.hellblock.gui.icon.PreviousPageItem;
import com.swiftlicious.hellblock.playerdata.HellblockPlayer;
import com.swiftlicious.hellblock.utils.LogUtils;
import com.swiftlicious.hellblock.utils.wrappers.ShadedAdventureComponentWrapper;

import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.gui.PagedGui;
import xyz.xenondevs.invui.gui.structure.Markers;
import xyz.xenondevs.invui.item.Item;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.builder.SkullBuilder;
import xyz.xenondevs.invui.item.builder.SkullBuilder.HeadTexture;
import xyz.xenondevs.invui.item.impl.AbstractItem;
import xyz.xenondevs.invui.item.impl.SimpleItem;
import xyz.xenondevs.invui.util.MojangApiUtils.MojangApiException;
import xyz.xenondevs.invui.window.AnvilWindow;

public class InvitationMenu {

	private final String SEARCH;
	private final Player player;
	private String prefix;
	private long coolDown;

	public InvitationMenu(Player player) {
		this.player = player;
		this.SEARCH = "Type Name Here";
		this.prefix = SEARCH;
		this.updateMenu(SEARCH);
	}

	public void updateMenu(String search) {
		var confirmIcon = new ConfirmIcon();
		Item border = new SimpleItem(new ItemBuilder(Material.AIR));
		Gui upperGui = Gui.normal().setStructure("a # b")
				.addIngredient('a', new SimpleItem(new ItemBuilder(Material.NAME_TAG).setDisplayName(search)))
				.addIngredient('#', border).addIngredient('b', confirmIcon).build();

		var gui = PagedGui.items()
				.setStructure("x x x x x x x x x", "x x x x x x x x x", "x x x x x x x x x", "# # a # # # b # #")
				.addIngredient('x', Markers.CONTENT_LIST_SLOT_HORIZONTAL).addIngredient('#', new BackGroundItem())
				.addIngredient('a', new PreviousPageItem()).addIngredient('b', new NextPageItem())
				.setContent(getItemList()).build();

		var window = AnvilWindow
				.split().setViewer(player).setTitle(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
						.getAdventureManager().getComponentFromMiniMessage("<red>Hellblock Invitations")))
				.addRenameHandler(s -> {
					long current = System.currentTimeMillis();
					if (current - coolDown < 100)
						return;
					if (s.equals(search))
						return;
					prefix = s;
					coolDown = current;
					confirmIcon.notifyWindows();
					updateMenu(s);
				}).setUpperGui(upperGui).setLowerGui(gui).build();

		window.open();
	}

	public List<Item> getItemList() {
		List<Item> itemList = new ArrayList<>();
		for (Entry<UUID, HellblockPlayer> entry : HellblockPlugin.getInstance().getHellblockHandler().getActivePlayers()
				.entrySet()) {
			UUID key = entry.getKey();
			if (entry.getValue() instanceof HellblockPlayer hbPlayer) {
				if (!prefix.equals(SEARCH))
					continue;
				if (key.equals(player.getUniqueId()))
					continue;
				if (hbPlayer.getPlayer() == null || !hbPlayer.getPlayer().isOnline())
					continue;
				SkullBuilder skullBuilder = null;
				try {
					skullBuilder = new SkullBuilder(hbPlayer.getPlayer().getUniqueId());
				} catch (MojangApiException | IOException ex) {
					LogUtils.warn(String.format("Unable to retrieve skull data for the player %s", prefix), ex);
				}
				itemList.add(new ItemInList(hbPlayer.getPlayer().getName(), skullBuilder, this));
				continue;
			}
			try {
				itemList.add(new ItemInList("???", new SkullBuilder(HeadTexture.of("MHF_QUESTION")), this));
			} catch (MojangApiException | IOException ignored) {
				// ignored
			}
		}
		return itemList;
	}

	public static class ItemInList extends AbstractItem {

		private String key;
		private final SkullBuilder skullBuilder;
		private final InvitationMenu InvitationMenu;

		public ItemInList(String key, SkullBuilder skullBuilder, InvitationMenu invitationMenu) {
			this.key = key;
			this.skullBuilder = skullBuilder
					.setDisplayName(new ShadedAdventureComponentWrapper(
							HellblockPlugin.getInstance().getAdventureManager().getComponentFromMiniMessage(key)))
					.addLoreLines(
							new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
									.getComponentFromMiniMessage("<light_purple>Click to invite this player!")));
			this.InvitationMenu = invitationMenu;
		}

		@Override
		public ItemProvider getItemProvider() {
			return skullBuilder;
		}

		@Override
		public void handleClick(@NotNull ClickType clickType, @NotNull Player player,
				@NotNull InventoryClickEvent event) {
			if (clickType.isRightClick()) {
				this.InvitationMenu.updateMenu(key);
			}
		}
	}

	public class ConfirmIcon extends AbstractItem {

		@Override
		public ItemProvider getItemProvider() {
			if (prefix != null && prefix.matches("^[a-zA-Z0-9_]+$")
					&& HellblockPlugin.getInstance().getHellblockHandler().getActivePlayers().values().stream()
							.filter(hbPlayer -> hbPlayer.getPlayer() != null
									&& !hbPlayer.getPlayer().getUniqueId().equals(player.getUniqueId()))
							.map(hbPlayer -> hbPlayer.getPlayer().getName()).collect(Collectors.toList())
							.contains(prefix)) {
				SkullBuilder builder = null;
				try {
					builder = new SkullBuilder(prefix);
				} catch (MojangApiException | IOException ex) {
					LogUtils.warn(String.format("Unable to retrieve skull data for the player %s", prefix), ex);
					try {
						builder = new SkullBuilder("MHF_QUESTION");
					} catch (MojangApiException | IOException ignored) {
						// ignored
					}
				}
				builder.setDisplayName(new ShadedAdventureComponentWrapper(
						HellblockPlugin.getInstance().getAdventureManager().getComponentFromMiniMessage(prefix)));
				builder.addLoreLines(
						new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
								.getComponentFromMiniMessage("<light_purple>Click to invite this player!")));
				return builder;
			} else {
				return new ItemBuilder(Material.BARRIER).setDisplayName(
						new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
								.getComponentFromMiniMessage("<red>No player online with that username!")));
			}
		}

		@Override
		public void handleClick(@NotNull ClickType clickType, @NotNull Player player,
				@NotNull InventoryClickEvent event) {
			if (prefix != null && prefix.matches("^[a-zA-Z0-9_]+$")
					&& HellblockPlugin.getInstance().getHellblockHandler().getActivePlayers().values().stream()
							.filter(hbPlayer -> hbPlayer.getPlayer() != null
									&& !hbPlayer.getPlayer().getUniqueId().equals(player.getUniqueId()))
							.map(hbPlayer -> hbPlayer.getPlayer().getName()).collect(Collectors.toList())
							.contains(prefix)) {
				if (Bukkit.getPlayer(prefix) == null || !Bukkit.getPlayer(prefix).isOnline()) {
					LogUtils.warn(String.format("Unable to invite player %s because they returned null.", prefix));
					return;
				}
				HellblockPlayer hbPlayer = HellblockPlugin.getInstance().getHellblockHandler()
						.getActivePlayer(player.getUniqueId());
				HellblockPlayer invitingPlayer = HellblockPlugin.getInstance().getHellblockHandler()
						.getActivePlayer(Bukkit.getPlayer(prefix).getUniqueId());
				HellblockPlugin.getInstance().getCoopManager().addMemberToHellblock(hbPlayer, invitingPlayer);
			} else {
				return;
			}
			prefix = SEARCH;
			new CoopMenu(player);
		}
	}
}
