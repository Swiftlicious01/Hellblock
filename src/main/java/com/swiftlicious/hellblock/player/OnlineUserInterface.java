package com.swiftlicious.hellblock.player;

import org.bukkit.entity.Player;

public interface OnlineUserInterface extends OfflineUserInterface {

	/**
	 * Get the bukkit player
	 *
	 * @return player
	 */
	Player getPlayer();
}
