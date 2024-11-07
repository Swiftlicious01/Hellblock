package com.swiftlicious.hellblock.playerdata;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.AbstractMap.SimpleEntry;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ThreadLocalRandom;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.FireworkEffect.Type;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.Nullable;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.challenges.ChallengeData;
import com.swiftlicious.hellblock.challenges.HellblockChallenge;
import com.swiftlicious.hellblock.challenges.HellblockChallenge.ChallengeType;
import com.swiftlicious.hellblock.challenges.HellblockChallenge.CompletionStatus;
import com.swiftlicious.hellblock.challenges.ProgressBar;
import com.swiftlicious.hellblock.coop.HellblockParty;
import com.swiftlicious.hellblock.generation.HellBiome;
import com.swiftlicious.hellblock.generation.HellblockBorderTask;
import com.swiftlicious.hellblock.generation.IslandOptions;
import com.swiftlicious.hellblock.listeners.NetherAnimalSpawningTask;
import com.swiftlicious.hellblock.protection.HellblockFlag;
import com.swiftlicious.hellblock.protection.HellblockFlag.AccessType;
import com.swiftlicious.hellblock.protection.HellblockFlag.FlagType;
import com.swiftlicious.hellblock.utils.FireworkUtils;
import com.swiftlicious.hellblock.utils.LogUtils;
import lombok.NonNull;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;

public class HellblockPlayer {

	private final UUID id;
	private int hellblockID;
	private float hellblockLevel;
	private boolean hasHellblock;
	private UUID hellblockOwner;
	private UUID linkedHellblock;
	private HellblockBorderTask borderTask;
	private NetherAnimalSpawningTask animalSpawningTask;
	private BoundingBox hellblockBoundingBox;
	private Set<UUID> hellblockParty;
	private Set<UUID> whoHasTrusted;
	private Set<UUID> bannedPlayers;
	private Map<UUID, Long> invitations;
	private Map<FlagType, AccessType> protectionFlags;
	private Map<ChallengeType, Entry<CompletionStatus, ChallengeData>> challenges;
	private Location hellblockLocation;
	private Location homeLocation;
	private long creationTime;
	private int totalVisitors;
	private HellBiome hellblockBiome;
	private IslandOptions islandChoice;
	private String schematic;
	private boolean lockedStatus;
	private boolean isAbandoned;
	private boolean wearingGlowstoneArmor, holdingGlowstoneTool;
	private long resetCooldown, biomeCooldown;
	private File file;
	// TODO: convert to database choices
	private YamlConfiguration pi;

	public final static float DEFAULT_LEVEL = 1.0F;

	public HellblockPlayer(@NonNull UUID id) {
		this.id = id;
		this.loadHellblockPlayer();
	}

