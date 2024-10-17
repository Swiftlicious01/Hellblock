package com.swiftlicious.hellblock.playerdata;

import org.bukkit.entity.Player;

/**
 * Implementation of the OnlineUser interface, extending OfflineUser to
 * represent online player data.
 */
public class OnlineUser extends OfflineUser implements OnlineUserInterface {

	private final Player player;

	/**
	 * Constructor to create an OnlineUser instance.
	 *
	 * @param player     The online player associated with this user.
	 * @param playerData The player's data, including earnings.
	 */
	public OnlineUser(Player player, PlayerData playerData) {
		super(player.getUniqueId(), player.getName(), playerData);
		this.player = player;
	}

	@Override
	public Player getPlayer() {
		return player;
	}
}