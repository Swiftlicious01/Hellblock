package com.swiftlicious.hellblock.commands;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;

import org.bukkit.command.CommandSender;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.bukkit.CloudBukkitCapabilities;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.commands.sub.AboutCommand;
import com.swiftlicious.hellblock.commands.sub.AddStatisticsCommand;
import com.swiftlicious.hellblock.commands.sub.AdminAbandonCommand;
import com.swiftlicious.hellblock.commands.sub.AdminAbandonedCommand;
import com.swiftlicious.hellblock.commands.sub.AdminActivityCommand;
import com.swiftlicious.hellblock.commands.sub.AdminCleanupCommand;
import com.swiftlicious.hellblock.commands.sub.AdminDeleteCommand;
import com.swiftlicious.hellblock.commands.sub.AdminFixOwnerCommand;
import com.swiftlicious.hellblock.commands.sub.AdminForceBiomeCommand;
import com.swiftlicious.hellblock.commands.sub.AdminForceHomeCommand;
import com.swiftlicious.hellblock.commands.sub.AdminHelpCommand;
import com.swiftlicious.hellblock.commands.sub.AdminInspectCommand;
import com.swiftlicious.hellblock.commands.sub.AdminPurgeCommand;
import com.swiftlicious.hellblock.commands.sub.AdminResetCooldownCommand;
import com.swiftlicious.hellblock.commands.sub.AdminRestoreCommand;
import com.swiftlicious.hellblock.commands.sub.AdminRollbackCommand;
import com.swiftlicious.hellblock.commands.sub.AdminSetLevelCommand;
import com.swiftlicious.hellblock.commands.sub.AdminTeleportCommand;
import com.swiftlicious.hellblock.commands.sub.AdminTransferCommand;
import com.swiftlicious.hellblock.commands.sub.AdminUnlockCommand;
import com.swiftlicious.hellblock.commands.sub.CoopAcceptCommand;
import com.swiftlicious.hellblock.commands.sub.CoopCancelCommand;
import com.swiftlicious.hellblock.commands.sub.CoopHelpCommand;
import com.swiftlicious.hellblock.commands.sub.CoopInvitationsCommand;
import com.swiftlicious.hellblock.commands.sub.CoopInviteCommand;
import com.swiftlicious.hellblock.commands.sub.CoopKickCommand;
import com.swiftlicious.hellblock.commands.sub.CoopLeaveCommand;
import com.swiftlicious.hellblock.commands.sub.CoopOwnerCommand;
import com.swiftlicious.hellblock.commands.sub.CoopRejectCommand;
import com.swiftlicious.hellblock.commands.sub.CoopToggleCommand;
import com.swiftlicious.hellblock.commands.sub.CoopTrustCommand;
import com.swiftlicious.hellblock.commands.sub.CoopUntrustCommand;
import com.swiftlicious.hellblock.commands.sub.DebugBiomeCommand;
import com.swiftlicious.hellblock.commands.sub.DebugLootCommand;
import com.swiftlicious.hellblock.commands.sub.DebugNBTCommand;
import com.swiftlicious.hellblock.commands.sub.DebugSNBTCommand;
import com.swiftlicious.hellblock.commands.sub.DebugWorldsCommand;
import com.swiftlicious.hellblock.commands.sub.ExportDataCommand;
import com.swiftlicious.hellblock.commands.sub.GetItemCommand;
import com.swiftlicious.hellblock.commands.sub.GiveItemByUUIDCommand;
import com.swiftlicious.hellblock.commands.sub.GiveItemCommand;
import com.swiftlicious.hellblock.commands.sub.HellblockBanCommand;
import com.swiftlicious.hellblock.commands.sub.HellblockCommand;
import com.swiftlicious.hellblock.commands.sub.HellblockCreateCommand;
import com.swiftlicious.hellblock.commands.sub.HellblockDisplayToggleCommand;
import com.swiftlicious.hellblock.commands.sub.HellblockFixHomeCommand;
import com.swiftlicious.hellblock.commands.sub.HellblockHelpCommand;
import com.swiftlicious.hellblock.commands.sub.HellblockHomeCommand;
import com.swiftlicious.hellblock.commands.sub.HellblockHopperCommand;
import com.swiftlicious.hellblock.commands.sub.HellblockInfoCommand;
import com.swiftlicious.hellblock.commands.sub.HellblockIslandBioCommand;
import com.swiftlicious.hellblock.commands.sub.HellblockIslandNameCommand;
import com.swiftlicious.hellblock.commands.sub.HellblockLockCommand;
import com.swiftlicious.hellblock.commands.sub.HellblockNewHomeCommand;
import com.swiftlicious.hellblock.commands.sub.HellblockResetCommand;
import com.swiftlicious.hellblock.commands.sub.HellblockSetWarpCommand;
import com.swiftlicious.hellblock.commands.sub.HellblockTopCommand;
import com.swiftlicious.hellblock.commands.sub.HellblockUnbanCommand;
import com.swiftlicious.hellblock.commands.sub.HellblockUpgradePurchaseCommand;
import com.swiftlicious.hellblock.commands.sub.HellblockVisitCommand;
import com.swiftlicious.hellblock.commands.sub.HellblockVisitLogCommand;
import com.swiftlicious.hellblock.commands.sub.HellblockVisitorsCommand;
import com.swiftlicious.hellblock.commands.sub.HellblockWarpsCommand;
import com.swiftlicious.hellblock.commands.sub.ImportDataCommand;
import com.swiftlicious.hellblock.commands.sub.ImportItemCommand;
import com.swiftlicious.hellblock.commands.sub.OpenMarketCommand;
import com.swiftlicious.hellblock.commands.sub.QueryStatisticsCommand;
import com.swiftlicious.hellblock.commands.sub.ReloadCommand;
import com.swiftlicious.hellblock.commands.sub.ResetStatisticsCommand;
import com.swiftlicious.hellblock.commands.sub.SellFishCommand;
import com.swiftlicious.hellblock.commands.sub.SetStatisticsCommand;
import com.swiftlicious.hellblock.commands.sub.UnlockDataCommand;
import com.swiftlicious.hellblock.sender.Sender;

