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
import com.swiftlicious.hellblock.generation.HellBiome;
import com.swiftlicious.hellblock.player.HellblockData;
import com.swiftlicious.hellblock.player.UserData;

import net.kyori.adventure.text.Component;

public class HellblockBiomeCommand extends BukkitCommandFeature<CommandSender> {

	public HellblockBiomeCommand(HellblockCommandManager<CommandSender> commandManager) {
		super(commandManager);
	}

	@Override
	public Command.Builder<? extends CommandSender> assembleCommand(CommandManager<CommandSender> manager,
			Command.Builder<CommandSender> builder) {
		return builder.senderType(Player.class).required("biome", StringParser.stringComponent()
				.suggestionProvider((context, input) -> CompletableFuture.completedFuture(
						Arrays.stream(HellBiome.values()).map(Enum::toString).map(Suggestion::suggestion).toList())))
				.handler(context -> {
					final Player player = context.sender();
					final Optional<UserData> onlineUserOpt = HellblockPlugin.getInstance().getStorageManager()
							.getOnlineUser(player.getUniqueId());

					if (onlineUserOpt.isEmpty()) {
						handleFeedback(context, MessageConstants.COMMAND_DATA_FAILURE_NOT_LOADED);
						return;
					}

					final UserData onlineUser = onlineUserOpt.get();
					final HellblockData data = onlineUser.getHellblockData();

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

					if (!data.isOwner(ownerUUID)) {
						handleFeedback(context, MessageConstants.MSG_NOT_OWNER_OF_HELLBLOCK);
						return;
					}

					if (data.isAbandoned()) {
						handleFeedback(context, MessageConstants.MSG_HELLBLOCK_IS_ABANDONED);
						return;
					}

					final String biomeInput = context.getOrDefault("biome", "NETHER_WASTES").toUpperCase();
					final Optional<HellBiome> biomeOpt = Arrays.stream(HellBiome.values())
							.filter(b -> b.name().equalsIgnoreCase(biomeInput)).findFirst();

					if (biomeOpt.isEmpty()) {
						handleFeedback(context, MessageConstants.MSG_HELLBLOCK_INVALID_BIOME);
						return;
					}

					final HellBiome biome = biomeOpt.get();

					if (data.getBiome() == biome) {
						handleFeedback(context, MessageConstants.MSG_HELLBLOCK_BIOME_SAME_BIOME
								.arguments(Component.text(biome.getName())));
						return;
					}

					if (data.getBiomeCooldown() > 0) {
						handleFeedback(context, MessageConstants.MSG_HELLBLOCK_BIOME_ON_COOLDOWN.arguments(Component
								.text(HellblockPlugin.getInstance().getFormattedCooldown(data.getBiomeCooldown()))));
						return;
					}

					HellblockPlugin.getInstance().getBiomeHandler().changeHellblockBiome(onlineUser, biome, false, false);
				});
	}

	@Override
	public String getFeatureID() {
		return "hellblock_biome";
	}
}