package com.swiftlicious.hellblock.sender;

import java.util.UUID;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.utils.extras.Tristate;

import net.kyori.adventure.text.Component;

/**
 * Simple implementation of {@link Sender} using a {@link SenderFactory}
 *
 * @param <T> the command sender type
 */
public final class AbstractSender<T> implements Sender {
	private final HellblockPlugin plugin;
	private final SenderFactory<?, T> factory;
	private final T sender;

	private final UUID uniqueId;
	private final String name;
	private final boolean isConsole;

	AbstractSender(HellblockPlugin plugin, SenderFactory<?, T> factory, T sender) {
		this.plugin = plugin;
		this.factory = factory;
		this.sender = sender;
		this.uniqueId = factory.getUniqueId(this.sender);
		this.name = factory.getName(this.sender);
		this.isConsole = this.factory.isConsole(this.sender);
	}

	@Override
	public HellblockPlugin getPlugin() {
		return this.plugin;
	}

	@Override
	public UUID getUniqueId() {
		return this.uniqueId;
	}

	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public void sendMessage(Component message) {
		this.factory.sendMessage(this.sender, message);
	}

	@Override
	public void sendMessage(Component message, boolean ignoreEmpty) {
		if (ignoreEmpty && message.equals(Component.empty())) {
			return;
		}
		sendMessage(message);
	}

	@Override
	public Tristate getPermissionValue(String permission) {
		return (isConsole() && this.factory.consoleHasAllPermissions()) ? Tristate.TRUE
				: this.factory.getPermissionValue(this.sender, permission);
	}

	@Override
	public boolean hasPermission(String permission) {
		return (isConsole() && this.factory.consoleHasAllPermissions())
				|| this.factory.hasPermission(this.sender, permission);
	}

	@Override
	public void performCommand(String commandLine) {
		this.factory.performCommand(this.sender, commandLine);
	}

	@Override
	public boolean isConsole() {
		return this.isConsole;
	}

	@Override
	public boolean equals(Object o) {
		if (o == this)
			return true;
		if (!(o instanceof AbstractSender<?> that))
			return false;
		return this.getUniqueId().equals(that.getUniqueId());
	}

	@Override
	public int hashCode() {
		return this.uniqueId.hashCode();
	}
}