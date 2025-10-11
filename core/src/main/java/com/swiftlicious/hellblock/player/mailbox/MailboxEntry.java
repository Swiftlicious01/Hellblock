package com.swiftlicious.hellblock.player.mailbox;

import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.google.gson.annotations.Expose;

import net.kyori.adventure.text.Component;

public class MailboxEntry {

	@Expose
	private final String messageKey;
	@Expose
	private final List<Component> arguments;
	@Expose
	private final Set<MailboxFlag> flags;

	public MailboxEntry(@Nullable String messageKey, @Nullable List<Component> arguments,
			@NotNull Set<MailboxFlag> flags) {
		this.messageKey = messageKey;
		this.arguments = arguments != null ? List.copyOf(arguments) : List.of();
		this.flags = Set.copyOf(flags);
	}

	public @Nullable String getMessageKey() {
		return messageKey;
	}

	public @NotNull List<Component> getArguments() {
		return arguments;
	}

	public @NotNull Set<MailboxFlag> getFlags() {
		return flags;
	}
}