	public void loadHellblockPlayer() {
		this.file = new File(HellblockPlugin.getInstance().getHellblockHandler().getPlayersDirectory() + File.separator
				+ this.id + ".yml");
		if (!this.file.exists()) {
			try {
				this.file.createNewFile();
			} catch (IOException ex) {
				LogUtils.severe(
						String.format("Could not create hellblock player file for %s!", this.getPlayer().getName()),
						ex);
				return;
			}
		}

		this.pi = YamlConfiguration.loadConfiguration(this.file);
		this.hasHellblock = this.getHellblockPlayer().getBoolean("player.hasHellblock");
		if (this.getHellblockPlayer().contains("player.challenges")) {
			this.challenges = new HashMap<>();
			this.getHellblockPlayer().getConfigurationSection("player.challenges").getKeys(false).forEach(key -> {
				ChallengeType challenge = ChallengeType.valueOf(key);
				CompletionStatus completion = CompletionStatus
						.valueOf(this.getHellblockPlayer().getString("player.challenges." + key + ".status"));
				int progress = this.getHellblockPlayer().getInt("player.challenges." + key + ".progress",
						challenge.getNeededAmount());
				boolean claimedReward = this.getHellblockPlayer()
						.getBoolean("player.challenges." + key + ".claimed-reward", false);
				this.challenges.put(challenge, new SimpleEntry<CompletionStatus, ChallengeData>(completion,
						new ChallengeData(progress, claimedReward)));
			});
		} else {
			this.challenges = new HashMap<>();
		}
		this.isAbandoned = this.getHellblockPlayer().getBoolean("player.abandoned", false);
		if (this.getHellblockPlayer().contains("player.trusted-on-islands")
				&& !this.getHellblockPlayer().getStringList("player.trusted-on-islands").isEmpty()) {
			for (String trusted : this.getHellblockPlayer().getStringList("player.trusted-on-islands")) {
				this.whoHasTrusted = new HashSet<>();
				UUID uuid = null;
				try {
					uuid = UUID.fromString(trusted);
				} catch (IllegalArgumentException ignored) {
					// ignored
				}
				if (uuid != null)
					this.whoHasTrusted.add(uuid);
			}
		} else {
			this.whoHasTrusted = new HashSet<>();
		}
		if (this.hasHellblock) {
			this.islandChoice = IslandOptions
					.valueOf(this.getHellblockPlayer().get("player.island-choice.type").toString().toUpperCase());
			this.hellblockID = this.getHellblockPlayer().getInt("player.hellblock-id");
			this.hellblockLevel = (float) this.getHellblockPlayer().getDouble("player.hellblock-level", DEFAULT_LEVEL);
			if (this.islandChoice == IslandOptions.SCHEMATIC) {
				this.schematic = this.getHellblockPlayer().getString("player.island-choice.used-schematic");
			}
			if (this.getHellblockPlayer().contains("player.linked-hellblock")) {
				try {
					this.linkedHellblock = UUID
							.fromString(this.getHellblockPlayer().getString("player.linked-hellblock"));
				} catch (IllegalArgumentException ex) {
					this.hellblockOwner = null;
					LogUtils.severe(String.format("Could not find the UUID from the linked hellblock owner: %s",
							this.getHellblockPlayer().getString("player.linked-hellblock")), ex);
				}
			} else {
				this.linkedHellblock = null;
			}
			this.lockedStatus = this.getHellblockPlayer().getBoolean("player.locked-island", false);
			this.hellblockLocation = this.deserializeLocation("player.hellblock");
			this.homeLocation = this.deserializeLocation("player.home");
			this.creationTime = this.getHellblockPlayer().getLong("player.creation-time");
			this.resetCooldown = this.getHellblockPlayer().getLong("player.reset-cooldown", 0L);
			this.biomeCooldown = this.getHellblockPlayer().getLong("player.biome-cooldown", 0L);
			this.totalVisitors = this.getHellblockPlayer().getInt("player.total-visits", 0);
			this.hellblockBiome = HellblockPlugin.getInstance().getBiomeHandler().convertBiomeToHellBiome(
					Biome.valueOf(this.getHellblockPlayer().getString("player.biome", "NETHER_WASTES")));
			try {
				this.hellblockOwner = UUID.fromString(this.getHellblockPlayer().getString("player.owner"));
			} catch (IllegalArgumentException ex) {
				this.hellblockOwner = null;
				LogUtils.severe(String.format("Could not find the UUID from the hellblock owner: %s",
						this.getHellblockPlayer().getString("player.owner")), ex);
			}
			if (this.getHellblockPlayer().contains("player.party")
					&& !this.getHellblockPlayer().getStringList("player.party").isEmpty()) {
				for (String member : this.getHellblockPlayer().getStringList("player.party")) {
					this.hellblockParty = new HashSet<>();
					UUID uuid = null;
					try {
						uuid = UUID.fromString(member);
					} catch (IllegalArgumentException ignored) {
						// ignored
					}
					if (uuid != null)
						this.hellblockParty.add(uuid);
				}
			} else {
				this.hellblockParty = new HashSet<>();
			}
			if (this.getHellblockPlayer().contains("player.banned-from-island")
					&& !this.getHellblockPlayer().getStringList("player.banned-from-island").isEmpty()) {
				for (String banned : this.getHellblockPlayer().getStringList("player.banned-from-island")) {
					this.bannedPlayers = new HashSet<>();
					UUID uuid = null;
					try {
						uuid = UUID.fromString(banned);
					} catch (IllegalArgumentException ignored) {
						// ignored
					}
					if (uuid != null)
						this.bannedPlayers.add(uuid);
				}
			} else {
				this.bannedPlayers = new HashSet<>();
			}
			if (this.getHellblockPlayer().contains("player.bounding-box")) {
				double x1 = this.getHellblockPlayer().getDouble("player.bounding-box.min-x");
				double y1 = HellblockPlugin.getInstance().getHellblockHandler().getHellblockWorld().getMinHeight();
				double z1 = this.getHellblockPlayer().getDouble("player.bounding-box.min-z");
				double x2 = this.getHellblockPlayer().getDouble("player.bounding-box.max-x");
				double y2 = HellblockPlugin.getInstance().getHellblockHandler().getHellblockWorld().getMaxHeight();
				double z2 = this.getHellblockPlayer().getDouble("player.bounding-box.max-z");
				this.hellblockBoundingBox = new BoundingBox(x1, y1, z1, x2, y2, z2);
			}
			if (this.getHellblockPlayer().contains("player.protection-flags")) {
				this.protectionFlags = new HashMap<>();
				this.getHellblockPlayer().getConfigurationSection("player.protection-flags").getKeys(false)
						.forEach(key -> {
							FlagType flag = FlagType.valueOf(key);
							AccessType status = AccessType
									.valueOf(this.getHellblockPlayer().getString("player.protection-flags." + key));
							this.protectionFlags.put(flag, status);
						});
			} else {
				this.protectionFlags = new HashMap<>();
			}
		} else {
			this.hellblockID = 0;
			this.hellblockLevel = 0.0F;
			this.hellblockBiome = null;
			this.hellblockLocation = null;
			this.hellblockBoundingBox = null;
			this.resetCooldown = 0L;
			this.biomeCooldown = 0L;
			this.creationTime = 0L;
			this.totalVisitors = 0;
			this.homeLocation = null;
			this.hellblockOwner = null;
			this.linkedHellblock = null;
			this.protectionFlags = new HashMap<>();
			this.bannedPlayers = new HashSet<>();
			this.hellblockParty = new HashSet<>();
			if (this.getHellblockPlayer().contains("player.invitations")) {
				this.invitations = new HashMap<>();
				this.getHellblockPlayer().getConfigurationSection("player.invitations").getKeys(false).forEach(key -> {
					UUID invitee = UUID.fromString(key);
					long expirationTime = this.getHellblockPlayer().getLong("player.invitations." + key);
					this.invitations.put(invitee, expirationTime);
				});
			} else {
				this.invitations = new HashMap<>();
			}
		}
	}

