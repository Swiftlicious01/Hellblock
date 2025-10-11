package com.swiftlicious.hellblock.nms.util;

import java.util.concurrent.ThreadLocalRandom;

public class SelfIncreaseEntityID {

	private static int id = ThreadLocalRandom.current().nextInt(Integer.MAX_VALUE / 4, Integer.MAX_VALUE / 2);

	public static int getAndIncrease() {
		final int i = id;
		id++;
		return i;
	}
}