import net.kyori.adventure.util.Index;

public final class BukkitCommandManager extends AbstractCommandManager<CommandSender> {

	private final AdminRollbackCommand rollbackCommand;

	private final List<CommandFeature<CommandSender>> FEATURES = List.of(new ReloadCommand(this),
			new SellFishCommand(this), new GetItemCommand(this), new GiveItemCommand(this),
			new GiveItemByUUIDCommand(this), new ImportItemCommand(this), new AboutCommand(this),
			new OpenMarketCommand(this), new UnlockDataCommand(this), new ImportDataCommand(this),
			new ExportDataCommand(this), new AddStatisticsCommand(this), new SetStatisticsCommand(this),
			new ResetStatisticsCommand(this), new QueryStatisticsCommand(this), new DebugLootCommand(this),
			new DebugNBTCommand(this), new DebugBiomeCommand(this), new DebugSNBTCommand(this),
			new DebugWorldsCommand(this), new HellblockCommand(this), new AdminTeleportCommand(this),
			new AdminHelpCommand(this), rollbackCommand = new AdminRollbackCommand(this),
			rollbackCommand.new AdminRollbackListCommand(this), new AdminInspectCommand(this),
			new AdminUnlockCommand(this), new AdminForceBiomeCommand(this), new AdminForceHomeCommand(this),
			new AdminFixOwnerCommand(this), new AdminCleanupCommand(this), new AdminActivityCommand(this),
			new AdminRestoreCommand(this), new AdminAbandonCommand(this), new AdminAbandonedCommand(this),
			new AdminTransferCommand(this), new AdminSetLevelCommand(this), new AdminResetCooldownCommand(this),
			new AdminDeleteCommand(this), new AdminPurgeCommand(this), new CoopLeaveCommand(this),
			new CoopCancelCommand(this), new CoopKickCommand(this), new CoopRejectCommand(this),
			new CoopInviteCommand(this), new CoopOwnerCommand(this), new CoopTrustCommand(this),
			new CoopUntrustCommand(this), new CoopInvitationsCommand(this), new CoopToggleCommand(this),
			new CoopAcceptCommand(this), new CoopHelpCommand(this), new HellblockHelpCommand(this),
			new HellblockInfoCommand(this), new HellblockCreateCommand(this), new HellblockHopperCommand(this),
			new HellblockLockCommand(this), new HellblockTopCommand(this), new HellblockHomeCommand(this),
			new HellblockFixHomeCommand(this), new HellblockNewHomeCommand(this),
			new HellblockUpgradePurchaseCommand(this), new HellblockBanCommand(this), new HellblockUnbanCommand(this),
			new HellblockVisitCommand(this), new HellblockVisitorsCommand(this), new HellblockIslandBioCommand(this),
			new HellblockDisplayToggleCommand(this), new HellblockIslandNameCommand(this),
			new HellblockResetCommand(this), new HellblockVisitLogCommand(this), new HellblockWarpsCommand(this),
			new HellblockSetWarpCommand(this));

