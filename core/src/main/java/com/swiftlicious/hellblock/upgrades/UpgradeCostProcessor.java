package com.swiftlicious.hellblock.upgrades;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import com.swiftlicious.hellblock.HellblockPlugin;

public class UpgradeCostProcessor {

	private final Map<String, CostHandler> handlers = new HashMap<>();

	public UpgradeCostProcessor() {
		this.player = null;
	}

	public void registerHandler(String type, CostHandler handler) {
		handlers.put(type.toUpperCase(), handler);
	}

	public void registerHandler(List<String> type, CostHandler handler) {
		type.forEach(cost -> handlers.put(cost.toUpperCase(), handler));
	}

	private final Player player;

	public UpgradeCostProcessor(@NotNull Player player) {
		this.player = player;
	}

	public boolean canAfford(List<UpgradeCost> costs) {
		return processPayment(costs).isSuccess();
	}

	public ProcessResult processPayment(List<UpgradeCost> costs) {
		final List<String> missing = new ArrayList<>();
		for (UpgradeCost cost : costs) {
			final CostHandler handler = handlers.get(cost.getType());
			if (handler == null) {
				HellblockPlugin.getInstance().getPluginLogger()
						.warn("No handler registered for cost type: " + cost.getType());
				missing.add("Unknown cost: " + cost.getType());
				continue;
			}
			if (!handler.canAfford(player, cost)) {
				missing.add(handler.describe(cost));
			}
		}
		return missing.isEmpty() ? ProcessResult.success() : ProcessResult.fail(missing);
	}

	public void deduct(List<UpgradeCost> costs) {
		costs.forEach(cost -> {
			final CostHandler handler = handlers.get(cost.getType());
			if (handler != null) {
				handler.deduct(player, cost);
			}
		});
	}

	public String getCostSummary(List<UpgradeCost> costs) {
		return CostFormatter.format(costs, handlers);
	}
}