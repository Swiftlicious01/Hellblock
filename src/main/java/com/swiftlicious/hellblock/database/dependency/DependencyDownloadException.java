package com.swiftlicious.hellblock.database.dependency;

/**
 * Exception thrown if a dependency cannot be downloaded.
 */
public class DependencyDownloadException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -6369230146193750527L;

	public DependencyDownloadException() {

	}

	public DependencyDownloadException(String message) {
		super(message);
	}

	public DependencyDownloadException(String message, Throwable cause) {
		super(message, cause);
	}

	public DependencyDownloadException(Throwable cause) {
		super(cause);
	}
}