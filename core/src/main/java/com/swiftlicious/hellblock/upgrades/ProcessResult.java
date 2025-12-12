package com.swiftlicious.hellblock.upgrades;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NotNull;

/**
 * Represents the result of a process that may succeed or fail with missing
 * requirements.
 * <p>
 * Commonly used to determine whether an upgrade cost can be afforded, along
 * with a list of reasons (e.g., missing resources) if it cannot.
 */
public class ProcessResult {

	private final boolean success;
	private final List<String> missingRequirements;

	/**
	 * Constructs a new {@code ProcessResult}.
	 *
	 * @param success             {@code true} if the process succeeded
	 * @param missingRequirements list of missing or unmet requirements if failed
	 */
	public ProcessResult(boolean success, @NotNull List<String> missingRequirements) {
		this.success = success;
		this.missingRequirements = missingRequirements;
	}

	/**
	 * @return {@code true} if the process succeeded; {@code false} otherwise
	 */
	public boolean isSuccess() {
		return success;
	}

	/**
	 * @return a list of missing requirements (if any), or empty if successful
	 */
	@NotNull
	public List<String> getMissingRequirements() {
		return missingRequirements;
	}

	/**
	 * Creates a successful {@code ProcessResult} with no missing requirements.
	 *
	 * @return a success result
	 */
	@NotNull
	public static ProcessResult success() {
		return new ProcessResult(true, new ArrayList<>());
	}

	/**
	 * Creates a failed {@code ProcessResult} with the given list of missing
	 * requirements.
	 *
	 * @param missing list of missing resources or constraints
	 * @return a failure result with reasons
	 */
	@NotNull
	public static ProcessResult fail(@NotNull List<String> missing) {
		return new ProcessResult(false, missing);
	}
}