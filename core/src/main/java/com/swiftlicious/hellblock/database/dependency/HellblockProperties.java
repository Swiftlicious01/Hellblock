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
				final Map<String, String> versionMap = new HashMap<>();
				final Properties properties = new Properties();
				properties.load(inputStream);
				properties.entrySet().stream()
						.filter(entry -> entry.getKey() instanceof String && entry.getValue() instanceof String)
						.forEach(entry -> {
							final String key = (String) entry.getKey();
							final String value = (String) entry.getValue();
							versionMap.put(key, value);
						});
				return new HellblockProperties(versionMap);
			} catch (IOException ex) {
				throw new RuntimeException(ex);
			}
		}
	}
}