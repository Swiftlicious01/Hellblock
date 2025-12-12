package com.swiftlicious.hellblock.commands.sub;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.incendo.cloud.Command;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.parser.standard.StringParser;

import com.swiftlicious.hellblock.commands.BukkitCommandFeature;
import com.swiftlicious.hellblock.commands.HellblockCommandManager;
import com.swiftlicious.hellblock.config.locale.MessageConstants;
import com.swiftlicious.hellblock.handlers.AdventureHelper;
import com.swiftlicious.hellblock.utils.ItemStackUtils;

import dev.dejvokep.boostedyaml.YamlDocument;

public class ImportItemCommand extends BukkitCommandFeature<CommandSender> {

	public ImportItemCommand(HellblockCommandManager<CommandSender> commandManager) {
		super(commandManager);
	}

	@Override
	public Command.Builder<? extends CommandSender> assembleCommand(CommandManager<CommandSender> manager,
			Command.Builder<CommandSender> builder) {
		return builder.senderType(Player.class).required("id", StringParser.stringParser())
				.flag(manager.flagBuilder("silent").withAliases("s").build()).handler(context -> {
					Player player = context.sender();
					ItemStack item = player.getInventory().getItemInMainHand();
					String id = context.get("id");
					if (item.getType() == Material.AIR) {
						handleFeedback(context, MessageConstants.COMMAND_ITEM_IMPORT_FAILURE_NO_ITEM);
						return;
					}
					File saved = new File(plugin.getDataFolder(), "imported_items.yml");
					if (!saved.exists()) {
						try {
							saved.createNewFile();
						} catch (IOException e) {
							throw new RuntimeException(e);
						}
					}
					YamlDocument document = plugin.getConfigManager().loadData(saved);
					Map<String, Object> map = ItemStackUtils.itemStackToMap(item);
					document.set(id, map);
					try {
						document.save(saved);
						handleFeedback(context, MessageConstants.COMMAND_ITEM_IMPORT_SUCCESS,
								AdventureHelper.miniMessageToComponent(id));
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
				});
	}

	@Override
	public String getFeatureID() {
		return "import_item";
	}
}