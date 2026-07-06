package dev.monkeycraft.craftledgerjobs;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JobsConfigTest {
    @TempDir
    Path tempDir;

    @Test
    void loadCreatesDefaultJobsWhenMissing() throws Exception {
        Path path = tempDir.resolve("jobs.json");

        JobsConfig config = JobsConfig.load(path);

        assertTrue(Files.exists(path));
        assertTrue(config.enabled);
        assertTrue(config.allowSwitching);
        assertTrue(config.notifyPayouts);
        assertTrue(config.trackPlacedBlocks);
        assertEquals(50000, config.maxTrackedPlacedBlocks);
        assertTrue(config.progressionEnabled);
        assertEquals(100, config.maxJobLevel);
        assertEquals(100.0D, config.baseJobXpRequired);
        assertEquals(1.25D, config.jobXpGrowth);
        assertEquals(10.0D, config.jobXpPerPayout);
        assertEquals(0.02D, config.payoutMultiplierPerLevel);
        assertEquals(0, config.payoutCooldownSeconds);
        assertEquals(0.0D, config.dailyPayoutLimit);
        assertTrue(config.jobs.containsKey("miner"));
        assertTrue(config.jobs.containsKey("farmer"));
        assertTrue(config.jobs.containsKey("hunter"));
        assertTrue(config.jobs.containsKey("woodcutter"));
        assertEquals(20.0D, config.jobs.get("miner").blockBreak.get("minecraft:diamond_ore"));
        assertEquals(10, config.jobs.get("miner").blockBreakXp.get("minecraft:diamond_ore"));
        assertEquals("Earn money from ores and mining materials.", config.jobs.get("miner").description);
        assertEquals(2.00D, config.jobs.get("hunter").entityKill.get("minecraft:creeper"));
    }

    @Test
    void loadNormalizesMissingJobFields() throws Exception {
        Path path = tempDir.resolve("jobs.json");
        Files.writeString(path, """
                {
                  "jobs": {
                    "custom": {}
                  }
                }
                """);

        JobsConfig config = JobsConfig.load(path);
        JobsConfig.JobDefinition custom = config.jobs.get("custom");

        assertEquals("Job", custom.displayName);
        assertEquals("", custom.description);
        assertNotNull(custom.blockBreak);
        assertNotNull(custom.entityKill);
        assertNotNull(custom.blockBreakXp);
        assertNotNull(custom.entityKillXp);
        assertTrue(custom.blockBreak.isEmpty());
        assertTrue(custom.entityKill.isEmpty());
        assertTrue(custom.blockBreakXp.isEmpty());
        assertTrue(custom.entityKillXp.isEmpty());
    }

    @Test
    void loadRejectsInvalidJobId() throws Exception {
        Path path = tempDir.resolve("jobs.json");
        Files.writeString(path, """
                {
                  "jobs": {
                    "Bad Job": {
                      "displayName": "Bad",
                      "blockBreak": {},
                      "entityKill": {}
                    }
                  }
                }
                """);

        assertThrows(ConfigValidationException.class, () -> JobsConfig.load(path));
    }

    @Test
    void loadRejectsInvalidPayout() throws Exception {
        Path path = tempDir.resolve("jobs.json");
        Files.writeString(path, """
                {
                  "jobs": {
                    "miner": {
                      "displayName": "Miner",
                      "blockBreak": {
                        "minecraft:coal_ore": -1.0
                      },
                      "entityKill": {}
                    }
                  }
                }
                """);

        assertThrows(ConfigValidationException.class, () -> JobsConfig.load(path));
    }

    @Test
    void loadRejectsInvalidPayoutResourceId() throws Exception {
        Path path = tempDir.resolve("jobs.json");
        Files.writeString(path, """
                {
                  "jobs": {
                    "miner": {
                      "displayName": "Miner",
                      "blockBreak": {
                        "coal_ore": 1.0
                      },
                      "entityKill": {}
                    }
                  }
                }
                """);

        assertThrows(ConfigValidationException.class, () -> JobsConfig.load(path));
    }

    @Test
    void loadParsesGlobalJobOptionsAndDescription() throws Exception {
        Path path = tempDir.resolve("jobs.json");
        Files.writeString(path, """
                {
                  "allowSwitching": false,
                  "enabled": false,
                  "notifyPayouts": false,
                  "trackPlacedBlocks": false,
                  "maxTrackedPlacedBlocks": 123,
                  "progressionEnabled": false,
                  "maxJobLevel": 50,
                  "baseJobXpRequired": 25.0,
                  "jobXpGrowth": 1.1,
                  "jobXpPerPayout": 4.0,
                  "payoutMultiplierPerLevel": 0.05,
                  "payoutCooldownSeconds": 10,
                  "dailyPayoutLimit": 250.0,
                  "jobs": {
                    "miner": {
                      "displayName": "Miner",
                      "description": "Dig carefully.",
                      "blockBreak": {
                        "minecraft:coal_ore": 1.0
                      },
                      "entityKill": {},
                      "blockBreakXp": {
                        "minecraft:coal_ore": 3
                      },
                      "entityKillXp": {}
                    }
                  }
                }
                """);

        JobsConfig config = JobsConfig.load(path);

        assertFalse(config.enabled);
        assertFalse(config.allowSwitching);
        assertFalse(config.notifyPayouts);
        assertFalse(config.trackPlacedBlocks);
        assertEquals(123, config.maxTrackedPlacedBlocks);
        assertFalse(config.progressionEnabled);
        assertEquals(50, config.maxJobLevel);
        assertEquals(25.0D, config.baseJobXpRequired);
        assertEquals(1.1D, config.jobXpGrowth);
        assertEquals(4.0D, config.jobXpPerPayout);
        assertEquals(0.05D, config.payoutMultiplierPerLevel);
        assertEquals(10, config.payoutCooldownSeconds);
        assertEquals(250.0D, config.dailyPayoutLimit);
        assertEquals(3, config.jobs.get("miner").blockBreakXp.get("minecraft:coal_ore"));
        assertEquals("Dig carefully.", config.jobs.get("miner").description);
    }

    @Test
    void loadRejectsInvalidJobLimits() throws Exception {
        Path path = tempDir.resolve("jobs.json");
        Files.writeString(path, """
                {
                  "payoutCooldownSeconds": -1,
                  "jobs": {}
                }
                """);

        assertThrows(ConfigValidationException.class, () -> JobsConfig.load(path));

        Files.writeString(path, """
                {
                  "maxJobLevel": 0,
                  "jobs": {}
                }
                """);

        assertThrows(ConfigValidationException.class, () -> JobsConfig.load(path));

        Files.writeString(path, """
                {
                  "jobXpGrowth": 0.5,
                  "jobs": {}
                }
                """);

        assertThrows(ConfigValidationException.class, () -> JobsConfig.load(path));

        Files.writeString(path, """
                {
                  "maxTrackedPlacedBlocks": -1,
                  "jobs": {}
                }
                """);

        assertThrows(ConfigValidationException.class, () -> JobsConfig.load(path));

        Files.writeString(path, """
                {
                  "dailyPayoutLimit": -1.0,
                  "jobs": {}
                }
                """);

        assertThrows(ConfigValidationException.class, () -> JobsConfig.load(path));
    }

    @Test
    void loadRejectsInvalidXpPayout() throws Exception {
        Path path = tempDir.resolve("jobs.json");
        Files.writeString(path, """
                {
                  "jobs": {
                    "miner": {
                      "displayName": "Miner",
                      "blockBreak": {},
                      "entityKill": {},
                      "blockBreakXp": {
                        "minecraft:coal_ore": 0
                      },
                      "entityKillXp": {}
                    }
                  }
                }
                """);

        assertThrows(ConfigValidationException.class, () -> JobsConfig.load(path));
    }
}
