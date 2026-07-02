package dev.monkeycraft.craftledgerjobs;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class JobPayoutStore implements JobPayoutDataStore {
    private final Path path;
    private final Map<String, Map<String, Double>> dailyTotals;

    private JobPayoutStore(Path path, Map<String, Map<String, Double>> dailyTotals) {
        this.path = path;
        this.dailyTotals = dailyTotals;
    }

    public static JobPayoutStore load(Path path) throws IOException {
        if (Files.notExists(path)) {
            JobPayoutStore store = new JobPayoutStore(path, new LinkedHashMap<>());
            store.save();
            return store;
        }
        JobPayoutFile file = JsonFiles.read(path, JobPayoutFile.class);
        Map<String, Map<String, Double>> totals = file == null || file.dailyTotals == null ? new LinkedHashMap<>() : file.dailyTotals;
        return new JobPayoutStore(path, totals);
    }

    @Override
    public boolean allowAndRecord(UUID playerUuid, Instant instant, double payout, double dailyLimit) {
        if (dailyLimit <= 0) {
            return true;
        }
        String date = LocalDate.ofInstant(instant, ZoneOffset.UTC).toString();
        String uuid = playerUuid.toString();
        Map<String, Double> day = dailyTotals.computeIfAbsent(date, ignored -> new LinkedHashMap<>());
        double current = day.getOrDefault(uuid, 0.0D);
        double next = current + payout;
        if (!Double.isFinite(next) || next > dailyLimit) {
            return false;
        }
        day.put(uuid, next);
        if (save()) {
            return true;
        }
        if (current == 0.0D) {
            day.remove(uuid);
        } else {
            day.put(uuid, current);
        }
        return false;
    }

    @Override
    public double total(UUID playerUuid, LocalDate date) {
        return dailyTotals.getOrDefault(date.toString(), Map.of()).getOrDefault(playerUuid.toString(), 0.0D);
    }

    public boolean save() {
        try {
            JsonFiles.writeAtomic(path, new JobPayoutFile(dailyTotals));
            return true;
        } catch (IOException ex) {
            CraftLedgerJobs.LOGGER.error("Failed to save CraftLedger job payout data", ex);
            return false;
        }
    }

    private static final class JobPayoutFile {
        public int version = 1;
        public Map<String, Map<String, Double>> dailyTotals;

        JobPayoutFile(Map<String, Map<String, Double>> dailyTotals) {
            this.dailyTotals = dailyTotals;
        }
    }
}
