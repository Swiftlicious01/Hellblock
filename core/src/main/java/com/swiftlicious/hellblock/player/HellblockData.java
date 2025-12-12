package com.swiftlicious.hellblock.player;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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
import com.swiftlicious.hellblock.sender.MessagePair;
import com.swiftlicious.hellblock.sender.Sender;
import com.swiftlicious.hellblock.upgrades.IslandUpgradeType;
import com.swiftlicious.hellblock.upgrades.UpgradeCost;
import com.swiftlicious.hellblock.upgrades.UpgradeData;
import com.swiftlicious.hellblock.utils.adapters.HellblockTypeAdapterFactory.EmptyCheck;
import com.swiftlicious.hellblock.world.HellblockWorld;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;

/**
 * Represents the complete persistent data model for a Hellblock island.
 * <p>
 * This class encapsulates all major island-related metadata, including:
 * <ul>
 * <li>Ownership and membership information (owner, party, trusted, banned)</li>
 * <li>Island progression and upgrade state (level, upgrades, flags)</li>
 * <li>Location data (island location, home, bounding box)</li>
 * <li>Display settings (name, bio, visibility preferences)</li>
 * <li>Cooldowns and timestamps (creation time, reset cooldown, biome cooldown,
 * etc.)</li>
 * <li>Event state tracking (invasion, wither, skysiege data)</li>
 * <li>Visit records and invitation handling</li>
 * <li>Biome and schematic selection</li>
 * <li>Island linking (Nether portal travel to other islands)</li>
 * </ul>
 *
 * <p>
 * This object is intended to be serialized and deserialized via your storage
 * backend (e.g., YAML, database, or JSON). It acts as the central structure
 * that supports island-related logic in both runtime and persistence layers.
 * All changes to island state should go through this object to ensure
 * consistency.
 * </p>
 *
 * <p>
 * Implements {@link EmptyCheck} to determine whether the island has meaningful
 * data or is in a default/uninitialized state.
 * </p>
 */
public class HellblockData implements EmptyCheck {

	@Expose
	@SerializedName("islandId")
	protected int id;

	@Expose
	@SerializedName("islandLevel")
	protected float level;

	@Expose
	@SerializedName("hellblockExists")
	protected boolean hasHellblock;

	@Expose
	@SerializedName("ownerUUID")
	@Nullable
	protected UUID ownerUUID;

	@Expose
	@SerializedName("linkedPortalUUID")
	@Nullable
	protected UUID linkedPortalUUID;

	@Expose
	@SerializedName("islandBounds")
	@Nullable
	protected BoundingBox boundingBox;

	@Expose
	@SerializedName("displaySettings")
	@NotNull
	protected DisplaySettings display;

	@Expose
	@SerializedName("chatPreference")
	@NotNull
	protected CoopChatSetting chat;

	@Expose
	@SerializedName("partyMembers")
	@NotNull
	protected Set<UUID> party;

	@Expose
	@SerializedName("trustedMembers")
	@NotNull
	protected Set<UUID> trusted;

	@Expose
	@SerializedName("bannedMembers")
	@NotNull
	protected Set<UUID> banned;

	@Expose
	@SerializedName("islandInvitations")
	@NotNull
	protected Map<UUID, Long> invitations;

	@Expose
	@SerializedName("protectionFlags")
	@NotNull
	protected EnumMap<FlagType, HellblockFlag> flags;

	@Expose
	@SerializedName("islandUpgrades")
	@NotNull
	protected EnumMap<IslandUpgradeType, Integer> upgrades;

	@Expose
	@SerializedName("hellblockLocation")
	@Nullable
	protected Location location;

	@Expose
	@SerializedName("homeLocation")
	@Nullable
	protected Location home;

	@Expose
	@SerializedName("creationTime")
	protected long creationTime;

	@Expose
	@SerializedName("visitData")
	@NotNull
	protected VisitData visitData;

	@Expose
	@SerializedName("recentVisitors")
	@NotNull
	protected List<VisitRecord> recentVisitors;

	@Expose
	@SerializedName("islandBiome")
	@Nullable
	protected HellBiome biome;

	@Expose
	@SerializedName("islandChoice")
	@Nullable
	protected IslandOptions choice;

	@Expose
	@SerializedName("usedSchematic")
	@Nullable
	protected String schematic;

	@Expose
	@SerializedName("isLocked")
	protected boolean locked;

	@Expose
	@SerializedName("isAbandoned")
	protected boolean abandoned;

	@Expose
	@SerializedName("resetCooldown")
	protected long resetCooldown;

	@Expose
	@SerializedName("biomeCooldown")
	protected long biomeCooldown;

	@Expose
	@SerializedName("transferCooldown")
	protected long transferCooldown;

	@Expose
	@SerializedName("lastIslandActivity")
	protected long lastIslandActivity;

	@Expose
	@SerializedName("lastWorldAccess")
	protected long lastWorldAccess;

	@Expose
	@SerializedName("invasionData")
	@NotNull
	protected InvasionData invasionData;

	@Expose
	@SerializedName("witherData")
	@NotNull
	protected WitherData witherData;

	@Expose
	@SerializedName("skysiegeData")
	@NotNull
	protected SkysiegeData skysiegeData;

	public transient static final float DEFAULT_LEVEL = 1.0F;
	private transient static final long NO_EXPIRY = 0L;
	private transient static final long ACTIVITY_UPDATE_INTERVAL = 60 * 1000L; // 1 minute

	/**
	 * `preservedBoundingBox` is excluded from copying and serialization because it
	 * is only used at runtime to temporarily store the original bounding box during
	 * island resets. This allows the island to retain its previous location without
	 * triggering a new placement search. It is not part of persistent data.
	 */
	@Nullable
	private transient BoundingBox preservedBoundingBox = null;

	/**
	 * Constructs a new {@link HellblockData} object, representing the full state of
	 * a player's island (hellblock).
	 *
	 * @param id                 The unique island ID.
	 * @param level              The current island level.
	 * @param hasHellblock       Whether the player has an active hellblock island.
	 * @param ownerUUID          The UUID of the island owner.
	 * @param linkedPortalUUID   The UUID of a linked user, if any (for shared
	 *                           nether portal travel between each other).
	 * @param boundingBox        The bounding box defining the island region.
	 * @param display            Display settings for island name, bio, and display
	 *                           type.
	 * @param chat               The player's island chat setting (e.g., global or
	 *                           party).
	 * @param party              Set of UUIDs representing island party members.
	 * @param trusted            Set of UUIDs representing trusted players.
	 * @param banned             Set of UUIDs representing banned players.
	 * @param invitations        Map of pending island invitations with their
	 *                           timestamp.
	 * @param flags              Protection flags mapping {@link FlagType} to
	 *                           {@link HellblockFlag}.
	 * @param upgrades           Map of upgrade tiers for each
	 *                           {@link IslandUpgradeType}.
	 * @param location           The island's base location that it was created at
	 *                           (aka the center of the island).
	 * @param home               The island home location.
	 * @param creationTime       The time the island was created (epoch millis).
	 * @param visitData          Visit statistics and warp location.
	 * @param recentVisitors     List of recent visitors to the island.
	 * @param biome              The biome type of the island.
	 * @param choice             The chosen island template option.
	 * @param schematic          The name of the schematic used to generate the
	 *                           island.
	 * @param locked             Whether the island is currently locked.
	 * @param abandoned          Whether the island is marked as abandoned.
	 * @param resetCooldown      The cooldown timestamp for resetting the island.
	 * @param biomeCooldown      The cooldown timestamp for changing biome.
	 * @param transferCooldown   The cooldown timestamp for ownership transfer.
	 * @param lastIslandActivity The last island based activity of the player
	 *                           online.
	 * @param lastWorldAccess    THe last time the world was meaningfully accessed.
	 * @param invasionData       Data related to past invasions on the island.
	 * @param witherData         Data related to wither boss spawns and fights.
	 * @param skysiegeData       Data related to skysiege events on the island.
	 */
	public HellblockData(int id, float level, boolean hasHellblock, @Nullable UUID ownerUUID,
			@Nullable UUID linkedPortalUUID, @Nullable BoundingBox boundingBox, @NotNull DisplaySettings display,
			@NotNull CoopChatSetting chat, @NotNull Set<UUID> party, @NotNull Set<UUID> trusted,
			@NotNull Set<UUID> banned, @NotNull Map<UUID, Long> invitations,
			@NotNull EnumMap<FlagType, HellblockFlag> flags, @NotNull EnumMap<IslandUpgradeType, Integer> upgrades,
			@Nullable Location location, @Nullable Location home, long creationTime, @NotNull VisitData visitData,
			@NotNull List<VisitRecord> recentVisitors, @Nullable HellBiome biome, @Nullable IslandOptions choice,
			@Nullable String schematic, boolean locked, boolean abandoned, long resetCooldown, long biomeCooldown,
			long transferCooldown, long lastIslandActivity, long lastWorldAccess, @NotNull InvasionData invasionData,
			@NotNull WitherData witherData, @NotNull SkysiegeData skysiegeData) {
		this.id = id;
		this.level = level;
		this.hasHellblock = hasHellblock;
		this.ownerUUID = ownerUUID;
		this.linkedPortalUUID = linkedPortalUUID;
		this.boundingBox = boundingBox != null ? boundingBox.clone() : null;
		this.display = display.copy();
		this.chat = chat;
		this.party = new HashSet<>(party);
		this.trusted = new HashSet<>(trusted);
		this.banned = new HashSet<>(banned);
		this.invitations = new HashMap<>(invitations);
		this.flags = new EnumMap<>(FlagType.class);
		flags.forEach((key, value) -> this.flags.put(key, value.copy()));
		this.upgrades = new EnumMap<>(upgrades);
		this.location = location != null ? location.clone() : null;
		this.home = home != null ? home.clone() : null;
		this.creationTime = creationTime;
		this.visitData = visitData.copy();
		this.recentVisitors = recentVisitors.stream().map(VisitRecord::copy).toList();
		this.biome = biome;
		this.choice = choice;
		this.schematic = schematic;
		this.locked = locked;
		this.abandoned = abandoned;
		this.resetCooldown = resetCooldown;
		this.biomeCooldown = biomeCooldown;
		this.transferCooldown = transferCooldown;
		this.lastIslandActivity = lastIslandActivity;
		this.lastWorldAccess = lastWorldAccess;
		this.invasionData = invasionData.copy();
		this.witherData = witherData.copy();
		this.skysiegeData = skysiegeData.copy();
	}

