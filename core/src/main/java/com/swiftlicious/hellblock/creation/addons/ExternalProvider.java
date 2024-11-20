package com.swiftlicious.hellblock.creation.addons;

/**
 * The ExternalProvider interface serves as a base interface for various
 * external providers
 */
public interface ExternalProvider {

	/**
	 * Gets the identification of the external provider.
	 *
	 * @return The identification string of the external provider.
	 */
	String identifier();
}