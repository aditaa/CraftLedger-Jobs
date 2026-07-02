package dev.monkeycraft.craftledgerjobs;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JobPayoutLimiterTest {
    @TempDir
    Path tempDir;

    @Test
    void allowsRepeatedPayoutsWhenLimitsAreDisabled() throws Exception {
        JobPayoutLimiter limiter = new JobPayoutLimiter(fixedClock("2026-07-01T12:00:00Z"), store());
        JobsConfig config = new JobsConfig();
        UUID player = UUID.randomUUID();

        assertTrue(limiter.allow(player, "job_block_break:minecraft:coal_ore", 1.0D, config));
        assertTrue(limiter.allow(player, "job_block_break:minecraft:coal_ore", 1.0D, config));
    }

    @Test
    void blocksSamePayoutInsideCooldownWindow() throws Exception {
        JobPayoutLimiter limiter = new JobPayoutLimiter(fixedClock("2026-07-01T12:00:00Z"), store());
        JobsConfig config = new JobsConfig();
        config.payoutCooldownSeconds = 30;
        UUID player = UUID.randomUUID();

        assertTrue(limiter.allow(player, "job_block_break:minecraft:coal_ore", 1.0D, config));
        assertFalse(limiter.allow(player, "job_block_break:minecraft:coal_ore", 1.0D, config));
        assertTrue(limiter.allow(player, "job_block_break:minecraft:iron_ore", 1.0D, config));
    }

    @Test
    void blocksPayoutsAboveDailyLimit() throws Exception {
        JobPayoutStore store = store();
        JobPayoutLimiter limiter = new JobPayoutLimiter(fixedClock("2026-07-01T12:00:00Z"), store);
        JobsConfig config = new JobsConfig();
        config.dailyPayoutLimit = 5.0D;
        UUID player = UUID.randomUUID();

        assertTrue(limiter.allow(player, "a", 3.0D, config));
        assertFalse(limiter.allow(player, "b", 3.0D, config));
        assertTrue(limiter.allow(player, "b", 2.0D, config));
        assertFalse(limiter.allow(player, "c", 0.01D, config));
        assertEquals(5.0D, store.total(player, LocalDate.parse("2026-07-01")));
    }

    @Test
    void dailyLimitsSurviveReloadingTheStore() throws Exception {
        UUID player = UUID.randomUUID();
        JobsConfig config = new JobsConfig();
        config.dailyPayoutLimit = 5.0D;
        Path path = tempDir.resolve("job_payouts.json");

        JobPayoutLimiter first = new JobPayoutLimiter(fixedClock("2026-07-01T12:00:00Z"), JobPayoutStore.load(path));
        assertTrue(first.allow(player, "a", 3.0D, config));

        JobPayoutLimiter second = new JobPayoutLimiter(fixedClock("2026-07-01T13:00:00Z"), JobPayoutStore.load(path));
        assertFalse(second.allow(player, "b", 3.0D, config));
        assertTrue(second.allow(player, "b", 2.0D, config));
    }

    @Test
    void dailyLimitsResetOnNextUtcDay() throws Exception {
        UUID player = UUID.randomUUID();
        JobsConfig config = new JobsConfig();
        config.dailyPayoutLimit = 5.0D;
        Path path = tempDir.resolve("job_payouts.json");

        JobPayoutLimiter first = new JobPayoutLimiter(fixedClock("2026-07-01T23:59:00Z"), JobPayoutStore.load(path));
        assertTrue(first.allow(player, "a", 5.0D, config));

        JobPayoutLimiter second = new JobPayoutLimiter(fixedClock("2026-07-02T00:01:00Z"), JobPayoutStore.load(path));
        assertTrue(second.allow(player, "a", 5.0D, config));
    }

    private static Clock fixedClock(String instant) {
        return Clock.fixed(Instant.parse(instant), ZoneOffset.UTC);
    }

    private JobPayoutStore store() throws Exception {
        return JobPayoutStore.load(tempDir.resolve(UUID.randomUUID() + ".json"));
    }
}
