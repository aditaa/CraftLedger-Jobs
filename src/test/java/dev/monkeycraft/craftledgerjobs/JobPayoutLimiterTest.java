package dev.monkeycraft.craftledgerjobs;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JobPayoutLimiterTest {
    @Test
    void allowsRepeatedPayoutsWhenLimitsAreDisabled() {
        JobPayoutLimiter limiter = new JobPayoutLimiter(fixedClock("2026-07-01T12:00:00Z"));
        JobsConfig config = new JobsConfig();
        UUID player = UUID.randomUUID();

        assertTrue(limiter.allow(player, "job_block_break:minecraft:coal_ore", 1.0D, config));
        assertTrue(limiter.allow(player, "job_block_break:minecraft:coal_ore", 1.0D, config));
    }

    @Test
    void blocksSamePayoutInsideCooldownWindow() {
        JobPayoutLimiter limiter = new JobPayoutLimiter(fixedClock("2026-07-01T12:00:00Z"));
        JobsConfig config = new JobsConfig();
        config.payoutCooldownSeconds = 30;
        UUID player = UUID.randomUUID();

        assertTrue(limiter.allow(player, "job_block_break:minecraft:coal_ore", 1.0D, config));
        assertFalse(limiter.allow(player, "job_block_break:minecraft:coal_ore", 1.0D, config));
        assertTrue(limiter.allow(player, "job_block_break:minecraft:iron_ore", 1.0D, config));
    }

    @Test
    void blocksPayoutsAboveDailyLimit() {
        JobPayoutLimiter limiter = new JobPayoutLimiter(fixedClock("2026-07-01T12:00:00Z"));
        JobsConfig config = new JobsConfig();
        config.dailyPayoutLimit = 5.0D;
        UUID player = UUID.randomUUID();

        assertTrue(limiter.allow(player, "a", 3.0D, config));
        assertFalse(limiter.allow(player, "b", 3.0D, config));
        assertTrue(limiter.allow(player, "b", 2.0D, config));
    }

    private static Clock fixedClock(String instant) {
        return Clock.fixed(Instant.parse(instant), ZoneOffset.UTC);
    }
}
