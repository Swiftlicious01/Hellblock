package com.swiftlicious.hellblock.playerdata;

import com.google.gson.annotations.SerializedName;

public class EarningData {

	@SerializedName("earnings")
	public double earnings;
	@SerializedName("date")
	public int date;

	public EarningData(double earnings, int date) {
		this.earnings = earnings;
		this.date = date;
	}

	public static EarningData empty() {
		return new EarningData(0d, 0);
	}
}
