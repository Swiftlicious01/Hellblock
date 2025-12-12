package com.swiftlicious.hellblock.player;

import org.jetbrains.annotations.NotNull;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.swiftlicious.hellblock.utils.adapters.HellblockTypeAdapterFactory.EmptyCheck;

/**
 * Stores invasion statistics for a single island.
 * <p>
 * This includes success and failure counts, boss defeats, current win streak,
 * highest difficulty tier reached, and the timestamp of the last invasion.
 * <p>
 * The data is saved and loaded with the island's persistent state, and is not
 * tied to any specific player. It enables features like invasion streak
 * tracking, progression rewards, boss kill achievements, and invasion cooldown
 * management.
 */
public class InvasionData implements EmptyCheck {

	@Expose
	@SerializedName("totalInvasions")
	protected int totalInvasions;

	@Expose
	@SerializedName("successfulInvasions")
	protected int successfulInvasions;

	@Expose
	@SerializedName("failedInvasions")
	protected int failedInvasions;

	@Expose
	@SerializedName("bossKills")
	protected int bossKills;

	@Expose
	@SerializedName("currentStreak")
	protected int currentStreak;

	@Expose
	@SerializedName("lastInvasionTime")
	protected long lastInvasionTime;

	@Expose
	@SerializedName("highestDifficultyTierReached")
	protected int highestDifficultyTierReached;

	/**
	 * Creates a new instance of {@code InvasionData} with all counters reset.
	 */
	public InvasionData() {
		this.totalInvasions = 0;
		this.successfulInvasions = 0;
		this.failedInvasions = 0;
		this.bossKills = 0;
		this.currentStreak = 0;
		this.lastInvasionTime = 0L;
		this.highestDifficultyTierReached = 0;
	}

	/**
	 * Records a successful invasion attempt.
	 * <p>
	 * Increments total and successful counters, and updates the current streak.
	 *
	 * @param bossDefeated {@code true} if the boss was defeated during the invasion
	 */
	public void recordVictory(boolean bossDefeated) {
		totalInvasions++;
		successfulInvasions++;
		currentStreak++;
		if (bossDefeated) {
			bossKills++;
		}
	}

	/**
	 * Records a failed invasion attempt (e.g., timeout, no players, or defeat).
	 * <p>
	 * Increments total and failed counters and resets the win streak.
	 */
	public void recordFailure() {
		totalInvasions++;
		failedInvasions++;
		currentStreak = 0;
	}

	/**
	 * Returns the total number of invasions attempted by this island.
	 *
	 * @return total invasions attempted
	 */
	public int getTotalInvasions() {
		return totalInvasions;
	}

	/**
	 * Returns the number of successful invasions completed.
	 *
	 * @return number of successful invasions
	 */
	public int getSuccessfulInvasions() {
		return successfulInvasions;
	}

	/**
	 * Returns the number of failed invasions.
	 *
	 * @return number of failed invasions
	 */
	public int getFailedInvasions() {
		return failedInvasions;
	}

	/**
	 * Returns the number of bosses defeated across all invasions.
	 *
	 * @return total boss kills
	 */
	public int getBossKills() {
		return bossKills;
	}

	/**
	 * Returns the current win streak of successful invasions.
	 *
	 * @return current streak count
	 */
	public int getCurrentStreak() {
		return currentStreak;
	}

	/**
	 * Returns the timestamp (in epoch millis) of the most recent invasion.
	 *
	 * @return time of last invasion
	 */
	public long getLastInvasionTime() {
		return lastInvasionTime;
	}

	/**
	 * Returns the highest difficulty tier reached by the island in any invasion.
	 *
	 * @return highest difficulty tier reached
	 */
	public int getHighestDifficultyTierReached() {
		return highestDifficultyTierReached;
	}

	/**
	 * Sets the total number of invasions attempted.
	 *
	 * @param totalInvasions total invasion count
	 */
	public void setTotalInvasions(int totalInvasions) {
		this.totalInvasions = totalInvasions;
	}

