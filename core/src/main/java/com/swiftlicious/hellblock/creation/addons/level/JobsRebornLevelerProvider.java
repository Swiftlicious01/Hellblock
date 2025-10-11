package com.swiftlicious.hellblock.creation.addons.level;

import java.util.List;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import com.gamingmesh.jobs.Jobs;
import com.gamingmesh.jobs.container.Job;
import com.gamingmesh.jobs.container.JobProgression;
import com.gamingmesh.jobs.container.JobsPlayer;

public class JobsRebornLevelerProvider implements LevelerProvider {

	@Override
	public void addXp(@NotNull Player player, @NotNull String target, double amount) {
		final JobsPlayer jobsPlayer = Jobs.getPlayerManager().getJobsPlayer(player);
		final Job job = Jobs.getJob(target);
		if (jobsPlayer != null && jobsPlayer.isInJob(job)) {
			Jobs.getPlayerManager().addExperience(jobsPlayer, job, amount);
		}
	}

	@Override
	public int getLevel(@NotNull Player player, @NotNull String target) {
		final JobsPlayer jobsPlayer = Jobs.getPlayerManager().getJobsPlayer(player);
		if (jobsPlayer != null) {
			final List<JobProgression> jobs = jobsPlayer.getJobProgression();
			final Job job = Jobs.getJob(target);
			for (JobProgression progression : jobs) {
				if (progression.getJob().equals(job)) {
					return progression.getLevel();
				}
			}
		}
		return 0;
	}

	@Override
	public String identifier() {
		return "JobsReborn";
	}
}