	public @Nullable Player getPlayer() {
		return Bukkit.getPlayer(this.id);
	}

	public @NonNull UUID getUUID() {
		return this.id;
	}

	public @NonNull HellblockParty getParty() {
		return new HellblockParty(this);
	}

	public boolean hasHellblock() {
		return this.hasHellblock;
	}

	public @Nullable Location getHomeLocation() {
		return this.homeLocation;
	}

	public int getID() {
		return this.hellblockID;
	}

	public @Nullable Location getHellblockLocation() {
		return this.hellblockLocation;
	}

	public @Nullable UUID getHellblockOwner() {
		return this.hellblockOwner;
	}

	public @Nullable UUID getLinkedHellblock() {
		return this.linkedHellblock;
	}

	public boolean hasLinkedHellblock() {
		return this.linkedHellblock != null;
	}

	public @NonNull Set<UUID> getHellblockParty() {
		return this.hellblockParty;
	}

	public @NonNull Set<UUID> getWhoTrusted() {
		return this.whoHasTrusted;
	}

	public @NonNull Set<UUID> getBannedPlayers() {
		return this.bannedPlayers;
	}

	public @Nullable HellBiome getHellblockBiome() {
		return this.hellblockBiome;
	}

	public long getResetCooldown() {
		return this.resetCooldown;
	}

	public long getBiomeCooldown() {
		return this.biomeCooldown;
	}

	public @Nullable IslandOptions getIslandChoice() {
		return this.islandChoice;
	}

	public @Nullable String getUsedSchematic() {
		return this.schematic;
	}

	public boolean getLockedStatus() {
		return this.lockedStatus;
	}

	public int getTotalVisitors() {
		return this.totalVisitors;
	}

	public float getLevel() {
		return this.hellblockLevel;
	}

	public long getCreation() {
		return this.creationTime;
	}

