package com.swiftlicious.hellblock.player.mailbox;

import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import net.kyori.adventure.text.Component;

/**
 * Represents a single mailbox entry for a player or island.
 * <p>
 * A mailbox entry consists of a translation key (used for displaying messages),
 * a list of {@link net.kyori.adventure.text.Component} arguments used for
 * dynamic message rendering, and a set of {@link MailboxFlag}s that determine
 * metadata like read status or delivery properties.
 * <p>
 * This class is immutable and safely serializable using Gson.
 */
public class MailboxEntry {

	@Expose
	@SerializedName("messageKey")
	private final String messageKey;

	@Expose
	@SerializedName("arguments")
	private final List<Component> arguments;

	@Expose
	@SerializedName("flags")
	private final Set<MailboxFlag> flags;

	/**
	 * Constructs a new {@link MailboxEntry}.
	 *
	 * @param messageKey the translation key for this message (may be {@code null})
	 * @param arguments  a list of Adventure {@link Component} arguments (can be
	 *                   empty or {@code null})
	 * @param flags      a set of {@link MailboxFlag}s describing the entry's state
	 */
	public MailboxEntry(@Nullable String messageKey, @Nullable List<Component> arguments,
			@NotNull Set<MailboxFlag> flags) {
		this.messageKey = messageKey;
		this.arguments = arguments != null && !arguments.isEmpty() ? List.copyOf(arguments) : List.of();
		this.flags = Set.copyOf(flags);
	}

	/**
	 * Returns the translation key used for this mailbox entry, or {@code null} if
	 * none was set.
	 *
	 * @return the message translation key, or {@code null}
	 */
	@Nullable
	public String getMessageKey() {
		return messageKey;
	}

	/**
	 * Returns the immutable list of {@link Component} arguments used for rendering
	 * the message.
	 *
	 * @return the list of message arguments, never {@code null}
	 */
	@NotNull
	public List<Component> getArguments() {
		return arguments;
	}

	/**
	 * Returns the immutable set of flags that define the behavior or metadata of
	 * this mailbox entry.
	 *
	 * @return a set of {@link MailboxFlag}s
	 */
	@NotNull
	public Set<MailboxFlag> getFlags() {
		return flags;
	}
}