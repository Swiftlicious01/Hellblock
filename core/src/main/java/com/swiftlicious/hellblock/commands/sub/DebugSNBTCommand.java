package com.swiftlicious.hellblock.commands.sub;

import com.saicone.rtag.item.ItemTagStream;
import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.commands.BukkitCommandFeature;
import com.swiftlicious.hellblock.commands.HellblockCommandManager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.incendo.cloud.Command;
import org.incendo.cloud.CommandManager;

public class DebugSNBTCommand extends BukkitCommandFeature<CommandSender> {

	public DebugSNBTCommand(HellblockCommandManager<CommandSender> commandManager) {
		super(commandManager);
	}

	@Override
	public Command.Builder<? extends CommandSender> assembleCommand(CommandManager<CommandSender> manager,
			Command.Builder<CommandSender> builder) {
		return builder.senderType(Player.class).handler(context -> {
			ItemStack itemStack = context.sender().getInventory().getItemInMainHand();
			if (itemStack == null || itemStack.getType() == Material.AIR)
				return;
			String snbt = ItemTagStream.INSTANCE.toString(itemStack);
			HellblockPlugin.getInstance().getSenderFactory().wrap(context.sender())
					.sendMessage(Component.text(snbt)
							.hoverEvent(HoverEvent.showText(Component.text("Copy").color(NamedTextColor.GREEN)))
							.clickEvent(ClickEvent.copyToClipboard(snbt)));
		});
	}

	@Override
	public String getFeatureID() {
		return "debug_snbt";
	}
}