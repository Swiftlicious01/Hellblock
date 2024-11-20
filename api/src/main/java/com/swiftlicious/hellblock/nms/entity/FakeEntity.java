package com.swiftlicious.hellblock.nms.entity;

import org.bukkit.entity.Player;

public interface FakeEntity {

	void destroy(Player player);

	void spawn(Player player);

	void updateMetaData(Player player);

	int entityID();
}