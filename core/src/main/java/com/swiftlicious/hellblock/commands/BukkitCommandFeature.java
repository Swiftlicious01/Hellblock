package com.swiftlicious.hellblock.commands;

import java.util.Collection;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.incendo.cloud.bukkit.data.Selector;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.sender.SenderFactory;
import com.swiftlicious.hellblock.utils.extras.Pair;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;

public abstract class BukkitCommandFeature<C extends CommandSender> extends AbstractCommandFeature<C> {

	public BukkitCommandFeature(HellblockCommandManager<C> commandManager) {
		super(commandManager);
	}

	@Override
	@SuppressWarnings("unchecked")
	protected SenderFactory<?, C> getSenderFactory() {
		return (SenderFactory<?, C>) HellblockPlugin.getInstance().getSenderFactory();
	}

	public Pair<TranslatableComponent.Builder, Component> resolveSelector(Selector<? extends Entity> selector,
			TranslatableComponent.Builder single, TranslatableComponent.Builder multiple) {
		Collection<? extends Entity> entities = selector.values();
		if (entities.size() == 1) {
			return Pair.of(single, Component.text(entities.iterator().next().getName()));
		} else {
			return Pair.of(multiple, Component.text(entities.size()));
		}
	}

	public Pair<TranslatableComponent.Builder, Component> resolveSelector(Collection<? extends Entity> selector,
			TranslatableComponent.Builder single, TranslatableComponent.Builder multiple) {
		if (selector.size() == 1) {
			return Pair.of(single, Component.text(selector.iterator().next().getName()));
		} else {
			return Pair.of(multiple, Component.text(selector.size()));
		}
	}
}