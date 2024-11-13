package com.swiftlicious.hellblock.player;

import java.util.Calendar;

import org.checkerframework.checker.nullness.qual.NonNull;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class EarningData {

	@Expose
	@SerializedName("earnings")
	protected double earnings;
	@Expose
	@SerializedName("date")
	protected int date;

	public EarningData(double earnings, int date) {
		this.earnings = earnings;
		this.date = date;
		this.refresh();
	}

	public double getEarnings() {
		return this.earnings;
	}

	public int getDate() {
		return this.date;
	}

	public void setEarnings(double earnings) {
		this.earnings = earnings;
	}

	public void setDate(int date) {
		this.date = date;
	}

	/**
	 * Creates an instance of EarningData with default values (zero earnings and
	 * date).
	 *
	 * @return a new instance of EarningData with default values.
	 */
	public static @NonNull EarningData empty() {
		return new EarningData(0.0D, 0);
	}

	public @NonNull EarningData copy() {
		return new EarningData(earnings, date);
	}

	public void refresh() {
		Calendar calendar = Calendar.getInstance();
		int dat = (calendar.get(Calendar.MONTH) + 1) * 100 + calendar.get(Calendar.DATE);
		if (dat != date) {
			date = dat;
			earnings = 0;
		}
	}
}