	/**
	 * Checks whether this player currently owns or is part of an active Hellblock
	 * island.
	 *
	 * @return {@code true} if the player has an active Hellblock; {@code false}
	 *         otherwise.
	 */
	public boolean hasHellblock() {
		return this.hasHellblock;
	}

	/**
	 * Checks whether this Hellblock island is locked. Locked islands prevent
	 * interaction and access by other players.
	 *
	 * @return {@code true} if the island is locked.
	 */
	public boolean isLocked() {
		return this.locked;
	}

	/**
	 * Checks whether this Hellblock island is marked as abandoned.
	 *
	 * @return {@code true} if the island has been abandoned.
	 */
	public boolean isAbandoned() {
		return this.abandoned;
	}

	/**
	 * Gets the unique ID of this Hellblock island.
	 *
	 * @return the island ID.
	 */
	public int getIslandId() {
		return this.id;
	}

	/**
	 * Gets the current level of this Hellblock island.
	 *
	 * @return the island's level as a floating-point number.
	 */
	public float getIslandLevel() {
		return this.level;
	}

	/**
	 * Gets the invasion data associated with this Hellblock island.
	 *
	 * @return a non-null {@link InvasionData} object.
	 */
	@NotNull
	public InvasionData getInvasionData() {
		if (this.invasionData == null) {
			return InvasionData.empty();
		}
		return this.invasionData;
	}

	/**
	 * Gets the wither event data for this Hellblock island.
	 *
	 * @return a non-null {@link WitherData} object.
	 */
	@NotNull
	public WitherData getWitherData() {
		if (this.witherData == null) {
			return WitherData.empty();
		}
		return this.witherData;
	}

	/**
	 * Gets the skysiege event data for this Hellblock island.
	 *
	 * @return a non-null {@link SkysiegeData} object.
	 */
	@NotNull
	public SkysiegeData getSkysiegeData() {
		if (this.skysiegeData == null) {
			return SkysiegeData.empty();
		}
		return this.skysiegeData;
	}

	/**
	 * Gets the visitor statistics and warp information for this Hellblock island.
	 *
	 * @return a non-null {@link VisitData} object containing visitor info.
	 */
	@NotNull
	public VisitData getVisitData() {
		if (this.visitData == null) {
			return VisitData.empty();
		}
		return this.visitData;
	}

	/**
	 * Gets a read-only list of recent visitors to this island.
	 *
	 * @return an unmodifiable {@link List} of {@link VisitRecord} entries.
	 */
	@NotNull
	public List<VisitRecord> getRecentVisitors() {
		if (this.recentVisitors == null) {
			return new ArrayList<>();
		}
		return Collections.unmodifiableList(this.recentVisitors);
	}

	/**
	 * Gets the current display settings for this island, including name, bio, and
	 * display choice (e.g., chat or title).
	 *
	 * @return the {@link DisplaySettings} for this island.
	 */
	@NotNull
	public DisplaySettings getDisplaySettings() {
		if (this.display == null) {
			return DisplaySettings.empty();
		}
		return this.display;
	}

	/**
	 * Gets the chat preference for this player, such as whether messages are sent
	 * in global or coop chat.
	 *
	 * @return the current {@link CoopChatSetting}.
	 */
	@NotNull
	public CoopChatSetting getChatSetting() {
		if (this.chat == null) {
			return CoopChatSetting.GLOBAL;
		}
		return this.chat;
	}

	/**
	 * Builds the default island biography text if no custom bio is set.
	 *
	 * @return the formatted default bio text as a string.
	 */
	@NotNull
	public String getDefaultIslandBio() {
		final HellblockPlugin plugin = HellblockPlugin.getInstance();

		// 1. Resolve the base translation
		String template = plugin.getTranslationManager()
				.miniMessageTranslation(
						MessageConstants.MSG_HELLBLOCK_BIO_DEFAULT
								.arguments(AdventureHelper.miniMessageToComponent(getResolvedOwnerName()),
										AdventureHelper.miniMessageToComponent(String.valueOf(getIslandLevel())),
										AdventureHelper.miniMessageToComponent(String.valueOf(getPartyMembers().size())),
										AdventureHelper.miniMessageToComponent(getDisplaySettings().getIslandName()))
								.build().key());

		// 2. Fallback in case translation is missing
		if (template == null || template.isBlank()) {
			template = "<gray>Welcome to</gray> <red><arg:0></red><gray>'s Hellblock island.</gray>";
		}

		return template;
	}

	/**
	 * Builds the default island display name if none is set.
	 *
	 * @return the formatted default island name.
	 */
	@NotNull
	public String getDefaultIslandName() {
		final HellblockPlugin plugin = HellblockPlugin.getInstance();

		// 1. Resolve the base translation
		String template = plugin.getTranslationManager()
				.miniMessageTranslation(MessageConstants.MSG_HELLBLOCK_NAME_DEFAULT
						.arguments(AdventureHelper.miniMessageToComponent(getResolvedOwnerName())).build().key());

		// 2. Fallback in case translation is missing
		if (template == null || template.isBlank()) {
			template = "<red><arg:0></red><dark_red>'s Hellblock</dark_red>";
		}

		return template;
	}

	/**
	 * Builds and returns the full island bio text as a MiniMessage component, with
	 * placeholders replaced dynamically (e.g. owner name, level, etc).
	 *
	 * @return a {@link Component} representing the island bio.
	 */
	@NotNull
	public Component displayIslandBioWithContext() {
		final String rawBio = Optional.of(getDisplaySettings().getIslandBio()).filter(b -> !b.isBlank())
				.orElseGet(this::getDefaultIslandBio);

		// Replace placeholders dynamically
		String resolved = rawBio.replace("<arg:0>", getResolvedOwnerName())
				.replace("<arg:1>", String.valueOf(getIslandLevel()))
				.replace("<arg:2>", String.valueOf(getPartyMembers().size()))
				.replace("<arg:3>", getDisplaySettings().getIslandName());

		return AdventureHelper.miniMessageToComponent(resolved);
	}

	/**
	 * Builds and returns the island name text as a MiniMessage component, replacing
	 * placeholders with dynamic context (e.g. owner name).
	 *
	 * @return a {@link Component} representing the island name.
	 */
	@NotNull
	public Component displayIslandNameWithContext() {
		final String rawName = Optional.of(getDisplaySettings().getIslandName()).filter(b -> !b.isBlank())
				.orElseGet(this::getDefaultIslandName);

		// Replace placeholders dynamically
		String resolved = rawName.replace("<arg:0>", getResolvedOwnerName());

		return AdventureHelper.miniMessageToComponent(resolved);
	}

