package com.swiftlicious.hellblock.database;

import com.swiftlicious.hellblock.HellblockPlugin;

public class MariaDBHandler extends AbstractHikariDatabase {

	public MariaDBHandler(HellblockPlugin plugin) {
		super(plugin);
	}

	@Override
	public StorageType getStorageType() {
		return StorageType.MariaDB;
	}
}