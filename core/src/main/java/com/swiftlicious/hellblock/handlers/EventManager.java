package com.swiftlicious.hellblock.handlers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.context.Context;
import com.swiftlicious.hellblock.context.ContextKeys;
import com.swiftlicious.hellblock.mechanics.MechanicType;
import com.swiftlicious.hellblock.utils.extras.ActionTrigger;

public class EventManager implements EventManagerInterface, Listener {

	private final Map<String, EventCarrier> carriers = new HashMap<>();
	protected final HellblockPlugin instance;

	public EventManager(HellblockPlugin plugin) {
		this.instance = plugin;
	}

	@Override
	public void unload() {
		this.carriers.clear();
		HandlerList.unregisterAll(this);
	}

	@Override
	public void load() {
		Bukkit.getPluginManager().registerEvents(this, this.instance);
	}

	@Override
	public Optional<EventCarrier> getEventCarrier(String id, MechanicType type) {
		return Optional.ofNullable(this.carriers.get(type.getType() + ":" + id));
	}

	@Override
	public boolean registerEventCarrier(EventCarrier carrier) {
		if (this.carriers.containsKey(carrier.id()))
			return false;
		this.carriers.put(carrier.type().getType() + ":" + carrier.id(), carrier);
		return true;
	}

	@EventHandler
	public void onInteract(PlayerInteractEvent event) {
		if (event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_AIR
				&& event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK)
			return;
		EquipmentSlot slot = event.getHand();
		if (slot == null) {
			return;
		}
		ItemStack itemStack = event.getPlayer().getInventory().getItem(slot);
		if (itemStack.getType() == Material.AIR || itemStack.getAmount() == 0)
			return;
		String id = instance.getItemManager().getItemID(itemStack);
		Context<Player> context = Context.player(event.getPlayer());
		Block clicked = event.getClickedBlock();
		context.arg(ContextKeys.OTHER_LOCATION,
				clicked == null ? event.getPlayer().getLocation() : clicked.getLocation());
		context.arg(ContextKeys.SLOT, event.getHand());
		List<MechanicType> mechanics = MechanicType.getTypeByID(id);
		if (mechanics != null) {
			for (MechanicType type : mechanics) {
				if (type == MechanicType.ROD)
					continue;
				trigger(context, id, type, ActionTrigger.INTERACT);
			}
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onConsumeItem(PlayerItemConsumeEvent event) {
		Context<Player> context = Context.player(event.getPlayer());
		trigger(context, instance.getItemManager().getItemID(event.getItem()), MechanicType.LOOT,
				ActionTrigger.CONSUME);
	}
}