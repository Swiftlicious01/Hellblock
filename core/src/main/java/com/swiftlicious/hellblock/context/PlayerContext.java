package com.swiftlicious.hellblock.context;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

/**
 * The PlayerContext class implements the Context interface specifically for the
 * Player type. It allows for storing and retrieving arguments related to a
 * player.
 */
public final class PlayerContext extends AbstractContext<Player> {

	public PlayerContext(@Nullable Player player, boolean sync) {
        super(player, sync);
        if (player == null) return;
		final Location location = player.getLocation();
		arg(ContextKeys.PLAYER, player.getName());
		updateLocation(location);
	}

	@Override
	public String toString() {
		return "PlayerContext{" + "args=" + args() + ", player=" + holder() + '}';
	}
}