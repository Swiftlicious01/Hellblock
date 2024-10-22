package com.swiftlicious.hellblock.database.dependency;

/**
 * Runtime exception used if there is a problem during loading
 */
public class LoadingException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8097627772719905157L;

	public LoadingException(String message) {
		super(message);
	}

	public LoadingException(String message, Throwable cause) {
		super(message, cause);
	}

}