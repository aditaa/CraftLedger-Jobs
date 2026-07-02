package dev.monkeycraft.craftledgerjobs;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class JobPayoutLimiter {
    private final Clock clock;
    private final Map<String, Instant> lastPayouts = new HashMap<>();
    private final Map<UUID, DailyTotal> dailyTotals = new HashMap<>();

    public JobPayoutLimiter() {
        this(Clock.systemUTC());
    }

    JobPayoutLimiter(Clock clock) {
        this.clock = clock;
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
        if (dailyLimit > 0) {
            LocalDate today = LocalDate.ofInstant(now, ZoneOffset.UTC);
            DailyTotal total = dailyTotals.compute(playerUuid, (ignored, existing) ->
                    existing == null || !existing.date.equals(today) ? new DailyTotal(today, 0) : existing);
            if (total.amount + payout > dailyLimit) {
                return false;
            }
            total.amount += payout;
        }
        if (cooldownSeconds > 0) {
            lastPayouts.put(playerUuid + "|" + payoutKey, now);
        }
        return true;
    }

    private static final class DailyTotal {
        private final LocalDate date;
        private double amount;

        private DailyTotal(LocalDate date, double amount) {
            this.date = date;
            this.amount = amount;
        }
    }
}
