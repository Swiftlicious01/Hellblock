package com.swiftlicious.hellblock.protection;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.util.BoundingBox;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.api.Reloadable;
import com.swiftlicious.hellblock.player.UserData;
import com.swiftlicious.hellblock.protection.HellblockFlag.FlagType;
import com.swiftlicious.hellblock.sender.Sender;

import net.kyori.adventure.text.Component;

//TODO: put event handlers for each flag type 
public class ProtectionEvents implements Listener, Reloadable {

	protected final HellblockPlugin instance;

	public ProtectionEvents(HellblockPlugin plugin) {
		instance = plugin;
	}

	@Override
	public void load() {
		Bukkit.getPluginManager().registerEvents(this, instance);
	}

	@Override
	public void unload() {
		HandlerList.unregisterAll(this);
	}

	@EventHandler
	public void onMoveHellblockMessage(PlayerMoveEvent event) {
		final Player player = event.getPlayer();
		if (!instance.getHellblockHandler().isInCorrectWorld(player))
			return;
		if (player.getLocation() == null)
			return;
		if (event.getTo().getBlockX() == event.getFrom().getBlockX()
				&& event.getTo().getBlockY() == event.getFrom().getBlockY()
				&& event.getTo().getBlockZ() == event.getFrom().getBlockZ()) {
			return; // user didnt actually move a full block
		}

		instance.getCoopManager().getHellblockOwnerOfVisitingIsland(player).thenAccept(ownerUUID -> {
			if (ownerUUID == null)
				return;
			Sender audience = instance.getSenderFactory().wrap(player);
			instance.getStorageManager().getOfflineUserData(ownerUUID, instance.getConfigManager().lockData())
					.thenAccept((result) -> {
						if (result.isEmpty())
							return;
						UserData offlineUser = result.get();
						BoundingBox hellblockBounds = offlineUser.getHellblockData().getBoundingBox();
						if (hellblockBounds != null) {
							BoundingBox playerBounds = player.getBoundingBox();
							if (hellblockBounds.contains(playerBounds)) {
								String greetFlag = offlineUser.getHellblockData()
										.getProtectionData(FlagType.GREET_MESSAGE);
								if (greetFlag != null) {
									audience.sendMessage(Component.text(greetFlag));
								}
							} else {
								String farewellFlag = offlineUser.getHellblockData()
										.getProtectionData(FlagType.FAREWELL_MESSAGE);
								if (farewellFlag != null) {
									audience.sendMessage(Component.text(farewellFlag));
								}
							}
						}
					});
		});
	}

	@EventHandler
	public void onTeleportHellblockMessage(PlayerTeleportEvent event) {
		if (event.getCause() == TeleportCause.ENDER_PEARL || event.getCause() == TeleportCause.CHORUS_FRUIT
				|| event.getCause() == TeleportCause.DISMOUNT || event.getCause() == TeleportCause.COMMAND) {
			final Player player = event.getPlayer();
			if (!instance.getHellblockHandler().isInCorrectWorld(player))
				return;
			if (player.getLocation() == null)
				return;

			instance.getCoopManager().getHellblockOwnerOfVisitingIsland(player).thenAccept(ownerUUID -> {
				if (ownerUUID == null)
					return;
				Sender audience = instance.getSenderFactory().wrap(player);
				instance.getStorageManager().getOfflineUserData(ownerUUID, instance.getConfigManager().lockData())
						.thenAccept((result) -> {
							if (result.isEmpty())
								return;
							UserData offlineUser = result.get();
							BoundingBox hellblockBounds = offlineUser.getHellblockData().getBoundingBox();
							if (hellblockBounds != null) {
								BoundingBox playerBounds = player.getBoundingBox();
								if (hellblockBounds.contains(playerBounds)) {
									String greetFlag = offlineUser.getHellblockData()
											.getProtectionData(FlagType.GREET_MESSAGE);
									if (greetFlag != null) {
										audience.sendMessage(Component.text(greetFlag));
									}
								} else {
									String farewellFlag = offlineUser.getHellblockData()
											.getProtectionData(FlagType.FAREWELL_MESSAGE);
									if (farewellFlag != null) {
										audience.sendMessage(Component.text(farewellFlag));
									}
								}
							}
						});
			});
		}
	}
}