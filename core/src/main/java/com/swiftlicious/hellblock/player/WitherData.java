package com.swiftlicious.hellblock.player;

import org.jetbrains.annotations.NotNull;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.swiftlicious.hellblock.utils.adapters.HellblockTypeAdapterFactory.EmptyCheck;

/**
 * Stores all Wither fight-related data for a specific island.
 * <p>
 * This includes statistics such as:
 * <ul>
 * <li>Total spawns</li>
 * <li>Kills and despawns</li>
 * <li>Fight duration tracking (longest, shortest)</li>
 * <li>Total heals and minion wave summons</li>
 * <li>Last recorded Wither spawn timestamp</li>
 * </ul>
 *
 * This data is saved and persisted per island across sessions.
 */
public class WitherData implements EmptyCheck {

	@Expose
	@SerializedName("totalSpawns")
	private int totalSpawns;

	@Expose
	@SerializedName("kills")
	private int kills;

	@Expose
	@SerializedName("despawns")
	private int despawns;

	@Expose
	@SerializedName("longestFightMillis")
	private long longestFightMillis;

	@Expose
	@SerializedName("shortestFightMillis")
	private long shortestFightMillis;

	@Expose
	@SerializedName("totalHeals")
	private int totalHeals;

	@Expose
	@SerializedName("totalMinionWaves")
	private int totalMinionWaves;

	@Expose
	@SerializedName("lastSpawnTime")
	private long lastSpawnTime;

	/**
	 * Constructs a new {@code WitherData} instance with all values set to their
	 * default state. All counters are initialized to zero, and all timestamps are
	 * reset.
	 */
	public WitherData() {
		this.totalSpawns = 0;
		this.kills = 0;
		this.despawns = 0;
		this.longestFightMillis = 0L;
		this.shortestFightMillis = 0L;
		this.totalHeals = 0;
		this.totalMinionWaves = 0;
		this.lastSpawnTime = 0L;
	}

	/**
	 * Records a new Wither spawn event on the island. Increments the total spawn
	 * count.
	 */
	public void recordSpawn() {
		totalSpawns++;
	}

	/**
	 * Records a successful Wither kill. Updates fight duration statistics for
	 * shortest and longest fight times.
	 *
	 * @param fightMillis the duration of the fight in milliseconds
	 */
	public void recordKill(long fightMillis) {
		kills++;
		if (fightMillis > longestFightMillis) {
			longestFightMillis = fightMillis;
		}
		if (shortestFightMillis == 0 || fightMillis < shortestFightMillis) {
			shortestFightMillis = fightMillis;
		}
	}

	/**
	 * Records a Wither despawn event, such as when no players are nearby or a
	 * timeout occurs.
	 */
	public void recordDespawn() {
		despawns++;
	}

	/**
	 * Records a Wither self-heal action. Increments the total heal counter.
	 */
	public void recordHeal() {
		totalHeals++;
	}

	/**
	 * Records a Wither summoning a wave of minions. Increments the total minion
	 * wave counter.
	 */
	public void recordMinionWave() {
		totalMinionWaves++;
	}

	/**
	 * Returns the total number of Wither spawns recorded.
	 *
	 * @return the total spawn count
	 */
	public int getTotalSpawns() {
		return totalSpawns;
	}

	/**
	 * Returns the total number of Wither kills recorded.
	 *
	 * @return the total kill count
	 */
	public int getKills() {
		return kills;
	}

	/**
	 * Returns the total number of Wither despawns recorded.
	 *
	 * @return the total despawn count
	 */
	public int getDespawns() {
		return despawns;
	}

	/**
	 * Returns the longest recorded Wither fight duration, in milliseconds.
	 *
	 * @return the longest fight duration
	 */
	public long getLongestFightMillis() {
		return longestFightMillis;
	}

	/**
	 * Returns the shortest recorded Wither fight duration, in milliseconds.
	 *
	 * @return the shortest fight duration
	 */
	public long getShortestFightMillis() {
		return shortestFightMillis;
	}

	/**
	 * Returns the total number of Wither self-heal actions recorded.
	 *
	 * @return the total heal count
	 */
	public int getTotalHeals() {
		return totalHeals;
	}

	/**
	 * Returns the total number of minion waves summoned by Withers.
	 *
	 * @return the total minion wave count
	 */
	public int getTotalMinionWaves() {
		return totalMinionWaves;
	}

	/**
	 * Returns the last recorded Wither spawn timestamp, in epoch milliseconds.
	 *
	 * @return the last spawn timestamp
	 */
	public long getLastSpawnTime() {
		return lastSpawnTime;
	}

