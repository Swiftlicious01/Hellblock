package com.swiftlicious.hellblock.player;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.config.locale.MessageConstants;
import com.swiftlicious.hellblock.config.locale.TranslationManager;
import com.swiftlicious.hellblock.generation.HellBiome;
import com.swiftlicious.hellblock.generation.IslandOptions;
import com.swiftlicious.hellblock.handlers.AdventureHelper;
import com.swiftlicious.hellblock.handlers.VersionHelper;
import com.swiftlicious.hellblock.handlers.VisitManager;
import com.swiftlicious.hellblock.handlers.VisitManager.VisitRecord;
import com.swiftlicious.hellblock.player.DisplaySettings.DisplayChoice;
import com.swiftlicious.hellblock.protection.HellblockFlag;
import com.swiftlicious.hellblock.protection.HellblockFlag.AccessType;
import com.swiftlicious.hellblock.protection.HellblockFlag.FlagType;
import com.swiftlicious.hellblock.sender.Sender;
import com.swiftlicious.hellblock.upgrades.IslandUpgradeType;
import com.swiftlicious.hellblock.upgrades.UpgradeCost;
import com.swiftlicious.hellblock.upgrades.UpgradeData;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

public class HellblockData {

	@Expose
	@SerializedName("id")
	protected int id;
	@Expose
	@SerializedName("level")
	protected float level;
	@Expose
	@SerializedName("exists")
	protected boolean hasHellblock;
	@Expose
	@SerializedName("owner")
	protected UUID ownerUUID;
	@Expose
	@SerializedName("linked")
	protected UUID linkedUUID;
	@Expose
	@SerializedName("bounds")
	protected BoundingBox boundingBox;
	@Expose
	@SerializedName("display")
	protected DisplaySettings display;
	@Expose
	@SerializedName("party")
	protected Set<UUID> party;
	@Expose
	@SerializedName("trusted")
	protected Set<UUID> trusted;
	@Expose
	@SerializedName("banned")
	protected Set<UUID> banned;
	@Expose
	@SerializedName("invitations")
	protected Map<UUID, Long> invitations;
	@Expose
	@SerializedName("flags")
	protected Map<FlagType, AccessType> flags;
	@Expose
	@SerializedName("upgrades")
	protected EnumMap<IslandUpgradeType, Integer> upgrades;
	@Expose
	@SerializedName("location")
	protected Location location;
	@Expose
	@SerializedName("home")
	protected Location home;
	@Expose
	@SerializedName("creation")
	protected long creationTime;
	@Expose
	@SerializedName("visitdata")
	protected VisitData visitData;
	@Expose
	@SerializedName("recentvisitors")
	protected List<VisitRecord> recentVisitors;
	@Expose
	@SerializedName("biome")
	protected HellBiome biome;
	@Expose
	@SerializedName("choice")
	protected IslandOptions choice;
	@Expose
	@SerializedName("schematic")
	protected String schematic;
	@Expose
	@SerializedName("locked")
	protected boolean locked;
	@Expose
	@SerializedName("abandoned")
	protected boolean abandoned;
	@Expose
	@SerializedName("resetcooldown")
	protected long resetCooldown;
	@Expose
	@SerializedName("biomecooldown")
	protected long biomeCooldown;
	@Expose
	@SerializedName("transfercooldown")
	protected long transferCooldown;

	public static final float DEFAULT_LEVEL = 1.0F;

	private static final long NO_EXPIRY = 0L;

	public HellblockData(int id, float level, boolean hasHellblock, UUID ownerUUID, UUID linkedUUID,
			BoundingBox boundingBox, DisplaySettings display, Set<UUID> party, Set<UUID> trusted, Set<UUID> banned,
			Map<UUID, Long> invitations, Map<FlagType, AccessType> flags, EnumMap<IslandUpgradeType, Integer> upgrades,
			Location location, Location home, long creationTime, VisitData visitData, List<VisitRecord> recentVisitors,
			HellBiome biome, IslandOptions choice, String schematic, boolean locked, boolean abandoned,
			long resetCooldown, long biomeCooldown, long transferCooldown) {
		this.id = id;
		this.level = level;
		this.hasHellblock = hasHellblock;
		this.ownerUUID = ownerUUID;
		this.linkedUUID = linkedUUID;
		this.boundingBox = boundingBox;
		this.display = display;
		this.party = party;
		this.trusted = trusted;
		this.banned = banned;
		this.invitations = invitations;
		this.flags = flags;
		this.upgrades = upgrades;
		this.location = location;
		this.home = home;
		this.creationTime = creationTime;
		this.visitData = visitData;
		this.recentVisitors = recentVisitors;
		this.biome = biome;
		this.choice = choice;
		this.schematic = schematic;
		this.locked = locked;
		this.abandoned = abandoned;
		this.resetCooldown = resetCooldown;
		this.biomeCooldown = biomeCooldown;
		this.transferCooldown = transferCooldown;
	}

