package com.swiftlicious.hellblock.listeners;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.World.Environment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.playerdata.HellblockPlayer;

public class PlayerListener implements Listener {

	private final HellblockPlugin instance;

	public PlayerListener(HellblockPlugin plugin) {
		instance = plugin;
		Bukkit.getPluginManager().registerEvents(this, instance);
	}

	@EventHandler
	public void onJoin(PlayerJoinEvent event) {
		instance.debug("Loading player info for " + event.getPlayer().getName() + "!");
		UUID id = event.getPlayer().getUniqueId();
		HellblockPlayer pi = new HellblockPlayer(id);
		instance.getNetherFarming().trackNetherFarms(pi);
		instance.getHellblockHandler().addActivePlayer(event.getPlayer(), pi);
		if (event.getPlayer().hasPermission("hellblock.updates") && instance.isUpdateAvailable()) {
			instance.getAdventureManager().sendMessageWithPrefix(event.getPlayer(), "<red>There is a new update available!: <dark_red><u>https://github.com/Swiftlicious01/Hellblock<!u>");
		}
	}

	@EventHandler
	public void onQuit(PlayerQuitEvent event) {
		instance.debug("Saving player info for " + event.getPlayer().getName() + "!");
		instance.getHellblockHandler().removeActivePlayer(event.getPlayer());
	}

	@EventHandler
	public void onPortal(PlayerPortalEvent event) {
		if (event.getPlayer().getWorld().getEnvironment() == Environment.NETHER) {
			event.getPlayer().chat(instance.getHellblockHandler().getNetherCMD());
			event.setCancelled(true);
		}

		if (event.getPlayer().getWorld().getEnvironment() == Environment.NORMAL
				&& event.getCause() == TeleportCause.NETHER_PORTAL) {
			event.setCancelled(true);
			HellblockPlayer pi = (HellblockPlayer) instance.getHellblockHandler().getActivePlayers()
					.get(event.getPlayer().getUniqueId());
			if (pi.hasHellblock()) {
				event.getPlayer().teleport(pi.getHomeLocation());
			} else {
				instance.getHellblockHandler().createHellblock(event.getPlayer());
			}
		}

	}
}