	/**
	 * Sets the number of successful invasions.
	 *
	 * @param successfulInvasions number of successes
	 */
	public void setSuccessfulInvasions(int successfulInvasions) {
		this.successfulInvasions = successfulInvasions;
	}

	/**
	 * Sets the number of failed invasions.
	 *
	 * @param failedInvasions number of failures
	 */
	public void setFailedInvasions(int failedInvasions) {
		this.failedInvasions = failedInvasions;
	}

	/**
	 * Sets the number of bosses defeated.
	 *
	 * @param bossKills number of boss kills
	 */
	public void setBossKills(int bossKills) {
		this.bossKills = bossKills;
	}

	/**
	 * Sets the current win streak of the island.
	 *
	 * @param currentStreak number of consecutive victories
	 */
	public void setCurrentStreak(int currentStreak) {
		this.currentStreak = currentStreak;
	}

	/**
	 * Sets the last invasion time in epoch milliseconds.
	 *
	 * @param timeMillis last invasion timestamp
	 */
	public void setLastInvasionTime(long timeMillis) {
		this.lastInvasionTime = timeMillis;
	}

	/**
	 * Sets the highest difficulty tier reached in any invasion.
	 *
	 * @param tier maximum difficulty tier
	 */
	public void setHighestDifficultyTierReached(int tier) {
		this.highestDifficultyTierReached = tier;
	}

	/**
	 * Returns the success rate of all invasions as a percentage.
	 *
	 * @return percentage of successful invasions, or 0.0 if none attempted
	 */
	public double getSuccessRate() {
		return totalInvasions == 0 ? 0.0 : (successfulInvasions * 100.0) / totalInvasions;
	}

	/**
	 * Returns whether the island has ever defeated an invasion boss.
	 *
	 * @return {@code true} if at least one boss was killed
	 */
	public boolean hasDefeatedBossBefore() {
		return bossKills > 0;
	}

	/**
	 * Returns whether the island has ever completed a successful invasion.
	 *
	 * @return {@code true} if any successful invasion has occurred
	 */
	public boolean hasEverSucceeded() {
		return successfulInvasions > 0;
	}

	/**
	 * Returns whether the island is currently on a win streak.
	 *
	 * @return {@code true} if the current streak is active
	 */
	public boolean hasActiveStreak() {
		return currentStreak > 0;
	}

	/**
	 * Returns a new {@code InvasionData} instance with all values reset to
	 * defaults.
	 *
	 * @return a fresh {@code InvasionData} object
	 */
	@NotNull
	public static InvasionData empty() {
		return new InvasionData();
	}

	/**
	 * Creates a deep copy of this {@code InvasionData} object.
	 * <p>
	 * All fields are copied to a new instance with no shared references.
	 *
	 * @return cloned {@code InvasionData} object with same values
	 */
	@NotNull
	public final InvasionData copy() {
		InvasionData copy = new InvasionData();
		copy.totalInvasions = this.totalInvasions;
		copy.successfulInvasions = this.successfulInvasions;
		copy.failedInvasions = this.failedInvasions;
		copy.bossKills = this.bossKills;
		copy.currentStreak = this.currentStreak;
		copy.lastInvasionTime = this.lastInvasionTime;
		copy.highestDifficultyTierReached = this.highestDifficultyTierReached;
		return copy;
	}

	/**
	 * Returns a formatted string representation of this {@code InvasionData}
	 * instance, useful for debugging and logging.
	 *
	 * @return a string describing the current Invasion statistics
	 */
	@Override
	public String toString() {
		return "InvasionData{" + "total=" + totalInvasions + ", success=" + successfulInvasions + ", fails="
				+ failedInvasions + ", bosses=" + bossKills + ", streak=" + currentStreak + ", lastTime="
				+ lastInvasionTime + ", highestTier=" + highestDifficultyTierReached + '}';
	}

	@Override
	public boolean isEmpty() {
		return totalInvasions == 0 && successfulInvasions == 0 && failedInvasions == 0 && bossKills == 0
				&& currentStreak == 0 && lastInvasionTime == 0L && highestDifficultyTierReached == 0;
	}
}