	public boolean hasHellblock() {
		return this.hasHellblock;
	}

	public boolean isLocked() {
		return this.locked;
	}

	public boolean isAbandoned() {
		return this.abandoned;
	}

	public int getID() {
		return this.id;
	}

	public float getLevel() {
		return this.level;
	}

	public @NotNull VisitData getVisitData() {
		return this.visitData;
	}

	public @NotNull List<VisitRecord> getRecentVisitors() {
		return Collections.unmodifiableList(recentVisitors);
	}

	public @NotNull DisplaySettings getDisplaySettings() {
		return this.display;
	}

	public String getDefaultIslandBio() {
		final HellblockPlugin plugin = HellblockPlugin.getInstance();

		// 1. Resolve the base translation
		String template = plugin.getTranslationManager()
				.miniMessageTranslation(MessageConstants.MSG_HELLBLOCK_BIO_DEFAULT
						.arguments(Component.text(getResolvedOwnerName()), Component.text(this.level),
								Component.text(this.party.size()), Component.text(this.display.getIslandName()))
						.build().key());

		// 2. Fallback in case translation is missing
		if (template == null || template.isBlank()) {
			template = "<gray>Welcome to</gray> <red><arg:0></red><gray>'s Hellblock island.</gray>";
		}

		return template;
	}

	public String getDefaultIslandName() {
		final HellblockPlugin plugin = HellblockPlugin.getInstance();

		// 1. Resolve the base translation
		String template = plugin.getTranslationManager()
				.miniMessageTranslation(MessageConstants.MSG_HELLBLOCK_NAME_DEFAULT
						.arguments(Component.text(getResolvedOwnerName())).build().key());

		// 2. Fallback in case translation is missing
		if (template == null || template.isBlank()) {
			template = "<red><arg:0></red><dark_red>'s Hellblock</dark_red>";
		}

		return template;
	}

	public Component displayIslandBioWithContext() {
		final HellblockPlugin plugin = HellblockPlugin.getInstance();
		final String rawBio = Optional.of(getDisplaySettings().getIslandBio()).filter(b -> !b.isBlank())
				.orElseGet(this::getDefaultIslandBio);

		// Replace placeholders dynamically
		String resolved = rawBio.replace("<arg:0>", getResolvedOwnerName())
				.replace("<arg:1>", String.valueOf(getLevel())).replace("<arg:2>", String.valueOf(getParty().size()))
				.replace("<arg:3>", getDisplaySettings().getIslandName());

		try {
			return AdventureHelper.getMiniMessage().deserialize(resolved);
		} catch (Exception ex) {
			plugin.getPluginLogger()
					.warn("Failed to parse island bio for " + getResolvedOwnerName() + ": " + ex.getMessage());
			return Component.text(resolved);
		}
	}

	public Component displayIslandNameWithContext() {
		final HellblockPlugin plugin = HellblockPlugin.getInstance();
		final String rawName = Optional.of(getDisplaySettings().getIslandName()).filter(b -> !b.isBlank())
				.orElseGet(this::getDefaultIslandName);

		// Replace placeholders dynamically
		String resolved = rawName.replace("<arg:0>", getResolvedOwnerName());

		try {
			return AdventureHelper.getMiniMessage().deserialize(resolved);
		} catch (Exception ex) {
			plugin.getPluginLogger()
					.warn("Failed to parse island name for " + getResolvedOwnerName() + ": " + ex.getMessage());
			return Component.text(resolved);
		}
	}