	/**
	 * Sends the island's display name and bio to a player viewer, either via chat
	 * messages or as titles based on their display preference.
	 *
	 * @param viewer the {@link Player} viewing the island.
	 */
	public void sendDisplayTextTo(@NotNull Player viewer) {
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

	/**
	 * Splits a {@link Component} into smaller chunks that each fit within a
	 * specified maximum visible length. This is useful when sending long messages
	 * as titles/subtitles.
	 * 
	 * It preserves the formatting of the original component's children and attempts
	 * to split at word boundaries for readability.
	 *
	 * @param component        the base component to split.
	 * @param maxVisibleLength the maximum visible character length per chunk.
	 * @return a list of {@link Component} chunks, each within the length limit.
	 */
	@NotNull
	private List<Component> splitComponentByLength(@NotNull Component component, int maxVisibleLength) {
		List<Component> result = new ArrayList<>();

		List<Component> currentChunk = new ArrayList<>();
		int currentLength = 0;

		for (Component child : component.children()) {
			String plain = AdventureHelper.componentToPlainText(child);
			String[] words = plain.split(" ");

			for (String word : words) {
				int wordLength = word.length() + 1; // +1 for space

				if (currentLength + wordLength > maxVisibleLength && !currentChunk.isEmpty()) {
					// Flush current chunk
					JoinConfiguration config = JoinConfiguration.builder().separator(AdventureHelper.miniMessageToComponent(" "))
							.build();
					result.add(Component.join(config, currentChunk));
					currentChunk.clear();
					currentLength = 0;
				}

				// Preserve the original formatting by copying styles
				// preserve color/bold/etc
				Component styledWord = AdventureHelper.miniMessageToComponent(word).style(child.style());
				currentChunk.add(styledWord);
				currentLength += wordLength;
			}
		}

		if (!currentChunk.isEmpty()) {
			JoinConfiguration config = JoinConfiguration.builder().separator(AdventureHelper.miniMessageToComponent(" ")).build();
			result.add(Component.join(config, currentChunk));
		}

		return result;
	}

	/**
	 * Gets the biome type of this Hellblock island.
	 *
	 * @return the {@link HellBiome}, or {@code null} if not set.
	 */
	@Nullable
	public HellBiome getBiome() {
		return this.biome;
	}

	/**
	 * Gets the island template or generation option used for this island.
	 *
	 * @return the {@link IslandOptions} selected during creation, or {@code null}.
	 */
	@Nullable
	public IslandOptions getIslandChoice() {
		return this.choice;
	}

	/**
	 * Gets the schematic file name used for island generation.
	 *
	 * @return the schematic file name, or {@code null} if none was used.
	 */
	@Nullable
	public String getUsedSchematic() {
		return this.schematic;
	}

	/**
	 * Gets the timestamp when this island was created.
	 *
	 * @return creation time in milliseconds since epoch.
	 */
	public long getCreationTime() {
		return this.creationTime;
	}

	/**
	 * Formats and returns the island creation date as a human-readable string,
	 * based on the server's configured locale.
	 *
	 * @return formatted creation date string.
	 */
	@NotNull
	public String getCreationTimeFormatted() {
		final LocalDateTime localDate = LocalDateTime.ofInstant(Instant.ofEpochMilli(getCreationTime()),
				ZoneId.systemDefault());
		final Locale locale = HellblockPlugin.getInstance().getTranslationManager().getForcedLocale();
		final DateTimeFormatter formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.LONG, FormatStyle.MEDIUM)
				.withLocale(locale);
		return localDate.format(formatter);
	}

	/**
	 * Gets the remaining reset cooldown time for this island.
	 *
	 * @return the reset cooldown in milliseconds.
	 */
	public long getResetCooldown() {
		return this.resetCooldown;
	}

	/**
	 * Gets the remaining biome change cooldown for this island.
	 *
	 * @return the biome cooldown in milliseconds.
	 */
	public long getBiomeCooldown() {
		return this.biomeCooldown;
	}

	/**
	 * Gets the remaining island transfer cooldown time.
	 *
	 * @return the transfer cooldown in milliseconds.
	 */
	public long getTransferCooldown() {
		return this.transferCooldown;
	}

	/**
	 * Gets the last activity based on their island activity.
	 * 
	 * @return the last island based activity for the player.
	 */
	public long getLastIslandActivity() {
		return this.lastIslandActivity;
	}

	/**
	 * Retrieves the last recorded timestamp when this island's world was accessed.
	 *
	 * <p>
	 * The value is stored as epoch time in milliseconds (UTC) and is used for
	 * determining world inactivity for purging or statistics.
	 *
	 * @return the last access time in milliseconds since epoch
	 */
	public long getLastWorldAccess() {
		return this.lastWorldAccess;
	}

	/**
	 * Gets the UUID of the island owner.
	 *
	 * @return the owner's {@link UUID}, or {@code null} if none.
	 */
	@Nullable
	public UUID getOwnerUUID() {
		return this.ownerUUID;
	}

	/**
	 * Resolves and returns the owner's player name, or a localized "Unknown"
	 * fallback.
	 *
	 * @return the resolved owner name.
	 */
	@NotNull
	public String getResolvedOwnerName() {
		if (getOwnerUUID() == null) {
			return HellblockPlugin.getInstance().getTranslationManager()
					.miniMessageTranslation(MessageConstants.FORMAT_UNKNOWN.build().key());
		}

		OfflinePlayer offline = Bukkit.getOfflinePlayer(getOwnerUUID());
		if (offline.hasPlayedBefore() && offline.getName() != null) {
			return offline.getName();
		}
		return HellblockPlugin.getInstance().getTranslationManager()
				.miniMessageTranslation(MessageConstants.FORMAT_UNKNOWN.build().key());
	}

	/**
	 * Checks if the given UUID is the owner of this island.
	 *
	 * @param playerId the UUID to check.
	 * @return {@code true} if the UUID matches the island owner.
	 */
	public boolean isOwner(@NotNull UUID playerId) {
		return getOwnerUUID() != null && getOwnerUUID().equals(playerId);
	}

	/**
	 * Gets the UUID of the player whose island is linked to this one for
	 * cross-island Nether portal travel.
	 * <p>
	 * When two islands are linked, players can travel between them using Nether
	 * portals instead of being sent to their own island's Nether counterpart. This
	 * method returns the UUID of the linked island owner, if such a connection
	 * exists.
	 * </p>
	 *
	 * @return the linked player's {@link UUID} if Nether portal linking is active,
	 *         or {@code null} if this island is not linked to another.
	 */
	@Nullable
	public UUID getLinkedPortalUUID() {
		return this.linkedPortalUUID;
	}

	/**
	 * Gets the bounding box representing the protected area of the island.
	 *
	 * @return the {@link BoundingBox} or {@code null} if not defined.
	 */
	@Nullable
	public BoundingBox getBoundingBox() {
		return this.boundingBox;
	}

	/**
	 * Gets the preserved bounding box representing the protected area of the island
	 * (for reset purposes).
	 *
	 * @return the {@link BoundingBox} or {@code null} if not defined.
	 */
	@Nullable
	public BoundingBox getPreservedBoundingBox() {
		return this.preservedBoundingBox;
	}

	/**
	 * Gets the location of the Hellblock center on this island.
	 *
	 * @return the {@link Location} of the Hellblock, or {@code null} if unset.
	 */
	@Nullable
	public Location getHellblockLocation() {
		return this.location;
	}

	/**
	 * Gets the home teleport location for this island.
	 *
	 * @return the {@link Location} representing the island's home.
	 */
	@Nullable
	public Location getHomeLocation() {
		return this.home;
	}

	/**
	 * Gets all current party member UUIDs for this island.
	 *
	 * @return a {@link Set} of player UUIDs.
	 */
	@NotNull
	public Set<UUID> getPartyMembers() {
		if (this.party == null) {
			return new HashSet<>();
		}
		return this.party;
	}

	/**
	 * Checks whether the given UUID is part of the island's party.
	 *
	 * @param playerId the UUID of the player to check.
	 * @return {@code true} if the player is in the party.
	 */
	public boolean isInParty(@NotNull UUID playerId) {
		return getPartyMembers().contains(playerId);
	}

	/**
	 * Gets a set of UUIDs including all party members and the island owner.
	 *
	 * @return a combined {@link Set} of party members and the owner.
	 */
	@NotNull
	public Set<UUID> getPartyPlusOwner() {
		final Set<UUID> members = new HashSet<>();
		if (getOwnerUUID() != null) {
			members.add(getOwnerUUID());
		}
		if (!getPartyMembers().isEmpty()) {
			members.addAll(getPartyMembers());
		}
		return members;
	}

	/**
	 * Gets all trusted player UUIDs for this island.
	 *
	 * @return a {@link Set} of trusted player UUIDs.
	 */
	@NotNull
	public Set<UUID> getTrustedMembers() {
		if (this.trusted == null) {
			return new HashSet<>();
		}
		return this.trusted;
	}

	/**
	 * Gets all banned player UUIDs for this island.
	 *
	 * @return a {@link Set} of banned player UUIDs.
	 */
	@NotNull
	public Set<UUID> getBannedMembers() {
		if (this.banned == null) {
			return new HashSet<>();
		}
		return this.banned;
	}

	/**
	 * Gets all UUIDs with access to this island — including the owner, party
	 * members, and trusted players.
	 *
	 * @return a combined {@link Set} of all island members.
	 */
	@NotNull
	public Set<UUID> getIslandMembers() {
		final Set<UUID> members = new HashSet<>();
		if (getOwnerUUID() != null) {
			members.add(getOwnerUUID());
		}
		if (!getPartyMembers().isEmpty()) {
			members.addAll(getPartyMembers());
		}
		if (!getTrustedMembers().isEmpty()) {
			members.addAll(getTrustedMembers());
		}
		return members;
	}

	/**
	 * Checks whether the given player ID can access this island (owner, party
	 * member, or trusted).
	 *
	 * @param playerId the player’s UUID.
	 * @return {@code true} if the player has access to the island.
	 */
	public boolean canAccess(@NotNull UUID playerId) {
		return getIslandMembers().contains(playerId);
	}

	/**
	 * Gets a map of pending island invitations.
	 *
	 * @return a {@link Map} of player UUIDs to invitation expiry timestamps.
	 */
	@NotNull
	public Map<UUID, Long> getInvitations() {
		if (this.invitations == null) {
			return new HashMap<>();
		}
		return this.invitations;
	}

	/**
	 * Checks whether a specific player has an active invitation to this island.
	 *
	 * @param playerId the player's UUID.
	 * @return {@code true} if the player has been invited.
	 */
	public boolean hasInvite(@NotNull UUID playerId) {
		return getInvitations().containsKey(playerId);
	}

