package com.swiftlicious.hellblock.mechanics.hook;

import java.util.List;

public class HookConfig implements HookConfigInterface {

	private final String id;
	private final int maxUsages;
	private final List<String> lore;

	public HookConfig(String id, int maxUsages, List<String> lore) {
		this.id = id;
		this.maxUsages = maxUsages;
		this.lore = lore;
	}

	@Override
	public String id() {
		return id;
	}

	@Override
	public List<String> lore() {
		return lore;
	}

	@Override
	public int maxUsages() {
		return maxUsages;
	}

	public static class Builder implements BuilderInterface {
		private String id;
		private int maxUsages;
		private List<String> lore;

		@Override
		public Builder id(String id) {
			this.id = id;
			return this;
		}

		@Override
		public Builder maxUsages(int maxUsages) {
			this.maxUsages = maxUsages;
			return this;
		}

		@Override
		public Builder lore(List<String> lore) {
			this.lore = lore;
			return this;
		}

		@Override
		public HookConfig build() {
			return new HookConfig(id, maxUsages, lore);
		}
	}
}