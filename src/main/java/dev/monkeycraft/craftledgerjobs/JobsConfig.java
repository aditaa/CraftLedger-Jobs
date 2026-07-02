package dev.monkeycraft.craftledgerjobs;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

public final class JobsConfig {
    private static final Pattern SIMPLE_ID = Pattern.compile("[a-z0-9_-]+");
    private static final Pattern RESOURCE_ID = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+");

    public boolean allowSwitching = true;
    public boolean notifyPayouts = true;
    public int payoutCooldownSeconds = 0;
    public double dailyPayoutLimit = 0;
    public Map<String, JobDefinition> jobs = new LinkedHashMap<>();

    public static JobsConfig load(Path path) throws IOException {
        if (Files.notExists(path)) {
            JobsConfig config = defaults();
            JsonFiles.writeAtomic(path, config);
            return config;
        }
        JobsConfig config = JsonFiles.read(path, JobsConfig.class);
        return config == null ? defaults() : config.normalize();
    }

    public JobsConfig normalize() {
        if (jobs == null) {
            jobs = new LinkedHashMap<>();
        }
        jobs.values().forEach(JobDefinition::normalize);
        validate();
        return this;
    }

    private void validate() {
        if (payoutCooldownSeconds < 0) {
            throw new ConfigValidationException("jobs.json payoutCooldownSeconds must be greater than or equal to 0.");
        }
        if (!Double.isFinite(dailyPayoutLimit) || dailyPayoutLimit < 0) {
            throw new ConfigValidationException("jobs.json dailyPayoutLimit must be a finite number greater than or equal to 0.");
        }
        jobs.forEach((jobId, job) -> {
            if (jobId == null || !SIMPLE_ID.matcher(jobId).matches()) {
                throw new ConfigValidationException("jobs.json contains invalid job id: " + jobId);
            }
            if (job == null) {
                throw new ConfigValidationException("jobs.json job " + jobId + " must not be null.");
            }
            job.blockBreak.forEach((blockId, payout) -> validatePayout("blockBreak", jobId, blockId, payout));
            job.entityKill.forEach((entityId, payout) -> validatePayout("entityKill", jobId, entityId, payout));
        });
    }

    private static void validatePayout(String section, String jobId, String resourceId, double payout) {
        if (resourceId == null || !RESOURCE_ID.matcher(resourceId).matches()) {
            throw new ConfigValidationException("jobs.json " + section + " for job " + jobId + " contains invalid id: " + resourceId);
        }
        if (!Double.isFinite(payout) || payout <= 0) {
            throw new ConfigValidationException("jobs.json " + section + " payout for " + resourceId + " in job " + jobId + " must be a finite number greater than 0.");
        }
    }

    private static JobsConfig defaults() {
        JobsConfig config = new JobsConfig();
        config.jobs.put("miner", new JobDefinition("Miner", "Earn money from ores and mining materials.", Map.of(
                "minecraft:coal_ore", 1.0D,
                "minecraft:iron_ore", 3.0D,
                "minecraft:deepslate_iron_ore", 3.5D,
                "minecraft:diamond_ore", 20.0D
        ), Map.of()));
        config.jobs.put("farmer", new JobDefinition("Farmer", "Earn money from fully grown crops.", Map.of(
                "minecraft:wheat", 0.35D,
                "minecraft:carrots", 0.35D,
                "minecraft:potatoes", 0.35D
        ), Map.of()));
        config.jobs.put("woodcutter", new JobDefinition("Woodcutter", "Earn money from chopping logs.", Map.of(
                "minecraft:oak_log", 0.50D,
                "minecraft:spruce_log", 0.50D,
                "minecraft:birch_log", 0.50D
        ), Map.of()));
        config.jobs.put("hunter", new JobDefinition("Hunter", "Earn money from killing hostile mobs.", Map.of(), Map.of(
                "minecraft:zombie", 1.25D,
                "minecraft:skeleton", 1.50D,
                "minecraft:creeper", 2.00D,
                "minecraft:spider", 1.00D
        )));
        return config;
    }

    public static final class JobDefinition {
        public String displayName;
        public String description = "";
        public Map<String, Double> blockBreak = new LinkedHashMap<>();
        public Map<String, Double> entityKill = new LinkedHashMap<>();

        public JobDefinition(String displayName, Map<String, Double> blockBreak, Map<String, Double> entityKill) {
            this(displayName, "", blockBreak, entityKill);
        }

        public JobDefinition(String displayName, String description, Map<String, Double> blockBreak, Map<String, Double> entityKill) {
            this.displayName = displayName;
            this.description = description;
            this.blockBreak = new LinkedHashMap<>(blockBreak);
            this.entityKill = new LinkedHashMap<>(entityKill);
        }

        void normalize() {
            if (displayName == null || displayName.isBlank()) {
                displayName = "Job";
            }
            if (description == null) {
                description = "";
            }
            if (blockBreak == null) {
                blockBreak = new LinkedHashMap<>();
            }
            if (entityKill == null) {
                entityKill = new LinkedHashMap<>();
            }
        }
    }
}
