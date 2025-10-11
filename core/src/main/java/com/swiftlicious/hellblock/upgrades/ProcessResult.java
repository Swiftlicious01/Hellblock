package com.swiftlicious.hellblock.upgrades;

import java.util.ArrayList;
import java.util.List;

public class ProcessResult {
	
    private final boolean success;
    private final List<String> missingRequirements;

    public ProcessResult(boolean success, List<String> missingRequirements) {
        this.success = success;
        this.missingRequirements = missingRequirements;
    }

    public boolean isSuccess() {
        return success;
    }

    public List<String> getMissingRequirements() {
        return missingRequirements;
    }

    public static ProcessResult success() {
        return new ProcessResult(true, new ArrayList<>());
    }

    public static ProcessResult fail(List<String> missing) {
        return new ProcessResult(false, missing);
    }
}
