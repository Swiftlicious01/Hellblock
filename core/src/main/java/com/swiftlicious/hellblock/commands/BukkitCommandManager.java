package com.swiftlicious.hellblock.commands;

import java.util.List;

import org.bukkit.command.CommandSender;
import org.incendo.cloud.SenderMapper;
import org.incendo.cloud.bukkit.CloudBukkitCapabilities;
import org.incendo.cloud.execution.ExecutionCoordinator;
import org.incendo.cloud.paper.LegacyPaperCommandManager;
import org.incendo.cloud.setting.ManagerSetting;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.commands.sub.AboutCommand;
import com.swiftlicious.hellblock.commands.sub.AddStatisticsCommand;
import com.swiftlicious.hellblock.commands.sub.AdminDeleteCommand;
import com.swiftlicious.hellblock.commands.sub.AdminHelpCommand;
import com.swiftlicious.hellblock.commands.sub.AdminPurgeCommand;
import com.swiftlicious.hellblock.commands.sub.AdminTeleportCommand;
import com.swiftlicious.hellblock.commands.sub.CoopAcceptCommand;
import com.swiftlicious.hellblock.commands.sub.CoopCancelCommand;
import com.swiftlicious.hellblock.commands.sub.CoopHelpCommand;
import com.swiftlicious.hellblock.commands.sub.CoopInvitationsCommand;
import com.swiftlicious.hellblock.commands.sub.CoopInviteCommand;
import com.swiftlicious.hellblock.commands.sub.CoopKickCommand;
import com.swiftlicious.hellblock.commands.sub.CoopLeaveCommand;
import com.swiftlicious.hellblock.commands.sub.CoopOwnerCommand;
import com.swiftlicious.hellblock.commands.sub.CoopRejectCommand;
import com.swiftlicious.hellblock.commands.sub.CoopTrustCommand;
import com.swiftlicious.hellblock.commands.sub.CoopUntrustCommand;
import com.swiftlicious.hellblock.commands.sub.DebugBiomeCommand;
import com.swiftlicious.hellblock.commands.sub.DebugLootCommand;
import com.swiftlicious.hellblock.commands.sub.DebugNBTCommand;
import com.swiftlicious.hellblock.commands.sub.DebugWorldsCommand;
import com.swiftlicious.hellblock.commands.sub.ExportDataCommand;
import com.swiftlicious.hellblock.commands.sub.GetItemCommand;
import com.swiftlicious.hellblock.commands.sub.GiveItemCommand;
import com.swiftlicious.hellblock.commands.sub.HellblockBanCommand;
import com.swiftlicious.hellblock.commands.sub.HellblockCommand;
import com.swiftlicious.hellblock.commands.sub.HellblockCreateCommand;
import com.swiftlicious.hellblock.commands.sub.HellblockFixHomeCommand;
import com.swiftlicious.hellblock.commands.sub.ImportDataCommand;
import com.swiftlicious.hellblock.commands.sub.ImportItemCommand;
import com.swiftlicious.hellblock.commands.sub.OpenMarketCommand;
import com.swiftlicious.hellblock.commands.sub.QueryStatisticsCommand;
import com.swiftlicious.hellblock.commands.sub.ReloadCommand;
import com.swiftlicious.hellblock.commands.sub.ResetStatisticsCommand;
import com.swiftlicious.hellblock.commands.sub.SellFishCommand;
import com.swiftlicious.hellblock.commands.sub.SetStatisticsCommand;
import com.swiftlicious.hellblock.commands.sub.UnlockDataCommand;
import com.swiftlicious.hellblock.commands.sub.HellblockHelpCommand;
import com.swiftlicious.hellblock.commands.sub.HellblockHomeCommand;
import com.swiftlicious.hellblock.commands.sub.HellblockInfoCommand;
import com.swiftlicious.hellblock.commands.sub.HellblockLockCommand;
import com.swiftlicious.hellblock.commands.sub.HellblockNewHomeCommand;
import com.swiftlicious.hellblock.commands.sub.HellblockResetCommand;
import com.swiftlicious.hellblock.commands.sub.HellblockTopCommand;
import com.swiftlicious.hellblock.commands.sub.HellblockUnbanCommand;
import com.swiftlicious.hellblock.commands.sub.HellblockVisitCommand;
import com.swiftlicious.hellblock.sender.Sender;

import net.kyori.adventure.util.Index;

public class BukkitCommandManager extends AbstractCommandManager<CommandSender> {

	private final HellblockResetCommand resetCommand;

	private final List<CommandFeature<CommandSender>> FEATURES = List.of(new ReloadCommand(this),
			new SellFishCommand(this), new GetItemCommand(this), new GiveItemCommand(this), new ImportItemCommand(this),
			new AboutCommand(this), new OpenMarketCommand(this), new UnlockDataCommand(this),
			new ImportDataCommand(this), new ExportDataCommand(this), new AddStatisticsCommand(this),
			new SetStatisticsCommand(this), new ResetStatisticsCommand(this), new QueryStatisticsCommand(this),
			new DebugLootCommand(this), new DebugNBTCommand(this), new DebugBiomeCommand(this),
			new DebugWorldsCommand(this), new HellblockCommand(this), new AdminTeleportCommand(this),
			new AdminHelpCommand(this), new AdminDeleteCommand(this), new AdminPurgeCommand(this),
			new CoopLeaveCommand(this), new CoopCancelCommand(this), new CoopKickCommand(this),
			new CoopRejectCommand(this), new CoopInviteCommand(this), new CoopOwnerCommand(this),
			new CoopTrustCommand(this), new CoopUntrustCommand(this), new CoopInvitationsCommand(this),
			new CoopAcceptCommand(this), new CoopHelpCommand(this), new HellblockHelpCommand(this),
			new HellblockInfoCommand(this), new HellblockCreateCommand(this), new HellblockLockCommand(this),
			new HellblockTopCommand(this), new HellblockHomeCommand(this), new HellblockFixHomeCommand(this),
			new HellblockNewHomeCommand(this), new HellblockBanCommand(this), new HellblockUnbanCommand(this),
			new HellblockVisitCommand(this), resetCommand = new HellblockResetCommand(this),
			resetCommand.new HellblockResetConfirmCommand(this));

	private final Index<String, CommandFeature<CommandSender>> INDEX = Index.create(CommandFeature::getFeatureID,
			FEATURES);

	public BukkitCommandManager(HellblockPlugin plugin) {
		super(plugin, new LegacyPaperCommandManager<>(plugin, ExecutionCoordinator.simpleCoordinator(),
				SenderMapper.identity()));
		final LegacyPaperCommandManager<CommandSender> manager = (LegacyPaperCommandManager<CommandSender>) getCommandManager();
		manager.settings().set(ManagerSetting.ALLOW_UNSAFE_REGISTRATION, true);
		if (manager.hasCapability(CloudBukkitCapabilities.NATIVE_BRIGADIER)) {
			manager.registerBrigadier();
			manager.brigadierManager().setNativeNumberSuggestions(true);
		} else if (manager.hasCapability(CloudBukkitCapabilities.ASYNCHRONOUS_COMPLETION)) {
			manager.registerAsynchronousCompletions();
		}
	}

	@Override
	protected Sender wrapSender(CommandSender sender) {
		return ((HellblockPlugin) plugin).getSenderFactory().wrap(sender);
	}

	@Override
	public Index<String, CommandFeature<CommandSender>> getFeatures() {
		return INDEX;
	}
}