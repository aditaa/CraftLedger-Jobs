package dev.monkeycraft.craftledgerjobs;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public final class JobsConfig {
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
        return this;
    }

    private static JobsConfig defaults() {
        JobsConfig config = new JobsConfig();
        config.jobs.put("miner", new JobDefinition("Miner", Map.of(
                "minecraft:coal_ore", 1.0D,
                "minecraft:iron_ore", 3.0D,
                "minecraft:deepslate_iron_ore", 3.5D,
                "minecraft:diamond_ore", 20.0D
        ), Map.of()));
        config.jobs.put("farmer", new JobDefinition("Farmer", Map.of(
                "minecraft:wheat", 0.35D,
                "minecraft:carrots", 0.35D,
                "minecraft:potatoes", 0.35D
        ), Map.of()));
        config.jobs.put("woodcutter", new JobDefinition("Woodcutter", Map.of(
                "minecraft:oak_log", 0.50D,
                "minecraft:spruce_log", 0.50D,
                "minecraft:birch_log", 0.50D
        ), Map.of()));
        config.jobs.put("hunter", new JobDefinition("Hunter", Map.of(), Map.of(
                "minecraft:zombie", 1.25D,
                "minecraft:skeleton", 1.50D,
                "minecraft:creeper", 2.00D,
                "minecraft:spider", 1.00D
        )));
        return config;
    }

    public static final class JobDefinition {
        public String displayName;
        public Map<String, Double> blockBreak = new LinkedHashMap<>();
        public Map<String, Double> entityKill = new LinkedHashMap<>();

        public JobDefinition(String displayName, Map<String, Double> blockBreak, Map<String, Double> entityKill) {
            this.displayName = displayName;
            this.blockBreak = new LinkedHashMap<>(blockBreak);
            this.entityKill = new LinkedHashMap<>(entityKill);
        }

        void normalize() {
            if (displayName == null || displayName.isBlank()) {
                displayName = "Job";
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
