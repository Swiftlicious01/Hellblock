package com.swiftlicious.hellblock.creation.entity;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.player.Context;
import com.swiftlicious.hellblock.player.ContextKeys;

import static java.util.Objects.requireNonNull;

public class EntityManager implements EntityManagerInterface {

	protected final HellblockPlugin instance;
	private final Map<String, EntityProvider> entityProviders = new HashMap<>();
	private final Map<String, EntityConfig> entities = new HashMap<>();

	public EntityManager(HellblockPlugin plugin) {
		this.instance = plugin;
		this.registerEntityProvider(new EntityProvider() {
			@Override
			public String identifier() {
				return "vanilla";
			}

			@NotNull
			@Override
			public Entity spawn(@NotNull Location location, @NotNull String id,
					@NotNull Map<String, Object> propertyMap) {
				return location.getWorld().spawnEntity(location, EntityType.valueOf(id.toUpperCase(Locale.ENGLISH)));
			}
		});
	}

	@Override
	public void load() {
		for (EntityProvider provider : entityProviders.values()) {
			instance.debug("Registered EntityProvider: " + provider.identifier());
		}
		instance.debug("Loaded " + entities.size() + " entities");
	}

	@Override
	public void unload() {
		this.entities.clear();
	}

	@Override
	public void disable() {
		unload();
		this.entityProviders.clear();
	}

	@Override
	public Optional<EntityConfig> getEntity(String id) {
		return Optional.ofNullable(this.entities.get(id));
	}

	@Override
	public boolean registerEntity(EntityConfig entity) {
		if (entities.containsKey(entity.id()))
			return false;
		this.entities.put(entity.id(), entity);
		return true;
	}

	public boolean registerEntityProvider(EntityProvider entityProvider) {
		if (entityProviders.containsKey(entityProvider.identifier()))
			return false;
		else
			entityProviders.put(entityProvider.identifier(), entityProvider);
		return true;
	}

	public boolean unregisterEntityProvider(String id) {
		return entityProviders.remove(id) != null;
	}

	public EntityProvider getEntityProvider(String id) {
		return entityProviders.get(id);
	}

	@NotNull
	@Override
	public Entity summonEntityLoot(Context<Player> context) {
		String id = context.arg(ContextKeys.ID);
		EntityConfig config = requireNonNull(entities.get(id), "Entity " + id + " not found");
		Location hookLocation = requireNonNull(context.arg(ContextKeys.OTHER_LOCATION));
		Location playerLocation = requireNonNull(context.holder().getLocation());
		String entityID = config.entityID();
		Entity entity;
		if (entityID.contains(":")) {
			String[] split = entityID.split(":", 2);
			EntityProvider provider = requireNonNull(entityProviders.get(split[0]),
					"EntityProvider " + split[0] + " doesn't exist");
			entity = requireNonNull(provider.spawn(hookLocation, split[1], config.propertyMap()),
					"Entity " + entityID + " doesn't exist");
		} else {
			entity = entityProviders.get("vanilla").spawn(hookLocation, entityID, config.propertyMap());
		}
		double d0 = playerLocation.getX() - hookLocation.getX();
		double d1 = playerLocation.getY() - hookLocation.getY();
		double d2 = playerLocation.getZ() - hookLocation.getZ();
		double d3 = config.horizontalVector().evaluate(context);
		double d4 = config.verticalVector().evaluate(context);
		Vector vector = new Vector(d0 * 0.1D * d3,
				d1 * 0.1D + Math.sqrt(Math.sqrt(d0 * d0 + d1 * d1 + d2 * d2)) * 0.08D * d4, d2 * 0.1D * d3);
		entity.setVelocity(vector);
		return entity;
	}
}