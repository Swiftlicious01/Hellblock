package com.swiftlicious.hellblock.effects;

import com.swiftlicious.hellblock.utils.extras.Condition;

public interface EffectModifier {

	void modify(FishingEffect effect, Condition condition);
}
