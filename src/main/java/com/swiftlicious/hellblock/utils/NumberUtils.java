package com.swiftlicious.hellblock.utils;

public class NumberUtils {

	public String money(double money) {
		String str = String.format("%.2f", money);
		return str.replace(",", ".");
	}
}