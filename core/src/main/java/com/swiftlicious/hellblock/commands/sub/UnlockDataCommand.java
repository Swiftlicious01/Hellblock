package com.swiftlicious.hellblock.commands.sub;

import java.util.UUID;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.incendo.cloud.Command;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.bukkit.parser.PlayerParser;
import org.incendo.cloud.parser.standard.EitherParser;
import org.incendo.cloud.parser.standard.UUIDParser;
import org.incendo.cloud.type.Either;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.commands.BukkitCommandFeature;
import com.swiftlicious.hellblock.commands.HellblockCommandManager;
import com.swiftlicious.hellblock.config.locale.MessageConstants;

import net.kyori.adventure.text.Component;

public class UnlockDataCommand extends BukkitCommandFeature<CommandSender> {

	public UnlockDataCommand(HellblockCommandManager<CommandSender> commandManager) {
		super(commandManager);
	}

	@Override
	public Command.Builder<? extends CommandSender> assembleCommand(CommandManager<CommandSender> manager,
			Command.Builder<CommandSender> builder) {
		return builder.flag(manager.flagBuilder("silent").withAliases("s"))
				.required("uuid", EitherParser.eitherParser(UUIDParser.uuidParser(), PlayerParser.playerParser()))
				.handler(context -> {
					Either<UUID, Player> either = context.get("uuid");
					UUID uuid = either.primaryOrMapFallback(Entity::getUniqueId);
					HellblockPlugin.getInstance().getStorageManager().getDataSource().lockOrUnlockPlayerData(uuid,
							false);
					handleFeedback(context, MessageConstants.COMMAND_DATA_UNLOCK_SUCCESS,
							Component.text(uuid.toString()));
				});
	}

	@Override
	public String getFeatureID() {
		return "data_unlock";
	}
}