package dev.monkeycraft.craftledgerjobs;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JobViewsTest {
    @Test
    void listMarksCurrentJobAndPaginates() {
        JobsConfig config = new JobsConfig();
        config.jobs = new LinkedHashMap<>();
        config.jobs.put("miner", new JobsConfig.JobDefinition("Miner", Map.of(), Map.of()));
        config.jobs.put("farmer", new JobsConfig.JobDefinition("Farmer", Map.of(), Map.of()));
        config.jobs.put("hunter", new JobsConfig.JobDefinition("Hunter", Map.of(), Map.of()));

        String output = JobViews.list(config, "farmer", 1);

        assertTrue(output.contains("farmer - Farmer (current)"));
        assertTrue(output.endsWith("Current job: farmer"));
    }

    @Test
    void infoIncludesDescriptionAndPayouts() {
        JobsConfig config = new JobsConfig();
        config.jobs.put("miner", new JobsConfig.JobDefinition("Miner", "Break rocks.", Map.of(
                "minecraft:coal_ore", 1.0D
        ), Map.of(
                "minecraft:zombie", 2.0D
        )));

        String output = JobViews.info(config, TestConfigs.common(true, 100, "coins", "$"), "miner", 1);

        assertEquals("""
                Miner (miner)
                Break rocks.
                Payouts (page 1/1):
                Break minecraft:coal_ore: $1.00 coins
                Kill minecraft:zombie: $2.00 coins""", output);
    }

    @Test
    void infoIncludesXpPayoutsAndHidesCurrencyWhenDisabled() {
        JobsConfig config = new JobsConfig();
        config.jobs.put("miner", new JobsConfig.JobDefinition("Miner", "Break rocks.", Map.of(
                "minecraft:coal_ore", 1.0D
        ), Map.of(), Map.of(
                "minecraft:coal_ore", 3
        ), Map.of()));

        String output = JobViews.info(config, TestConfigs.common(false, 100, "coins", "$"), "miner", 1);

        assertEquals("""
                Miner (miner)
                Break rocks.
                Payouts (page 1/1):
                Break minecraft:coal_ore: 3 XP""", output);
    }

    @Test
    void infoHandlesJobsWithoutPayouts() {
        JobsConfig config = new JobsConfig();
        config.jobs.put("builder", new JobsConfig.JobDefinition("Builder", "Coming soon.", Map.of(), Map.of()));

        assertEquals("""
                Builder (builder)
                Coming soon.
                No payouts configured.""", JobViews.info(config, TestConfigs.common(true, 100, "coins", "$"), "builder", 1));
    }
}
