package com.swiftlicious.hellblock.player;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.IsoFields;
import java.util.Objects;

import org.bukkit.Location;
import org.jetbrains.annotations.Nullable;

/**
 * Represents all island visit tracking data for an island (daily, weekly,
 * monthly counts) and FEATURED slot status for Visit GUI integration.
 */
public class VisitData {

	private Location warp;
	private int totalVisits;
	private int visitsToday;
	private int visitsThisWeek;
	private int visitsThisMonth;
	private long lastVisitReset; // epoch millis for visit stat resets

	private long featuredUntil; // epoch millis, 0 = not featured

	// Needed for deserialization
	public VisitData() {
		this(null);
	}

	public VisitData(@Nullable Location warp) {
		this.warp = warp;
		this.lastVisitReset = System.currentTimeMillis();
		this.featuredUntil = 0L;
	}

	/**
	 * Called whenever a player visits this island. Automatically resets
	 * daily/weekly/monthly counts if needed.
	 */
	public void increment() {
		resetIfNeeded();
		totalVisits++;
		visitsToday++;
		visitsThisWeek++;
		visitsThisMonth++;
	}

	/**
	 * Clears all visit and featured data.
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
	 * Checks if this island's FEATURED status is still active.
	 *
	 * @return true if featuredUntil is in the future, false otherwise.
	 */
	public boolean isFeatured() {
		return featuredUntil > System.currentTimeMillis();
	}

	/**
	 * Gets the expiration timestamp for this island's FEATURED slot.
	 */
	public long getFeaturedUntil() {
		return featuredUntil;
	}

	/**
	 * Sets the FEATURED slot expiration timestamp.
	 *
	 * @param until epoch millis until expiration
	 */
	public void setFeaturedUntil(long until) {
		this.featuredUntil = until;
	}

	/**
	 * Clears the FEATURED slot (e.g., when expired).
	 */
	public void removeFeatured() {
		this.featuredUntil = 0L;
	}

	/**
	 * Returns a featured ranking value. Lower values mean higher priority. Used for
	 * sorting in VisitSorter.FEATURED.
	 */
	public int getFeaturedRanking() {
		if (!isFeatured()) {
			return Integer.MAX_VALUE;
		}

		// Sort by expiration time: soonest to expire comes first
		return (int) (getFeaturedUntil() - System.currentTimeMillis());
	}

	/**
	 * Resets visit counters when a new day, week, or month starts.
	 */
	public void resetIfNeeded() {
		resetIfNeeded(System.currentTimeMillis());
	}

	/**
	 * Resets counters intelligently based on the last recorded reset time.
	 */
	public void resetIfNeeded(long now) {
		LocalDateTime last = Instant.ofEpochMilli(lastVisitReset).atZone(ZoneId.systemDefault()).toLocalDateTime();
		LocalDateTime current = Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()).toLocalDateTime();

		boolean changed = false;

		// New day
		if (current.toLocalDate().isAfter(last.toLocalDate())) {
			visitsToday = 0;
			changed = true;
		}

		// New week
		int lastWeek = last.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
		int currentWeek = current.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
		if (currentWeek > lastWeek || current.getYear() > last.getYear()) {
			visitsThisWeek = 0;
			changed = true;
		}

		// New month
		if (current.getMonthValue() > last.getMonthValue() || current.getYear() > last.getYear()) {
			visitsThisMonth = 0;
			changed = true;
		}

		if (changed) {
			lastVisitReset = now;
		}
	}

	public @Nullable Location getWarpLocation() {
		return warp;
	}

	public void setWarpLocation(@Nullable Location warp) {
		this.warp = warp;
	}

	public int getTotalVisits() {
		resetIfNeeded();
		return totalVisits;
	}

	public int getDailyVisits() {
		resetIfNeeded();
		return visitsToday;
	}

	public int getWeeklyVisits() {
		resetIfNeeded();
		return visitsThisWeek;
	}

	public int getMonthlyVisits() {
		resetIfNeeded();
		return visitsThisMonth;
	}

	public long getLastVisitReset() {
		return lastVisitReset;
	}

	public void setTotalVisits(int totalVisits) {
		this.totalVisits = totalVisits;
	}

	public void setVisitsToday(int visitsToday) {
		this.visitsToday = visitsToday;
	}

	public void setVisitsThisWeek(int visitsThisWeek) {
		this.visitsThisWeek = visitsThisWeek;
	}

	public void setVisitsThisMonth(int visitsThisMonth) {
		this.visitsThisMonth = visitsThisMonth;
	}

	public void setLastVisitReset(long lastVisitReset) {
		this.lastVisitReset = lastVisitReset;
	}

	/**
	 * Returns true if this island currently has any visit activity.
	 */
	public boolean hasVisits() {
		return totalVisits > 0 || visitsToday > 0 || visitsThisWeek > 0 || visitsThisMonth > 0;
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

	@Override
	public String toString() {
		return "VisitData{" + "warp=" + (warp != null ? warp.toString() : "null") + ", totalVisits=" + totalVisits
				+ ", today=" + visitsToday + ", week=" + visitsThisWeek + ", month=" + visitsThisMonth + ", lastReset="
				+ lastVisitReset + ", featuredUntil=" + featuredUntil + '}';
	}
}