	/**
	 * Checks whether a player's invitation has expired.
	 *
	 * @param playerId the player's UUID.
	 * @return {@code true} if the invitation has expired; {@code false} otherwise.
	 */
	public boolean hasInviteExpired(@NotNull UUID playerId) {
		final Long expiry = getInvitations().get(playerId);
		if (expiry == null) {
			return false; // no invite
		}
		return expiry != NO_EXPIRY && System.currentTimeMillis() > expiry;
	}

	/**
	 * Gets all protection flags for this island.
	 *
	 * @return a {@link EnumMap} mapping {@link FlagType} to {@link HellblockFlag}.
	 */
	@NotNull
	public EnumMap<FlagType, HellblockFlag> getProtectionFlags() {
		if (this.flags == null) {
			return new EnumMap<>(FlagType.class);
		}
		return this.flags;
	}

	/**
	 * Gets the access value for a specific protection flag.
	 *
	 * @param flagType the flag type to check.
	 * @return the {@link AccessType} value for the flag.
	 */
	@NotNull
	public AccessType getProtectionValue(@NotNull FlagType flagType) {
		HellblockFlag flag = getProtectionFlags().get(flagType);
		if (flag != null) {
			return flag.getStatus();
		}
		return flagType.getDefaultValue() ? AccessType.ALLOW : AccessType.DENY;
	}

	/**
	 * Returns the custom data string associated with the specified flag, if
	 * available. Only supports {@link FlagType#GREET_MESSAGE} and
	 * {@link FlagType#FAREWELL_MESSAGE} flags.
	 *
	 * @param flagType the flag to query
	 * @return the data string for the flag, or {@code null} if not found or
	 *         unsupported
	 */
	@Nullable
	public String getProtectionData(@NotNull FlagType flagType) {
		if (flagType != FlagType.GREET_MESSAGE && flagType != FlagType.FAREWELL_MESSAGE) {
			return null;
		}

		HellblockFlag flag = getProtectionFlags().get(flagType);
		return (flag != null) ? flag.getData() : null;
	}

	/**
	 * Checks if the specified protection flag has custom string data set.
	 * <p>
	 * Only {@link FlagType#GREET_MESSAGE} and {@link FlagType#FAREWELL_MESSAGE} are
	 * supported.
	 *
	 * @param flagType the flag to check
	 * @return {@code true} if data exists and is non-empty, {@code false} otherwise
	 */
	public boolean hasProtectionData(@NotNull FlagType flagType) {
		if (flagType != FlagType.GREET_MESSAGE && flagType != FlagType.FAREWELL_MESSAGE) {
			return false;
		}

		HellblockFlag flag = getProtectionFlags().get(flagType);
		return flag != null && flag.getData() != null && !flag.getData().isBlank();
	}

	/**
	 * Gets all island upgrade levels.
	 *
	 * @return an {@link EnumMap} of upgrade types and their levels.
	 */
	@NotNull
	public EnumMap<IslandUpgradeType, Integer> getIslandUpgrades() {
		if (this.upgrades == null) {
			return new EnumMap<>(IslandUpgradeType.class);
		}
		return this.upgrades;
	}

	/**
	 * Retrieves the current level of a given island upgrade.
	 *
	 * @param upgradeType the {@link IslandUpgradeType} to check.
	 * @return the level of the upgrade, or 0 if not unlocked.
	 */
	public int getUpgradeLevel(@NotNull IslandUpgradeType upgradeType) {
		return getIslandUpgrades().getOrDefault(upgradeType, 0);
	}

	/**
	 * Determines if the island can be upgraded for a specific upgrade type.
	 *
	 * @param upgradeType the {@link IslandUpgradeType}.
	 * @return {@code true} if the upgrade can be increased further.
	 */
	public boolean canUpgrade(@NotNull IslandUpgradeType upgradeType) {
		return getUpgradeLevel(upgradeType) < HellblockPlugin.getInstance().getUpgradeManager()
				.getMaxTierFor(upgradeType);
	}

	/**
	 * Gets the upgrade value (e.g., range, chance, rate) associated with a specific
	 * upgrade type.
	 *
	 * @param upgradeType the {@link IslandUpgradeType} to get the value for.
	 * @return a {@link Number} representing the value of the current tier.
	 */
	@NotNull
	public Number getValue(@NotNull IslandUpgradeType upgradeType) {
		return HellblockPlugin.getInstance().getUpgradeManager().getTier(getUpgradeLevel(upgradeType))
				.getUpgrade(upgradeType).getValue();
	}

	/**
	 * Gets the upgrade value for the given upgrade type as an integer.
	 *
	 * @param upgradeType the {@link IslandUpgradeType} to get.
	 * @return the integer value.
	 */
	@NotNull
	public Integer getIntValue(IslandUpgradeType upgradeType) {
		return (Integer) getValue(upgradeType);
	}

	/**
	 * Gets the upgrade value for the given upgrade type as a double.
	 *
	 * @param upgradeType the {@link IslandUpgradeType} to get.
	 * @return the double value.
	 */
	@NotNull
	public Double getDoubleValue(IslandUpgradeType upgradeType) {
		return (Double) getValue(upgradeType);
	}

	/**
	 * Gets the cost of the current tier for a given upgrade type.
	 *
	 * @param upgradeType the {@link IslandUpgradeType} to evaluate.
	 * @return a list of {@link UpgradeCost} representing the current tier cost.
	 */
	@NotNull
	public List<UpgradeCost> getCurrentCosts(@NotNull IslandUpgradeType upgradeType) {
		return HellblockPlugin.getInstance().getUpgradeManager().getTier(getUpgradeLevel(upgradeType))
				.getUpgrade(upgradeType).getCosts();
	}

	/**
	 * Gets the cost of the next upgrade tier for the specified type, if available.
	 *
	 * @param upgradeType the {@link IslandUpgradeType} to evaluate.
	 * @return a list of {@link UpgradeCost} for the next tier, or {@code null} if
	 *         maxed out.
	 */
	@Nullable
	public List<UpgradeCost> getNextCosts(@NotNull IslandUpgradeType upgradeType) {
		final UpgradeData next = HellblockPlugin.getInstance().getUpgradeManager()
				.getNextUpgradeData(getUpgradeLevel(upgradeType), upgradeType);
		return (next != null) ? next.getCosts() : null;
	}

