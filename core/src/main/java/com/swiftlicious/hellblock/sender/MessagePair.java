package com.swiftlicious.hellblock.sender;

import net.kyori.adventure.text.Component;

public class MessagePair {
	public final Component ownerMsg;
	public final Component memberMsg;

	public MessagePair(Component ownerMsg, Component memberMsg) {
		this.ownerMsg = ownerMsg;
		this.memberMsg = memberMsg;
	}
}
