package com.swiftlicious.hellblock.listeners.fishing;

import java.util.ArrayList;
import java.util.List;

public class HookSetting {

	private final String key;
	private int maxDurability;
	private List<String> lore;

	public HookSetting(String key) {
		this.key = key;
	}

	public String getKey() {
		return key;
	}

	public int getMaxDurability() {
		return maxDurability;
	}

	public List<String> getLore() {
		return lore == null ? new ArrayList<>() : lore;
	}

	public static class Builder {

		private final HookSetting setting;

		public Builder(String key) {
			this.setting = new HookSetting(key);
		}

		public Builder durability(int maxDurability) {
			setting.maxDurability = maxDurability;
			return this;
		}

		public Builder lore(List<String> lore) {
			setting.lore = lore;
			return this;
		}

		public HookSetting build() {
			return setting;
		}
	}
}