	/**
	 * Applies an upgrade to this island for the given upgrade type. This method
	 * increases the stored tier and applies any side effects, such as expanding the
	 * protection range or updating values.
	 * 
	 * <p>
	 * <b>Note:</b> This method does not perform permission or payment checks. These
	 * must be validated before calling this method.
	 * </p>
	 *
	 * @param upgradeType the {@link IslandUpgradeType} to apply.
	 * @throws IllegalStateException if the island owner is not online when the
	 *                               upgrade is applied.
	 */
	public void applyUpgrade(@NotNull IslandUpgradeType upgradeType) {
		if (!canUpgrade(upgradeType))
			return;

		upgradeTier(upgradeType);

		if (getOwnerUUID() == null)
			return;

		final Player ownerPlayer = Bukkit.getPlayer(getOwnerUUID());
		if (ownerPlayer == null || !ownerPlayer.isOnline()) {
			throw new IllegalStateException("Owner is not online while trying to apply an upgrade.");
		}

		final Sender owner = HellblockPlugin.getInstance().getSenderFactory().wrap(ownerPlayer);
		final TranslationManager tm = HellblockPlugin.getInstance().getTranslationManager();

		CompletableFuture<MessagePair> messageFuture;

		switch (upgradeType) {
		case PROTECTION_RANGE -> {
			int newRange = getMaxProtectionRange();

			messageFuture = expandBoundingBox(newRange).thenApply(v -> {
				Component arg = AdventureHelper.miniMessageToComponent(String.valueOf(newRange));
				Component message = tm.render(MessageConstants.MSG_HELLBLOCK_UPGRADE_RANGE.arguments(arg).build());
				Component memberMessage = tm.render(MessageConstants.MSG_HELLBLOCK_UPGRADE_RANGE_MEMBER
						.arguments(AdventureHelper.miniMessageToComponent(ownerPlayer.getName()), arg).build());
				return new MessagePair(message, memberMessage);
			});
		}
		case PARTY_SIZE -> {
			int newPartySize = getMaxPartySize();
			Component arg = AdventureHelper.miniMessageToComponent(String.valueOf(newPartySize));
			Component message = tm.render(MessageConstants.MSG_HELLBLOCK_UPGRADE_PARTY.arguments(arg).build());
			Component memberMessage = tm.render(MessageConstants.MSG_HELLBLOCK_UPGRADE_PARTY_MEMBER
					.arguments(AdventureHelper.miniMessageToComponent(ownerPlayer.getName()), arg).build());
			messageFuture = CompletableFuture.completedFuture(new MessagePair(message, memberMessage));
		}
		case HOPPER_LIMIT -> {
			int newLimit = getMaxHopperLimit();
			Component arg = AdventureHelper.miniMessageToComponent(String.valueOf(newLimit));
			Component message = tm.render(MessageConstants.MSG_HELLBLOCK_UPGRADE_HOPPER.arguments(arg).build());
			Component memberMessage = tm.render(MessageConstants.MSG_HELLBLOCK_UPGRADE_HOPPER_MEMBER
					.arguments(AdventureHelper.miniMessageToComponent(ownerPlayer.getName()), arg).build());
			messageFuture = CompletableFuture.completedFuture(new MessagePair(message, memberMessage));
		}
		case GENERATOR_CHANCE -> {
			double chance = getNewGeneratorChance();
			Component arg = AdventureHelper.miniMessageToComponent(String.valueOf(chance));
			Component message = tm.render(MessageConstants.MSG_HELLBLOCK_UPGRADE_GENERATOR.arguments(arg).build());
			Component memberMessage = tm.render(MessageConstants.MSG_HELLBLOCK_UPGRADE_GENERATOR_MEMBER
					.arguments(AdventureHelper.miniMessageToComponent(ownerPlayer.getName()), arg).build());
			messageFuture = CompletableFuture.completedFuture(new MessagePair(message, memberMessage));
		}
		case PIGLIN_BARTERING -> {
			double chance = getNewBarteringChance();
			Component arg = AdventureHelper.miniMessageToComponent(String.valueOf(chance));
			Component message = tm.render(MessageConstants.MSG_HELLBLOCK_UPGRADE_BARTERING.arguments(arg).build());
			Component memberMessage = tm.render(MessageConstants.MSG_HELLBLOCK_UPGRADE_BARTERING_MEMBER
					.arguments(AdventureHelper.miniMessageToComponent(ownerPlayer.getName()), arg).build());
			messageFuture = CompletableFuture.completedFuture(new MessagePair(message, memberMessage));
		}
		case CROP_GROWTH -> {
			double rate = getNewCropGrowthRate();
			Component arg = AdventureHelper.miniMessageToComponent(String.valueOf(rate));
			Component message = tm.render(MessageConstants.MSG_HELLBLOCK_UPGRADE_CROP.arguments(arg).build());
			Component memberMessage = tm.render(MessageConstants.MSG_HELLBLOCK_UPGRADE_CROP_MEMBER
					.arguments(AdventureHelper.miniMessageToComponent(ownerPlayer.getName()), arg).build());
			messageFuture = CompletableFuture.completedFuture(new MessagePair(message, memberMessage));
		}
		case MOB_SPAWN_RATE -> {
			double rate = getNewMobSpawningRate();
			Component arg = AdventureHelper.miniMessageToComponent(String.valueOf(rate));
			Component message = tm.render(MessageConstants.MSG_HELLBLOCK_UPGRADE_MOB.arguments(arg).build());
			Component memberMessage = tm.render(MessageConstants.MSG_HELLBLOCK_UPGRADE_MOB_MEMBER
					.arguments(AdventureHelper.miniMessageToComponent(ownerPlayer.getName()), arg).build());
			messageFuture = CompletableFuture.completedFuture(new MessagePair(message, memberMessage));
		}
		default -> {
			return;
		}
		}

		// Only send message after any async operation (if any)
		messageFuture.thenAccept(pair -> {
			owner.sendMessage(pair.ownerMsg);

			getPartyMembers().stream().map(Bukkit::getPlayer).filter(Objects::nonNull).filter(Player::isOnline)
					.map(p -> HellblockPlugin.getInstance().getSenderFactory().wrap(p))
					.forEach(sender -> sender.sendMessage(pair.memberMsg));
		});
	}

	/**
	 * Expands or rebuilds the island's protection bounding box centered on the
	 * hellblock.
	 * <p>
	 * This method recalculates the bounding box using the provided protection range
	 * and sets it to cover the vertical range of the entire world.
	 *
	 * @param newRange The new protection radius (from the center) to apply to the
	 *                 island.
	 * @throws IllegalStateException if the world is not loaded for the island's
	 *                               hellblock.
	 */
	private CompletableFuture<Void> expandBoundingBox(int newRange) {
		final Location center = getHellblockLocation();
		final World world = center.getWorld();

		if (world == null) {
			throw new IllegalStateException(
					"World not loaded for island center to expand BoundingBox for islandId=" + getIslandId());
		}

		HellblockPlugin instance = HellblockPlugin.getInstance();

		Optional<HellblockWorld<?>> hellWorldOpt = instance.getWorldManager().getWorld(world);
		if (hellWorldOpt.isEmpty() || hellWorldOpt.get().bukkitWorld() == null) {
			throw new IllegalStateException(
					"World not created in hellblock database to expand BoundingBox for islandId=" + getIslandId());
		}

		HellblockWorld<?> hellWorld = hellWorldOpt.get();

		final double half = newRange / 2.0;

		// Update and store new bounding box
		setBoundingBox(new BoundingBox(center.getX() - half, world.getMinHeight(), center.getZ() - half,
				center.getX() + half, world.getMaxHeight(), center.getZ() + half));
		HellblockPlugin.getInstance().getPlacementDetector().cacheIslandBoundingBox(getIslandId(), getBoundingBox());

		// Get user data for chunk preloading
		Optional<UserData> userDataOpt = instance.getStorageManager().getOnlineUser(getOwnerUUID());
		if (userDataOpt.isEmpty()) {
			throw new IllegalStateException(
					"UserData not available to expand BoundingBox for islandId=" + getIslandId());
		}

		UserData userData = userDataOpt.get();

		// Preload the new bounding box area and then invalidate cache
		return instance.getIslandGenerator().preloadIslandChunks(hellWorld, null, // No construction stages used here
				userData, 10, // chunks per tick
				progress -> instance.debug("expandBoundingBox: Preload progress: " + (int) (progress * 100) + "%"),
				300L, // timeout in ticks
				true, // verbose logging
				3, // max retries
				10, // retry delay
				() -> instance.debug("expandBoundingBox: Finished preloading island chunks successfully!"))
				.thenAccept(failedChunks -> {
					if (!failedChunks.isEmpty()) {
						instance.getPluginLogger().warn("expandBoundingBox: Some chunks failed to load for islandId="
								+ getIslandId() + ": " + failedChunks.size());
					}
					instance.getProtectionManager().invalidateIslandChunkCache(getIslandId());
				});
	}

	/**
	 * Gets the current maximum party size for the island based on its upgrade tier.
	 *
	 * @return The maximum number of party members allowed.
	 */
	public int getMaxPartySize() {
		return getIntValue(IslandUpgradeType.PARTY_SIZE);
	}

	/**
	 * Gets the total maximum number of players allowed in the party, including the
	 * owner.
	 *
	 * @return The max size of the party + 1 for the owner.
	 */
	public int getMaxPlayersIncludingOwner() {
		return 1 + getMaxPartySize();
	}

	/**
	 * Returns the current maximum protection range of the island from the center
	 * block.
	 *
	 * @return The number of blocks in each direction the protection extends.
	 */
	public int getMaxProtectionRange() {
		return getIntValue(IslandUpgradeType.PROTECTION_RANGE);
	}

	/**
	 * Gets the current hopper limit for the island based on upgrade tier.
	 *
	 * @return Maximum number of hoppers allowed to be placed.
	 */
	public int getMaxHopperLimit() {
		return getIntValue(IslandUpgradeType.HOPPER_LIMIT);
	}

	/**
	 * Gets the current resource generator chance for the island (e.g., for
	 * netherrack generators).
	 *
	 * @return Generator chance as a double (0.0 - 1.0).
	 */
	public double getNewGeneratorChance() {
		return getDoubleValue(IslandUpgradeType.GENERATOR_CHANCE);
	}

	/**
	 * Gets the current piglin bartering bonus chance for the island.
	 *
	 * @return Bartering chance as a double (0.0 - 1.0).
	 */
	public double getNewBarteringChance() {
		return getDoubleValue(IslandUpgradeType.PIGLIN_BARTERING);
	}

	/**
	 * Returns the current crop growth multiplier for the island.
	 *
	 * @return Growth rate multiplier (e.g., 1.0 = normal speed, 2.0 = double
	 *         speed).
	 */
	public double getNewCropGrowthRate() {
		return getDoubleValue(IslandUpgradeType.CROP_GROWTH);
	}

	/**
	 * Gets the current mob spawn rate modifier for the island.
	 *
	 * @return Mob spawning rate multiplier (1.0 is normal, higher is faster).
	 */
	public double getNewMobSpawningRate() {
		return getDoubleValue(IslandUpgradeType.MOB_SPAWN_RATE);
	}

	/**
	 * Sets default island data for a newly created hellblock island.
	 *
	 * @param hasHellblock      Whether the player has an active hellblock.
	 * @param hellblockLocation The location of the hellblock center (can be null).
	 * @param hellblockID       The unique ID to assign to the island.
	 */
	public void setDefaultHellblockData(boolean hasHellblock, @Nullable Location hellblockLocation, int hellblockID) {
		setHasHellblock(hasHellblock);
		setHellblockLocation(hellblockLocation);
		setIslandId(hellblockID);
		updateLastIslandActivity();
		updateLastWorldAccess();
		setIslandLevel(DEFAULT_LEVEL);
		setDisplaySettings(new DisplaySettings(getDefaultIslandName(), getDefaultIslandBio(), DisplayChoice.CHAT));
		getDisplaySettings().setAsDefaultIslandName();
		getDisplaySettings().setAsDefaultIslandBio();
	}

