package com.swiftlicious.hellblock.api;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.bukkit.entity.Player;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.scheduler.SchedulerTask;

/**
 * Manages and updates ActionBar messages for a specific player in a context.
 */
public class ActionBarSender {

	private final Player player;
	private int refreshTimer;
	private int switchTimer;
	private int counter;
	private final DynamicText[] texts;
	private SchedulerTask senderTask;
	private final ActionBarConfig config;
	private boolean isShown;
	private final Map<String, String> privatePlaceholders;

	/**
	 * Creates a new ActionBarSender instance for a player.
	 *
	 * @param player The player to manage ActionBar messages for.
	 * @param config The configuration for ActionBar messages.
	 */
	public ActionBarSender(Player player, ActionBarConfig config) {
		this.player = player;
		this.config = config;
		this.isShown = false;
		this.privatePlaceholders = new HashMap<>();
		this.privatePlaceholders.put("{player}", player.getName());
		this.updatePrivatePlaceholders();

		String[] str = config.getTexts();
		texts = new DynamicText[str.length];
		for (int i = 0; i < str.length; i++) {
			texts[i] = new DynamicText(player, str[i]);
			texts[i].update(privatePlaceholders);
		}
	}

	/**
	 * Updates private placeholders used in ActionBar messages.
	 */
	private void updatePrivatePlaceholders() {

	}

	/**
	 * Shows the ActionBar message to the player.
	 */
	public void show() {
		this.isShown = true;
		senderTask = HellblockPlugin.getInstance().getScheduler().asyncRepeating(() -> {
			switchTimer++;
			if (switchTimer > config.getSwitchInterval()) {
				switchTimer = 0;
				counter++;
			}
			if (refreshTimer < config.getRefreshRate()) {
				refreshTimer++;
			} else {
				refreshTimer = 0;
				DynamicText text = texts[counter % (texts.length)];
				updatePrivatePlaceholders();
				text.update(privatePlaceholders);
				HellblockPlugin.getInstance().getAdventureManager().sendActionbar(player, text.getLatestValue());
			}
		}, 50, 50, TimeUnit.MILLISECONDS);
	}

	/**
	 * Hides the ActionBar message from the player.
	 */
	public void hide() {
		if (senderTask != null)
			senderTask.cancel();
		this.isShown = false;
	}

	/**
	 * Checks if the ActionBar message is currently visible to the player.
	 *
	 * @return True if the ActionBar message is visible, false otherwise.
	 */
	public boolean isVisible() {
		return this.isShown;
	}

	/**
	 * Gets the ActionBar configuration.
	 *
	 * @return The ActionBar configuration.
	 */
	public ActionBarConfig getConfig() {
		return config;
	}
}
