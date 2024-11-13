package com.swiftlicious.hellblock.database.dependency;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class HellblockProperties {

	private final Map<String, String> propertyMap;

	private HellblockProperties(Map<String, String> propertyMap) {
		this.propertyMap = propertyMap;
	}

	public static String getValue(String key) {
		if (!SingletonHolder.INSTANCE.propertyMap.containsKey(key)) {
			throw new RuntimeException("Unknown key: " + key);
		}
		return SingletonHolder.INSTANCE.propertyMap.get(key);
	}

	private static class SingletonHolder {

		private static final HellblockProperties INSTANCE = getInstance();

		private static HellblockProperties getInstance() {
			try (InputStream inputStream = HellblockProperties.class.getClassLoader()
					.getResourceAsStream("hellblock.properties")) {
				Map<String, String> versionMap = new HashMap<>();
				Properties properties = new Properties();
				properties.load(inputStream);
				for (Map.Entry<Object, Object> entry : properties.entrySet()) {
					if (entry.getKey() instanceof String key && entry.getValue() instanceof String value) {
						versionMap.put(key, value);
					}
				}
				return new HellblockProperties(versionMap);
			} catch (IOException ex) {
				throw new RuntimeException(ex);
			}
		}
	}

}
