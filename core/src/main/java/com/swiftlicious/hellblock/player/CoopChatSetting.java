package com.swiftlicious.hellblock.player;

import com.google.gson.annotations.SerializedName;

/**
 * Defines the chat scope preferences for a Hellblock island player.
 *
 * <p>
 * This setting controls how player messages are routed during cooperative
 * gameplay on an island. It is primarily used to filter or direct chat messages
 * based on the selected communication scope.
 * </p>
 *
 * <ul>
 * <li>{@code GLOBAL} – Messages are broadcast globally to all players (subject
 * to server rules).</li>
 * <li>{@code LOCAL} – Messages are visible to players within a defined local
 * range (e.g., nearby on the island).</li>
 * <li>{@code PARTY} – Messages are only visible to island party members.</li>
 * </ul>
 *
 * <p>
 * This enum is typically stored as part of {@code HellblockData} to persist the
 * player's last selected chat mode.
 * </p>
 */
public enum CoopChatSetting {
	@SerializedName("global")
	GLOBAL,

	@SerializedName("local")
	LOCAL,

	@SerializedName("party")
	PARTY;
}