	private final Index<String, CommandFeature<CommandSender>> INDEX = Index.create(CommandFeature::getFeatureID,
			FEATURES);

	public BukkitCommandManager(HellblockPlugin plugin) {
		super(plugin, createManager(plugin));
		super.init();
	}

	@SuppressWarnings("unchecked")
	private static CommandManager<CommandSender> createManager(final HellblockPlugin plugin) {
		try {
			ClassLoader cloudLoader = plugin.getCloudDependencyHelper().getClassLoader();

			// Load Cloud classes from the isolated loader
			Class<?> legacyClass = Class.forName(
					"com.swiftlicious.hellblock.libraries.cloud.paper.LegacyPaperCommandManager", true, cloudLoader);
			Class<?> coordinatorClass = Class.forName(
					"com.swiftlicious.hellblock.libraries.cloud.execution.ExecutionCoordinator", true, cloudLoader);
			Class<?> senderMapperClass = Class.forName("com.swiftlicious.hellblock.libraries.cloud.SenderMapper", true,
					cloudLoader);

			// Find the constructor using org.bukkit.plugin.Plugin (not JavaPlugin)
			Constructor<?> matchingConstructor = null;
			for (Constructor<?> c : legacyClass.getConstructors()) {
				Class<?>[] params = c.getParameterTypes();
				if (params.length == 3 && "org.bukkit.plugin.Plugin".equals(params[0].getName())
						&& params[1].equals(coordinatorClass) && params[2].equals(senderMapperClass)) {
					matchingConstructor = c;
					break;
				}
			}

			if (matchingConstructor == null) {
				throw new NoSuchMethodException("No compatible LegacyPaperCommandManager constructor found!");
			}

			// Build ExecutionCoordinator.simpleCoordinator() & SenderMapper.identity()
			Object coordinator = coordinatorClass.getMethod("simpleCoordinator").invoke(null);
			Object mapper = senderMapperClass.getMethod("identity").invoke(null);

			// Instantiate the manager
			Object instance = matchingConstructor.newInstance(plugin, coordinator, mapper);
			CommandManager<CommandSender> manager = (CommandManager<CommandSender>) instance;

			// Register brigadier / async completions reflectively
			if (manager.hasCapability(CloudBukkitCapabilities.BRIGADIER)) {
				Method registerBrigadier = legacyClass.getMethod("registerBrigadier");
				registerBrigadier.invoke(instance);

				if (manager.hasCapability(CloudBukkitCapabilities.NATIVE_BRIGADIER)) {
					Method brigadierManager = legacyClass.getMethod("brigadierManager");
					Object brigadierMgrInstance = brigadierManager.invoke(instance);
					Method setNativeSuggestions = brigadierMgrInstance.getClass()
							.getMethod("setNativeNumberSuggestions", boolean.class);
					setNativeSuggestions.invoke(brigadierMgrInstance, true);
				}
			} else if (manager.hasCapability(CloudBukkitCapabilities.ASYNCHRONOUS_COMPLETION)) {
				Method registerAsync = legacyClass.getMethod("registerAsynchronousCompletions");
				registerAsync.invoke(instance);
			}

			return manager;

		} catch (Exception ex) {
			plugin.getPluginLogger().severe("Failed to initialize Cloud command manager.");
			throw new RuntimeException(ex);
		}
	}

	@Override
	protected Sender wrapSender(CommandSender sender) {
		return plugin.getSenderFactory().wrap(sender);
	}

	@Override
	public Index<String, CommandFeature<CommandSender>> getFeatures() {
		return INDEX;
	}
}