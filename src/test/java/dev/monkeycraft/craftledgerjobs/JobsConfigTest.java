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
        assertTrue(config.allowSwitching);
        assertTrue(config.notifyPayouts);
        assertEquals(0, config.payoutCooldownSeconds);
        assertEquals(0.0D, config.dailyPayoutLimit);
        assertTrue(config.jobs.containsKey("miner"));
        assertTrue(config.jobs.containsKey("farmer"));
        assertTrue(config.jobs.containsKey("hunter"));
        assertTrue(config.jobs.containsKey("woodcutter"));
        assertEquals(20.0D, config.jobs.get("miner").blockBreak.get("minecraft:diamond_ore"));
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
        assertTrue(custom.blockBreak.isEmpty());
        assertTrue(custom.entityKill.isEmpty());
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
                  "notifyPayouts": false,
                  "payoutCooldownSeconds": 10,
                  "dailyPayoutLimit": 250.0,
                  "jobs": {
                    "miner": {
                      "displayName": "Miner",
                      "description": "Dig carefully.",
                      "blockBreak": {
                        "minecraft:coal_ore": 1.0
                      },
                      "entityKill": {}
                    }
                  }
                }
                """);

        JobsConfig config = JobsConfig.load(path);

        assertFalse(config.allowSwitching);
        assertFalse(config.notifyPayouts);
        assertEquals(10, config.payoutCooldownSeconds);
        assertEquals(250.0D, config.dailyPayoutLimit);
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
                  "dailyPayoutLimit": -1.0,
                  "jobs": {}
                }
                """);

        assertThrows(ConfigValidationException.class, () -> JobsConfig.load(path));
    }
}
