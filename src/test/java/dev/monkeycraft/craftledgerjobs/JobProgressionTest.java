package dev.monkeycraft.craftledgerjobs;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JobProgressionTest {
    @Test
    void multiplierScalesByLevel() {
        JobsConfig config = new JobsConfig();
        config.payoutMultiplierPerLevel = 0.10D;
        config.maxJobLevel = 5;

        assertEquals(1.0D, JobProgression.multiplier(1, config));
        assertEquals(1.2D, JobProgression.multiplier(3, config));
        assertEquals(1.4D, JobProgression.multiplier(99, config));
    }

    @Test
    void applyLevelsUpAndCarriesRemainingXp() {
        JobsConfig config = new JobsConfig();
        config.baseJobXpRequired = 100.0D;
        config.jobXpGrowth = 1.0D;
        config.maxJobLevel = 10;

        JobProgression.Progress progress = JobProgression.apply(1, 90.0D, 25.0D, config);

        assertEquals(2, progress.level());
        assertEquals(15.0D, progress.xp());
        assertEquals(25.0D, progress.gainedXp());
        assertTrue(progress.leveled());
    }

    @Test
    void applyStopsAtMaxLevel() {
        JobsConfig config = new JobsConfig();
        config.baseJobXpRequired = 10.0D;
        config.jobXpGrowth = 1.0D;
        config.maxJobLevel = 2;

        JobProgression.Progress progress = JobProgression.apply(1, 0.0D, 100.0D, config);

        assertEquals(2, progress.level());
        assertEquals(0.0D, progress.xp());
        assertTrue(progress.leveled());

        JobProgression.Progress capped = JobProgression.apply(2, 0.0D, 100.0D, config);
        assertEquals(2, capped.level());
        assertEquals(0.0D, capped.xp());
        assertFalse(capped.leveled());
    }

    @Test
    void requiredXpReturnsZeroAtOrAboveConfiguredMaxLevel() {
        JobsConfig config = new JobsConfig();
        config.maxJobLevel = 3;

        assertEquals(0.0D, JobProgression.requiredXpForNextLevel(3, config));
        assertEquals(0.0D, JobProgression.requiredXpForNextLevel(99, config));
    }
}
