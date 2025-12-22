package com.swiftlicious.hellblock.gui.party;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.mojang.authlib.GameProfile;
import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.context.Context;
import com.swiftlicious.hellblock.creation.item.Item;
import com.swiftlicious.hellblock.gui.display.AbstractAnvilInputGUI;
import com.swiftlicious.hellblock.handlers.AdventureHelper;
import com.swiftlicious.hellblock.handlers.VersionHelper;
import com.swiftlicious.hellblock.player.GameProfileBuilder;
import com.swiftlicious.hellblock.player.UserData;
import com.swiftlicious.hellblock.utils.LocationUtils;

import dev.dejvokep.boostedyaml.block.implementation.Section;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;

public class AnvilTrustInputGUI extends AbstractAnvilInputGUI {

	private final HellblockPlugin plugin;
	private final UserData data;
	private final Player opener;
	private final Context<Integer> islandContext;
	private final boolean isOwner;
	private final Section anvilSettings;

	// Track UUID to prevent redundant profile fetches
	private UUID currentTargetUUID;
	private long lastSkinFetch = 0L;
	private static final long SKIN_FETCH_COOLDOWN = 20L * 2; // 2 seconds

	public AnvilTrustInputGUI(HellblockPlugin plugin, Player player, Context<Integer> islandContext, UserData data,
			boolean isOwner) {
		super(plugin, player, plugin.getPartyGUIManager().trustIconSection.getSection("anvil-settings")
				.getString("title", "<dark_green>Enter Player Name to Trust"), "");
		this.plugin = plugin;
		this.data = data;
		this.opener = player;
		this.islandContext = islandContext;
		this.isOwner = isOwner;
		this.anvilSettings = plugin.getPartyGUIManager().trustIconSection.getSection("anvil-settings");
	}

	@Override
	public void open() {
		Inventory anvil = Bukkit.createInventory(new AnvilGUIHolder(), InventoryType.ANVIL);

		Item<ItemStack> nameTag = plugin.getItemManager().wrap(new ItemStack(Material.NAME_TAG));
		nameTag.displayName(initialText);
		anvil.setItem(0, nameTag.loadCopy());

		anvil.setItem(1, plugin.getItemManager().wrap(new ItemStack(Material.ANVIL)).loadCopy());

		// Default skull before input is validated
		ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
		anvil.setItem(2, skull);

		player.openInventory(anvil);

		VersionHelper.getNMSManager().updateInventoryTitle(player,
				AdventureHelper.componentToJson(AdventureHelper.parseCenteredTitleMultiline(title)));

		startPolling();
	}

	@Override
	protected void startPolling() {
		cancel();

		pollingTask = plugin.getScheduler().sync().runRepeating(() -> {
			Component input = getInputText(player);

			// Skip if empty input
			if (AdventureHelper.isEmpty(input)) {
				return;
			}

			// Get plain text form (actual text player typed)
			String plainInput = AdventureHelper.componentToPlainText(input);

			// Cancel command
			if ("cancel".equalsIgnoreCase(plainInput)) {
				cancel();
				onCancel();
				return;
			}

			// SLOT 0: Name Tag update
			Item<ItemStack> nameTag = plugin.getItemManager().wrap(new ItemStack(Material.NAME_TAG));
			nameTag.displayName(AdventureHelper.componentToJson(AdventureHelper.miniMessageToComponent(anvilSettings
					.getString("dynamic-input", "<gray>Input: <white>{input}").replace("{input}", plainInput))));
			player.getOpenInventory().setItem(0, nameTag.loadCopy());

			// SLOT 2: Player head logic
			ItemStack slot2Item;
			String displayMsg = null;

			OfflinePlayer target = Bukkit.getOfflinePlayer(plainInput);
			boolean valid = target.hasPlayedBefore() && target.getName() != null;

			if (!valid) {
				displayMsg = anvilSettings.getString("not-found", "<red>Player not found.");
				slot2Item = new ItemStack(Material.BARRIER);
				currentTargetUUID = null;
			} else {
				UUID targetUUID = target.getUniqueId();

				if (data.getHellblockData().getOwnerUUID().equals(targetUUID)) {
					displayMsg = anvilSettings.getString("self", "<red>You cannot trust yourself.");
					slot2Item = new ItemStack(Material.BARRIER);
				} else if (data.getHellblockData().getPartyMembers().contains(targetUUID)) {
					displayMsg = anvilSettings.getString("already-member", "<red>This player is a coop member.");
					slot2Item = new ItemStack(Material.BARRIER);
				} else if (data.getHellblockData().getTrustedMembers().contains(targetUUID)) {
					displayMsg = anvilSettings.getString("already-trusted", "<red>This player is already trusted.");
					slot2Item = new ItemStack(Material.BARRIER);
				} else if (data.getHellblockData().getBannedMembers().contains(targetUUID)) {
					displayMsg = anvilSettings.getString("banned", "<red>This player is banned. Unban them first.");
					slot2Item = new ItemStack(Material.BARRIER);
				} else {
					// Valid
					try {
						Item<ItemStack> skull = plugin.getItemManager().wrap(new ItemStack(Material.PLAYER_HEAD));
						if (!target.isOnline()) {
							if (!targetUUID.equals(currentTargetUUID)
									|| (System.currentTimeMillis() - lastSkinFetch > SKIN_FETCH_COOLDOWN * 50L)) {

								GameProfile profile = GameProfileBuilder.fetch(targetUUID);
								skull.skull(profile.getProperties().get("textures").iterator().next().getValue());
								lastSkinFetch = System.currentTimeMillis();
								currentTargetUUID = targetUUID;
							}
						}
						skull.displayName(AdventureHelper
								.componentToJson(AdventureHelper.miniMessageToComponent("<green>" + target.getName())));
						skull.lore(List.of(AdventureHelper.miniMessageToJson(
								anvilSettings.getString("confirmation-click", "<yellow>Click to confirm!"))));
						slot2Item = skull.loadCopy();
					} catch (Exception e) {
						displayMsg = anvilSettings.getString("failed-to-load-skin-data",
								"<red>Could not load skin data.");
						slot2Item = new ItemStack(Material.BARRIER);
					}
				}
			}

			if (slot2Item.getType() == Material.BARRIER) {
				Item<ItemStack> errorItem = plugin.getItemManager().wrap(slot2Item);
				errorItem.displayName(
						AdventureHelper.componentToJson(AdventureHelper.miniMessageToComponent(displayMsg)));
				player.getOpenInventory().setItem(2, errorItem.loadCopy());
			} else {
				player.getOpenInventory().setItem(2, slot2Item);
			}

			// Timeout
			idleTicks += POLL_INTERVAL_TICKS;
			if (idleTicks >= timeoutTicks) {
				cancel();
				onCancel();
			}
		}, 0L, POLL_INTERVAL_TICKS, LocationUtils.getAnyLocationInstance());
	}

