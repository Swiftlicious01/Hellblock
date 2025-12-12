package com.swiftlicious.hellblock.gui.display;

import java.lang.reflect.Method;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.creation.item.Item;
import com.swiftlicious.hellblock.handlers.AdventureHelper;
import com.swiftlicious.hellblock.handlers.VersionHelper;
import com.swiftlicious.hellblock.scheduler.SchedulerTask;
import com.swiftlicious.hellblock.utils.LocationUtils;

import net.kyori.adventure.text.Component;

public abstract class AbstractAnvilInputGUI implements Listener {

	protected final HellblockPlugin plugin;
	protected final Player player;
	protected final String initialText;
	protected final String title;

	protected SchedulerTask pollingTask;
	protected static final long POLL_INTERVAL_TICKS = 2L;
	protected static final long DEFAULT_TIMEOUT_TICKS = 200L; // ~10 seconds

	protected long idleTicks = 0;
	protected final long timeoutTicks;

	public AbstractAnvilInputGUI(HellblockPlugin plugin, Player player, String title, String initialText) {
		this(plugin, player, title, initialText, DEFAULT_TIMEOUT_TICKS);
	}

	public AbstractAnvilInputGUI(HellblockPlugin plugin, Player player, String title, String initialText,
			long timeoutTicks) {
		this.plugin = plugin;
		this.player = player;
		this.title = title;
		this.initialText = initialText;
		this.timeoutTicks = timeoutTicks;
	}

	public void init() {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	/**
	 * Opens the Anvil input screen.
	 */
	public void open() {
		Inventory anvil = Bukkit.createInventory(new AnvilGUIHolder(), InventoryType.ANVIL);

		Item<ItemStack> inputItem = plugin.getItemManager().wrap(new ItemStack(Material.PAPER));
		inputItem.displayName(initialText);

		anvil.setItem(0, inputItem.loadCopy());
		anvil.setItem(1, plugin.getItemManager().wrap(new ItemStack(Material.ANVIL)).loadCopy());
		anvil.setItem(2, plugin.getItemManager().wrap(new ItemStack(Material.BOOK)).loadCopy());

		player.openInventory(anvil);

		VersionHelper.getNMSManager().updateInventoryTitle(player,
				AdventureHelper.componentToJson(AdventureHelper.parseCenteredTitleMultiline(title)));

		startPolling();
	}

	/**
	 * Starts polling rename text every few ticks.
	 */
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

			// Handle cancel keyword
			if ("cancel".equalsIgnoreCase(plainInput)) {
				cancel();
				onCancel();
				return;
			}

			// If player typed a new value different from initial
			if (!plainInput.equals(initialText)) {
				cancel();
				onInput(plainInput);
				return;
			}

			// Idle timeout check
			idleTicks += POLL_INTERVAL_TICKS;
			if (idleTicks >= timeoutTicks) {
				cancel();
				onCancel();
			}

		}, 0L, POLL_INTERVAL_TICKS, LocationUtils.getAnyLocationInstance());
	}

	protected void updateInputDisplay(String error) {
		Item<ItemStack> inputItem = plugin.getItemManager().wrap(new ItemStack(Material.PAPER));
		inputItem.displayName(AdventureHelper.componentToJson(AdventureHelper.miniMessageToComponent(error)));
		player.getOpenInventory().setItem(0, inputItem.loadCopy());
	}

	/**
	 * Stops polling.
	 */
	public void cancel() {
		if (pollingTask != null && !pollingTask.isCancelled()) {
			pollingTask.cancel();
			pollingTask = null;
		}
	}

	/**
	 * Cleanup event listener on inventory close.
	 */
	@EventHandler
	public void onInventoryClose(InventoryCloseEvent event) {
		if (!event.getPlayer().getUniqueId().equals(player.getUniqueId()))
			return;
		if (!(event.getInventory().getHolder() instanceof AnvilGUIHolder))
			return;

		cancel();
		HandlerList.unregisterAll(this);
		onCancel();
	}

	/**
	 * Cleanup event listener on player quit.
	 */
	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event) {
		Player quitPlayer = event.getPlayer();
		if (!quitPlayer.getUniqueId().equals(player.getUniqueId()))
			return;

		InventoryView view = quitPlayer.getOpenInventory();
		Inventory topInv = view.getTopInventory();
		if (!(topInv.getHolder() instanceof AnvilGUIHolder))
			return;

		cancel();
		HandlerList.unregisterAll(this);
		onCancel();
	}

	/**
	 * Gets current rename text from Paper or Spigot.
	 */
	@SuppressWarnings("removal")
	@NotNull
	public Component getInputText(@NotNull Player player) {
		InventoryView view = player.getOpenInventory();
		Inventory top = view.getTopInventory();
		if (!(top instanceof AnvilInventory anvilInventory))
			return Component.empty();

		String raw = null;

		if (VersionHelper.isVersionNewerThan1_21()) {
			// Try using org.bukkit.inventory.view.AnvilView#getRenameText (1.21+)
			try {
				Class<?> anvilViewClass = Class.forName("org.bukkit.inventory.view.AnvilView");
				if (anvilViewClass.isInstance(view)) {
					Method getRenameText = anvilViewClass.getMethod("getRenameText");
					Object result = getRenameText.invoke(view);
					if (result instanceof String str) {
						raw = str;
					}
				}
			} catch (ClassNotFoundException e) {
				// <1.21 — class doesn’t exist, ignore
			} catch (Throwable t) {
				plugin.getPluginLogger().warn("Failed to access AnvilView#getRenameText", t);
			}
		} else {
			// Fallback to older API (1.20 and below)
			if (raw == null) {
				try {
					raw = anvilInventory.getRenameText();
				} catch (Throwable ignored) {
				}
			}
		}

		if (raw == null || raw.isEmpty())
			return Component.empty();

		// Deserialize into a Component (plain text or MiniMessage)
		return AdventureHelper.miniMessageToComponent(raw);
	}

	/**
	 * Called when player confirms a new input.
	 */
	protected abstract void onInput(String input);

	/**
	 * Called when player cancels, times out, or closes the GUI.
	 */
	protected void onCancel() {
		// default: do nothing. Subclass can override.
	}

	/**
	 * Represents a holder for the anvil input GUI inventory.
	 */
	public class AnvilGUIHolder implements InventoryHolder {
		private Inventory inventory;

		public void setInventory(Inventory inventory) {
			this.inventory = inventory;
		}

		@Override
		public @NotNull Inventory getInventory() {
			return inventory;
		}
	}
}