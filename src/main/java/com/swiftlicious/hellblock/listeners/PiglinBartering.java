package com.swiftlicious.hellblock.listeners;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Piglin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PiglinBarterEvent;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.challenges.HellblockChallenge.ChallengeType;
import com.swiftlicious.hellblock.playerdata.HellblockPlayer;

public class PiglinBartering implements Listener {

	private final HellblockPlugin instance;

	private final Set<Material> netherBarteringItems;

	public PiglinBartering(HellblockPlugin plugin) {
		instance = plugin;
		this.netherBarteringItems = new HashSet<>();
		Bukkit.getPluginManager().registerEvents(this, instance);
	}

	private Set<Material> getNetherBarteringItems() {
		if (!this.netherBarteringItems.isEmpty())
			return this.netherBarteringItems;
		Set<String> barteringItems = new HashSet<>(
				instance.getConfig("config.yml").getStringList("piglin-bartering.materials"));
		for (String barterItem : barteringItems) {
			Material mat = Material.getMaterial(barterItem);
			if (mat == null)
				continue;
			this.netherBarteringItems.add(mat);
		}
		return this.netherBarteringItems;
	}

	@EventHandler
	public void onBarter(PiglinBarterEvent event) {
		final Piglin piglin = event.getEntity();
		if (!piglin.getWorld().getName().equalsIgnoreCase(instance.getHellblockHandler().getWorldName()))
			return;

		if (!getNetherBarteringItems().isEmpty())
			getNetherBarteringItems().forEach(barter -> piglin.addBarterMaterial(barter));
		if (event.getOutcome().stream().filter(item -> getNetherBarteringItems().contains(item.getType())).findAny()
				.isPresent()) {
			Collection<Player> playersNearby = piglin.getWorld().getNearbyPlayers(piglin.getLocation(), 10, 10, 10);
			Player player = instance.getNetherrackGeneratorHandler().getClosestPlayer(piglin.getLocation(),
					playersNearby);
			if (player != null) {
				HellblockPlayer pi = instance.getHellblockHandler().getActivePlayer(player);
				if (!pi.isChallengeActive(ChallengeType.NETHER_TRADING_CHALLENGE)
						&& !pi.isChallengeCompleted(ChallengeType.NETHER_TRADING_CHALLENGE)) {
					pi.beginChallengeProgression(ChallengeType.NETHER_TRADING_CHALLENGE);
				} else {
					pi.updateChallengeProgression(ChallengeType.NETHER_TRADING_CHALLENGE, 1);
					if (pi.isChallengeCompleted(ChallengeType.NETHER_TRADING_CHALLENGE)) {
						pi.completeChallenge(ChallengeType.NETHER_TRADING_CHALLENGE);
					}
				}
			}
		}
	}
}
