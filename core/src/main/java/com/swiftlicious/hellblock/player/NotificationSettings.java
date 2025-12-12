package com.swiftlicious.hellblock.player;

import org.jetbrains.annotations.NotNull;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.swiftlicious.hellblock.utils.adapters.HellblockTypeAdapterFactory.EmptyCheck;

/**
 * The {@code NotificationSettings} class stores user preferences related to
 * in-game notifications, such as join and invitation notifications.
 * <p>
 * This class implements {@link EmptyCheck} to determine whether both
 * notification types are enabled (i.e., the default state).
 */
public class NotificationSettings implements EmptyCheck {

	@Expose
	@SerializedName("joinNotifications")
	private boolean joinNotifications;

	@Expose
	@SerializedName("inviteNotifications")
	private boolean inviteNotifications;

	/**
	 * Constructs a new {@code NotificationSettings} instance with the specified
	 * values.
	 *
	 * @param joinNotifications   whether join notifications are enabled
	 * @param inviteNotifications whether invite notifications are enabled
	 */
	public NotificationSettings(boolean joinNotifications, boolean inviteNotifications) {
		this.joinNotifications = joinNotifications;
		this.inviteNotifications = inviteNotifications;
	}

	/**
	 * Checks if join notifications are enabled.
	 *
	 * @return {@code true} if enabled; {@code false} otherwise
	 */
	public boolean hasJoinNotifications() {
		return joinNotifications;
	}

	/**
	 * Checks if invite notifications are enabled.
	 *
	 * @return {@code true} if enabled; {@code false} otherwise
	 */
	public boolean hasInviteNotifications() {
		return inviteNotifications;
	}

	/**
	 * Sets whether join notifications are enabled.
	 *
	 * @param joinNotifications {@code true} to enable; {@code false} to disable
	 */
	public void setJoinNotifications(boolean joinNotifications) {
		this.joinNotifications = joinNotifications;
	}

	/**
	 * Sets whether invite notifications are enabled.
	 *
	 * @param inviteNotifications {@code true} to enable; {@code false} to disable
	 */
	public void setInviteNotifications(boolean inviteNotifications) {
		this.inviteNotifications = inviteNotifications;
	}

	/**
	 * Creates a new {@code NotificationSettings} instance with default values: both
	 * join and invite notifications enabled.
	 *
	 * @return a default {@code NotificationSettings} instance
	 */
	@NotNull
	public static NotificationSettings empty() {
		return new NotificationSettings(true, true);
	}

	/**
	 * Creates a deep copy of this {@code NotificationSettings} instance.
	 *
	 * @return a new {@code NotificationSettings} object with identical values
	 */
	@NotNull
	public final NotificationSettings copy() {
		return new NotificationSettings(this.joinNotifications, this.inviteNotifications);
	}

	/**
	 * Determines if the current notification settings are in their default (empty)
	 * state, where both join and invite notifications are enabled.
	 *
	 * @return {@code true} if both notifications are enabled; {@code false}
	 *         otherwise
	 */
	@Override
	public boolean isEmpty() {
		return this.joinNotifications && this.inviteNotifications;
	}
}