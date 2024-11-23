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
import com.swiftlicious.hellblock.utils.ConfigUtils;

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
				HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
						"<red>Please hold an item to check its NBT tag data.");
				return;
			}
			List<String> list = new ArrayList<>();
			ConfigUtils.mapToReadableStringList(new RtagItem(item).get(), list, true);
			for (String line : list) {
				HellblockPlugin.getInstance().getAdventureManager().sendMessage(player, line);
			}
		});
	}

	@Override
	public String getFeatureID() {
		return "debug_nbt";
	}
}