	/**
	 * Transfers all hellblock-related data from the provided {@link UserData} to
	 * this island.
	 * <p>
	 * Also ensures that default names and bios are recalculated if needed during
	 * transfer. Hopper data is re-linked to the new bounding box.
	 *
	 * @param transferee The user data containing the new hellblock information.
	 */
	public void transferHellblockData(@NotNull UserData transferee) {
		this.id = transferee.getHellblockData().getIslandId();
		this.hasHellblock = transferee.getHellblockData().hasHellblock();
		this.display = transferee.getHellblockData().getDisplaySettings().copy();
		if (getDisplaySettings().isDefaultIslandBio()
				&& !getDisplaySettings().getIslandBio().contains(getResolvedOwnerName())) {
			getDisplaySettings().setIslandBio(getDefaultIslandBio());
			getDisplaySettings().setAsDefaultIslandBio();
			HellblockPlugin.getInstance()
					.debug("Updated island bio for " + getResolvedOwnerName() + " due to ownership transfer.");
		}
		if (getDisplaySettings().isDefaultIslandName()
				&& !getDisplaySettings().getIslandName().contains(getResolvedOwnerName())) {
			getDisplaySettings().setIslandName(getDefaultIslandName());
			getDisplaySettings().setAsDefaultIslandName();
			HellblockPlugin.getInstance()
					.debug("Updated island name for " + getResolvedOwnerName() + " due to ownership transfer.");
		}
		this.location = transferee.getHellblockData().getHellblockLocation() != null
				? transferee.getHellblockData().getHellblockLocation().clone()
				: null;
		this.home = transferee.getHellblockData().getHomeLocation() != null
				? transferee.getHellblockData().getHomeLocation().clone()
				: null;
		this.level = transferee.getHellblockData().getIslandLevel();
		this.party = new HashSet<>(transferee.getHellblockData().getPartyMembers());
		this.trusted = new HashSet<>(transferee.getHellblockData().getTrustedMembers());
		this.banned = new HashSet<>(transferee.getHellblockData().getBannedMembers());
		this.flags = transferee.getHellblockData().getProtectionFlags().entrySet().stream().collect(Collectors.toMap(
				Map.Entry::getKey, entry -> entry.getValue().copy(), (a, b) -> b, () -> new EnumMap<>(FlagType.class)));
		this.upgrades = new EnumMap<>(transferee.getHellblockData().getIslandUpgrades());
		this.biome = transferee.getHellblockData().getBiome();
		this.biomeCooldown = transferee.getHellblockData().getBiomeCooldown();
		this.resetCooldown = transferee.getHellblockData().getResetCooldown();
		this.lastWorldAccess = transferee.getHellblockData().getLastWorldAccess();
		this.creationTime = transferee.getHellblockData().getCreationTime();
		BoundingBox oldBoundingBox = (getBoundingBox() != null) ? getBoundingBox().clone() : null;
		BoundingBox sourceBox = transferee.getHellblockData().getBoundingBox();
		this.boundingBox = (sourceBox != null) ? sourceBox.clone() : null;
		this.ownerUUID = transferee.getHellblockData().getOwnerUUID();
		this.choice = transferee.getHellblockData().getIslandChoice();
		this.schematic = transferee.getHellblockData().getUsedSchematic();
		this.locked = transferee.getHellblockData().isLocked();
		this.visitData = transferee.getHellblockData().getVisitData().copy();
		this.recentVisitors = transferee.getHellblockData().getRecentVisitors().stream().map(VisitRecord::copy)
				.collect(Collectors.toCollection(ArrayList::new));
		this.invasionData = transferee.getHellblockData().getInvasionData().copy();
		this.witherData = transferee.getHellblockData().getWitherData().copy();
		this.skysiegeData = transferee.getHellblockData().getSkysiegeData().copy();
		if (oldBoundingBox != null && getBoundingBox() != null) {
			HellblockPlugin.getInstance().getHopperHandler().transferHoppers(oldBoundingBox, getBoundingBox().clone());
		}
	}

	/**
	 * Resets all hellblock island data to its default empty state.
	 * <p>
	 * This includes clearing all members, flags, upgrades, locations, and events.
	 * It also clears any hopper data and resets the bounding box.
	 */
	public void resetHellblockData() {
		setHasHellblock(false);
		setHellblockLocation(null);
		setHomeLocation(null);
		setIslandLevel(0.0F);
		getPartyMembers().clear();
		getTrustedMembers().clear();
		getBannedMembers().clear();
		getProtectionFlags().clear();
		getIslandUpgrades().clear();
		setDisplaySettings(DisplaySettings.empty());
		setBiome(null);
		setBiomeCooldown(0L);
		setCreationTime(0L);
		updateLastWorldAccess();
		setPreservedBoundingBox(getBoundingBox() != null ? getBoundingBox().clone() : null);
		if (getBoundingBox() != null) {
			HellblockPlugin.getInstance().getHopperHandler().clearHoppers(getBoundingBox().clone());
		}
		setBoundingBox(null);
		HellblockPlugin.getInstance().getPlacementDetector().removeCachedIslandBoundingBox(getIslandId());
		setOwnerUUID(null);
		setIslandChoice(null);
		setUsedSchematic(null);
		setLockedStatus(false);
		setChatSetting(CoopChatSetting.GLOBAL);
		getVisitData().reset();
		setInvasionData(InvasionData.empty());
		setWitherData(WitherData.empty());
		setSkysiegeData(SkysiegeData.empty());
		getRecentVisitors().clear();
	}

	/**
	 * Sets whether this player has an active hellblock.
	 *
	 * @param hasHellblock True if the player has a hellblock created, false
	 *                     otherwise.
	 */
	public void setHasHellblock(boolean hasHellblock) {
		this.hasHellblock = hasHellblock;
	}

	/**
	 * Sets the locked state of this island.
	 *
	 * @param locked {@code true} to lock the island.
	 */
	public void setLockedStatus(boolean locked) {
		this.locked = locked;
	}

	/**
	 * Sets the abandoned status of the island.
	 *
	 * @param abandoned True if the island is considered abandoned.
	 */
	public void setAsAbandoned(boolean abandoned) {
		this.abandoned = abandoned;
	}

	/**
	 * Sets the unique identifier for this island.
	 *
	 * @param id The integer ID to assign to the island.
	 */
	public void setIslandId(int id) {
		this.id = id;
	}

	/**
	 * Updates the island’s current level.
	 *
	 * @param level the new island level.
	 */
	public void setIslandLevel(float level) {
		this.level = level;
		if (hasHellblock() && getIslandLevel() <= 0) {
			this.level = DEFAULT_LEVEL;
		}
	}

	/**
	 * Increases the island level by 1.
	 */
	public void increaseIslandLevel() {
		setIslandLevel(getIslandLevel() + 1);
	}

	/**
	 * Decreases the island level by 1.
	 */
	public void decreaseIslandLevel() {
		setIslandLevel(getIslandLevel() - 1);
	}

	/**
	 * Adds the specified amount to the island level.
	 *
	 * @param levels The number of levels to add.
	 */
	public void addToIslandLevel(float levels) {
		setIslandLevel(getIslandLevel() + levels);
	}

	/**
	 * Removes the specified amount from the island level.
	 *
	 * @param levels The number of levels to subtract.
	 */
	public void removeFromIslandLevel(float levels) {
		setIslandLevel(getIslandLevel() - levels);
	}

	/**
	 * Sets the visit data object for the island.
	 *
	 * @param visitData The {@link VisitData} to associate with the island.
	 */
	public void setVisitData(@NotNull VisitData visitData) {
		this.visitData = visitData;
	}

	/**
	 * Sets the active invasion data for the island.
	 *
	 * @param invasionData The {@link InvasionData} to apply.
	 */
	public void setInvasionData(@NotNull InvasionData invasionData) {
		this.invasionData = invasionData;
	}

	/**
	 * Sets the wither event data for the island.
	 *
	 * @param witherData The {@link WitherData} to assign.
	 */
	public void setWitherData(@NotNull WitherData witherData) {
		this.witherData = witherData;
	}

	/**
	 * Sets the skysiege event data for the island.
	 *
	 * @param skysiegeData The {@link SkysiegeData} to assign.
	 */
	public void setSkysiegeData(@NotNull SkysiegeData skysiegeData) {
		this.skysiegeData = skysiegeData;
	}

	/**
	 * Adds a new visit record for the specified player UUID.
	 * <p>
	 * The visitor will be added to the recent visitors list, and the oldest entry
	 * will be removed if the list exceeds 50 entries.
	 *
	 * @param visitorId The UUID of the visiting player.
	 */
	public void addVisitor(@NotNull UUID visitorId) {
		VisitManager visitManager = HellblockPlugin.getInstance().getVisitManager();
		getRecentVisitors().add(visitManager.new VisitRecord(visitorId));
		if (getRecentVisitors().size() > 50) {
			getRecentVisitors().remove(0); // remove oldest
		}
	}

	/**
	 * Removes visit records that occurred before the given cutoff timestamp.
	 *
	 * @param cutoff The timestamp (in milliseconds) used to filter old records.
	 */
	public void cleanupOldVisitors(long cutoff) {
		getRecentVisitors().removeIf(record -> record.getTimestamp() < cutoff);
	}

