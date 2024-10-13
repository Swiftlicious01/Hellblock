package com.swiftlicious.hellblock.utils.factory;

import com.swiftlicious.hellblock.utils.extras.Action;

public interface ActionFactory {

	Action build(Object args, double chance);
}
