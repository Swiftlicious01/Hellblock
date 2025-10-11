package com.swiftlicious.hellblock.player;

import java.time.LocalDate;

import org.jetbrains.annotations.NotNull;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public final class EarningData {

	@Expose
	@SerializedName("earnings")
	protected double earnings;
	@Expose
	@SerializedName("date")
	protected LocalDate date;

	public EarningData(double earnings, LocalDate date) {
		this.earnings = earnings;
		this.date = date;
		refresh();
	}

	public double getEarnings() {
		return this.earnings;
	}

	public LocalDate getDate() {
		return this.date;
	}

	public void setEarnings(double earnings) {
		this.earnings = earnings;
	}

	public void setDate(LocalDate date) {
		this.date = date;
	}

	/**
	 * Creates an instance of EarningData with default values (zero earnings and
	 * date).
	 *
	 * @return a new instance of EarningData with default values.
	 */
	public static @NotNull EarningData empty() {
		return new EarningData(0.0D, LocalDate.now());
	}

	public @NotNull EarningData copy() {
		return new EarningData(earnings, date);
	}

	/**
	 * Resets earnings if the stored date does not match today's date.
	 */
	public void refresh() {
		LocalDate today = LocalDate.now();
		if (!today.equals(date)) {
			this.date = today;
			this.earnings = 0.0D;
		}
	}
}