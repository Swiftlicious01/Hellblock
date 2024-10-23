package com.swiftlicious.hellblock.gui.hellblock;

import java.io.IOException;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.config.HBLocale;
import com.swiftlicious.hellblock.playerdata.HellblockPlayer;
import com.swiftlicious.hellblock.utils.LogUtils;
import com.swiftlicious.hellblock.utils.wrappers.ShadedAdventureComponentWrapper;

import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.item.Item;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.builder.SkullBuilder;
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
		this.SEARCH = HBLocale.GUI_SEARCH;
		this.prefix = SEARCH;
		this.updateMenu(SEARCH);
	}

	public void updateMenu(String search) {
		var confirmIcon = new ConfirmIcon();
		Item border = new SimpleItem(new ItemBuilder(Material.AIR));
		Gui gui = Gui.normal().setStructure("a # b")
				.addIngredient('a', new SimpleItem(new ItemBuilder(Material.NAME_TAG).setDisplayName(search)))
				.addIngredient('b', confirmIcon).addIngredient('#', border).build();

		var window = AnvilWindow
				.single().setViewer(player).setTitle(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
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
				}).setGui(gui).build();

		window.open();
	}

	public class ConfirmIcon extends AbstractItem {

		@Override
		public ItemProvider getItemProvider() {
			if (prefix != null && prefix.matches("^[a-zA-Z0-9_]+$")
					&& HellblockPlugin.getInstance().getHellblockHandler().getActivePlayers().values().stream()
							.filter(hbPlayer -> hbPlayer.getPlayer() != null
									&& !hbPlayer.getPlayer().getName().equalsIgnoreCase(player.getName()))
							.map(hbPlayer -> hbPlayer.getPlayer().getName()).collect(Collectors.toList())
							.contains(prefix)) {
				SkullBuilder builder = null;
				try {
					builder = new SkullBuilder(prefix);
				} catch (MojangApiException | IOException ex) {
					LogUtils.warn(String.format("Unable to retrieve skull data for the player %s", prefix), ex);
				}
				builder.setDisplayName(new ShadedAdventureComponentWrapper(
						HellblockPlugin.getInstance().getAdventureManager().getComponentFromMiniMessage(prefix)));
				builder.addLoreLines(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
						.getAdventureManager().getComponentFromMiniMessage(HBLocale.GUI_CLICK_CONFIRM)))
						.addLoreLines(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
								.getAdventureManager().getComponentFromMiniMessage(HBLocale.GUI_RIGHT_CLICK_CANCEL)));
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
			if (clickType.isLeftClick()) {
				if (prefix != null && prefix.matches("^[a-zA-Z0-9_]+$")
						&& HellblockPlugin.getInstance().getHellblockHandler().getActivePlayers().values().stream()
								.filter(hbPlayer -> hbPlayer.getPlayer() != null
										&& !hbPlayer.getPlayer().getName().equalsIgnoreCase(player.getName()))
								.map(hbPlayer -> hbPlayer.getPlayer().getName()).collect(Collectors.toList())
								.contains(prefix)) {
					HellblockPlayer hbPlayer = HellblockPlugin.getInstance().getHellblockHandler()
							.getActivePlayer(player.getUniqueId());
					if (Bukkit.getPlayer(prefix) == null) {
						LogUtils.warn(String.format("Unable to invite player %s because they returned null.", prefix));
						return;
					}
					HellblockPlayer invitingPlayer = HellblockPlugin.getInstance().getHellblockHandler()
							.getActivePlayer(Bukkit.getPlayer(prefix).getUniqueId());
					HellblockPlugin.getInstance().getCoopManager().addMemberToHellblock(hbPlayer, invitingPlayer);
				} else {
					return;
				}
			}
			prefix = SEARCH;
			new CoopMenu(player);
		}
	}
}
