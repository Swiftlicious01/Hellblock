package com.swiftlicious.hellblock.database;

import com.swiftlicious.hellblock.HellblockPlugin;

public class PostgreSQLHandler extends AbstractHikariDatabase {

	public PostgreSQLHandler(HellblockPlugin plugin) {
		super(plugin, StorageType.PostgreSQL);
	}
}