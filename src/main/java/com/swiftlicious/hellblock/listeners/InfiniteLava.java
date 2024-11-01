package com.swiftlicious.hellblock.listeners;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Levelled;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.Event.Result;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BlockIterator;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.challenges.HellblockChallenge.ChallengeType;
import com.swiftlicious.hellblock.playerdata.HellblockPlayer;

import lombok.Getter;
import lombok.NonNull;

@Getter
public class InfiniteLava implements Listener {

	private final HellblockPlugin instance;

	private final boolean infiniteLavaEnabled;

	public InfiniteLava(HellblockPlugin plugin) {
		instance = plugin;
		this.infiniteLavaEnabled = instance.getConfig("config.yml").getBoolean("infinite-lava-options.enabled", true);
		Bukkit.getPluginManager().registerEvents(this, instance);
	}

	@EventHandler
	public void onInfiniteLavaFall(PlayerInteractEvent event) {
		if (!isInfiniteLavaEnabled()) {
			return;
		}
		final Player player = event.getPlayer();
		if (!player.getWorld().getName().equalsIgnoreCase(instance.getHellblockHandler().getWorldName()))
			return;

		ItemStack hand = event.getItem();
		if (hand != null && hand.getType() == Material.BUCKET) {
			BlockIterator iter = new BlockIterator(player, 5);
			Block lastBlock = iter.next();
			while (iter.hasNext()) {
				lastBlock = iter.next();
				if (lastBlock.getType() != Material.LAVA) {
					continue;
				}
				break;
			}
			if (lastBlock.getType() == Material.LAVA && isLavaFall(lastBlock)) {
				event.setUseItemInHand(Result.ALLOW);
				event.getItem().setAmount(event.getItem().getAmount() > 0 ? event.getItem().getAmount() - 1 : 0);
				if (player.getInventory().firstEmpty() != -1) {
					player.getInventory().addItem(new ItemStack(Material.LAVA_BUCKET));
				} else {
					lastBlock.getWorld().dropItemNaturally(player.getLocation(), new ItemStack(Material.LAVA_BUCKET));
				}
				player.swingMainHand();
				instance.getAdventureManager().sendSound(player, net.kyori.adventure.sound.Sound.Source.PLAYER,
						net.kyori.adventure.key.Key.key("minecraft:item.bucket.fill_lava"), 1, 1);
				player.updateInventory();
				HellblockPlayer pi = instance.getHellblockHandler().getActivePlayer(player);
				if (!pi.isChallengeActive(ChallengeType.INFINITE_LAVA_CHALLENGE)
						&& !pi.isChallengeCompleted(ChallengeType.INFINITE_LAVA_CHALLENGE)) {
					pi.beginChallengeProgression(ChallengeType.INFINITE_LAVA_CHALLENGE);
				} else {
					pi.updateChallengeProgression(ChallengeType.INFINITE_LAVA_CHALLENGE, 1);
					if (pi.isChallengeCompleted(ChallengeType.INFINITE_LAVA_CHALLENGE)) {
						pi.completeChallenge(ChallengeType.INFINITE_LAVA_CHALLENGE);
					}
				}

			}
		}
	}

	private boolean isLavaFall(@NonNull Block block) {
		boolean isLavaFall = false;
		Levelled level = (Levelled) block.getBlockData();
		isLavaFall = level.getLevel() >= 8 && level.getLevel() <= 15;
		return isLavaFall;
	}
}
