package com.swiftlicious.hellblock.player;

import org.jetbrains.annotations.NotNull;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.swiftlicious.hellblock.utils.adapters.HellblockTypeAdapterFactory.EmptyCheck;

/**
 * The {@code SkysiegeData} class stores persistent SkySiege event statistics
 * for a specific island.
 * <p>
 * SkySiege is a special event or challenge mode, and this class tracks
 * performance and activity data related to those events. It maintains a
 * complete record of the island’s SkySiege history, including successful and
 * failed attempts, kills, durations, and participation counts.
 * <p>
 * All values are persisted along with the island’s save data and remain
 * available between sessions.
 */
public class SkysiegeData implements EmptyCheck {

	@Expose
	@SerializedName("totalSkysieges")
	protected int totalSkysieges;

	@Expose
	@SerializedName("successfulSkysieges")
	protected int successfulSkysieges;

	@Expose
	@SerializedName("failedSkysieges")
	protected int failedSkysieges;

	@Expose
	@SerializedName("queenKills")
	protected int queenKills;

	@Expose
	@SerializedName("totalWavesCompleted")
	protected int totalWavesCompleted;

	@Expose
	@SerializedName("totalGhastsKilled")
	protected int totalGhastsKilled;

	@Expose
	@SerializedName("longestDurationMillis")
	protected long longestDurationMillis;

	@Expose
	@SerializedName("shortestDurationMillis")
	protected long shortestDurationMillis;

	@Expose
	@SerializedName("lastSkysiegeTime")
	protected long lastSkysiegeTime;

	/**
	 * Constructs a new {@code SkysiegeData} instance with all values initialized to
	 * defaults.
	 * <p>
	 * This serves as the baseline for tracking a new island’s SkySiege statistics.
	 */
	public SkysiegeData() {
		this.totalSkysieges = 0;
		this.successfulSkysieges = 0;
		this.failedSkysieges = 0;
		this.queenKills = 0;
		this.totalWavesCompleted = 0;
		this.totalGhastsKilled = 0;
		this.longestDurationMillis = 0L;
		this.shortestDurationMillis = 0L;
		this.lastSkysiegeTime = 0L;
	}

	/**
	 * Records the start of a new SkySiege event.
	 * <p>
	 * Increments the total number of SkySiege attempts for this island.
	 */
	public void recordStart() {
		totalSkysieges++;
	}

	/**
	 * Records the completion of a successful SkySiege event.
	 * <p>
	 * This method updates multiple statistics based on the provided parameters,
	 * such as total waves completed, ghasts killed, whether the queen was defeated,
	 * and duration metrics. The recorded duration contributes to both the shortest
	 * and longest time comparisons.
	 *
	 * @param queenDefeated whether the queen boss was defeated during the event
	 * @param waves         the number of waves completed
	 * @param ghastsKilled  the number of ghasts killed during the event
	 * @param duration      the total duration of the event in milliseconds
	 */
	public void recordSuccess(boolean queenDefeated, int waves, int ghastsKilled, long duration) {
		successfulSkysieges++;
		totalWavesCompleted += waves;
		totalGhastsKilled += ghastsKilled;
		if (queenDefeated)
			queenKills++;

		if (duration > longestDurationMillis)
			longestDurationMillis = duration;
		if (shortestDurationMillis == 0 || duration < shortestDurationMillis)
			shortestDurationMillis = duration;

		lastSkysiegeTime = System.currentTimeMillis();
	}

	/**
	 * Records a failed SkySiege attempt.
	 * <p>
	 * This method increments the failure counter and updates the last event
	 * timestamp.
	 */
	public void recordFailure() {
		failedSkysieges++;
		lastSkysiegeTime = System.currentTimeMillis();
	}

	/**
	 * Returns the total number of SkySiege events attempted by this island.
	 *
	 * @return the total SkySiege count
	 */
	public int getTotalSkysieges() {
		return totalSkysieges;
	}

	/**
	 * Returns the number of successful SkySiege events completed by this island.
	 *
	 * @return the total number of successful SkySieges
	 */
	public int getSuccessfulSkysieges() {
		return successfulSkysieges;
	}

	/**
	 * Returns the number of failed SkySiege events attempted by this island.
	 *
	 * @return the total number of failed SkySieges
	 */
	public int getFailedSkysieges() {
		return failedSkysieges;
	}

	/**
	 * Returns the number of SkySiege queen bosses defeated by this island.
	 *
	 * @return the total number of queen kills
	 */
	public int getQueenKills() {
		return queenKills;
	}

	/**
	 * Returns the total number of waves completed across all SkySiege events.
	 *
	 * @return the total completed waves
	 */
	public int getTotalWavesCompleted() {
		return totalWavesCompleted;
	}

	/**
	 * Returns the total number of ghasts killed across all SkySiege events.
	 *
	 * @return the total number of ghasts killed
	 */
	public int getTotalGhastsKilled() {
		return totalGhastsKilled;
	}

	/**
	 * Returns the longest recorded duration of any SkySiege event in milliseconds.
	 *
	 * @return the longest duration
	 */
	public long getLongestDurationMillis() {
		return longestDurationMillis;
	}

	/**
	 * Returns the shortest recorded duration of any SkySiege event in milliseconds.
	 *
	 * @return the shortest duration
	 */
	public long getShortestDurationMillis() {
		return shortestDurationMillis;
	}

	/**
	 * Returns the epoch timestamp of the most recent SkySiege event.
	 *
	 * @return the last recorded SkySiege time in milliseconds
	 */
	public long getLastSkysiegeTime() {
		return lastSkysiegeTime;
	}

