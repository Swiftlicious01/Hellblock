package com.swiftlicious.hellblock.creation.entity;

import java.util.Map;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import com.swiftlicious.hellblock.utils.extras.MathValue;

public class EntityConfig implements EntityConfigInterface {

	private final String id;
	private final String entityID;
	private final MathValue<Player> horizontalVector;
	private final MathValue<Player> verticalVector;
	private final Map<String, Object> propertyMap;

	public EntityConfig(String id, String entityID, MathValue<Player> horizontalVector,
			MathValue<Player> verticalVector, Map<String, Object> propertyMap) {
		this.id = id;
		this.entityID = entityID;
		this.horizontalVector = horizontalVector;
		this.verticalVector = verticalVector;
		this.propertyMap = propertyMap;
	}

	@Override
	public String id() {
		return id;
	}

	@Override
	public MathValue<Player> horizontalVector() {
		return horizontalVector;
	}

	@Override
	public MathValue<Player> verticalVector() {
		return verticalVector;
	}

	@NotNull
	@Override
	public String entityID() {
		return entityID;
	}

	@NotNull
	@Override
	public Map<String, Object> propertyMap() {
		return propertyMap;
	}

	public static class Builder implements BuilderInterface {
		private String entity = DEFAULT_ENTITY_ID;
		private MathValue<Player> horizontalVector = DEFAULT_HORIZONTAL_VECTOR;
		private MathValue<Player> verticalVector = DEFAULT_VERTICAL_VECTOR;
		private Map<String, Object> propertyMap = DEFAULT_PROPERTY_MAP;
		private String id;

		@Override
		public Builder id(String id) {
			this.id = id;
			return this;
		}

		@Override
		public Builder entityID(String value) {
			this.entity = value;
			return this;
		}

		@Override
		public Builder verticalVector(MathValue<Player> value) {
			this.verticalVector = value;
			return this;
		}

		@Override
		public Builder horizontalVector(MathValue<Player> value) {
			this.horizontalVector = value;
			return this;
		}

		@Override
		public Builder propertyMap(Map<String, Object> value) {
			this.propertyMap = value;
			return this;
		}

		@Override
		public EntityConfig build() {
			return new EntityConfig(id, entity, horizontalVector, verticalVector, propertyMap);
		}
	}
}