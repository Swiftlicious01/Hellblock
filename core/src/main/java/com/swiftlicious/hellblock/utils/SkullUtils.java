package com.swiftlicious.hellblock.utils;

import java.util.Base64;

public class SkullUtils {

	public static String identifierFromBase64(String base64) {
		final byte[] decodedBytes = Base64.getDecoder().decode(base64);
		final String decodedString = new String(decodedBytes);
		final int urlStartIndex = decodedString.indexOf("\"url\":\"") + 7;
		final int urlEndIndex = decodedString.indexOf("\"", urlStartIndex);
		final String textureUrl = decodedString.substring(urlStartIndex, urlEndIndex);
		return textureUrl.substring(textureUrl.lastIndexOf('/') + 1);
	}
}