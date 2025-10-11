package com.swiftlicious.hellblock.commands.sub;

import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.incendo.cloud.Command;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.parser.standard.StringParser;
import org.incendo.cloud.suggestion.Suggestion;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.commands.BukkitCommandFeature;
import com.swiftlicious.hellblock.commands.HellblockCommandManager;
import com.swiftlicious.hellblock.config.locale.MessageConstants;
import com.swiftlicious.hellblock.player.HellblockData;
import com.swiftlicious.hellblock.player.UserData;
import com.swiftlicious.hellblock.upgrades.IslandUpgradeType;
import com.swiftlicious.hellblock.upgrades.UpgradeCostProcessor;
import com.swiftlicious.hellblock.utils.StringUtils;

import net.kyori.adventure.text.Component;

public class HellblockUpgradePurchaseCommand extends BukkitCommandFeature<CommandSender> {

	public HellblockUpgradePurchaseCommand(HellblockCommandManager<CommandSender> commandManager) {
		super(commandManager);
	}

	@Override
	public Command.Builder<? extends CommandSender> assembleCommand(CommandManager<CommandSender> manager,
			Command.Builder<CommandSender> builder) {
		return builder.senderType(Player.class)
				.required("upgrade",
						StringParser.stringComponent().suggestionProvider((context,
								input) -> CompletableFuture.completedFuture(Arrays.stream(IslandUpgradeType.values())
										.map(Enum::toString).map(Suggestion::suggestion).toList())))
				.handler(context -> {
					final Player player = context.sender();
					final UUID playerUUID = player.getUniqueId();

					final Optional<UserData> onlineUserOpt = HellblockPlugin.getInstance().getStorageManager()
							.getOnlineUser(playerUUID);

					if (onlineUserOpt.isEmpty()) {
						handleFeedback(context, MessageConstants.COMMAND_DATA_FAILURE_NOT_LOADED);
						return;
					}

					final UserData user = onlineUserOpt.get();
					final HellblockData data = user.getHellblockData();

					if (!data.hasHellblock()) {
						handleFeedback(context, MessageConstants.MSG_HELLBLOCK_NOT_FOUND);
						return;
					}

					final UUID ownerUUID = data.getOwnerUUID();
					if (ownerUUID == null) {
						HellblockPlugin.getInstance().getPluginLogger()
								.severe("Hellblock owner UUID was null for player " + player.getName() + " ("
										+ player.getUniqueId() + "). This indicates corrupted data or a serious bug.");
						throw new IllegalStateException(
								"Owner reference was null. This should never happen — please report to the developer.");
					}

					if (!data.isOwner(playerUUID)) {
						handleFeedback(context, MessageConstants.MSG_NOT_OWNER_OF_HELLBLOCK);
						return;
					}

					if (data.isAbandoned()) {
						handleFeedback(context, MessageConstants.MSG_HELLBLOCK_IS_ABANDONED);
						return;
					}

					final String upgradeInput = context.getOrDefault("upgrade", IslandUpgradeType.HOPPER_LIMIT.name())
							.toUpperCase();
					final Optional<IslandUpgradeType> upgradeOpt = Arrays.stream(IslandUpgradeType.values())
							.filter(up -> up.name().equalsIgnoreCase(upgradeInput)).findFirst();

					if (upgradeOpt.isEmpty()) {
						handleFeedback(context, MessageConstants.MSG_HELLBLOCK_INVALID_UPGRADE);
						return;
					}

					final IslandUpgradeType upgrade = upgradeOpt.get();

					if (!data.canUpgrade(upgrade)) {
						handleFeedback(context, MessageConstants.MSG_HELLBLOCK_UPGRADE_MAX_TIER.arguments(
								Component.text(StringUtils.toProperCase(upgrade.toString().replace("_", " ")))));
						return;
					}

					final UpgradeCostProcessor payment = new UpgradeCostProcessor(player);
					if (!payment.canAfford(data.getNextCosts(upgrade))) {
						handleFeedback(context, MessageConstants.MSG_HELLBLOCK_UPGRADE_CANNOT_AFFORD);
						return;
					}

					HellblockPlugin.getInstance().getUpgradeManager().attemptPurchase(data, player, upgrade);
				});
	}

	@Override
	public String getFeatureID() {
		return "hellblock_upgrade";
	}
}
