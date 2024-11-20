package com.swiftlicious.hellblock.utils.extras;

import com.swiftlicious.hellblock.player.Context;

public class PlainTextValue<T> implements TextValue<T> {

	private final String raw;

	public PlainTextValue(String raw) {
		this.raw = raw;
	}

	@Override
	public String render(Context<T> context) {
		return raw;
	}
}