package com.swiftlicious.hellblock.commands.sub;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.incendo.cloud.Command;
import org.incendo.cloud.CommandManager;

import com.saicone.rtag.RtagItem;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.commands.BukkitCommandFeature;
import com.swiftlicious.hellblock.commands.HellblockCommandManager;
import com.swiftlicious.hellblock.config.locale.MessageConstants;
import com.swiftlicious.hellblock.utils.ItemStackUtils;

import net.kyori.adventure.text.Component;

public class DebugNBTCommand extends BukkitCommandFeature<CommandSender> {

	public DebugNBTCommand(HellblockCommandManager<CommandSender> commandManager) {
		super(commandManager);
	}

	@Override
	public Command.Builder<? extends CommandSender> assembleCommand(CommandManager<CommandSender> manager,
			Command.Builder<CommandSender> builder) {
		return builder.senderType(Player.class).handler(context -> {
			final Player player = context.sender();
			ItemStack item = player.getInventory().getItemInMainHand();
			if (item.getType() == Material.AIR) {
				handleFeedback(context, MessageConstants.COMMAND_DEBUG_NBT_NO_ITEM_IN_HAND);
				return;
			}
			List<String> list = new ArrayList<>();
			ItemStackUtils.mapToReadableStringList(new RtagItem(item).get(), list, true);
			for (String line : list) {
				HellblockPlugin.getInstance().getSenderFactory().wrap(player).sendMessage(Component.text(line));
			}
		});
	}

	@Override
	public String getFeatureID() {
		return "debug_nbt";
	}
}