	/**
	 * Replaces the current list of recent visitors.
	 *
	 * @param recentVisitors The list of {@link VisitRecord}s to set.
	 */
	public void setRecentVisitors(@NotNull List<VisitRecord> recentVisitors) {
		this.recentVisitors = recentVisitors;
	}

	/**
	 * Sets the display settings for the island, including name, bio, and display
	 * method.
	 *
	 * @param display The {@link DisplaySettings} to apply.
	 */
	public void setDisplaySettings(@NotNull DisplaySettings display) {
		this.display = display;
	}

	/**
	 * Sets the cooperative chat setting for the player.
	 *
	 * @param chat The {@link CoopChatSetting} value to assign.
	 */
	public void setChatSetting(@NotNull CoopChatSetting chat) {
		this.chat = chat;
	}

	/**
	 * Sets the biome type currently active on the island.
	 *
	 * @param biome The {@link HellBiome} to assign, or null to clear.
	 */
	public void setBiome(@Nullable HellBiome biome) {
		this.biome = biome;
	}

	/**
	 * Sets the chosen island type (options/configured layout) used during creation.
	 *
	 * @param choice The {@link IslandOptions} selected, or null if not applicable.
	 */
	public void setIslandChoice(@Nullable IslandOptions choice) {
		this.choice = choice;
	}

	/**
	 * Sets the name of the schematic used to generate this island.
	 *
	 * @param schematic The schematic filename, or null to clear.
	 */
	public void setUsedSchematic(@Nullable String schematic) {
		this.schematic = schematic;
	}

	/**
	 * Sets the timestamp for when the island was created.
	 *
	 * @param creationTime The creation time in milliseconds since epoch.
	 */
	public void setCreationTime(long creationTime) {
		this.creationTime = creationTime;
	}

	/**
	 * Sets the cooldown before the island can be reset again.
	 *
	 * @param resetCooldown The cooldown timestamp in milliseconds.
	 */
	public void setResetCooldown(long resetCooldown) {
		this.resetCooldown = resetCooldown;
	}

	/**
	 * Sets the cooldown before the island biome can be changed again.
	 *
	 * @param biomeCooldown The cooldown timestamp in milliseconds.
	 */
	public void setBiomeCooldown(long biomeCooldown) {
		this.biomeCooldown = biomeCooldown;
	}

	/**
	 * Sets the cooldown before the island can be transferred to another player.
	 *
	 * @param transferCooldown The cooldown timestamp in milliseconds.
	 */
	public void setTransferCooldown(long transferCooldown) {
		this.transferCooldown = transferCooldown;
	}

	/**
	 * Updates the activity of the player when performing island based actions.
	 */
	public void updateLastIslandActivity() {
		long now = System.currentTimeMillis();
		if (now - getLastIslandActivity() > ACTIVITY_UPDATE_INTERVAL) {
			this.lastIslandActivity = now;
		}
	}

	/**
	 * Updates the last world access time to the current system time.
	 *
	 * <p>
	 * This should be called whenever the island's world is loaded, entered, or
	 * otherwise interacted with by a player or process.
	 */
	public void updateLastWorldAccess() {
		if (!HellblockPlugin.getInstance().getConfigManager().perPlayerWorlds()) {
			return;
		}
		this.lastWorldAccess = System.currentTimeMillis();
	}

	/**
	 * Clears all island-related cooldowns including reset, biome change, and owner
	 * transfership.
	 */
	public void resetAllCooldowns() {
		setResetCooldown(0L);
		setBiomeCooldown(0L);
		setTransferCooldown(0L);
	}

	/**
	 * Sets the UUID of the island's owner.
	 *
	 * @param ownerUUID The {@link UUID} of the owning player, or null to unassign.
	 */
	public void setOwnerUUID(@Nullable UUID ownerUUID) {
		this.ownerUUID = ownerUUID;
	}

	/**
	 * Sets the UUID of another island owner that this island is linked to for
	 * shared Nether portal travel.
	 *
	 * @param linkedPortalUUID The UUID of the linked player, or null to unlink.
	 */
	public void setLinkedPortalUUID(@Nullable UUID linkedPortalUUID) {
		this.linkedPortalUUID = linkedPortalUUID;
	}

	/**
	 * Sets the bounding box (protection area) for the island.
	 *
	 * @param boundingBox The {@link BoundingBox} to apply, or null to unset.
	 */
	public void setBoundingBox(@Nullable BoundingBox boundingBox) {
		this.boundingBox = boundingBox;
	}

	/**
	 * Sets the preserved bounding box (protection area) for the island (for reset
	 * purposes).
	 *
	 * @param boundingBox The {@link BoundingBox} to apply, or null to unset.
	 */
	public void setPreservedBoundingBox(@Nullable BoundingBox preservedBoundingBox) {
		this.preservedBoundingBox = preservedBoundingBox;
	}

	/**
	 * Sets the central location of the island's hellblock.
	 *
	 * @param location The {@link Location} of the hellblock, or null if not set.
	 */
	public void setHellblockLocation(@Nullable Location location) {
		this.location = location;
	}

	/**
	 * Sets the home teleport location for the island.
	 *
	 * @param home The {@link Location} to set as the home point, or null to unset.
	 */
	public void setHomeLocation(@Nullable Location home) {
		this.home = home;
	}

	/**
	 * Adds a new party member to this island.
	 *
	 * @param newMember the UUID of the new party member.
	 */
	public void addToParty(@NotNull UUID newMember) {
		if (!getPartyMembers().contains(newMember)) {
			getPartyMembers().add(newMember);
		}
	}

	/**
	 * Removes a member from the party if they are currently in it.
	 *
	 * @param oldMember The UUID of the player to be removed from the party.
	 */
	public void kickFromParty(@NotNull UUID oldMember) {
		if (getPartyMembers().contains(oldMember)) {
			getPartyMembers().remove(oldMember);
		}
	}

	/**
	 * Sets the current party member list to the provided set.
	 *
	 * @param partyMembers The new set of party member UUIDs.
	 */
	public void setPartyMembers(@NotNull Set<UUID> partyMembers) {
		this.party = partyMembers;
	}

	/**
	 * Adds a player to the island's trusted list, if not already present.
	 *
	 * @param newTrustee The UUID of the player to trust.
	 */
	public void addTrustPermission(@NotNull UUID newTrustee) {
		if (!getTrustedMembers().contains(newTrustee)) {
			getTrustedMembers().add(newTrustee);
		}
	}

	/**
	 * Removes a player from the island's trusted list, if they are currently
	 * trusted.
	 *
	 * @param oldTrustee The UUID of the player to remove from trusted members.
	 */
	public void removeTrustPermission(@NotNull UUID oldTrustee) {
		if (getTrustedMembers().contains(oldTrustee)) {
			getTrustedMembers().remove(oldTrustee);
		}
	}

	/**
	 * Replaces the current set of trusted members with the provided set.
	 *
	 * @param trustedMembers The new set of trusted player UUIDs.
	 */
	public void setTrustedMembers(@NotNull Set<UUID> trustedMembers) {
		this.trusted = trustedMembers;
	}

	/**
	 * Adds a player to the banned list, if not already banned.
	 *
	 * @param bannedPlayer The UUID of the player to ban from the island.
	 */
	public void banPlayer(@NotNull UUID bannedPlayer) {
		if (!getBannedMembers().contains(bannedPlayer)) {
			getBannedMembers().add(bannedPlayer);
		}
	}

	/**
	 * Removes a player from the banned list, if they are currently banned.
	 *
	 * @param unbannedPlayer The UUID of the player to unban.
	 */
	public void unbanPlayer(@NotNull UUID unbannedPlayer) {
		if (getBannedMembers().contains(unbannedPlayer)) {
			getBannedMembers().remove(unbannedPlayer);
		}
	}

	/**
	 * Sets the list of banned players on the island.
	 *
	 * @param bannedPlayers The new set of banned player UUIDs.
	 */
	public void setBannedMembers(@NotNull Set<UUID> bannedPlayers) {
		this.banned = bannedPlayers;
	}

	/**
	 * Replaces the island's invitation map with a new set of invitations.
	 *
	 * @param invitations A map of player UUIDs to expiration timestamps (in
	 *                    milliseconds).
	 */
	public void setInvitations(@NotNull Map<UUID, Long> invitations) {
		this.invitations = invitations;
	}

	/**
	 * Sends an island invitation to the specified player, with a default expiration
	 * time of 1 day.
	 *
	 * @param playerID The UUID of the player to invite.
	 */
	public void sendInvitation(@NotNull UUID playerID) {
		getInvitations().putIfAbsent(playerID, TimeUnit.SECONDS.toDays(86400));
	}

	/**
	 * Removes an existing invitation for the specified player.
	 *
	 * @param playerID The UUID of the player whose invitation should be revoked.
	 */
	public void removeInvitation(@NotNull UUID playerID) {
		if (getInvitations().containsKey(playerID)) {
			getInvitations().remove(playerID);
		}
	}

	/**
	 * Clears all current island invitations.
	 */
	public void clearInvitations() {
		getInvitations().clear();
	}

	/**
	 * Replaces the current island protection flags with the given map.
	 *
	 * @param flags A {@link EnumMap} of {@link FlagType}s to their assigned access
	 *              values.
	 */
	public void setProtectionFlags(@NotNull EnumMap<FlagType, HellblockFlag> flags) {
		this.flags = flags;
	}