	public void sendDisplayTextTo(Player viewer) {
		final HellblockPlugin plugin = HellblockPlugin.getInstance();
		final DisplaySettings settings = getDisplaySettings();
		final DisplayChoice choice = settings.getDisplayChoice();

		Component nameComponent = displayIslandNameWithContext();
		Component bioComponent = displayIslandBioWithContext();

		Sender audience = plugin.getSenderFactory().wrap(viewer);

		if (choice == DisplayChoice.CHAT) {
			AdventureHelper.sendCenteredMessage(audience, nameComponent);
			AdventureHelper.sendCenteredMessage(audience, bioComponent);
			return;
		}

		String titleJson = AdventureHelper.componentToJson(nameComponent);

		List<Component> subtitleChunks = splitComponentByLength(bioComponent, 80); // max 80 visible characters
		int intervalTicks = 70; // 3.5 seconds

		for (int i = 0; i < subtitleChunks.size(); i++) {
			Component chunk = subtitleChunks.get(i);
			String subtitleJson = AdventureHelper.componentToJson(chunk);
			int delay = i * intervalTicks;

			plugin.getScheduler().sync().runLater(() -> {
				try {
					VersionHelper.getNMSManager().sendTitle(viewer, titleJson, subtitleJson, 10, 40, 20);
				} catch (Exception ex) {
					plugin.getPluginLogger()
							.warn("Failed to send title to " + viewer.getName() + ": " + ex.getMessage());
					AdventureHelper.sendCenteredMessage(audience, nameComponent);
					AdventureHelper.sendCenteredMessage(audience, chunk);
				}
			}, delay, viewer.getLocation());
		}
	}

	private List<Component> splitComponentByLength(Component component, int maxVisibleLength) {
		List<Component> result = new ArrayList<>();

		List<Component> currentChunk = new ArrayList<>();
		int currentLength = 0;

		for (Component child : component.children()) {
			String plain = PlainTextComponentSerializer.plainText().serialize(child);
			String[] words = plain.split(" ");

			for (String word : words) {
				int wordLength = word.length() + 1; // +1 for space

				if (currentLength + wordLength > maxVisibleLength && !currentChunk.isEmpty()) {
					// Flush current chunk
					JoinConfiguration config = JoinConfiguration.builder().separator(Component.text(" ")).build();
					result.add(Component.join(config, currentChunk));
					currentChunk.clear();
					currentLength = 0;
				}

				// Preserve the original formatting by copying styles
				Component styledWord = Component.text(word).style(child.style()); // preserve color/bold/etc.
				currentChunk.add(styledWord);
				currentLength += wordLength;
			}
		}

		if (!currentChunk.isEmpty()) {
			JoinConfiguration config = JoinConfiguration.builder().separator(Component.text(" ")).build();
			result.add(Component.join(config, currentChunk));
		}

		return result;
	}

	public @Nullable HellBiome getBiome() {
		return this.biome;
	}

	public @Nullable IslandOptions getIslandChoice() {
		return this.choice;
	}

	public @Nullable String getUsedSchematic() {
		return this.schematic;
	}

	public long getCreation() {
		return this.creationTime;
	}

	public @NotNull String getCreationTime() {
		final LocalDateTime localDate = LocalDateTime.ofInstant(Instant.ofEpochMilli(this.creationTime),
				ZoneId.systemDefault());
		final Locale locale = HellblockPlugin.getInstance().getTranslationManager().parseLocale(
				HellblockPlugin.getInstance().getConfigManager().getMainConfig().getString("force-locale", ""));
		final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("KK:mm:ss a", locale);
		final String now = localDate.format(formatter);
		return "%s %s %s %s".formatted(localDate.getMonth().getDisplayName(TextStyle.FULL, locale),
				localDate.getDayOfMonth(), localDate.getYear(), now);
	}

	public long getResetCooldown() {
		return this.resetCooldown;
	}

	public long getBiomeCooldown() {
		return this.biomeCooldown;
	}

	public long getTransferCooldown() {
		return this.transferCooldown;
	}

	public @Nullable UUID getOwnerUUID() {
		return this.ownerUUID;
	}

