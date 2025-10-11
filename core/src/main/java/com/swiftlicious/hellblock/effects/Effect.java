package com.swiftlicious.hellblock.effects;

import com.swiftlicious.hellblock.loot.operation.WeightOperation;
import com.swiftlicious.hellblock.utils.extras.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Effect implements EffectInterface {

	private final Map<EffectProperties<?>, Object> properties = new HashMap<>();
	private double multipleLootChance = 0;
	private double sizeAdder = 0;
	private double sizeMultiplier = 1;
	private double waitTimeAdder = 0;
	private double waitTimeMultiplier = 1;
	private final List<Pair<String, WeightOperation>> weightOperations = new ArrayList<>();
	private final List<Pair<String, WeightOperation>> weightOperationsIgnored = new ArrayList<>();

	@Override
	public Map<EffectProperties<?>, Object> properties() {
		return properties;
	}

	@Override
	public Effect properties(Map<EffectProperties<?>, Object> properties) {
		this.properties.putAll(properties);
		return this;
	}

	@Override
	public <C> Effect arg(EffectProperties<C> key, C value) {
		properties.put(key, value);
		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <C> C arg(EffectProperties<C> key) {
		return (C) properties.get(key);
	}

	@Override
	public double multipleLootChance() {
		return multipleLootChance;
	}

	@Override
	public Effect multipleLootChance(double multipleLootChance) {
		this.multipleLootChance = multipleLootChance;
		return this;
	}

	@Override
	public double sizeAdder() {
		return sizeAdder;
	}

	@Override
	public Effect sizeAdder(double sizeAdder) {
		this.sizeAdder = sizeAdder;
		return this;
	}

	@Override
	public double sizeMultiplier() {
		return sizeMultiplier;
	}

	@Override
	public Effect sizeMultiplier(double sizeMultiplier) {
		this.sizeMultiplier = sizeMultiplier;
		return this;
	}

	@Override
	public double waitTimeAdder() {
		return waitTimeAdder;
	}

	@Override
	public Effect waitTimeAdder(double waitTimeAdder) {
		this.waitTimeAdder = waitTimeAdder;
		return this;
	}

	@Override
	public double waitTimeMultiplier() {
		return waitTimeMultiplier;
	}

	@Override
	public Effect waitTimeMultiplier(double waitTimeMultiplier) {
		this.waitTimeMultiplier = waitTimeMultiplier;
		return this;
	}

	@Override
	public List<Pair<String, WeightOperation>> weightOperations() {
		return weightOperations;
	}

	@Override
    public Effect weightOperations(List<Pair<String, WeightOperation>> weightOperations) {
		this.weightOperations.addAll(weightOperations);
		return this;
	}

	@Override
	public List<Pair<String, WeightOperation>> weightOperationsIgnored() {
		return weightOperationsIgnored;
	}

	@Override
	public Effect weightOperationsIgnored(
			List<Pair<String, WeightOperation>> weightOperations) {
		this.weightOperationsIgnored.addAll(weightOperations);
		return this;
	}

	@Override
	public void combine(Effect another) {
		if (another == null) {
			return;
		}
		this.sizeMultiplier += (another.sizeMultiplier() - 1);
		this.sizeAdder += another.sizeAdder();
		this.waitTimeMultiplier += (another.waitTimeMultiplier() - 1);
		this.waitTimeAdder += (another.waitTimeAdder());
		this.multipleLootChance += another.multipleLootChance();
		this.weightOperations.addAll(another.weightOperations());
		this.weightOperationsIgnored.addAll(another.weightOperationsIgnored());
		this.properties.putAll(another.properties());
	}

	@Override
	public Effect copy() {
		return EffectInterface.newInstance().sizeMultiplier(this.sizeMultiplier).sizeAdder(this.sizeAdder)
				.waitTimeMultiplier(this.waitTimeMultiplier).waitTimeAdder(this.waitTimeAdder)
				.multipleLootChance(this.multipleLootChance).weightOperations(this.weightOperations)
				.weightOperationsIgnored(this.weightOperationsIgnored).properties(this.properties);

	}

	@Override
	public String toString() {
		return "Effect{" + "properties=" + properties + ", multipleLootChance=" + multipleLootChance + ", sizeAdder="
				+ sizeAdder + ", sizeMultiplier=" + sizeMultiplier + ", waitTimeAdder=" + waitTimeAdder
				+ ", waitTimeMultiplier=" + waitTimeMultiplier + ", weightOperations=" + weightOperations.size()
				+ ", weightOperationsIgnored=" + weightOperationsIgnored.size() + '}';
	}
}