	@Override
	protected void onInput(String input) {
		String name = input.trim();

		if (name.isEmpty()) {
			updateInputDisplay(anvilSettings.getString("empty", "<red>Cannot be empty."));
			return;
		}

		OfflinePlayer target = Bukkit.getOfflinePlayer(name);

		if (!target.hasPlayedBefore() || target.getName() == null) {
			updateInputDisplay(anvilSettings.getString("not-found", "<red>Player not found."));
			return;
		}

		UUID targetUUID = target.getUniqueId();

		if (data.getHellblockData().getOwnerUUID().equals(targetUUID)) {
			updateInputDisplay(anvilSettings.getString("self", "<red>You cannot trust yourself."));
			return;
		}

		if (data.getHellblockData().getPartyMembers().contains(targetUUID)) {
			updateInputDisplay(anvilSettings.getString("already-member", "<red>This player is a coop member."));
			return;
		}

		if (data.getHellblockData().getTrustedMembers().contains(targetUUID)) {
			updateInputDisplay(anvilSettings.getString("already-trusted", "<red>This player is already trusted."));
			return;
		}

		if (data.getHellblockData().getBannedMembers().contains(targetUUID)) {
			updateInputDisplay(anvilSettings.getString("banned", "<red>This player is banned. Unban them first."));
			return;
		}

		plugin.getCoopManager().addTrustAccess(data, input, targetUUID).whenComplete((trusted, ex) -> {
			if (ex != null) {
				plugin.getPluginLogger().warn("addTrustAccess failed for " + input, ex);
				return;
			}

			plugin.getScheduler().runSync(() -> {
				String successMessage = anvilSettings.getString("success", "<green>Successfully trusted {name}.")
						.replace("{name}", name);

				plugin.getSenderFactory().wrap(opener)
						.sendMessage(AdventureHelper.miniMessageToComponent(successMessage));

				Section guiConfig = plugin.getPartyGUIManager().trustedSection;
				new TrustedMembersGUI(plugin, opener, islandContext, guiConfig, data, isOwner).open();
			});
		});
	}

	@EventHandler
	public void onInventoryClick(InventoryClickEvent event) {
		if (!(event.getWhoClicked() instanceof Player clicker))
			return;
		if (!clicker.getUniqueId().equals(player.getUniqueId()))
			return;
		if (!(event.getInventory().getHolder() instanceof AnvilGUIHolder))
			return;

		// Handle shift-clicks, number keys, or clicking outside GUI
		if (event.getClick().isShiftClick() || event.getClick().isKeyboardClick()) {
			event.setCancelled(true);
			return;
		}

		int slot = event.getRawSlot();
		event.setCancelled(true);

		if (slot == 2) {
			ItemStack clicked = event.getCurrentItem();
			if (clicked == null || clicked.getType() == Material.BARRIER) {
				AdventureHelper.playSound(plugin.getSenderFactory().getAudience(player),
						Sound.sound(net.kyori.adventure.key.Key.key("minecraft:entity.villager.no"),
								net.kyori.adventure.sound.Sound.Source.PLAYER, 1, 1));
				return;
			}

			Component input = getInputText(player);

			// Skip if empty input
			if (AdventureHelper.isEmpty(input)) {
				return;
			}

			// Get plain text form (actual text player typed)
			String plainInput = AdventureHelper.componentToPlainText(input);

			cancel();
			onInput(plainInput);
			if (plugin.getCooldownManager().shouldUpdateActivity(player.getUniqueId(), 15000)) {
				data.getHellblockData().updateLastIslandActivity();
			}
		}
	}

	@Override
	protected void onCancel() {
		plugin.getScheduler().executeSync(() -> {
			List<UUID> trusted = new ArrayList<>(data.getHellblockData().getTrustedMembers());

			if (trusted.isEmpty()) {
				plugin.getPartyGUIManager().openPartyGUI(opener, islandContext.holder(), isOwner);
			} else {
				Section guiConfig = plugin.getPartyGUIManager().trustedSection;
				new TrustedMembersGUI(plugin, opener, islandContext, guiConfig, data, isOwner).open();
			}
		});
	}
}