	public String getResolvedOwnerName() {
		if (this.ownerUUID == null) {
			return HellblockPlugin.getInstance().getTranslationManager()
					.miniMessageTranslation(MessageConstants.FORMAT_UNKNOWN.build().key());
		}

		OfflinePlayer offline = Bukkit.getOfflinePlayer(this.ownerUUID);
		if (offline.hasPlayedBefore() && offline.getName() != null) {
			return offline.getName();
		}
		return HellblockPlugin.getInstance().getTranslationManager()
				.miniMessageTranslation(MessageConstants.FORMAT_UNKNOWN.build().key());
	}

	public boolean isOwner(@NotNull UUID id) {
		return this.ownerUUID.equals(id);
	}

	public @Nullable UUID getLinkedUUID() {
		return this.linkedUUID;
	}

	public @Nullable BoundingBox getBoundingBox() {
		return this.boundingBox;
	}

	public @Nullable Location getHellblockLocation() {
		return this.location;
	}

	public @Nullable Location getHomeLocation() {
		return this.home;
	}

	public @NotNull Set<UUID> getParty() {
		return this.party;
	}

	public boolean isInParty(@NotNull UUID id) {
		return this.party.contains(id);
	}

	public @NotNull Set<UUID> getPartyPlusOwner() {
		final Set<UUID> members = new HashSet<>();
		if (this.ownerUUID != null) {
			members.add(this.ownerUUID);
		}
		if (!this.party.isEmpty()) {
			members.addAll(this.party);
		}
		return members;
	}

	public @NotNull Set<UUID> getTrusted() {
		return this.trusted;
	}

	public @NotNull Set<UUID> getBanned() {
		return this.banned;
	}

	public @NotNull Set<UUID> getIslandMembers() {
		final Set<UUID> members = new HashSet<>();
		if (this.ownerUUID != null) {
			members.add(this.ownerUUID);
		}
		if (!this.party.isEmpty()) {
			members.addAll(this.party);
		}
		if (!this.trusted.isEmpty()) {
			members.addAll(this.trusted);
		}
		return members;
	}

	public boolean canAccess(@NotNull UUID playerID) {
		return getIslandMembers().contains(playerID);
	}

	public @NotNull Map<UUID, Long> getInvitations() {
		return this.invitations;
	}

	public boolean hasInvite(@NotNull UUID playerID) {
		return invitations.containsKey(playerID);
	}

	public boolean hasInviteExpired(@NotNull UUID playerID) {
		final Long expiry = invitations.get(playerID);
		if (expiry == null) {
			return false; // no invite
		}
		return expiry != NO_EXPIRY && System.currentTimeMillis() > expiry;
	}

	public @NotNull Map<FlagType, AccessType> getProtectionFlags() {
		return this.flags;
	}

	public @NotNull AccessType getProtectionValue(@NotNull FlagType flag) {
		return flags.getOrDefault(flag, flag.getDefaultValue() ? AccessType.ALLOW : AccessType.DENY);
	}

	public @Nullable String getProtectionData(@NotNull FlagType flag) {
		if (flag != FlagType.GREET_MESSAGE && flag != FlagType.FAREWELL_MESSAGE) {
			return null;
		}
		// Either return the data from stored flag (if present) or fall back to the
		// enum’s data
		return flags.containsKey(flag) ? flag.getData() : flag.getData();
	}

	public @NotNull EnumMap<IslandUpgradeType, Integer> getIslandUpgrades() {
		return this.upgrades;
	}

	public int getUpgradeLevel(@NotNull IslandUpgradeType type) {
		return this.upgrades.getOrDefault(type, 0);
	}

	public boolean canUpgrade(@NotNull IslandUpgradeType type) {
		return getUpgradeLevel(type) < HellblockPlugin.getInstance().getUpgradeManager().getMaxTierFor(type);
	}

	public Number getValue(@NotNull IslandUpgradeType type) {
		return HellblockPlugin.getInstance().getUpgradeManager().getTier(getUpgradeLevel(type)).getUpgrade(type)
				.getValue();
	}

	public Integer getIntValue(IslandUpgradeType type) {
		return (Integer) getValue(type);
	}

	public Double getDoubleValue(IslandUpgradeType type) {
		return (Double) getValue(type);
	}

	public @NotNull List<UpgradeCost> getCurrentCosts(@NotNull IslandUpgradeType type) {
		return HellblockPlugin.getInstance().getUpgradeManager().getTier(getUpgradeLevel(type)).getUpgrade(type)
				.getCosts();
	}

