package dev.monkeycraft.craftledgerjobs;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class StorageMigrationService {
    private static final DateTimeFormatter BACKUP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").withZone(ZoneOffset.UTC);

    public MigrationResult migrateJsonToSqlite(Path dataDir, String sqliteFile, double startingBalance, boolean dryRun) throws IOException {
        Path playersPath = dataDir.resolve("players.json");
        Path payoutsPath = dataDir.resolve("job_payouts.json");
        Path transactionsPath = dataDir.resolve("transactions.log");
        Path sqlitePath = dataDir.resolve(sqliteFile);

        PlayerFile players = readPlayers(playersPath);
        JobPayoutFile payouts = readPayouts(payoutsPath);
        int transactionCount = countTransactions(transactionsPath);
        int playerCount = players.players.size();
        int payoutCount = countPayoutTotals(payouts.dailyTotals);

        if (dryRun) {
            return new MigrationResult(true, true, playerCount, payoutCount, transactionCount, "Dry run passed. No files were changed.");
        }
        if (Files.exists(sqlitePath)) {
            throw new ConfigValidationException("SQLite file already exists: " + sqliteFile + ". Move it before migrating.");
        }

        Path backupDir = createBackup(dataDir, playersPath, payoutsPath, transactionsPath);
        try (SqliteLedgerStore sqlite = SqliteLedgerStore.load(sqlitePath, startingBalance)) {
            if (sqlite.hasImportedData()) {
                throw new ConfigValidationException("SQLite file is not empty: " + sqliteFile);
            }
            importPlayers(sqlite, players);
            importPayouts(sqlite, payouts);
            importTransactions(sqlite, transactionsPath);
        }
        return new MigrationResult(true, false, playerCount, payoutCount, transactionCount,
                "Migrated JSON data to " + sqliteFile + ". Backup: " + backupDir.getFileName() + ". Set storageBackend=\"sqlite\" and restart the server.");
    }

    private static PlayerFile readPlayers(Path path) throws IOException {
        if (Files.notExists(path)) {
            return new PlayerFile();
        }
        PlayerFile file = JsonFiles.read(path, PlayerFile.class);
        if (file == null || file.players == null) {
            return new PlayerFile();
        }
        return file;
    }

    private static JobPayoutFile readPayouts(Path path) throws IOException {
        if (Files.notExists(path)) {
            return new JobPayoutFile();
        }
        JobPayoutFile file = JsonFiles.read(path, JobPayoutFile.class);
        if (file == null || file.dailyTotals == null) {
            return new JobPayoutFile();
        }
        return file;
    }

    private static void importPlayers(SqliteLedgerStore sqlite, PlayerFile players) {
        for (Map.Entry<String, PlayerStore.PlayerAccount> entry : players.players.entrySet()) {
            UUID uuid = parseUuid("players.json", entry.getKey());
            PlayerStore.PlayerAccount account = entry.getValue();
            if (account == null) {
                continue;
            }
            sqlite.importPlayer(uuid, account.lastKnownName, account.balance, account.job, account.initialized);
            if (account.jobLevels != null) {
                for (Map.Entry<String, Integer> progress : account.jobLevels.entrySet()) {
                    double xp = account.jobExperience == null ? 0.0D : account.jobExperience.getOrDefault(progress.getKey(), 0.0D);
                    sqlite.importJobProgress(uuid, progress.getKey(), progress.getValue(), xp);
                }
            }
        }
    }

    private static void importPayouts(SqliteLedgerStore sqlite, JobPayoutFile payouts) {
        for (Map.Entry<String, Map<String, Double>> dayEntry : payouts.dailyTotals.entrySet()) {
            LocalDate day = LocalDate.parse(dayEntry.getKey());
            for (Map.Entry<String, Double> playerEntry : dayEntry.getValue().entrySet()) {
                sqlite.importJobPayoutTotal(parseUuid("job_payouts.json", playerEntry.getKey()), day, playerEntry.getValue());
            }
        }
    }

    private static void importTransactions(SqliteLedgerStore sqlite, Path path) throws IOException {
        if (Files.notExists(path)) {
            return;
        }
        for (String line : Files.readAllLines(path)) {
            if (line.isBlank()) {
                continue;
            }
            String[] parts = line.split("\t", -1);
            if (parts.length < 6) {
                throw new ConfigValidationException("transactions.log contains a malformed line: " + line);
            }
            double amount;
            try {
                amount = Double.parseDouble(parts[4]);
            } catch (NumberFormatException ex) {
                throw new ConfigValidationException("transactions.log contains an invalid amount: " + parts[4]);
            }
            sqlite.writeRaw(parts[0], parts[1], parts[2], parts[3], amount, parts[5]);
        }
    }

    private static int countTransactions(Path path) throws IOException {
        if (Files.notExists(path)) {
            return 0;
        }
        int count = 0;
        for (String line : Files.readAllLines(path)) {
            if (!line.isBlank()) {
                count++;
            }
        }
        return count;
    }

    private static int countPayoutTotals(Map<String, Map<String, Double>> dailyTotals) {
        return dailyTotals.values().stream().mapToInt(Map::size).sum();
    }

    private static Path createBackup(Path dataDir, Path... paths) throws IOException {
        Path backupDir = dataDir.resolve("migration-backup-" + BACKUP_FORMAT.format(ZonedDateTime.now(ZoneOffset.UTC)));
        Files.createDirectories(backupDir);
        for (Path path : paths) {
            if (Files.exists(path)) {
                Files.copy(path, backupDir.resolve(path.getFileName()), StandardCopyOption.COPY_ATTRIBUTES);
            }
        }
        return backupDir;
    }

    private static UUID parseUuid(String source, String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ex) {
            throw new ConfigValidationException(source + " contains invalid UUID: " + value);
        }
    }

    public record MigrationResult(boolean success, boolean dryRun, int players, int payoutTotals, int transactions, String message) {
        public String summary() {
            String mode = dryRun ? "Migration dry run" : "Migration complete";
            return mode + ": " + players + " player(s), " + payoutTotals + " payout total(s), " + transactions + " transaction(s). " + message;
        }
    }

    private static final class PlayerFile {
        public Map<String, PlayerStore.PlayerAccount> players = new LinkedHashMap<>();
    }

    private static final class JobPayoutFile {
        public Map<String, Map<String, Double>> dailyTotals = new LinkedHashMap<>();
    }
}
