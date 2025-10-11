package com.swiftlicious.hellblock.nms.util;

public class ColorUtils {

	public static int rgbaToDecimal(String rgba) {
		final String[] split = rgba.split(",");
		final int r = Integer.parseInt(split[0]);
		final int g = Integer.parseInt(split[1]);
		final int b = Integer.parseInt(split[2]);
		final int a = Integer.parseInt(split[3]);
		return (a << 24) | (r << 16) | (g << 8) | b;
	}

	public static int rgbaToDecimal(int r, int g, int b, int a) {
		return (a << 24) | (r << 16) | (g << 8) | b;
	}
}