	public @NotNull List<UpgradeCost> getNextCosts(@NotNull IslandUpgradeType type) {
		final UpgradeData next = HellblockPlugin.getInstance().getUpgradeManager()
				.getNextUpgradeData(getUpgradeLevel(type), type);
		return (next != null) ? next.getCosts() : null;
	}

	/**
	 * Apply upgrade (increases stored level and applies bounds/values when needed).
	 * This method assumes permission/payment checks are done externally.
	 */
	public void applyUpgrade(@NotNull IslandUpgradeType type) {
		if (!canUpgrade(type)) {
			return;
		}

		// Increase stored tier for this upgrade type
		upgradeTier(type);

		final Player ownerPlayer = Bukkit.getPlayer(this.ownerUUID);
		if (ownerPlayer == null || !ownerPlayer.isOnline()) {
			throw new IllegalStateException("Owner is not online while trying to apply an upgrade.");
		}

		final Sender owner = HellblockPlugin.getInstance().getSenderFactory().wrap(ownerPlayer);
		final TranslationManager tm = HellblockPlugin.getInstance().getTranslationManager();

		final Component message;
		final Component memberMessage;
		Component arg;

		switch (type) {
		case PROTECTION_RANGE -> {
			int newRange = getMaxProtectionRange();
			expandBoundingBox(newRange);
			arg = Component.text(newRange);

			message = tm.render(MessageConstants.MSG_HELLBLOCK_UPGRADE_RANGE.arguments(arg).build());
			memberMessage = tm.render(MessageConstants.MSG_HELLBLOCK_UPGRADE_RANGE_MEMBER
					.arguments(Component.text(ownerPlayer.getName()), arg).build());
		}
		case PARTY_SIZE -> {
			int newPartySize = getMaxPartySize();
			arg = Component.text(newPartySize);

			message = tm.render(MessageConstants.MSG_HELLBLOCK_UPGRADE_PARTY.arguments(arg).build());
			memberMessage = tm.render(MessageConstants.MSG_HELLBLOCK_UPGRADE_PARTY_MEMBER
					.arguments(Component.text(ownerPlayer.getName()), arg).build());
		}
		case HOPPER_LIMIT -> {
			int newHopperLimit = getMaxHopperLimit();
			arg = Component.text(newHopperLimit);

			message = tm.render(MessageConstants.MSG_HELLBLOCK_UPGRADE_HOPPER.arguments(arg).build());
			memberMessage = tm.render(MessageConstants.MSG_HELLBLOCK_UPGRADE_HOPPER_MEMBER
					.arguments(Component.text(ownerPlayer.getName()), arg).build());
		}
		case GENERATOR_CHANCE -> {
			double newGeneratorChance = getNewGeneratorChance();
			arg = Component.text(newGeneratorChance);

			message = tm.render(MessageConstants.MSG_HELLBLOCK_UPGRADE_GENERATOR.arguments(arg).build());
			memberMessage = tm.render(MessageConstants.MSG_HELLBLOCK_UPGRADE_GENERATOR_MEMBER
					.arguments(Component.text(ownerPlayer.getName()), arg).build());
		}
		case PIGLIN_BARTERING -> {
			double piglinBarteringChance = getNewBarteringChance();
			arg = Component.text(piglinBarteringChance);

			message = tm.render(MessageConstants.MSG_HELLBLOCK_UPGRADE_BARTERING.arguments(arg).build());
			memberMessage = tm.render(MessageConstants.MSG_HELLBLOCK_UPGRADE_BARTERING_MEMBER
					.arguments(Component.text(ownerPlayer.getName()), arg).build());
		}
		case CROP_GROWTH -> {
			double cropGrowthRate = getNewCropGrowthRate();
			arg = Component.text(cropGrowthRate);

			message = tm.render(MessageConstants.MSG_HELLBLOCK_UPGRADE_CROP.arguments(arg).build());
			memberMessage = tm.render(MessageConstants.MSG_HELLBLOCK_UPGRADE_CROP_MEMBER
					.arguments(Component.text(ownerPlayer.getName()), arg).build());
		}
		case MOB_SPAWN_RATE -> {
			double mobSpawnRate = getNewMobSpawningRate();
			arg = Component.text(mobSpawnRate);

			message = tm.render(MessageConstants.MSG_HELLBLOCK_UPGRADE_MOB.arguments(arg).build());
			memberMessage = tm.render(MessageConstants.MSG_HELLBLOCK_UPGRADE_MOB_MEMBER
					.arguments(Component.text(ownerPlayer.getName()), arg).build());
		}
		default -> {
			return; // Safety for future upgrades
		}
		}

		// Send message to owner
		owner.sendMessage(message);

		// Send message to all online party members
		this.party.stream().map(Bukkit::getPlayer).filter(member -> member != null && member.isOnline())
				.map(member -> HellblockPlugin.getInstance().getSenderFactory().wrap(member))
				.forEach(sender -> sender.sendMessage(memberMessage));
	}

