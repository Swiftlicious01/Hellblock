package com.swiftlicious.hellblock.creation.addons.level;

import java.util.List;

import org.bukkit.entity.Player;

import com.gamingmesh.jobs.Jobs;
import com.gamingmesh.jobs.container.Job;
import com.gamingmesh.jobs.container.JobProgression;
import com.gamingmesh.jobs.container.JobsPlayer;
import com.swiftlicious.hellblock.creation.addons.LevelInterface;

public class JobsReborn implements LevelInterface {

	@Override
	public void addXp(Player player, String target, double amount) {
		JobsPlayer jobsPlayer = Jobs.getPlayerManager().getJobsPlayer(player);
		Job job = Jobs.getJob(target);
		if (jobsPlayer != null && jobsPlayer.isInJob(job))
			Jobs.getPlayerManager().addExperience(jobsPlayer, job, amount);
	}

	@Override
	public int getLevel(Player player, String target) {
		JobsPlayer jobsPlayer = Jobs.getPlayerManager().getJobsPlayer(player);
		if (jobsPlayer != null) {
			List<JobProgression> jobs = jobsPlayer.getJobProgression();
			Job job = Jobs.getJob(target);
			for (JobProgression progression : jobs)
				if (progression.getJob().equals(job))
					return progression.getLevel();
		}
		return 0;
	}
}