	/**
	 * Sets the total number of Wither spawns.
	 *
	 * @param totalSpawns the new total spawn count
	 */
	public void setTotalSpawns(int totalSpawns) {
		this.totalSpawns = totalSpawns;
	}

	/**
	 * Sets the total number of Wither kills.
	 *
	 * @param kills the new total kill count
	 */
	public void setKills(int kills) {
		this.kills = kills;
	}

	/**
	 * Sets the total number of Wither despawns.
	 *
	 * @param despawns the new total despawn count
	 */
	public void setDespawns(int despawns) {
		this.despawns = despawns;
	}

	/**
	 * Sets the longest recorded Wither fight duration.
	 *
	 * @param longestFightMillis the duration in milliseconds
	 */
	public void setLongestFightMillis(long longestFightMillis) {
		this.longestFightMillis = longestFightMillis;
	}

	/**
	 * Sets the shortest recorded Wither fight duration.
	 *
	 * @param shortestFightMillis the duration in milliseconds
	 */
	public void setShortestFightMillis(long shortestFightMillis) {
		this.shortestFightMillis = shortestFightMillis;
	}

	/**
	 * Sets the total number of Wither self-heal actions.
	 *
	 * @param totalHeals the total number of heals
	 */
	public void setTotalHeals(int totalHeals) {
		this.totalHeals = totalHeals;
	}

	/**
	 * Sets the total number of minion waves summoned by Withers.
	 *
	 * @param totalMinionWaves the total number of minion waves
	 */
	public void setTotalMinionWaves(int totalMinionWaves) {
		this.totalMinionWaves = totalMinionWaves;
	}

	/**
	 * Sets the last Wither spawn timestamp.
	 *
	 * @param lastSpawnTime the epoch milliseconds of the last spawn
	 */
	public void setLastSpawnTime(long lastSpawnTime) {
		this.lastSpawnTime = lastSpawnTime;
	}

	/**
	 * Calculates the Wither kill rate as a percentage of total spawns.
	 *
	 * @return the kill rate percentage (0.0 if no spawns have occurred)
	 */
	public double getKillRate() {
		return totalSpawns == 0 ? 0.0 : (kills * 100.0) / totalSpawns;
	}

	/**
	 * Calculates the average fight duration in milliseconds. Returns 0 if there are
	 * no recorded kills.
	 *
	 * @return the average fight duration in milliseconds
	 */
	public double getAverageFightMillis() {
		if (kills == 0)
			return 0;
		return (longestFightMillis + shortestFightMillis) / 2.0;
	}

	/**
	 * Creates an empty {@code WitherData} instance with all counters and values
	 * reset to defaults.
	 *
	 * @return a new, empty {@code WitherData} instance
	 */
	@NotNull
	public static WitherData empty() {
		return new WitherData();
	}

	/**
	 * Creates a deep copy of this {@code WitherData} instance. All primitive fields
	 * are duplicated to ensure a separate copy of the data.
	 *
	 * @return a new {@code WitherData} instance with identical values
	 */
	@NotNull
	public final WitherData copy() {
		WitherData copy = new WitherData();
		copy.totalSpawns = this.totalSpawns;
		copy.kills = this.kills;
		copy.despawns = this.despawns;
		copy.longestFightMillis = this.longestFightMillis;
		copy.shortestFightMillis = this.shortestFightMillis;
		copy.totalHeals = this.totalHeals;
		copy.totalMinionWaves = this.totalMinionWaves;
		copy.lastSpawnTime = this.lastSpawnTime;
		return copy;
	}

	/**
	 * Returns a formatted string representation of this {@code WitherData}
	 * instance, useful for debugging and logging.
	 *
	 * @return a string describing the current Wither statistics
	 */
	@Override
	public String toString() {
		return "WitherData{" + "spawns=" + totalSpawns + ", kills=" + kills + ", despawns=" + despawns + ", heals="
				+ totalHeals + ", minionWaves=" + totalMinionWaves + ", best=" + shortestFightMillis + ", worst="
				+ longestFightMillis + ", lastSpawnTime=" + lastSpawnTime + '}';
	}

	@Override
	public boolean isEmpty() {
		return totalSpawns == 0 && kills == 0 && despawns == 0 && longestFightMillis == 0 && shortestFightMillis == 0
				&& totalHeals == 0 && totalMinionWaves == 0 && lastSpawnTime == 0;
	}
}