	/**
	 * Expands or rebuilds the island bounding box to match the absolute protection
	 * range.
	 *
	 * @param newRange The absolute protection range (from upgrades).
	 */
	public void expandBoundingBox(int newRange) {
		final Location center = getHellblockLocation();
		final World world = center.getWorld();

		if (world == null) {
			throw new IllegalStateException("World not loaded for island center");
		}

		final double half = newRange / 2.0;

		// Create a new Bukkit BoundingBox using absolute coordinates
		this.boundingBox = new BoundingBox(center.getX() - half, world.getMinHeight(), center.getZ() - half,
				center.getX() + half, world.getMaxHeight(), center.getZ() + half);
	}

	public int getMaxPartySize() {
		return getIntValue(IslandUpgradeType.PARTY_SIZE);
	}

	public int getMaxPlayersIncludingOwner() {
		return 1 + getMaxPartySize();
	}

	public int getMaxProtectionRange() {
		return getIntValue(IslandUpgradeType.PROTECTION_RANGE);
	}

	public int getMaxHopperLimit() {
		return getIntValue(IslandUpgradeType.HOPPER_LIMIT);
	}

	public double getNewGeneratorChance() {
		return getDoubleValue(IslandUpgradeType.GENERATOR_CHANCE);
	}

	public double getNewBarteringChance() {
		return getDoubleValue(IslandUpgradeType.PIGLIN_BARTERING);
	}

	public double getNewCropGrowthRate() {
		return getDoubleValue(IslandUpgradeType.CROP_GROWTH);
	}

	public double getNewMobSpawningRate() {
		return getDoubleValue(IslandUpgradeType.MOB_SPAWN_RATE);
	}

	public void setDefaultHellblockData(boolean hasHellblock, @Nullable Location hellblockLocation, int hellblockID) {
		this.hasHellblock = hasHellblock;
		this.location = hellblockLocation;
		this.id = hellblockID;
		this.level = HellblockData.DEFAULT_LEVEL;
		this.biome = HellBiome.NETHER_WASTES;
		this.display = new DisplaySettings(getDefaultIslandName(), getDefaultIslandBio(), DisplayChoice.CHAT);
		this.display.setAsDefaultIslandName();
		this.display.setAsDefaultIslandBio();
	}

	public void transferHellblockData(@NotNull UserData transferee) {
		this.id = transferee.getHellblockData().id;
		this.hasHellblock = transferee.getHellblockData().hasHellblock;
		this.display = transferee.getHellblockData().display;
		if (this.display.isDefaultIslandBio() && !this.display.getIslandBio().contains(getResolvedOwnerName())) {
			this.display.setIslandBio(getDefaultIslandBio());
			this.display.setAsDefaultIslandBio();
			HellblockPlugin.getInstance()
					.debug("Updated island bio for " + getResolvedOwnerName() + " due to ownership transfer.");
		}
		if (this.display.isDefaultIslandName() && !this.display.getIslandName().contains(getResolvedOwnerName())) {
			this.display.setIslandName(getDefaultIslandName());
			this.display.setAsDefaultIslandName();
			HellblockPlugin.getInstance()
					.debug("Updated island name for " + getResolvedOwnerName() + " due to ownership transfer.");
		}
		this.location = transferee.getHellblockData().location;
		this.home = transferee.getHellblockData().home;
		this.level = transferee.getHellblockData().level;
		this.party = transferee.getHellblockData().party;
		this.trusted = transferee.getHellblockData().trusted;
		this.banned = transferee.getHellblockData().banned;
		this.flags = transferee.getHellblockData().flags;
		this.upgrades = transferee.getHellblockData().upgrades;
		this.biome = transferee.getHellblockData().biome;
		this.biomeCooldown = transferee.getHellblockData().biomeCooldown;
		this.resetCooldown = transferee.getHellblockData().resetCooldown;
		this.creationTime = transferee.getHellblockData().creationTime;
		final BoundingBox oldBoundingBox = this.boundingBox;
		this.boundingBox = transferee.getHellblockData().boundingBox;
		this.ownerUUID = transferee.getHellblockData().ownerUUID;
		this.choice = transferee.getHellblockData().choice;
		this.schematic = transferee.getHellblockData().schematic;
		this.locked = transferee.getHellblockData().locked;
		this.visitData = transferee.getHellblockData().visitData;
		this.recentVisitors = transferee.getHellblockData().recentVisitors;
		HellblockPlugin.getInstance().getHopperHandler().transferHoppers(oldBoundingBox,
				transferee.getHellblockData().boundingBox);
	}

