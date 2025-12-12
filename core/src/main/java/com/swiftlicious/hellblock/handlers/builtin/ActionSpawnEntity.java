package com.swiftlicious.hellblock.handlers.builtin;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Location;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.context.Context;
import com.swiftlicious.hellblock.context.ContextKeys;
import com.swiftlicious.hellblock.creation.entity.EntityProvider;
import com.swiftlicious.hellblock.utils.extras.MathValue;

import dev.dejvokep.boostedyaml.block.implementation.Section;

import static java.util.Objects.requireNonNull;

public class ActionSpawnEntity<T> extends AbstractBuiltInAction<T> {

	private final String id;
	private final Map<String, Object> properties;

	public ActionSpawnEntity(HellblockPlugin plugin, Section section, MathValue<T> chance) {
		super(plugin, chance);
		this.id = section.getString("id");
		final Section proeprtySection = section.getSection("properties");
		this.properties = proeprtySection == null ? new HashMap<>() : proeprtySection.getStringRouteMappedValues(false);
	}

	@Override
	protected void triggerAction(Context<T> context) {
		final Location location = requireNonNull(context.arg(ContextKeys.LOCATION));
		final String finalID;
		final EntityProvider provider;
		if (id.contains(":")) {
			final String[] split = id.split(":", 2);
			final String providerID = split[0];
			finalID = split[1];
			provider = plugin.getIntegrationManager().getEntityProvider(providerID);
		} else {
			finalID = id;
			provider = plugin.getIntegrationManager().getEntityProvider("vanilla");
		}
		if (provider == null) {
			plugin.getPluginLogger().warn("Failed to spawn entity: " + id);
			return;
		}
		provider.spawn(location, finalID, properties());
	}

	public String id() {
		return id;
	}

	public Map<String, Object> properties() {
		return properties;
	}
}