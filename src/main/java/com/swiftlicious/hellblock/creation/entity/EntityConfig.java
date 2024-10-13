package com.swiftlicious.hellblock.creation.entity;

import java.util.Map;

public class EntityConfig implements EntitySettings {

	private String entity;
	private double horizontalVector;
	private double verticalVector;
	private Map<String, Object> propertyMap;
	private boolean persist;

	@Override
	public boolean isPersist() {
		return persist;
	}

	@Override
	public double getHorizontalVector() {
		return horizontalVector;
	}

	@Override
	public double getVerticalVector() {
		return verticalVector;
	}

	@Override
	public String getEntityID() {
		return entity;
	}

	@Override
	public Map<String, Object> getPropertyMap() {
		return propertyMap;
	}

	public static class Builder {

		private final EntityConfig config;

		public Builder() {
			this.config = new EntityConfig();
		}

		public Builder entityID(String value) {
			this.config.entity = value;
			return this;
		}

		public Builder persist(boolean value) {
			this.config.persist = value;
			return this;
		}

		public Builder verticalVector(double value) {
			this.config.verticalVector = value;
			return this;
		}

		public Builder horizontalVector(double value) {
			this.config.horizontalVector = value;
			return this;
		}

		public Builder propertyMap(Map<String, Object> value) {
			this.config.propertyMap = value;
			return this;
		}

		public EntityConfig build() {
			return config;
		}
	}
}