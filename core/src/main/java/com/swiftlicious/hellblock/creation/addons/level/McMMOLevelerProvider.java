package com.swiftlicious.hellblock.creation.addons.level;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.gmail.nossr50.datatypes.player.PlayerProfile;
import com.gmail.nossr50.datatypes.skills.PrimarySkillType;
import com.gmail.nossr50.util.player.UserManager;

public class McMMOLevelerProvider implements LevelerProvider {

	@Override
	public void addXp(@NotNull Player player, @NotNull String skillName, double amount) {
		PrimarySkillType skill = getSkill(skillName);
		if (skill == null || !UserManager.hasPlayerDataKey(player))
			return;

		PlayerProfile profile = UserManager.getPlayer(player).getProfile();
		if (profile != null) {
			profile.addXp(skill, (float) amount);
		}
	}

	@Override
	public int getLevel(@NotNull Player player, @NotNull String skillName) {
		PrimarySkillType skill = getSkill(skillName);
		if (skill == null || !UserManager.hasPlayerDataKey(player))
			return 0;

		PlayerProfile profile = UserManager.getPlayer(player).getProfile();
		if (profile != null) {
			return profile.getSkillLevel(skill);
		}

		return 0;
	}

	@Override
	public String identifier() {
		return "mcMMO";
	}

	@Nullable
	private PrimarySkillType getSkill(@NotNull String name) {
		try {
			return PrimarySkillType.valueOf(name.toUpperCase());
		} catch (IllegalArgumentException e) {
			return null;
		}
	}
}