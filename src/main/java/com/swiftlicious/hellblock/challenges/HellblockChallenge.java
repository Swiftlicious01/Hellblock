package com.swiftlicious.hellblock.challenges;

public class HellblockChallenge {

	private ChallengeType type;
	private CompletionStatus status;
	private int progress;

	public HellblockChallenge(ChallengeType type, CompletionStatus status, int progress) {
		this.type = type;
		this.status = status;
		this.progress = progress;
	}

	public ChallengeType getChallengeType() {
		return this.type;
	}

	public CompletionStatus getCompletionStatus() {
		return this.status;
	}

	public int getProgress() {
		return this.progress;
	}

	public enum ChallengeType {
		NETHERRACK_GENERATOR_CHALLENGE("generated-blocks", 250, ActionType.BREAK),
		GLOWSTONE_TREE_CHALLENGE("grow-tree", 25, ActionType.GROW),
		LAVA_FISHING_CHALLENGE("lava-fish", 50, ActionType.FISH),
		NETHER_CRAFTING_CHALLENGE("nether-crafting", 36, ActionType.CRAFT),
		NETHER_BREWING_CHALLENGE("nether-brewing", 75, ActionType.INTERACT),
		NETHER_GOLEM_CHALLENGE("nether-golem", 40, ActionType.SPAWN);

		private String name;
		private int completionAmount;
		private ActionType action;

		ChallengeType(String name, int completionAmount, ActionType action) {
			this.name = name;
			this.completionAmount = completionAmount;
			this.action = action;
		}

		public String getName() {
			return this.name;
		}

		public int getNeededAmount() {
			return this.completionAmount;
		}

		public ActionType getAction() {
			return this.action;
		}
	}

	public enum CompletionStatus {
		COMPLETED("Completed"), IN_PROGRESS("In Progress"), NOT_STARTED("Not Started");

		private String name;

		CompletionStatus(String name) {
			this.name = name;
		}

		public String getName() {
			return this.name;
		}
	}

	public enum ActionType {
		BREAK, PLACE, GROW, FISH, INTERACT, CRAFT, FARM, ENCHANT, SMELT, SPAWN;
	}
}
