package com.swiftlicious.hellblock.player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

/**
 * The PlayerContext class implements the Context interface specifically for the
 * Player type. It allows for storing and retrieving arguments related to a
 * player.
 */
public final class PlayerContext implements Context<Player> {

	private final Player player;
	private final Map<ContextKeys<?>, Object> args;
	private final Map<String, String> placeholderMap;

	public PlayerContext(@Nullable Player player, boolean sync) {
		this.player = player;
		this.args = sync ? new ConcurrentHashMap<>() : new HashMap<>();
		this.placeholderMap = sync ? new ConcurrentHashMap<>() : new HashMap<>();
		if (player == null)
			return;
		final Location location = player.getLocation();
		arg(ContextKeys.PLAYER, player.getName()).arg(ContextKeys.LOCATION, location)
				.arg(ContextKeys.X, location.getBlockX()).arg(ContextKeys.Y, location.getBlockY())
				.arg(ContextKeys.Z, location.getBlockZ()).arg(ContextKeys.WORLD, location.getWorld().getName());
	}

	@Override
	public Map<ContextKeys<?>, Object> args() {
		return args;
	}

	@Override
	public Map<String, String> placeholderMap() {
		return placeholderMap;
	}

	@Override
	public <C> PlayerContext arg(ContextKeys<C> key, C value) {
		if (key == null || value == null)
			return this;
		this.args.put(key, value);
		this.placeholderMap.put("{" + key.key() + "}", value.toString());
		return this;
	}

	@Override
	public Context<Player> combine(Context<Player> other) {
		final PlayerContext otherContext = (PlayerContext) other;
		this.args.putAll(otherContext.args);
		this.placeholderMap.putAll(otherContext.placeholderMap);
		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <C> C arg(ContextKeys<C> key) {
		return (C) args.get(key);
	}

	@Nullable
	@SuppressWarnings("unchecked")
	@Override
	public <C> C remove(ContextKeys<C> key) {
		placeholderMap.remove("{" + key.key() + "}");
		return (C) args.remove(key);
	}

	@Override
	public Player holder() {
		return player;
	}

	@Override
	public void clearCustomData() {
		List<ContextKeys<?>> toRemove = new ArrayList<>();
		for (Map.Entry<ContextKeys<?>, Object> entry : args.entrySet()) {
			if (entry.getKey().key().startsWith("data_")) {
				toRemove.add(entry.getKey());
			}
		}
		for (ContextKeys<?> key : toRemove) {
			args.remove(key);
			placeholderMap.remove("{" + key.key() + "}");
		}
	}

	@Override
	public String toString() {
		return "PlayerContext{" + "args=" + args + ", player=" + player + '}';
	}
}