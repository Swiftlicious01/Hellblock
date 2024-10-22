package com.swiftlicious.hellblock.listeners;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World.Environment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.saicone.rtag.RtagItem;
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
		Player player = event.getPlayer();
		UUID id = player.getUniqueId();
		instance.debug("Loading player info for " + player.getName() + "!");

		instance.getScheduler().runTaskSyncLater(() -> {
			HellblockPlayer pi = new HellblockPlayer(id);
			instance.getHellblockHandler().addActivePlayer(player, pi);
			instance.getNetherFarming().trackNetherFarms(pi);
			if (player.hasPermission("hellblock.updates") && instance.isUpdateAvailable()) {
				instance.getAdventureManager().sendMessageWithPrefix(player,
						"<red>There is a new update available!: <dark_red><u>https://github.com/Swiftlicious01/Hellblock<!u>");
			}

			if (!player.getWorld().getName().equalsIgnoreCase(instance.getHellblockHandler().getWorldName()))
				return;

			ItemStack[] armorSet = player.getInventory().getArmorContents();
			boolean checkArmor = false;
			if (armorSet != null) {
				for (ItemStack item : armorSet) {
					if (item == null || item.getType() == Material.AIR)
						continue;
					RtagItem tag = new RtagItem(item);
					if (instance.getNetherArmor().checkNightVisionArmorStatus(tag)) {
						checkArmor = true;
						break;
					}
				}

				if (checkArmor) {
					player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 1));
					pi.isWearingGlowstoneArmor(true);
				}
			}

			ItemStack tool = player.getInventory().getItemInMainHand();
			if (tool.getType() == Material.AIR) {
				tool = player.getInventory().getItemInOffHand();
				if (tool.getType() == Material.AIR) {
					return;
				}
			}

			RtagItem tagItem = new RtagItem(tool);
			if (instance.getNetherTools().checkNightVisionToolStatus(tagItem)) {
				player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 1));
				pi.isHoldingGlowstoneTool(true);
			}

		}, player.getLocation(), 5, TimeUnit.SECONDS);
	}

	@EventHandler
	public void onQuit(PlayerQuitEvent event) {
		Player player = event.getPlayer();
		instance.debug("Saving player info for " + player.getName() + "!");
		instance.getHellblockHandler().removeActivePlayer(player);

		HellblockPlayer hbPlayer = new HellblockPlayer(player.getUniqueId());
		if (hbPlayer.hasGlowstoneToolEffect() || hbPlayer.hasGlowstoneArmorEffect()) {
			if (player.hasPotionEffect(PotionEffectType.NIGHT_VISION)) {
				player.removePotionEffect(PotionEffectType.NIGHT_VISION);
				hbPlayer.isHoldingGlowstoneTool(false);
				hbPlayer.isWearingGlowstoneArmor(false);
			}
		}
	}

	@EventHandler
	public void onPortal(PlayerPortalEvent event) {
		Player player = event.getPlayer();
		if (player.getWorld().getEnvironment() == Environment.NETHER) {
			player.chat(instance.getHellblockHandler().getNetherCMD());
			event.setCancelled(true);
		}

		if (player.getWorld().getEnvironment() == Environment.NORMAL
				&& event.getCause() == TeleportCause.NETHER_PORTAL) {
			event.setCancelled(true);
			HellblockPlayer pi = (HellblockPlayer) instance.getHellblockHandler().getActivePlayers()
					.get(player.getUniqueId());
			if (pi.hasHellblock()) {
				player.teleportAsync(pi.getHomeLocation());
			} else {
				new IslandChoiceMenu(player);
			}
		}

	}
}