	/**
	 * Sets the total number of SkySiege events for this island.
	 *
	 * @param totalSkysieges the new total value
	 */
	public void setTotalSkysieges(int totalSkysieges) {
		this.totalSkysieges = totalSkysieges;
	}

	/**
	 * Sets the total number of successful SkySiege events for this island.
	 *
	 * @param successfulSkysieges the new total successful count
	 */
	public void setSuccessfulSkysieges(int successfulSkysieges) {
		this.successfulSkysieges = successfulSkysieges;
	}

	/**
	 * Sets the total number of failed SkySiege events for this island.
	 *
	 * @param failedSkysieges the new total failed count
	 */
	public void setFailedSkysieges(int failedSkysieges) {
		this.failedSkysieges = failedSkysieges;
	}

	/**
	 * Sets the total number of SkySiege queen kills recorded for this island.
	 *
	 * @param queenKills the number of queen kills
	 */
	public void setQueenKills(int queenKills) {
		this.queenKills = queenKills;
	}

	/**
	 * Sets the total number of waves completed across all SkySiege events.
	 *
	 * @param totalWavesCompleted the total number of waves completed
	 */
	public void setTotalWavesCompleted(int totalWavesCompleted) {
		this.totalWavesCompleted = totalWavesCompleted;
	}

	/**
	 * Sets the total number of ghasts killed across all SkySiege events.
	 *
	 * @param totalGhastsKilled the total ghasts killed
	 */
	public void setTotalGhastsKilled(int totalGhastsKilled) {
		this.totalGhastsKilled = totalGhastsKilled;
	}

	/**
	 * Sets the longest recorded SkySiege duration in milliseconds.
	 *
	 * @param longestDurationMillis the duration in milliseconds
	 */
	public void setLongestDurationMillis(long longestDurationMillis) {
		this.longestDurationMillis = longestDurationMillis;
	}

	/**
	 * Sets the shortest recorded SkySiege duration in milliseconds.
	 *
	 * @param shortestDurationMillis the duration in milliseconds
	 */
	public void setShortestDurationMillis(long shortestDurationMillis) {
		this.shortestDurationMillis = shortestDurationMillis;
	}

	/**
	 * Sets the epoch timestamp of the last SkySiege event.
	 *
	 * @param lastSkysiegeTime the timestamp in milliseconds
	 */
	public void setLastSkysiegeTime(long lastSkysiegeTime) {
		this.lastSkysiegeTime = lastSkysiegeTime;
	}

	/**
	 * Calculates the success rate of SkySiege events as a percentage.
	 * <p>
	 * This measures how many of the total SkySieges were completed successfully.
	 *
	 * @return the success rate as a percentage (0.0 if no SkySieges were attempted)
	 */
	public double getSuccessRate() {
		return totalSkysieges == 0 ? 0.0 : (successfulSkysieges * 100.0) / totalSkysieges;
	}

	/**
	 * Calculates the average SkySiege duration in milliseconds.
	 * <p>
	 * This average is based on the shortest and longest recorded durations. Returns
	 * {@code 0} if no successful SkySieges have been recorded.
	 *
	 * @return the average duration in milliseconds
	 */
	public double getAverageDurationMillis() {
		if (successfulSkysieges == 0)
			return 0;
		return (longestDurationMillis + shortestDurationMillis) / 2.0;
	}

	/**
	 * Creates a new {@code SkysiegeData} instance with all values reset to
	 * defaults.
	 *
	 * @return a fresh {@code SkysiegeData} object
	 */
	@NotNull
	public static SkysiegeData empty() {
		return new SkysiegeData();
	}

	/**
	 * Creates a deep copy of this {@code SkysiegeData} instance.
	 * <p>
	 * All primitive values are duplicated to a new object, ensuring no shared
	 * references.
	 *
	 * @return a cloned {@code SkysiegeData} object with identical data
	 */
	@NotNull
	public final SkysiegeData copy() {
		SkysiegeData copy = new SkysiegeData();
		copy.totalSkysieges = this.totalSkysieges;
		copy.successfulSkysieges = this.successfulSkysieges;
		copy.failedSkysieges = this.failedSkysieges;
		copy.queenKills = this.queenKills;
		copy.totalWavesCompleted = this.totalWavesCompleted;
		copy.totalGhastsKilled = this.totalGhastsKilled;
		copy.longestDurationMillis = this.longestDurationMillis;
		copy.shortestDurationMillis = this.shortestDurationMillis;
		copy.lastSkysiegeTime = this.lastSkysiegeTime;
		return copy;
	}

	/**
	 * Returns a string representation of this {@code SkysiegeData} object for
	 * debugging and logging purposes.
	 *
	 * @return a human-readable summary of the current SkySiege statistics
	 */
	@Override
	public String toString() {
		return "SkysiegeData{" + "total=" + totalSkysieges + ", successes=" + successfulSkysieges + ", failures="
				+ failedSkysieges + ", queenKills=" + queenKills + ", totalWaves=" + totalWavesCompleted
				+ ", ghastsKilled=" + totalGhastsKilled + ", longest=" + longestDurationMillis + ", shortest="
				+ shortestDurationMillis + ", lastTime=" + lastSkysiegeTime + '}';
	}

	@Override
	public boolean isEmpty() {
		return totalSkysieges == 0 && successfulSkysieges == 0 && failedSkysieges == 0 && queenKills == 0
				&& totalWavesCompleted == 0 && totalGhastsKilled == 0 && longestDurationMillis == 0L
				&& shortestDurationMillis == 0L && lastSkysiegeTime == 0L;
	}
}