	public void resetHellblockData() {
		this.hasHellblock = false;
		this.location = null;
		this.home = null;
		this.level = 0.0F;
		this.party.clear();
		this.trusted.clear();
		this.banned.clear();
		this.flags.clear();
		this.upgrades.clear();
		setDefaultUpgradeTiers();
		this.biome = null;
		this.biomeCooldown = 0L;
		this.creationTime = 0L;
		HellblockPlugin.getInstance().getHopperHandler().clearHoppers(this.boundingBox);
		this.boundingBox = null;
		this.ownerUUID = null;
		this.choice = null;
		this.schematic = null;
		this.locked = false;
		getVisitData().reset();
		this.recentVisitors.clear();
	}

	public void setHasHellblock(boolean hasHellblock) {
		this.hasHellblock = hasHellblock;
	}

	public void setLockedStatus(boolean locked) {
		this.locked = locked;
	}

	public void setAsAbandoned(boolean abandoned) {
		this.abandoned = abandoned;
	}

	public void setID(int id) {
		this.id = id;
	}

	public void setLevel(float level) {
		this.level = level;
	}

	public void increaseIslandLevel() {
		this.level++;
	}

	public void decreaseIslandLevel() {
		this.level--;
	}

	public void addToLevel(float levels) {
		this.level = this.level + levels;
	}

	public void removeFromLevel(float levels) {
		this.level = this.level - levels;
	}

	public void setVisitData(@NotNull VisitData visitData) {
		this.visitData = visitData;
	}

	public void addVisitor(@NotNull UUID visitorId) {
		VisitManager visitManager = HellblockPlugin.getInstance().getVisitManager();
		recentVisitors.add(visitManager.new VisitRecord(visitorId));
		if (recentVisitors.size() > 50) {
			recentVisitors.remove(0); // remove oldest
		}
	}

	public void cleanupOldVisitors(long cutoff) {
		recentVisitors.removeIf(record -> record.getTimestamp() < cutoff);
	}

	public void setDisplaySettings(@Nullable DisplaySettings display) {
		this.display = display;
	}

	public void setBiome(@Nullable HellBiome biome) {
		this.biome = biome;
	}

	public void setIslandChoice(@Nullable IslandOptions choice) {
		this.choice = choice;
	}

	public void setUsedSchematic(@Nullable String schematic) {
		this.schematic = schematic;
	}

	public void setCreation(long creationTime) {
		this.creationTime = creationTime;
	}

	public void setResetCooldown(long resetCooldown) {
		this.resetCooldown = resetCooldown;
	}

	public void setBiomeCooldown(long biomeCooldown) {
		this.biomeCooldown = biomeCooldown;
	}

	public void setTransferCooldown(long transferCooldown) {
		this.transferCooldown = transferCooldown;
	}

	public void resetAllCooldowns() {
		this.resetCooldown = 0L;
		this.biomeCooldown = 0L;
		this.transferCooldown = 0L;
	}

	public void setOwnerUUID(@Nullable UUID ownerUUID) {
		this.ownerUUID = ownerUUID;
	}

	public void setLinkedUUID(@Nullable UUID linkedUUID) {
		this.linkedUUID = linkedUUID;
	}

