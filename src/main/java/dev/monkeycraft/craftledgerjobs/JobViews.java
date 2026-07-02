package dev.monkeycraft.craftledgerjobs;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

final class JobViews {
    private static final int PAGE_SIZE = 8;

    private JobViews() {
    }

    static String list(JobsConfig jobsConfig, String currentJob, int page) {
        List<String> rows = new ArrayList<>();
        for (Map.Entry<String, JobsConfig.JobDefinition> entry : jobsConfig.jobs.entrySet()) {
            String marker = entry.getKey().equals(currentJob) ? " (current)" : "";
            rows.add(entry.getKey() + " - " + entry.getValue().displayName + marker);
        }
        String output = PagedText.format("Jobs", rows, page, PAGE_SIZE);
        return output + "\nCurrent job: " + (currentJob == null ? "none" : currentJob);
    }

    static String info(JobsConfig jobsConfig, CommonConfig common, String jobId, int page) {
        JobsConfig.JobDefinition job = jobsConfig.jobs.get(jobId.toLowerCase(java.util.Locale.ROOT));
        if (job == null) {
            return "Unknown job: " + jobId;
        }

        List<String> rows = new ArrayList<>();
        job.blockBreak.forEach((id, amount) -> rows.add("Break " + id + ": " + common.format(amount)));
        job.entityKill.forEach((id, amount) -> rows.add("Kill " + id + ": " + common.format(amount)));

        String header = job.displayName + " (" + jobId.toLowerCase(java.util.Locale.ROOT) + ")";
        if (!job.description.isBlank()) {
            header += "\n" + job.description;
        }
        if (rows.isEmpty()) {
            return header + "\nNo payouts configured.";
        }
        return header + "\n" + PagedText.format("Payouts", rows, page, PAGE_SIZE);
    }
}
