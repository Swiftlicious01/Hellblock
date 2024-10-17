package com.swiftlicious.hellblock.creation.entity;

import java.util.Map;

public interface EntitySettings {
	boolean isPersist();

	double getHorizontalVector();

	double getVerticalVector();

	String getEntityID();

	Map<String, Object> getPropertyMap();
}
