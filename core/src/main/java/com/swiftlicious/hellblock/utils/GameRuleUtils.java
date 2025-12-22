package com.swiftlicious.hellblock.utils;

import java.lang.reflect.Field;

import org.bukkit.GameRule;

public class GameRuleUtils {
	
	public static final GameRule<Boolean> LIMITED_CRAFTING_RULE = resolveLimitedCraftingRule();

	@SuppressWarnings("unchecked")
	private static GameRule<Boolean> resolveLimitedCraftingRule() {
	    try {
	        // Try the new name first (1.21.11+)
	        Field newField = GameRule.class.getField("LIMITED_CRAFTING");
	        return (GameRule<Boolean>) newField.get(null);
	    } catch (NoSuchFieldException e) {
	        try {
	            // Fallback for older versions (pre-1.21.11)
	            Field oldField = GameRule.class.getField("DO_LIMITED_CRAFTING");
	            return (GameRule<Boolean>) oldField.get(null);
	        } catch (ReflectiveOperationException ex) {
	            throw new IllegalStateException("Cannot find LIMITED_CRAFTING or DO_LIMITED_CRAFTING GameRule!", ex);
	        }
	    } catch (IllegalAccessException e) {
	        throw new RuntimeException(e);
	    }
	}
}