package dev.monkeycraft.craftledgerjobs;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JobsConfigTest {
    @TempDir
    Path tempDir;

    @Test
    void loadCreatesDefaultJobsWhenMissing() throws Exception {
        Path path = tempDir.resolve("jobs.json");

        JobsConfig config = JobsConfig.load(path);

        assertTrue(Files.exists(path));
        assertTrue(config.jobs.containsKey("miner"));
        assertTrue(config.jobs.containsKey("farmer"));
        assertTrue(config.jobs.containsKey("hunter"));
        assertTrue(config.jobs.containsKey("woodcutter"));
        assertEquals(20.0D, config.jobs.get("miner").blockBreak.get("minecraft:diamond_ore"));
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
        assertNotNull(custom.blockBreak);
        assertNotNull(custom.entityKill);
        assertTrue(custom.blockBreak.isEmpty());
        assertTrue(custom.entityKill.isEmpty());
    }
}
