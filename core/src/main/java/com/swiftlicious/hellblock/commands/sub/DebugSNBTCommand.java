package com.swiftlicious.hellblock.commands.sub;

import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.incendo.cloud.Command;
import org.incendo.cloud.CommandManager;

import com.saicone.rtag.item.ItemTagStream;
import com.swiftlicious.hellblock.commands.BukkitCommandFeature;
import com.swiftlicious.hellblock.commands.HellblockCommandManager;
import com.swiftlicious.hellblock.config.locale.MessageConstants;
import com.swiftlicious.hellblock.handlers.AdventureHelper;

import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;

public class DebugSNBTCommand extends BukkitCommandFeature<CommandSender> {

	public DebugSNBTCommand(HellblockCommandManager<CommandSender> commandManager) {
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
			String snbt = ItemTagStream.INSTANCE.toString(item);
			plugin.getSenderFactory().wrap(context.sender()).sendMessage(AdventureHelper.miniMessageToComponent(snbt)
					.hoverEvent(HoverEvent.showText(AdventureHelper.miniMessageToComponent("Copy").color(NamedTextColor.GREEN)))
					.clickEvent(ClickEvent.copyToClipboard(snbt)));
		});
	}

	@Override
	public String getFeatureID() {
		return "debug_snbt";
	}
}