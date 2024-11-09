package com.swiftlicious.hellblock.player;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class EarningData {

	@Expose
	@SerializedName("earnings")
	public double earnings;
	@Expose
	@SerializedName("date")
	public int date;

	public EarningData(double earnings, int date) {
		this.earnings = earnings;
		this.date = date;
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

	public static EarningData empty() {
		return new EarningData(0.0D, 0);
	}
}
