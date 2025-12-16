package com.swiftlicious.hellblock.creation.addons;

import java.util.Set;

import org.bukkit.entity.Player;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.database.dependency.Dependency;

public class ProtocolLibHook {

	private static ProtocolManager protocolManager;
	private static boolean isHooked = false;
	private static PacketAdapter freezeListener;

	public static void register(HellblockPlugin instance) {
		instance.getDependencyManager().runWithLoader(Set.of(Dependency.PROTOCOL_LIB), () -> {
			protocolManager = ProtocolLibrary.getProtocolManager();

			if (protocolManager != null) {
				ProtocolLibHook.isHooked = true;
			}

			return protocolManager;
		});
	}

	public static boolean isHooked() {
		return isHooked;
	}

	public static void registerFreezeMovementPacket(HellblockPlugin instance) {
		if (!isHooked()) {
			return;
		}

		instance.getDependencyManager().runWithLoader(Set.of(Dependency.PROTOCOL_LIB), () -> {
			// Avoid double registration on reload
			if (freezeListener != null) {
				protocolManager.removePacketListener(freezeListener);
			}

			freezeListener = new PacketAdapter(instance, ListenerPriority.HIGHEST, PacketType.Play.Client.GROUND,
					PacketType.Play.Client.POSITION, PacketType.Play.Client.POSITION_LOOK,
					PacketType.Play.Client.LOOK) {

				@Override
				public void onPacketReceiving(PacketEvent event) {
					Player player = event.getPlayer();
					if (instance.getIslandGenerator().isAnimating(player.getUniqueId())) {
						// Cancel movement packet while in cinematic mode
						event.setCancelled(true);
						instance.debug("ProtocolLibHook: Cancelled movement packet for " + player.getName());
					}
				}
			};

			protocolManager.addPacketListener(freezeListener);
			return freezeListener;
		});
	}

	public static void unregisterAll(HellblockPlugin instance) {
		if (isHooked && protocolManager != null) {
			instance.getDependencyManager().runWithLoader(Set.of(Dependency.PROTOCOL_LIB), () -> {
				protocolManager.removePacketListeners(instance);
				freezeListener = null;
				return null;
			});
		}
	}
}