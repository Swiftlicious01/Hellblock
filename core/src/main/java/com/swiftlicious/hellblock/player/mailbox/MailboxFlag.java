package com.swiftlicious.hellblock.player.mailbox;

import com.google.gson.annotations.SerializedName;

/**
 * Represents various flags associated with a {@link MailboxEntry}.
 * <p>
 * These flags control what actions or notifications should occur when a player
 * receives mail through the in-game mailbox system.
 */
public enum MailboxFlag {

	/** Indicates the player should receive a chat message. */
	@SerializedName("sendMessage")
	SEND_MESSAGE,

	/** Resets the player's main inventory. */
	@SerializedName("resetInventory")
	RESET_INVENTORY,

	/** Resets the player's Ender Chest contents. */
	@SerializedName("resetEnderchest")
	RESET_ENDERCHEST,

	/** Indicates the player's location is unsafe. */
	@SerializedName("unsafeLocation")
	UNSAFE_LOCATION,

	/** Triggers the island reset confirmation GUI. */
	@SerializedName("showResetGUI")
	SHOW_RESET_GUI,

	/** Notifies the island owner about the message. */
	@SerializedName("notifyOwner")
	NOTIFY_OWNER,

	/** Notifies party members about the message. */
	@SerializedName("notifyParty")
	NOTIFY_PARTY,

	/** Notifies trusted members about the message. */
	@SerializedName("notifyTrusted")
	NOTIFY_TRUSTED,

	/** Queues a teleport to the player's home location */
	@SerializedName("teleportHome")
	QUEUE_TELEPORT_HOME,

	/** Shows title screen */
	@SerializedName("showTitleScreen")
	SHOW_TITLE,

	/** Resets player's gamemode to original */
	@SerializedName("resetGameMode")
	RESET_GAMEMODE,

	/** Plays a sound */
	@SerializedName("playSound")
	PLAY_SOUND;
}