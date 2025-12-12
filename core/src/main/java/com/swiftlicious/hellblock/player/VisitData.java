package com.swiftlicious.hellblock.player;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.IsoFields;
import java.util.Objects;

import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.swiftlicious.hellblock.utils.adapters.HellblockTypeAdapterFactory.EmptyCheck;

/**
 * Represents all island visit tracking data, including total, daily, weekly,
 * and monthly visit counts, along with featured status for visit GUI
 * integration and a custom warp location.
 * <p>
 * This class handles automatic resetting of visit counters based on time
 * intervals (daily, weekly, monthly), and includes logic for featured ranking
 * and expiry.
 */
public class VisitData implements EmptyCheck {

	@Expose
	@SerializedName("warpLocation")
	protected Location warp;

	@Expose
	@SerializedName("totalVisits")
	protected int totalVisits;

	@Expose
	@SerializedName("visitsToday")
	protected int visitsToday;

	@Expose
	@SerializedName("visitsThisWeek")
	protected int visitsThisWeek;

	@Expose
	@SerializedName("visitsThisMonth")
	protected int visitsThisMonth;

	@Expose
	@SerializedName("lastVisitReset")
	protected long lastVisitReset;

	@Expose
	@SerializedName("featuredUntil")
	protected long featuredUntil;

	/**
	 * No-args constructor for deserialization.
	 */
	public VisitData() {
		this(null);
	}

	/**
	 * Constructs a {@code VisitData} instance with an optional warp location.
	 *
	 * @param warp the warp location for visits
	 */
	public VisitData(@Nullable Location warp) {
		this.warp = warp;
		this.lastVisitReset = System.currentTimeMillis();
		this.featuredUntil = 0L;
	}

	/**
	 * Called when a player visits this island. Increments visit counters and resets
	 * them if needed.
	 */
	public void increment() {
		resetIfNeeded();
		totalVisits++;
		visitsToday++;
		visitsThisWeek++;
		visitsThisMonth++;
	}

	/**
	 * Resets all visit counters and featured data to defaults.
	 */
	public void reset() {
		warp = null;
		totalVisits = 0;
		visitsToday = 0;
		visitsThisWeek = 0;
		visitsThisMonth = 0;
		lastVisitReset = System.currentTimeMillis();
		featuredUntil = 0L;
	}

	/**
	 * Checks if the island is currently featured.
	 *
	 * @return {@code true} if featuredUntil is in the future, {@code false}
	 *         otherwise
	 */
	public boolean isFeatured() {
		return featuredUntil > System.currentTimeMillis();
	}

	/**
	 * Returns the epoch time (millis) until which the island is featured.
	 *
	 * @return the featured expiration time
	 */
	public long getFeaturedUntil() {
		return featuredUntil;
	}

	/**
	 * Sets the expiration time for the island's featured status.
	 *
	 * @param until the epoch millis until expiration
	 */
	public void setFeaturedUntil(long until) {
		this.featuredUntil = until;
	}

	/**
	 * Removes the island's featured status.
	 */
	public void removeFeatured() {
		this.featuredUntil = 0L;
	}

	/**
	 * Returns the featured ranking score. Lower values indicate higher ranking.
	 * Used for GUI sorting logic.
	 *
	 * @return the ranking score; {@code Integer.MAX_VALUE} if not featured
	 */
	public int getFeaturedRanking() {
		if (!isFeatured()) {
			return Integer.MAX_VALUE;
		}
		return (int) (getFeaturedUntil() - System.currentTimeMillis());
	}

	/**
	 * Resets visit counters if the current time indicates a new day, week, or
	 * month.
	 */
	public void resetIfNeeded() {
		resetIfNeeded(System.currentTimeMillis());
	}

	/**
	 * Resets visit counters based on the provided timestamp, checking for new
	 * day/week/month.
	 *
	 * @param now current time in epoch millis
	 */
	public void resetIfNeeded(long now) {
		LocalDateTime last = Instant.ofEpochMilli(lastVisitReset).atZone(ZoneId.systemDefault()).toLocalDateTime();
		LocalDateTime current = Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()).toLocalDateTime();

		boolean changed = false;

		if (current.toLocalDate().isAfter(last.toLocalDate())) {
			visitsToday = 0;
			changed = true;
		}

		int lastWeek = last.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
		int currentWeek = current.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
		if (currentWeek > lastWeek || current.getYear() > last.getYear()) {
			visitsThisWeek = 0;
			changed = true;
		}

		if (current.getMonthValue() > last.getMonthValue() || current.getYear() > last.getYear()) {
			visitsThisMonth = 0;
			changed = true;
		}

