package com.swiftlicious.hellblock.utils;

import java.util.Base64;

public class SkullUtils {

	public static String identifierFromBase64(String base64) {
		byte[] decodedBytes = Base64.getDecoder().decode(base64);
		String decodedString = new String(decodedBytes);
		int urlStartIndex = decodedString.indexOf("\"url\":\"") + 7;
		int urlEndIndex = decodedString.indexOf("\"", urlStartIndex);
		String textureUrl = decodedString.substring(urlStartIndex, urlEndIndex);
		return textureUrl.substring(textureUrl.lastIndexOf('/') + 1);
	}
}