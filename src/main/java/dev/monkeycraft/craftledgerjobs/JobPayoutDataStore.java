package dev.monkeycraft.craftledgerjobs;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public interface JobPayoutDataStore {
    boolean allowAndRecord(UUID playerUuid, Instant instant, double payout, double dailyLimit);

    double total(UUID playerUuid, LocalDate date);
}
