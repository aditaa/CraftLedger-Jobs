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
    public static final int CURRENT_VERSION = 1;

    public int version = CURRENT_VERSION;
    public boolean enabled = true;
    public boolean allowSwitching = true;
    public boolean notifyPayouts = true;
    public boolean trackPlacedBlocks = true;
    public int maxTrackedPlacedBlocks = 50000;
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
        if (version <= 0) {
            version = CURRENT_VERSION;
        }
        jobs.values().forEach(JobDefinition::normalize);
        validate();
        return this;
    }

    private void validate() {
        if (version < 1) {
            throw new ConfigValidationException("jobs.json version must be greater than or equal to 1.");
        }
        if (payoutCooldownSeconds < 0) {
            throw new ConfigValidationException("jobs.json payoutCooldownSeconds must be greater than or equal to 0.");
        }
        if (maxTrackedPlacedBlocks < 0) {
            throw new ConfigValidationException("jobs.json maxTrackedPlacedBlocks must be greater than or equal to 0.");
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
            job.blockBreakXp.forEach((blockId, xp) -> validateXpPayout("blockBreakXp", jobId, blockId, xp));
            job.entityKillXp.forEach((entityId, xp) -> validateXpPayout("entityKillXp", jobId, entityId, xp));
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

    private static void validateXpPayout(String section, String jobId, String resourceId, int payout) {
        if (resourceId == null || !RESOURCE_ID.matcher(resourceId).matches()) {
            throw new ConfigValidationException("jobs.json " + section + " for job " + jobId + " contains invalid id: " + resourceId);
        }
        if (payout <= 0) {
            throw new ConfigValidationException("jobs.json " + section + " payout for " + resourceId + " in job " + jobId + " must be greater than 0.");
        }
    }

    private static JobsConfig defaults() {
        JobsConfig config = new JobsConfig();
        config.jobs.put("miner", new JobDefinition("Miner", "Earn money from ores and mining materials.", payouts(
                "minecraft:coal_ore", 1.0D,
                "minecraft:deepslate_coal_ore", 1.25D,
                "minecraft:copper_ore", 1.75D,
                "minecraft:deepslate_copper_ore", 2.0D,
                "minecraft:iron_ore", 3.0D,
                "minecraft:deepslate_iron_ore", 3.5D,
                "minecraft:gold_ore", 5.0D,
                "minecraft:deepslate_gold_ore", 5.5D,
                "minecraft:redstone_ore", 4.0D,
                "minecraft:deepslate_redstone_ore", 4.5D,
                "minecraft:lapis_ore", 4.0D,
                "minecraft:deepslate_lapis_ore", 4.5D,
                "minecraft:emerald_ore", 15.0D,
                "minecraft:deepslate_emerald_ore", 17.5D,
                "minecraft:diamond_ore", 20.0D
        ), Map.of(), xp(
                "minecraft:coal_ore", 1,
                "minecraft:deepslate_coal_ore", 1,
                "minecraft:copper_ore", 1,
                "minecraft:deepslate_copper_ore", 1,
                "minecraft:iron_ore", 2,
                "minecraft:deepslate_iron_ore", 2,
                "minecraft:gold_ore", 3,
                "minecraft:deepslate_gold_ore", 3,
                "minecraft:redstone_ore", 2,
                "minecraft:deepslate_redstone_ore", 2,
                "minecraft:lapis_ore", 2,
                "minecraft:deepslate_lapis_ore", 2,
                "minecraft:emerald_ore", 7,
                "minecraft:deepslate_emerald_ore", 8,
                "minecraft:diamond_ore", 10
        ), Map.of()));
        config.jobs.put("farmer", new JobDefinition("Farmer", "Earn money from fully grown crops.", payouts(
                "minecraft:wheat", 0.35D,
                "minecraft:carrots", 0.35D,
                "minecraft:potatoes", 0.35D,
                "minecraft:beetroots", 0.35D,
                "minecraft:pumpkin", 0.80D,
                "minecraft:melon", 0.60D,
                "minecraft:sugar_cane", 0.25D,
                "minecraft:cactus", 0.25D,
                "minecraft:cocoa", 0.40D
        ), Map.of(), xp(
                "minecraft:wheat", 1,
                "minecraft:carrots", 1,
                "minecraft:potatoes", 1,
                "minecraft:beetroots", 1,
                "minecraft:pumpkin", 1,
                "minecraft:melon", 1,
                "minecraft:sugar_cane", 1,
                "minecraft:cactus", 1,
                "minecraft:cocoa", 1
        ), Map.of()));
        config.jobs.put("woodcutter", new JobDefinition("Woodcutter", "Earn money from chopping logs.", payouts(
                "minecraft:oak_log", 0.50D,
                "minecraft:spruce_log", 0.50D,
                "minecraft:birch_log", 0.50D,
                "minecraft:jungle_log", 0.55D,
                "minecraft:acacia_log", 0.55D,
                "minecraft:dark_oak_log", 0.60D,
                "minecraft:mangrove_log", 0.65D,
                "minecraft:cherry_log", 0.65D,
                "minecraft:crimson_stem", 0.75D,
                "minecraft:warped_stem", 0.75D
        ), Map.of(), xp(
                "minecraft:oak_log", 1,
                "minecraft:spruce_log", 1,
                "minecraft:birch_log", 1,
                "minecraft:jungle_log", 1,
                "minecraft:acacia_log", 1,
                "minecraft:dark_oak_log", 1,
                "minecraft:mangrove_log", 1,
                "minecraft:cherry_log", 1,
                "minecraft:crimson_stem", 1,
                "minecraft:warped_stem", 1
        ), Map.of()));
        config.jobs.put("hunter", new JobDefinition("Hunter", "Earn money from killing hostile mobs.", Map.of(), payouts(
                "minecraft:zombie", 1.25D,
                "minecraft:skeleton", 1.50D,
                "minecraft:creeper", 2.00D,
                "minecraft:spider", 1.00D,
                "minecraft:enderman", 5.00D,
                "minecraft:witch", 4.00D,
                "minecraft:slime", 1.00D,
                "minecraft:drowned", 1.50D,
                "minecraft:husk", 1.50D,
                "minecraft:stray", 1.75D,
                "minecraft:pillager", 3.00D
        ), Map.of(), xp(
                "minecraft:zombie", 2,
                "minecraft:skeleton", 3,
                "minecraft:creeper", 5,
                "minecraft:spider", 2,
                "minecraft:enderman", 8,
                "minecraft:witch", 6,
                "minecraft:slime", 2,
                "minecraft:drowned", 3,
                "minecraft:husk", 3,
                "minecraft:stray", 3,
                "minecraft:pillager", 5
        )));
        return config;
    }

    private static Map<String, Double> payouts(Object... entries) {
        LinkedHashMap<String, Double> values = new LinkedHashMap<>();
        for (int index = 0; index < entries.length; index += 2) {
            values.put((String) entries[index], (Double) entries[index + 1]);
        }
        return values;
    }

    private static Map<String, Integer> xp(Object... entries) {
        LinkedHashMap<String, Integer> values = new LinkedHashMap<>();
        for (int index = 0; index < entries.length; index += 2) {
            values.put((String) entries[index], (Integer) entries[index + 1]);
        }
        return values;
    }

    public static final class JobDefinition {
        public String displayName;
        public String description = "";
        public Map<String, Double> blockBreak = new LinkedHashMap<>();
        public Map<String, Double> entityKill = new LinkedHashMap<>();
        public Map<String, Integer> blockBreakXp = new LinkedHashMap<>();
        public Map<String, Integer> entityKillXp = new LinkedHashMap<>();

        public JobDefinition(String displayName, Map<String, Double> blockBreak, Map<String, Double> entityKill) {
            this(displayName, "", blockBreak, entityKill);
        }

        public JobDefinition(String displayName, String description, Map<String, Double> blockBreak, Map<String, Double> entityKill) {
            this(displayName, description, blockBreak, entityKill, Map.of(), Map.of());
        }

        public JobDefinition(
                String displayName,
                String description,
                Map<String, Double> blockBreak,
                Map<String, Double> entityKill,
                Map<String, Integer> blockBreakXp,
                Map<String, Integer> entityKillXp
        ) {
            this.displayName = displayName;
            this.description = description;
            this.blockBreak = new LinkedHashMap<>(blockBreak);
            this.entityKill = new LinkedHashMap<>(entityKill);
            this.blockBreakXp = new LinkedHashMap<>(blockBreakXp);
            this.entityKillXp = new LinkedHashMap<>(entityKillXp);
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
            if (blockBreakXp == null) {
                blockBreakXp = new LinkedHashMap<>();
            }
            if (entityKillXp == null) {
                entityKillXp = new LinkedHashMap<>();
            }
        }
    }
}
