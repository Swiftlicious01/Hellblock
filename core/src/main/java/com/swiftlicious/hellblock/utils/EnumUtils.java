package com.swiftlicious.hellblock.utils;

public class EnumUtils {

	public static <T extends Enum<T>> boolean isValidEnum(Class<T> enumClass, String value) {
		if (value == null)
			return false;
		try {
			Enum.valueOf(enumClass, value.toUpperCase());
			return true;
		} catch (IllegalArgumentException e) {
			return false;
		}
	}

	@SuppressWarnings("unchecked")
	public static <T extends Enum<T>> T getEnum(Class<T> enumClass, String value) {
		for (Enum<T> enumVal : enumClass.getEnumConstants()) {
			if (enumVal.toString().equalsIgnoreCase(value)) {
				return (T) enumVal;
			}
		}
		return null;
	}
}
