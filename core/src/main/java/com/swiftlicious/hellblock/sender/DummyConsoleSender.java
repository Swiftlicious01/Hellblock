package com.swiftlicious.hellblock.sender;

import java.util.UUID;

import com.swiftlicious.hellblock.HellblockPlugin;

public abstract class DummyConsoleSender implements Sender {
	private final HellblockPlugin platform;

	public DummyConsoleSender(HellblockPlugin plugin) {
		this.platform = plugin;
	}

	@Override
	public void performCommand(String commandLine) {
	}

	@Override
	public boolean isConsole() {
		return true;
	}

	@Override
	public HellblockPlugin getPlugin() {
		return this.platform;
	}

	@Override
	public UUID getUniqueId() {
		return Sender.CONSOLE_UUID;
	}

	@Override
	public String getName() {
		return Sender.CONSOLE_NAME;
	}
}