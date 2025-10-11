package com.swiftlicious.hellblock.player;

import java.util.List;
import java.util.UUID;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.swiftlicious.hellblock.player.UserData.Builder;
import com.swiftlicious.hellblock.player.mailbox.MailboxEntry;

/**
 * Interface representing user data. This interface provides methods for
 * accessing and managing user-related information.
 */
public interface UserDataInterface {

	@NotNull
	String getName();

	@NotNull
	UUID getUUID();

	@Nullable
	Player getPlayer();

	@NotNull
	EarningData getEarningData();

	@NotNull
	FishingStatistics getStatisticData();

	@NotNull
	ChallengeData getChallengeData();

	@NotNull
	HellblockData getHellblockData();

	@NotNull
	LocationCacheData getLocationCacheData();

	boolean isOnline();

	boolean isLocked();

	@NotNull
	List<MailboxEntry> getMailbox();

	long getLastActivity();

	boolean hasHellblockInviteNotifications();

	boolean hasHellblockJoinNotifications();

	@NotNull
	PlayerData toPlayerData();

	static Builder builder() {
		return new UserData.Builder();
	}

	interface BuilderInterface {

		Builder setName(String name);

		Builder setUUID(UUID uuid);

		Builder setEarningData(EarningData earningData);

		Builder setStatisticData(FishingStatistics statisticData);

		Builder setChallengeData(ChallengeData challengeData);

		Builder setHellblockData(HellblockData hellblockData);

		Builder setLocationCacheData(LocationCacheData locationCacheData);

		Builder setLocked(boolean isLocked);

		Builder setMailbox(List<MailboxEntry> mailbox);

		Builder setHellblockInviteNotifications(boolean notifications);

		Builder setHellblockJoinNotifications(boolean notifications);

		Builder updateLastActivity();

		Builder setData(PlayerData playerData);

		UserData build();
	}
}