	public void setBoundingBox(@Nullable BoundingBox boundingBox) {
		this.boundingBox = boundingBox;
	}

	public void setHellblockLocation(@Nullable Location location) {
		this.location = location;
	}

	public void setHomeLocation(@Nullable Location home) {
		this.home = home;
	}

	public void addToParty(@NotNull UUID newMember) {
		if (!this.party.contains(newMember)) {
			this.party.add(newMember);
		}
	}

	public void kickFromParty(@NotNull UUID oldMember) {
		if (this.party.contains(oldMember)) {
			this.party.remove(oldMember);
		}
	}

	public void setParty(@NotNull Set<UUID> partyMembers) {
		this.party = partyMembers;
	}

	public void addTrustPermission(@NotNull UUID newTrustee) {
		if (!this.trusted.contains(newTrustee)) {
			this.trusted.add(newTrustee);
		}
	}

	public void removeTrustPermission(@NotNull UUID oldTrustee) {
		if (this.trusted.contains(oldTrustee)) {
			this.trusted.remove(oldTrustee);
		}
	}

	public void setTrusted(@NotNull Set<UUID> trustedMembers) {
		this.trusted = trustedMembers;
	}

	public void banPlayer(@NotNull UUID bannedPlayer) {
		if (!this.banned.contains(bannedPlayer)) {
			this.banned.add(bannedPlayer);
		}
	}

	public void unbanPlayer(@NotNull UUID unbannedPlayer) {
		if (this.banned.contains(unbannedPlayer)) {
			this.banned.remove(unbannedPlayer);
		}
	}

	public void setBanned(@NotNull Set<UUID> bannedPlayers) {
		this.banned = bannedPlayers;
	}

	public void setInvitations(@NotNull Map<UUID, Long> invitations) {
		this.invitations = invitations;
	}

	public void sendInvitation(@NotNull UUID playerID) {
		this.invitations.putIfAbsent(playerID, TimeUnit.SECONDS.toDays(86400));
	}

	public void removeInvitation(@NotNull UUID playerID) {
		if (this.invitations.containsKey(playerID)) {
			this.invitations.remove(playerID);
		}
	}

	public void clearInvitations() {
		this.invitations.clear();
	}

	public void setProtectionFlags(@NotNull Map<FlagType, AccessType> flags) {
		this.flags = flags;
	}

	public void setProtectionValue(@NotNull HellblockFlag flag) {
		final FlagType type = flag.getFlag();
		final AccessType defaultValue = type.getDefaultValue() ? AccessType.ALLOW : AccessType.DENY;

		if (flag.getStatus() != defaultValue) {
			this.flags.put(type, flag.getStatus());
		} else {
			this.flags.remove(type);
		}
	}

	public void setIslandUpgrades(@NotNull EnumMap<IslandUpgradeType, Integer> upgrades) {
		this.upgrades = upgrades;
	}

	public void upgradeTier(IslandUpgradeType type) {
		this.upgrades.put(type, getUpgradeLevel(type) + 1);
	}

	public void setDefaultUpgradeTiers() {
		for (IslandUpgradeType upgrade : IslandUpgradeType.values()) {
			this.upgrades.put(upgrade, 0);
		}
	}

	/**
	 * Creates an instance of HellblockData with default values (empty values).
	 *
	 * @return a new instance of HellblockData with default values.
	 */
	public static @NotNull HellblockData empty() {
		return new HellblockData(0, 0.0F, false, null, null, null, new DisplaySettings("", "", DisplayChoice.CHAT),
				new HashSet<>(), new HashSet<>(), new HashSet<>(), new HashMap<>(), new HashMap<>(),
				new EnumMap<>(IslandUpgradeType.class), null, null, 0L, new VisitData(), new ArrayList<>(), null, null,
				null, false, false, 0L, 0L, 0L);
	}

	public @NotNull HellblockData copy() {
		return new HellblockData(id, level, hasHellblock, ownerUUID, linkedUUID, boundingBox, display, party, trusted,
				banned, invitations, flags, upgrades, location, home, creationTime, visitData, recentVisitors, biome,
				choice, schematic, locked, abandoned, resetCooldown, biomeCooldown, transferCooldown);
	}
}