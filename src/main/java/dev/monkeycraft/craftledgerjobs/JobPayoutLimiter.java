package dev.monkeycraft.craftledgerjobs;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class JobPayoutLimiter {
    private final Clock clock;
    private final JobPayoutDataStore payoutStore;
    private final Map<String, Instant> lastPayouts = new HashMap<>();

    public JobPayoutLimiter(JobPayoutDataStore payoutStore) {
        this(Clock.systemUTC(), payoutStore);
    }

    JobPayoutLimiter(Clock clock, JobPayoutDataStore payoutStore) {
        this.clock = clock;
        this.payoutStore = payoutStore;
    }

    public boolean allow(UUID playerUuid, String payoutKey, double payout, JobsConfig config) {
        Instant now = clock.instant();
        int cooldownSeconds = Math.max(0, config.payoutCooldownSeconds);
        if (cooldownSeconds > 0) {
            String key = playerUuid + "|" + payoutKey;
            Instant last = lastPayouts.get(key);
            if (last != null && Duration.between(last, now).getSeconds() < cooldownSeconds) {
                return false;
            }
        }

        double dailyLimit = config.dailyPayoutLimit;
        if (dailyLimit > 0 && payout > 0 && !payoutStore.allowAndRecord(playerUuid, now, payout, dailyLimit)) {
            return false;
        }
        if (cooldownSeconds > 0) {
            lastPayouts.put(playerUuid + "|" + payoutKey, now);
        }
        return true;
    }
}