		if (changed) {
			lastVisitReset = now;
		}
	}

	/**
	 * Gets the warp location used for island visit teleportation.
	 *
	 * @return the warp location, or {@code null} if not set
	 */
	@Nullable
	public Location getWarpLocation() {
		return warp;
	}

	/**
	 * Sets the warp location used for visits.
	 *
	 * @param warp the new warp location
	 */
	public void setWarpLocation(@Nullable Location warp) {
		this.warp = warp;
	}

	/**
	 * Gets the total visit count.
	 *
	 * @return the total visits
	 */
	public int getTotalVisits() {
		resetIfNeeded();
		return totalVisits;
	}

	/**
	 * Gets the daily visit count.
	 *
	 * @return today's visits
	 */
	public int getDailyVisits() {
		resetIfNeeded();
		return visitsToday;
	}

	/**
	 * Gets the weekly visit count.
	 *
	 * @return this week's visits
	 */
	public int getWeeklyVisits() {
		resetIfNeeded();
		return visitsThisWeek;
	}

	/**
	 * Gets the monthly visit count.
	 *
	 * @return this month's visits
	 */
	public int getMonthlyVisits() {
		resetIfNeeded();
		return visitsThisMonth;
	}

	/**
	 * Gets the timestamp of the last visit reset.
	 *
	 * @return epoch millis of last reset
	 */
	public long getLastVisitReset() {
		return lastVisitReset;
	}

	/**
	 * Sets the total visit count.
	 *
	 * @param totalVisits the total visits
	 */
	public void setTotalVisits(int totalVisits) {
		this.totalVisits = totalVisits;
	}

	/**
	 * Sets the visit count for today.
	 *
	 * @param visitsToday today's visits
	 */
	public void setVisitsToday(int visitsToday) {
		this.visitsToday = visitsToday;
	}

	/**
	 * Sets the visit count for this week.
	 *
	 * @param visitsThisWeek this week's visits
	 */
	public void setVisitsThisWeek(int visitsThisWeek) {
		this.visitsThisWeek = visitsThisWeek;
	}

	/**
	 * Sets the visit count for this month.
	 *
	 * @param visitsThisMonth this month's visits
	 */
	public void setVisitsThisMonth(int visitsThisMonth) {
		this.visitsThisMonth = visitsThisMonth;
	}

	/**
	 * Sets the last visit reset timestamp.
	 *
	 * @param lastVisitReset epoch millis of last reset
	 */
	public void setLastVisitReset(long lastVisitReset) {
		this.lastVisitReset = lastVisitReset;
	}

	/**
	 * Checks whether the island has any recorded visits.
	 *
	 * @return {@code true} if any visit counter is non-zero
	 */
	public boolean hasVisits() {
		return totalVisits > 0 || visitsToday > 0 || visitsThisWeek > 0 || visitsThisMonth > 0;
	}

	/**
	 * Creates an empty {@code VisitData} instance with no warp and all stats reset.
	 *
	 * @return a new {@code VisitData} instance with default values
	 */
	@NotNull
	public static VisitData empty() {
		return new VisitData(null);
	}

	/**
	 * Creates a deep copy of this {@code VisitData} instance.
	 *
	 * @return a new {@code VisitData} object with duplicated values
	 */
	@NotNull
	public final VisitData copy() {
		VisitData copy = new VisitData(warp != null ? warp.clone() : null);
		copy.totalVisits = this.totalVisits;
		copy.visitsToday = this.visitsToday;
		copy.visitsThisWeek = this.visitsThisWeek;
		copy.visitsThisMonth = this.visitsThisMonth;
		copy.lastVisitReset = this.lastVisitReset;
		copy.featuredUntil = this.featuredUntil;
		return copy;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof VisitData that))
			return false;
		return Objects.equals(warp, that.warp);
	}

	@Override
	public int hashCode() {
		return Objects.hash(warp);
	}

	/**
	 * Returns a string representation of this {@code VisitData} object for
	 * debugging and logging purposes.
	 *
	 * @return a human-readable summary of the current Visit statistics
	 */
	@Override
	public String toString() {
		return "VisitData{" + "warp=" + (warp != null ? warp.toString() : "null") + ", totalVisits=" + totalVisits
				+ ", today=" + visitsToday + ", week=" + visitsThisWeek + ", month=" + visitsThisMonth + ", lastReset="
				+ lastVisitReset + ", featuredUntil=" + featuredUntil + '}';
	}

	@Override
	public boolean isEmpty() {
		return warp == null && totalVisits == 0 && visitsToday == 0 && visitsThisWeek == 0 && visitsThisMonth == 0
				&& featuredUntil == 0L;
	}
}