	/**
	 * Sets or removes a protection flag based on the specified
	 * {@link HellblockFlag}. If the status matches the default, the flag is removed
	 * to prevent redundant storage.
	 *
	 * @param flag The {@link HellblockFlag} instance containing flag type and
	 *             access status.
	 */
	public void setProtectionValue(@NotNull HellblockFlag flag) {
		final FlagType type = flag.getFlag();
		final AccessType defaultValue = type.getDefaultValue() ? AccessType.ALLOW : AccessType.DENY;

		if (flag.getStatus() != defaultValue || flag.getData() != null) {
			getProtectionFlags().put(type, flag); // Store entire flag with status + data
		} else {
			getProtectionFlags().remove(type);
		}
	}

	/**
	 * Sets custom string data for the specified protection flag on this island.
	 * <p>
	 * This is typically used for flags like {@link FlagType#GREET_MESSAGE} or
	 * {@link FlagType#FAREWELL_MESSAGE}, which support additional text input such
	 * as custom messages.
	 * <p>
	 * If the flag does not yet exist in the internal map, it will be created using
	 * the default access status defined by the flag type.
	 *
	 * @param flagType the {@link FlagType} for which to set the data
	 * @param data     the custom string data to associate with the flag (nullable)
	 * @throws IllegalArgumentException if the provided flag does not support data
	 */
	public void setProtectionData(@NotNull FlagType flagType, @Nullable String data) {
		if (flagType != FlagType.GREET_MESSAGE && flagType != FlagType.FAREWELL_MESSAGE) {
			throw new IllegalArgumentException("Only GREET_MESSAGE and FAREWELL_MESSAGE flags support custom data.");
		}

		HellblockFlag flag = getProtectionFlags().get(flagType);

		if (flag == null) {
			// Create a new flag with default access value and set the data
			AccessType defaultStatus = flagType.getDefaultValue() ? AccessType.ALLOW : AccessType.DENY;
			flag = new HellblockFlag(flagType, defaultStatus, data);
			getProtectionFlags().put(flagType, flag);
		} else {
			// Update the data on the existing flag
			flag.setData(data);
		}
	}

	/**
	 * Removes the custom string data associated with the specified protection flag,
	 * if applicable.
	 * <p>
	 * Only {@link FlagType#GREET_MESSAGE} and {@link FlagType#FAREWELL_MESSAGE}
	 * support data removal. If the flag exists, its data will be set to
	 * {@code null}. The flag itself remains in the map.
	 *
	 * @param flagType the {@link FlagType} to remove data for
	 * @throws IllegalArgumentException if the flag does not support custom data
	 */
	public void removeProtectionData(@NotNull FlagType flagType) {
		if (flagType != FlagType.GREET_MESSAGE && flagType != FlagType.FAREWELL_MESSAGE) {
			throw new IllegalArgumentException("Only GREET_MESSAGE and FAREWELL_MESSAGE flags support data removal.");
		}

		HellblockFlag flag = getProtectionFlags().get(flagType);
		if (flag != null) {
			flag.setData(null);
		}
	}

	/**
	 * Replaces the island's upgrade level mapping with the provided map.
	 *
	 * @param upgrades An {@link EnumMap} mapping each {@link IslandUpgradeType} to
	 *                 its tier level.
	 */
	public void setIslandUpgrades(@NotNull EnumMap<IslandUpgradeType, Integer> upgrades) {
		this.upgrades = upgrades;
	}

	/**
	 * Increments the current upgrade tier for the specified upgrade type by one
	 * level.
	 *
	 * @param upgradeType The {@link IslandUpgradeType} to upgrade.
	 */
	public void upgradeTier(@NotNull IslandUpgradeType upgradeType) {
		getIslandUpgrades().put(upgradeType, getUpgradeLevel(upgradeType) + 1);
	}

	/**
	 * Increments the current upgrade tier for the specified upgrade type by one
	 * level.
	 *
	 * @param type The {@link IslandUpgradeType} to upgrade.
	 */
	public void setDefaultUpgradeTiers() {
		for (IslandUpgradeType upgrade : IslandUpgradeType.values()) {
			getIslandUpgrades().put(upgrade, 0);
		}
	}

	/**
	 * Creates a new instance of {@link HellblockData} with all fields set to their
	 * default or empty values. This can be used as a placeholder or to initialize a
	 * fresh island.
	 *
	 * @return A new {@link HellblockData} object with empty/default values.
	 */
	@NotNull
	public static HellblockData empty() {
		return new HellblockData(0, 0.0F, false, null, null, null, DisplaySettings.empty(), CoopChatSetting.GLOBAL,
				new HashSet<>(), new HashSet<>(), new HashSet<>(), new HashMap<>(), new EnumMap<>(FlagType.class),
				new EnumMap<>(IslandUpgradeType.class), null, null, 0L, VisitData.empty(), new ArrayList<>(), null,
				null, null, false, false, 0L, 0L, 0L, 0L, 0L, InvasionData.empty(), WitherData.empty(),
				SkysiegeData.empty());
	}

	/**
	 * Creates a full deep copy of this {@link HellblockData} instance.
	 * <p>
	 * All primitive fields are copied directly. All mutable objects such as sets,
	 * maps, locations, and nested data classes (e.g. {@link VisitData},
	 * {@link DisplaySettings}, {@link InvasionData}) are deeply cloned using their
	 * respective {@code copy()} or {@code clone()} methods.
	 * <p>
	 * This ensures no shared mutable state between the original and the copy.
	 *
	 * @return a completely independent deep copy of this {@link HellblockData}
	 *         instance
	 */
	@NotNull
	public final HellblockData copy() {
		EnumMap<FlagType, HellblockFlag> flagsCopy = new EnumMap<>(FlagType.class);
		getProtectionFlags().entrySet().forEach(entry -> flagsCopy.put(entry.getKey(), entry.getValue().copy()));

		return new HellblockData(getIslandId(), getIslandLevel(), hasHellblock(), getOwnerUUID(), getLinkedPortalUUID(),
				getBoundingBox() != null ? getBoundingBox().clone() : null, getDisplaySettings().copy(),
				getChatSetting(), new HashSet<>(getPartyMembers()), new HashSet<>(getTrustedMembers()),
				new HashSet<>(getBannedMembers()), new HashMap<>(getInvitations()), flagsCopy,
				new EnumMap<>(getIslandUpgrades()),
				getHellblockLocation() != null ? getHellblockLocation().clone() : null,
				getHomeLocation() != null ? getHomeLocation().clone() : null, getCreationTime(), getVisitData().copy(),
				getRecentVisitors().stream().map(VisitRecord::copy).collect(Collectors.toList()), getBiome(),
				getIslandChoice(), getUsedSchematic(), isLocked(), isAbandoned(), getResetCooldown(),
				getBiomeCooldown(), getTransferCooldown(), getLastIslandActivity(), getLastWorldAccess(),
				getInvasionData().copy(), getWitherData().copy(), getSkysiegeData().copy());
	}

	/**
	 * Checks whether this {@link HellblockData} instance is considered empty. An
	 * island is considered empty if it has no ID, no level, no ownership, no
	 * members, no configuration, and no data set.
	 *
	 * @return True if all critical fields are unset or empty, false otherwise.
	 */
	@Override
	public boolean isEmpty() {
		return getIslandId() == 0 && getIslandLevel() == 0.0F && !hasHellblock() && getOwnerUUID() == null
				&& getLinkedPortalUUID() == null && getBoundingBox() == null && getHellblockLocation() == null
				&& getHomeLocation() == null && getCreationTime() == 0L && getBiome() == null
				&& getIslandChoice() == null && (getUsedSchematic() == null || getUsedSchematic().isBlank())
				&& !isLocked() && !isAbandoned() && getResetCooldown() == 0L && getBiomeCooldown() == 0L
				&& getTransferCooldown() == 0L && getLastIslandActivity() == 0L && getLastWorldAccess() == 0L
				&& getPartyMembers().isEmpty() && getTrustedMembers().isEmpty() && getBannedMembers().isEmpty()
				&& getInvitations().isEmpty() && getProtectionFlags().isEmpty() && getIslandUpgrades().isEmpty()
				&& getRecentVisitors().isEmpty();
	}

	@Override
	public String toString() {
		return "HellblockData{" + "id=" + getIslandId() + ", level=" + getIslandLevel() + ", hasHellblock="
				+ hasHellblock() + ", ownerUUID=" + (getOwnerUUID() != null ? getOwnerUUID() : "null") + ", biome="
				+ (getBiome() != null ? getBiome().toString() : "null") + ", choice="
				+ (getIslandChoice() != null ? getIslandChoice().toString() : "null") + ", schematic="
				+ (getUsedSchematic() != null ? "\"" + getUsedSchematic() + "\"" : "null") + ", locked=" + isLocked()
				+ ", abandoned=" + isAbandoned() + ", creationTime=" + getCreationTime() + ", partySize="
				+ getPartyMembers().size() + ", trustedSize=" + getTrustedMembers().size() + ", bannedSize="
				+ getBannedMembers().size() + ", recentVisitors=" + getRecentVisitors().size() + ", upgrades="
				+ getIslandUpgrades() + ", flags=" + getProtectionFlags().keySet() + '}';
	}
}