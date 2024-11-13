package com.swiftlicious.hellblock.handlers;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import com.swiftlicious.hellblock.api.Reloadable;
import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.gui.SectionPage;
import com.swiftlicious.hellblock.utils.extras.Pair;

import io.papermc.paper.event.player.AsyncChatEvent;

public class ChatCatcherManager implements Reloadable, Listener {

	protected final HellblockPlugin instance;
	private final ConcurrentHashMap<UUID, Pair<String, SectionPage>> pageMap;

	public ChatCatcherManager(HellblockPlugin plugin) {
		this.pageMap = new ConcurrentHashMap<>();
		this.instance = plugin;
	}

	@Override
	public void load() {
		Bukkit.getPluginManager().registerEvents(this, instance);
	}

	@Override
	public void unload() {
		this.pageMap.clear();
		HandlerList.unregisterAll(this);
	}

	@Override
	public void disable() {
		unload();
	}

	public void catchMessage(Player player, String key, SectionPage page) {
		this.pageMap.put(player.getUniqueId(), Pair.of(key, page));
	}

	@EventHandler
	public void onQuit(PlayerQuitEvent event) {
		pageMap.remove(event.getPlayer().getUniqueId());
	}

	@EventHandler
	public void onChat(AsyncChatEvent event) {
		var uuid = event.getPlayer().getUniqueId();
		var pair = pageMap.remove(uuid);
		if (pair == null)
			return;
		event.setCancelled(true);
		instance.getScheduler().executeSync(() -> {
			pair.right().getSection().set(pair.left(), event.message());
			pair.right().reOpen();
		}, event.getPlayer().getLocation());
	}
}