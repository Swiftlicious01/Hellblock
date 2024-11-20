package com.swiftlicious.hellblock.database;

import com.swiftlicious.hellblock.HellblockPlugin;

public class MySQLHandler extends AbstractHikariDatabase {

	public MySQLHandler(HellblockPlugin plugin) {
		super(plugin);
	}

	@Override
	public StorageType getStorageType() {
		return StorageType.MySQL;
	}
}