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

	private SchedulerTask pollingTask;
	private static final long POLL_INTERVAL_TICKS = 2L;
	private static final long DEFAULT_TIMEOUT_TICKS = 200L; // ~10 seconds

	private long idleTicks = 0;
	private final long timeoutTicks;

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
				AdventureHelper.componentToJson(Component.text(title)));

		startPolling();
	}

	/**
	 * Starts polling rename text every few ticks.
	 */
	private void startPolling() {
		cancel();

		pollingTask = plugin.getScheduler().sync().runRepeating(() -> {
			String input = getInputText(player);

			if (input != null && !input.isEmpty()) {

				// Handle cancel keyword
				if ("cancel".equalsIgnoreCase(input)) {
					cancel();
					onCancel();
					return;
				}

				// If player typed a new value different from initial
				if (!input.equals(initialText)) {
					cancel();
					onInput(input);
					return;
				}
			}

			// Idle timeout reached
			idleTicks += POLL_INTERVAL_TICKS;
			if (idleTicks >= timeoutTicks) {
				cancel();
				onCancel();
			}

		}, 0L, POLL_INTERVAL_TICKS, LocationUtils.getAnyLocationInstance());
	}

	protected void updateInputDisplay(String error) {
		Item<ItemStack> inputItem = plugin.getItemManager().wrap(new ItemStack(Material.PAPER));
		inputItem.displayName(AdventureHelper.componentToJson(AdventureHelper.miniMessage(error)));
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
	 * Gets current rename text from Paper or Spigot.
	 */
	@SuppressWarnings("removal")
	protected String getInputText(Player player) {
		final InventoryView view = player.getOpenInventory();
		try {
			Method getInputText = view.getClass().getMethod("getInputText");
			return (String) getInputText.invoke(view);
		} catch (NoSuchMethodException ignored) {
			Inventory top = view.getTopInventory();
			if (top instanceof AnvilInventory anvilInventory) {
				return anvilInventory.getRenameText();
			}
		} catch (Exception ex) {
			plugin.getPluginLogger().warn("Failed to get input text for Anvil GUI: ", ex);
		}
		return "";
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