package com.swiftlicious.hellblock.listeners.invasion;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.entity.Mob;

public class InvaderBehaviorQueue {

	private final Map<UUID, Deque<BehaviorCommand>> behaviorMap = new HashMap<>();

	public void enqueue(Mob mob, BehaviorCommand command) {
		behaviorMap.computeIfAbsent(mob.getUniqueId(), id -> new ArrayDeque<>()).addLast(command);
	}

	public void enqueueFront(Mob mob, BehaviorCommand command) {
		behaviorMap.computeIfAbsent(mob.getUniqueId(), id -> new ArrayDeque<>()).addFirst(command);
	}

	public BehaviorCommand poll(Mob mob) {
		Deque<BehaviorCommand> queue = behaviorMap.get(mob.getUniqueId());
		return (queue != null && !queue.isEmpty()) ? queue.poll() : null;
	}

	public boolean hasQueued(Mob mob) {
		Deque<BehaviorCommand> queue = behaviorMap.get(mob.getUniqueId());
		return queue != null && !queue.isEmpty();
	}

	public void clear(Mob mob) {
		Deque<BehaviorCommand> queue = behaviorMap.get(mob.getUniqueId());
		if (queue != null)
			queue.clear();
	}
	
	public void clearAll() {
		behaviorMap.values().forEach(Deque::clear);
		behaviorMap.clear();
	}

	public enum BehaviorCommand {
		REGROUP, RETREAT, CHARGE, CIRCLE_PLAYER, HOLD_POSITION, FOLLOW_LEADER;
	}
}