	public @Nullable String getCreationTime() {
		LocalDateTime localDate = LocalDateTime.ofInstant(Instant.ofEpochMilli(this.creationTime),
				ZoneId.systemDefault());
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("KK:mm:ss a", Locale.ENGLISH);
		String now = localDate.format(formatter);
		return String.format("%s %s %s %s", localDate.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH),
				localDate.getDayOfMonth(), localDate.getYear(), now);
	}

	public @Nullable BoundingBox getHellblockBoundingBox() {
		return this.hellblockBoundingBox;
	}

	public @Nullable Map<UUID, Long> getInvitations() {
		if (this.invitations == null)
			return new HashMap<>();
		return this.invitations;
	}

	public boolean hasInvite(@NonNull UUID playerID) {
		boolean inviteExists = false;
		if (!this.invitations.isEmpty()) {
			for (Entry<UUID, Long> invites : this.invitations.entrySet()) {
				if (invites.getKey().equals(playerID)) {
					inviteExists = true;
					break;
				}
			}
		}
		return inviteExists;
	}

	public boolean hasInviteExpired(@NonNull UUID playerID) {
		boolean expired = false;
		if (!this.invitations.isEmpty()) {
			for (Entry<UUID, Long> invites : this.invitations.entrySet()) {
				if (invites.getKey().equals(playerID)) {
					if (invites.getValue().longValue() == 0) {
						expired = true;
						break;
					}
				}
			}
		}
		return expired;
	}

	public boolean isAbandoned() {
		return this.isAbandoned;
	}

	public @NonNull Map<ChallengeType, Entry<CompletionStatus, ChallengeData>> getChallenges() {
		return this.challenges;
	}

	public int getChallengeProgress(ChallengeType challenge) {
		int progress = 0;
		if (!this.challenges.isEmpty()) {
			for (Entry<ChallengeType, Entry<CompletionStatus, ChallengeData>> challenges : this.challenges.entrySet()) {
				if (challenges.getKey().getName().equalsIgnoreCase(challenge.getName())) {
					if (challenges.getValue().getKey() == CompletionStatus.IN_PROGRESS) {
						progress = challenges.getValue().getValue().getProgress();
						break;
					}
				}
			}
		}
		return progress;
	}

	public boolean isChallengeActive(ChallengeType challenge) {
		boolean active = false;
		if (!this.challenges.isEmpty()) {
			for (Entry<ChallengeType, Entry<CompletionStatus, ChallengeData>> challenges : this.challenges.entrySet()) {
				if (challenges.getKey().getName().equalsIgnoreCase(challenge.getName())) {
					active = challenges.getValue().getKey() == CompletionStatus.IN_PROGRESS;
					break;
				}
			}
		}
		return active;
	}

	public boolean isChallengeCompleted(ChallengeType challenge) {
		boolean completed = false;
		if (!this.challenges.isEmpty()) {
			for (Entry<ChallengeType, Entry<CompletionStatus, ChallengeData>> challenges : this.challenges.entrySet()) {
				if (challenges.getKey().getName().equalsIgnoreCase(challenge.getName())) {
					completed = challenges.getValue().getValue().getProgress() >= challenge.getNeededAmount();
					break;
				}
			}
		}
		return completed;
	}

	public boolean isChallengeRewardClaimed(ChallengeType challenge) {
		boolean claimed = false;
		if (!this.challenges.isEmpty()) {
			for (Entry<ChallengeType, Entry<CompletionStatus, ChallengeData>> challenges : this.challenges.entrySet()) {
				if (challenges.getKey().getName().equalsIgnoreCase(challenge.getName())) {
					if (challenges.getValue().getKey() == CompletionStatus.COMPLETED) {
						if (challenges.getValue().getValue().getProgress() == challenge.getNeededAmount()) {
							claimed = challenges.getValue().getValue().isRewardClaimed();
							break;
						}
					}
				}
			}
		}
		return claimed;
	}

	public @NonNull Map<FlagType, AccessType> getProtectionFlags() {
		return this.protectionFlags;
	}

	public @NonNull AccessType getProtectionValue(FlagType flag) {
		AccessType returnValue = flag.getDefaultValue() ? AccessType.ALLOW : AccessType.DENY;
		if (!this.protectionFlags.isEmpty()) {
			for (Entry<FlagType, AccessType> flags : this.protectionFlags.entrySet()) {
				if (flags.getKey().getName().equalsIgnoreCase(flag.getName())) {
					returnValue = flags.getValue();
					break;
				}
			}
		}
		return returnValue;
	}

	public void setHellblock(boolean hasHellblock, @Nullable Location hellblockLocation, int hellblockID) {
		this.hasHellblock = hasHellblock;
		this.hellblockLocation = hellblockLocation;
		this.hellblockID = hellblockID;
		this.hellblockLevel = DEFAULT_LEVEL;
	}

	public void setHome(@Nullable Location homeLocation) {
		this.homeLocation = homeLocation;
	}

	public void showBorder() {
		this.borderTask = new HellblockBorderTask(HellblockPlugin.getInstance(), this.id);
	}

	public void hideBorder() {
		if (this.borderTask != null) {
			this.borderTask.cancelBorderShowcase();
			this.borderTask = null;
		}
	}

	public void startSpawningAnimals() {
		this.animalSpawningTask = new NetherAnimalSpawningTask(HellblockPlugin.getInstance(), this.id);
	}

	public void stopSpawningAnimals() {
		if (this.animalSpawningTask != null) {
			this.animalSpawningTask.stopAnimalSpawning();
			this.animalSpawningTask = null;
		}
	}

	public void setHellblockBoundingBox(@Nullable BoundingBox box) {
		this.hellblockBoundingBox = box;
	}

	public void setHellblockOwner(@Nullable UUID newOwner) {
		this.hellblockOwner = newOwner;
	}

	public void setLinkedHellblock(@Nullable UUID linkedOwner) {
		this.linkedHellblock = linkedOwner;
	}

	public void addToHellblockParty(@NonNull UUID newMember) {
		if (!this.hellblockParty.contains(newMember))
			this.hellblockParty.add(newMember);
	}

	public void kickFromHellblockParty(@NonNull UUID oldMember) {
		if (this.hellblockParty.contains(oldMember))
			this.hellblockParty.remove(oldMember);
	}

	public void setHellblockParty(@NonNull Set<UUID> partyMembers) {
		this.hellblockParty = partyMembers;
	}

	public void addTrustPermission(@NonNull UUID newMember) {
		if (!this.whoHasTrusted.contains(newMember))
			this.whoHasTrusted.add(newMember);
	}

	public void removeTrustPermission(@NonNull UUID oldMember) {
		if (this.whoHasTrusted.contains(oldMember))
			this.whoHasTrusted.remove(oldMember);
	}

	public void setWhoTrusted(@NonNull Set<UUID> trustedMembers) {
		this.whoHasTrusted = trustedMembers;
	}

	public void banPlayer(@NonNull UUID newMember) {
		if (!this.bannedPlayers.contains(newMember))
			this.bannedPlayers.add(newMember);
	}

	public void unbanPlayer(@NonNull UUID oldMember) {
		if (this.bannedPlayers.contains(oldMember))
			this.bannedPlayers.remove(oldMember);
	}

	public void setBannedPlayers(@NonNull Set<UUID> trustedMembers) {
		this.bannedPlayers = trustedMembers;
	}

	public void setHellblockBiome(@Nullable HellBiome biome) {
		this.hellblockBiome = biome;
	}

	public void setResetCooldown(long cooldown) {
		this.resetCooldown = cooldown;
	}

	public void setBiomeCooldown(long cooldown) {
		this.biomeCooldown = cooldown;
	}

	public void setIslandChoice(@Nullable IslandOptions choice) {
		this.islandChoice = choice;
	}

	public void setUsedSchematic(@Nullable String schematic) {
		this.schematic = schematic;
	}

	public void setLockedStatus(boolean locked) {
		this.lockedStatus = locked;
	}

	public void setCreationTime(long creation) {
		this.creationTime = creation;
	}

	public void addTotalVisit() {
		this.totalVisitors++;
	}

	public void setTotalVisits(int visits) {
		this.totalVisitors = visits;
	}

	public void increaseIslandLevel() {
		this.hellblockLevel++;
	}

	public void decreaseIslandLevel() {
		this.hellblockLevel--;
	}

	public void addToLevel(float levels) {
		this.hellblockLevel = this.hellblockLevel + levels;
	}

	public void removeFromLevel(float levels) {
		this.hellblockLevel = this.hellblockLevel - levels;
	}

	public void setLevel(float level) {
		this.hellblockLevel = level;
	}

	public void setChallenges(@NonNull Map<ChallengeType, Entry<CompletionStatus, ChallengeData>> challenges) {
		this.challenges = challenges;
	}

	public void setChallengeRewardAsClaimed(@NonNull ChallengeType challenge, boolean claimedReward) {
		if (this.challenges.containsKey(challenge)
				&& this.challenges.get(challenge).getKey() == CompletionStatus.COMPLETED) {
			this.challenges.get(challenge).setValue(new ChallengeData(challenge.getNeededAmount(), true));
		}
	}

	public void beginChallengeProgression(@NonNull ChallengeType challenge) {
		HellblockChallenge newChallenge = new HellblockChallenge(challenge, CompletionStatus.IN_PROGRESS, 1);
		this.challenges.putIfAbsent(newChallenge.getChallengeType(), new SimpleEntry<CompletionStatus, ChallengeData>(
				newChallenge.getCompletionStatus(), new ChallengeData(newChallenge.getProgress(), false)));
		if (getPlayer() != null)
			HellblockPlugin.getInstance().getAdventureManager()
					.sendActionbar(
							getPlayer(), String
									.format("<yellow>Progress <gold>(%s/%s)<gray>: %s",
											this.challenges.get(challenge).getValue().getProgress(),
											challenge.getNeededAmount(),
											ProgressBar.getProgressBar(
													new ProgressBar(challenge.getNeededAmount(),
															this.challenges.get(challenge).getValue().getProgress()),
													25)));
	}

	public void updateChallengeProgression(@NonNull ChallengeType challenge, int progressToAdd) {
		if (this.challenges.containsKey(challenge)
				&& this.challenges.get(challenge).getKey() == CompletionStatus.IN_PROGRESS) {
			this.challenges.get(challenge).setValue(new ChallengeData(
					(this.challenges.get(challenge).getValue().getProgress() + progressToAdd), false));
			if (getPlayer() != null)
				HellblockPlugin.getInstance().getAdventureManager().sendActionbar(getPlayer(),
						String.format("<yellow>Progress <gold>(%s/%s)<gray>: %s",
								this.challenges.get(challenge).getValue().getProgress(), challenge.getNeededAmount(),
								ProgressBar.getProgressBar(new ProgressBar(challenge.getNeededAmount(),
										this.challenges.get(challenge).getValue().getProgress()), 25)));
		}
	}

	public void completeChallenge(@NonNull ChallengeType challenge) {
		if (this.challenges.containsKey(challenge)
				&& this.challenges.get(challenge).getKey() == CompletionStatus.IN_PROGRESS) {
			this.challenges.remove(challenge);
			HellblockChallenge completedChallenge = new HellblockChallenge(challenge, CompletionStatus.COMPLETED,
					challenge.getNeededAmount());
			this.challenges.putIfAbsent(completedChallenge.getChallengeType(),
					new SimpleEntry<CompletionStatus, ChallengeData>(completedChallenge.getCompletionStatus(),
							new ChallengeData(challenge.getNeededAmount(), false)));
			performChallengeCompletionActions(challenge);
		}
	}

	public void performChallengeCompletionActions(@NonNull ChallengeType challenge) {
		Player player = this.getPlayer();
		if (player != null) {
			HellblockPlugin.getInstance().getAdventureManager().sendCenteredMessage(player,
					"<dark_gray>[+] <gray><strikethrough>--------------------------------------------<reset> <dark_gray>[+]");
			HellblockPlugin.getInstance().getAdventureManager().sendCenteredMessage(player, " ");
			HellblockPlugin.getInstance().getAdventureManager().sendCenteredMessage(player,
					"<dark_green>*** <green><bold>Challenge Completed!<reset> <dark_green>***");
			HellblockPlugin.getInstance().getAdventureManager().sendCenteredMessage(player,
					String.format("<gold>Claim your reward by clicking this challenge in the GUI menu!"));
			HellblockPlugin.getInstance().getAdventureManager().sendCenteredMessage(player, " ");
			HellblockPlugin.getInstance().getAdventureManager().sendCenteredMessage(player,
					"<dark_gray>[+] <gray><strikethrough>--------------------------------------------<reset> <dark_gray>[+]");
			HellblockPlugin.getInstance().getAdventureManager().sendSound(player, Sound.Source.PLAYER,
					Key.key("minecraft:entity.player.levelup"), 1.0F, 1.0F);
			int fireworkID = ThreadLocalRandom.current().nextInt(Integer.MAX_VALUE);
			ItemStack firework = new ItemStack(Material.FIREWORK_ROCKET);
			FireworkMeta meta = (FireworkMeta) firework.getItemMeta();
			meta.setPower(2);
			meta.addEffect(FireworkEffect.builder().with(Type.BURST).trail(false).flicker(false).withColor(Color.LIME)
					.withFade(Color.LIME).build());
			meta.getPersistentDataContainer().set(
					new NamespacedKey(HellblockPlugin.getInstance(), "challenge-firework"), PersistentDataType.BOOLEAN,
					true);
			firework.setItemMeta(meta);
			HellblockPlugin.getInstance().sendPackets(player,
					FireworkUtils.getSpawnFireworkPacket(fireworkID, player.getLocation()),
					FireworkUtils.getFireworkMetaPacket(fireworkID, firework),
					FireworkUtils.getFireworkStatusPacket(fireworkID),
					FireworkUtils.getFireworkDestroyPacket(fireworkID));
			saveHellblockPlayer();
		}
	}

	public void setInvitations(@NonNull Map<UUID, Long> invites) {
		this.invitations = invites;
	}

	public void sendInvitation(@NonNull UUID playerID) {
		this.invitations.putIfAbsent(playerID, 86400L);
	}

	public void removeInvitation(@NonNull UUID playerID) {
		if (this.invitations.containsKey(playerID)) {
			this.invitations.remove(playerID);
		}
	}

	public void clearInvites() {
		this.invitations.clear();
		this.getHellblockPlayer().set("player.invitations", null);
	}

	public void setProtectionFlags(@NonNull Map<FlagType, AccessType> flags) {
		this.protectionFlags = flags;
	}

	public void setProtectionValue(@NonNull HellblockFlag flag) {
		if (!this.protectionFlags.isEmpty()) {
			for (Iterator<Entry<FlagType, AccessType>> iterator = this.protectionFlags.entrySet().iterator(); iterator
					.hasNext();) {
				Entry<FlagType, AccessType> flags = iterator.next();
				if (flags.getKey().getName().equalsIgnoreCase(flag.getFlag().getName())) {
					iterator.remove();
					this.getHellblockPlayer().set("player.protection-flags." + flag.getFlag().toString(), null);
				}
			}
		}

		AccessType returnValue = flag.getFlag().getDefaultValue() ? AccessType.ALLOW : AccessType.DENY;
		if (flag.getStatus() != returnValue) {
			this.protectionFlags.put(flag.getFlag(), flag.getStatus());
		}
	}

	public boolean hasGlowstoneArmorEffect() {
		return this.wearingGlowstoneArmor;
	}

	public boolean hasGlowstoneToolEffect() {
		return this.holdingGlowstoneTool;
	}

	public void isWearingGlowstoneArmor(boolean wearingGlowstoneArmor) {
		this.wearingGlowstoneArmor = wearingGlowstoneArmor;
	}

	public void isHoldingGlowstoneTool(boolean holdingGlowstoneTool) {
		this.holdingGlowstoneTool = holdingGlowstoneTool;
	}

	public void reloadHellblockPlayer() {
		this.file = new File(HellblockPlugin.getInstance().getHellblockHandler().getPlayersDirectory() + File.separator
				+ this.id + ".yml");
		this.pi = YamlConfiguration.loadConfiguration(this.file);
	}

	public void saveHellblockPlayer() {
		this.getHellblockPlayer().set("player.hasHellblock", this.hasHellblock);
		if (this.challenges != null && !this.challenges.isEmpty()) {
			for (Entry<ChallengeType, Entry<CompletionStatus, ChallengeData>> challenges : this.challenges.entrySet()) {
				if (challenges.getValue().getKey() == CompletionStatus.NOT_STARTED)
					continue;
				this.getHellblockPlayer().set("player.challenges." + challenges.getKey().toString() + ".status",
						challenges.getValue().getKey().toString());
				if (challenges.getValue().getKey() == CompletionStatus.IN_PROGRESS) {
					this.getHellblockPlayer().set("player.challenges." + challenges.getKey().toString() + ".progress",
							challenges.getValue().getValue().getProgress());
				}
				if (challenges.getValue().getKey() == CompletionStatus.COMPLETED) {
					this.getHellblockPlayer().set("player.challenges." + challenges.getKey().toString() + ".progress",
							null);
					if (challenges.getValue().getValue().isRewardClaimed()) {
						this.getHellblockPlayer().set(
								"player.challenges." + challenges.getKey().toString() + ".claimed-reward",
								challenges.getValue().getValue().isRewardClaimed());
					}
				}
			}
		}
		if (this.whoHasTrusted != null && !this.whoHasTrusted.isEmpty()) {
			Set<String> trustedString = this.whoHasTrusted.stream().filter(Objects::nonNull).map(UUID::toString)
					.collect(Collectors.toSet());
			if (!trustedString.isEmpty()) {
				this.getHellblockPlayer().set("player.trusted-on-islands", trustedString);
			}
		}
		if (this.hasHellblock) {
			if (this.hellblockID > 0) {
				this.getHellblockPlayer().set("player.hellblock-id", this.hellblockID);
			}
			if (this.hellblockLevel > DEFAULT_LEVEL) {
				this.getHellblockPlayer().set("player.hellblock-level", this.hellblockLevel);
			}
			this.serializeLocation("player.hellblock", this.hellblockLocation);
			this.serializeLocation("player.home", this.homeLocation);
			this.getHellblockPlayer().set("player.creation-time", this.creationTime);
			this.getHellblockPlayer().set("player.island-choice.type", this.islandChoice.toString());
			if (this.islandChoice == IslandOptions.SCHEMATIC) {
				this.getHellblockPlayer().set("player.island-choice.used-schematic", this.schematic);
			}
			if (this.lockedStatus) {
				this.getHellblockPlayer().set("player.locked-island", this.lockedStatus);
			}
			if (this.totalVisitors > 0) {
				this.getHellblockPlayer().set("player.total-visits", this.totalVisitors);
			}
			if (this.hellblockOwner != null) {
				this.getHellblockPlayer().set("player.owner", this.hellblockOwner.toString());
			}
			if (this.hellblockBoundingBox != null) {
				this.getHellblockPlayer().set("player.bounding-box.min-x", this.hellblockBoundingBox.getMinX());
				this.getHellblockPlayer().set("player.bounding-box.min-z", this.hellblockBoundingBox.getMinZ());
				this.getHellblockPlayer().set("player.bounding-box.max-x", this.hellblockBoundingBox.getMaxX());
				this.getHellblockPlayer().set("player.bounding-box.max-z", this.hellblockBoundingBox.getMaxZ());
			}
			if (this.linkedHellblock != null && !this.linkedHellblock.equals(this.id)
					&& !this.hellblockParty.contains(this.linkedHellblock)) {
				this.getHellblockPlayer().set("player.linked-hellblock", this.linkedHellblock.toString());
			}
			if (this.resetCooldown > 0) {
				this.getHellblockPlayer().set("player.reset-cooldown", this.resetCooldown);
			}
			if (this.biomeCooldown > 0) {
				this.getHellblockPlayer().set("player.biome-cooldown", this.biomeCooldown);
			}
			if (this.hellblockBiome != null && this.hellblockBiome != HellBiome.NETHER_WASTES) {
				this.getHellblockPlayer().set("player.biome", this.hellblockBiome.toString());
			}
			if (this.hellblockParty != null && !this.hellblockParty.isEmpty()) {
				Set<String> partyString = this.hellblockParty.stream().filter(Objects::nonNull).map(UUID::toString)
						.collect(Collectors.toSet());
				if (!partyString.isEmpty()) {
					this.getHellblockPlayer().set("player.party", partyString);
				}
			}
			if (this.bannedPlayers != null && !this.bannedPlayers.isEmpty()) {
				Set<String> bannedString = this.bannedPlayers.stream().filter(Objects::nonNull).map(UUID::toString)
						.collect(Collectors.toSet());
				if (!bannedString.isEmpty()) {
					this.getHellblockPlayer().set("player.banned-from-island", bannedString);
				}
			}
			if (this.protectionFlags != null && !this.protectionFlags.isEmpty()) {
				for (Map.Entry<FlagType, AccessType> flags : this.protectionFlags.entrySet()) {
					AccessType returnValue = flags.getKey().getDefaultValue() ? AccessType.ALLOW : AccessType.DENY;
					if (flags.getValue() == returnValue)
						continue;
					this.getHellblockPlayer().set("player.protection-flags." + flags.getKey().toString(),
							flags.getValue().toString());
				}
			} else {
				if (this.getHellblockPlayer().getConfigurationSection("player.protection-flags") != null
						&& this.getHellblockPlayer().getConfigurationSection("player.protection-flags").getKeys(false)
								.isEmpty()) {
					this.getHellblockPlayer().set("player.protection-flags", null);
				}
			}
		} else {
			if (this.invitations != null && !this.invitations.isEmpty()) {
				for (Map.Entry<UUID, Long> invites : this.invitations.entrySet()) {
					if (invites.getValue() == 0)
						continue;
					this.getHellblockPlayer().set("player.invitations." + invites.getKey().toString(),
							invites.getValue().longValue());
				}
			}
		}

		try {
			this.getHellblockPlayer().save(this.file);
		} catch (IOException ex) {
			LogUtils.severe(String.format("Unable to save player file for %s!", this.getPlayer().getName()), ex);
		}
	}

	public void resetHellblockData() {
		this.getHellblockPlayer().set("player.hasHellblock", false);
		this.getHellblockPlayer().set("player.hellblock", null);
		this.getHellblockPlayer().set("player.hellblock-id", getID() == 0 ? null : getID());
		this.getHellblockPlayer().set("player.hellblock-level", null);
		this.getHellblockPlayer().set("player.home", null);
		this.getHellblockPlayer().set("player.owner", null);
		this.getHellblockPlayer().set("player.biome", null);
		this.getHellblockPlayer().set("player.party", null);
		this.getHellblockPlayer().set("player.bounding-box", null);
		this.getHellblockPlayer().set("player.creation-time", null);
		this.getHellblockPlayer().set("player.total-visits", null);
		this.getHellblockPlayer().set("player.locked-island", null);
		this.getHellblockPlayer().set("player.biome-cooldown", null);
		this.getHellblockPlayer().set("player.island-choice", null);
		this.getHellblockPlayer().set("player.protection-flags", null);
		this.getHellblockPlayer().set("player.banned-from-island", null);

		try {
			this.getHellblockPlayer().save(this.file);
		} catch (IOException ex) {
			LogUtils.severe(String.format("Unable to save player file for %s!", this.getPlayer().getName()), ex);
		}
	}

	public @NonNull YamlConfiguration getHellblockPlayer() {
		if (this.pi == null)
			this.reloadHellblockPlayer();
		return this.pi;
	}

	public @NonNull File getPlayerFile() {
		if (this.file == null)
			this.reloadHellblockPlayer();
		return this.file;
	}

	public void serializeLocation(@NonNull String path, @Nullable Location location) {
		String world = HellblockPlugin.getInstance().getHellblockHandler().getWorldName();
		double x = 0.0D;
		double y = (double) HellblockPlugin.getInstance().getHellblockHandler().getHeight();
		double z = 0.0D;
		float yaw = 0.0F;
		float pitch = 0.0F;
		if (location != null) {
			world = location.getWorld().getName();
			x = location.getX();
			y = location.getY();
			z = location.getZ();
			yaw = location.getYaw();
			pitch = location.getPitch();
		}

		this.getHellblockPlayer().set(path + ".world", world);
		this.getHellblockPlayer().set(path + ".x", round(x, 3));
		this.getHellblockPlayer().set(path + ".y", round(y, 3));
		this.getHellblockPlayer().set(path + ".z", round(z, 3));
		this.getHellblockPlayer().set(path + ".yaw", round(yaw, 3));
		this.getHellblockPlayer().set(path + ".pitch", round(pitch, 3));
	}

	public @Nullable Location deserializeLocation(@NonNull String path) {
		World world = Bukkit.getWorld(this.getHellblockPlayer().getString(path + ".world"));
		double x = this.getHellblockPlayer().getDouble(path + ".x");
		double y = this.getHellblockPlayer().getDouble(path + ".y");
		double z = this.getHellblockPlayer().getDouble(path + ".z");
		float yaw = (float) this.getHellblockPlayer().getDouble(path + ".yaw");
		float pitch = (float) this.getHellblockPlayer().getDouble(path + ".pitch");
		return new Location(world, x, y, z, yaw, pitch);
	}

	/**
	 * Rounds the specified value to the amount of decimals specified
	 *
	 * @param value    to round
	 * @param decimals count
	 * @return value round to the decimal count specified
	 */
	public double round(double value, int decimals) {
		double p = Math.pow(10, decimals);
		return Math.round(value * p) / p;
	}

	public enum HellblockData {
		BIOME, HOME, LOCK, LEVEL_ADDITION, LEVEL_REMOVAL, VISIT, RESET_COOLDOWN, BIOME_COOLDOWN, PARTY_REMOVAL,
		PARTY_ADDITION, OWNER, PROTECTION_FLAG, BAN, UNBAN;
	}
}
