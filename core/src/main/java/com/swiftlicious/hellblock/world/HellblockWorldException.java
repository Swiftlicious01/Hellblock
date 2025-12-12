package com.swiftlicious.hellblock.world;

/**
 * Thrown to indicate an error occurred during Hellblock world operations.
 * <p>
 * This can represent issues such as:
 * <ul>
 * <li>Failure to load or create a world</li>
 * <li>Serialization or deserialization problems</li>
 * <li>Unexpected world adapter behavior</li>
 * <li>Corrupted data in Slime or Bukkit-based worlds</li>
 * </ul>
 *
 * This exception should be used instead of raw {@link RuntimeException} to give
 * context to world-related operations in Hellblock.
 */
public class HellblockWorldException extends RuntimeException {

	private static final long serialVersionUID = 2421226923573696420L;

	/**
	 * Constructs a new HellblockWorldException with {@code null} as its detail
	 * message.
	 */
	public HellblockWorldException() {
		super();
	}

	/**
	 * Constructs a new HellblockWorldException with the specified detail message.
	 *
	 * @param message the detail message
	 */
	public HellblockWorldException(String message) {
		super(message);
	}

	/**
	 * Constructs a new HellblockWorldException with the specified cause.
	 *
	 * @param cause the cause of the exception
	 */
	public HellblockWorldException(Throwable cause) {
		super(cause);
	}

	/**
	 * Constructs a new HellblockWorldException with the specified detail message
	 * and cause.
	 *
	 * @param message the detail message
	 * @param cause   the cause of the exception
	 */
	public HellblockWorldException(String message, Throwable cause) {
		super(message, cause);
	}
}