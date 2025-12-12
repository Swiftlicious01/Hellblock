package com.swiftlicious.hellblock.upgrades;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import com.swiftlicious.hellblock.HellblockPlugin;

/**
 * Handles the registration and execution of {@link CostHandler}s for validating
 * and deducting upgrade costs per player.
 * <p>
 * This processor supports multiple cost types (e.g., MONEY, EXP, ITEM) and
 * provides:
 * <ul>
 * <li>Affordability checks</li>
 * <li>Cost deduction</li>
 * <li>Formatted cost summaries</li>
 * </ul>
 */
public class UpgradeCostProcessor {

	private final Map<UpgradeCostType, CostHandler> handlers = new HashMap<>();
	private final Player player;

	/**
	 * Constructs a processor without a player context.
	 * <p>
	 * Use this for handler registration or non-player-bound operations.
	 */
	public UpgradeCostProcessor() {
		this(null);
	}

	/**
	 * Constructs a processor tied to a specific player.
	 * <p>
	 * This instance can then check and deduct costs for the provided player.
	 *
	 * @param player the player for whom cost checks and deductions will apply
	 */
	public UpgradeCostProcessor(@NotNull Player player) {
		this.player = player;
	}

	/**
	 * Registers a handler for a single cost type.
	 *
	 * @param type    the type of the upgrade cost (e.g., MONEY, EXP)
	 * @param handler the handler responsible for processing that cost
	 */
	public void registerHandler(@NotNull UpgradeCostType type, @NotNull CostHandler handler) {
		handlers.put(type, handler);
	}

	/**
	 * Registers a handler for multiple cost types (aliases).
	 *
	 * @param type    a list of cost types
	 * @param handler the handler responsible for processing those types
	 */
	public void registerHandler(@NotNull List<UpgradeCostType> type, @NotNull CostHandler handler) {
		type.forEach(cost -> handlers.put(cost, handler));
	}

	/**
	 * Checks if the player can afford all the specified costs.
	 *
	 * @param costs the list of upgrade costs to check
	 * @return {@code true} if the player can afford all; {@code false} otherwise
	 */
	public boolean canAfford(@NotNull List<UpgradeCost> costs) {
		return processPayment(costs).isSuccess();
	}

	/**
	 * Performs a dry-run affordability check for the provided costs.
	 * <p>
	 * Returns a {@link ProcessResult} indicating whether all costs are affordable,
	 * and if not, which requirements are missing.
	 *
	 * @param costs the list of costs to validate
	 * @return the result of the validation process
	 */
	@NotNull
	public ProcessResult processPayment(@NotNull List<UpgradeCost> costs) {
		final List<String> missing = new ArrayList<>();
		for (UpgradeCost cost : costs) {
			final CostHandler handler = handlers.get(cost.getType());
			if (handler == null) {
				HellblockPlugin.getInstance().getPluginLogger()
						.warn("No handler registered for cost type: " + cost.getType());
				missing.add("Unknown cost: " + cost.getType());
				continue;
			}
			if (player != null && !handler.canAfford(player, cost)) {
				missing.add(handler.describe(cost));
			}
		}
		return missing.isEmpty() ? ProcessResult.success() : ProcessResult.fail(missing);
	}

	/**
	 * Deducts all specified costs from the player, if handlers are registered.
	 *
	 * @param costs the list of upgrade costs to deduct
	 */
	public void deduct(@NotNull List<UpgradeCost> costs) {
		if (player == null) {
			return;
		}
		costs.forEach(cost -> {
			final CostHandler handler = handlers.get(cost.getType());
			if (handler != null) {
				handler.deduct(player, cost);
			}
		});
	}

	/**
	 * Generates a formatted string summarizing all costs using their handlers.
	 *
	 * @param costs the list of upgrade costs to summarize
	 * @return a human-readable summary of the costs
	 */
	@NotNull
	public String getCostSummary(@NotNull List<UpgradeCost> costs) {
		return CostFormatter.format(costs, handlers);
	}
}