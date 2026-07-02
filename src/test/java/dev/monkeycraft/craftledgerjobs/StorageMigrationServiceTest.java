package dev.monkeycraft.craftledgerjobs;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StorageMigrationServiceTest {
    @TempDir
    Path tempDir;

    @Test
    void dryRunCountsJsonDataWithoutCreatingSqlite() throws Exception {
        UUID uuid = UUID.randomUUID();
        writeJsonData(uuid);

        StorageMigrationService.MigrationResult result = new StorageMigrationService()
                .migrateJsonToSqlite(tempDir, "craftledger.sqlite", 100.0D, true);

        assertTrue(result.dryRun());
        assertEquals(1, result.players());
        assertEquals(1, result.payoutTotals());
        assertEquals(1, result.transactions());
        assertFalse(Files.exists(tempDir.resolve("craftledger.sqlite")));
    }

    @Test
    void migratesJsonDataToSqliteAndCreatesBackup() throws Exception {
        UUID uuid = UUID.randomUUID();
        writeJsonData(uuid);

        StorageMigrationService.MigrationResult result = new StorageMigrationService()
                .migrateJsonToSqlite(tempDir, "craftledger.sqlite", 100.0D, false);

        assertFalse(result.dryRun());
        assertTrue(Files.exists(tempDir.resolve("craftledger.sqlite")));
        assertEquals(1, Files.list(tempDir).filter(path -> path.getFileName().toString().startsWith("migration-backup-")).count());

        try (SqliteLedgerStore sqlite = SqliteLedgerStore.load(tempDir.resolve("craftledger.sqlite"), 0.0D)) {
            assertEquals(250.0D, sqlite.balance(uuid, "Ignored"));
            assertEquals("miner", sqlite.job(uuid, "Ada"));
            assertEquals(12.5D, sqlite.total(uuid, LocalDate.parse("2026-07-02")));
            assertTrue(sqlite.tail(1).get(0).startsWith("2026-07-02T12:00:00Z\tadmin_balance_set"));
        }
    }

    @Test
    void liveMigrationRefusesExistingSqliteFile() throws Exception {
        writeJsonData(UUID.randomUUID());
        Files.writeString(tempDir.resolve("craftledger.sqlite"), "existing");

        assertThrows(ConfigValidationException.class, () -> new StorageMigrationService()
                .migrateJsonToSqlite(tempDir, "craftledger.sqlite", 100.0D, false));
    }

    private void writeJsonData(UUID uuid) throws Exception {
        Files.writeString(tempDir.resolve("players.json"), """
                {
                  "version": 1,
                  "players": {
                    "%s": {
                      "lastKnownName": "Ada",
                      "balance": 250.0,
                      "job": "miner",
                      "initialized": true
                    }
                  }
                }
                """.formatted(uuid));
        Files.writeString(tempDir.resolve("job_payouts.json"), """
                {
                  "version": 1,
                  "dailyTotals": {
                    "2026-07-02": {
                      "%s": 12.5
                    }
                  }
                }
                """.formatted(uuid));
        Files.writeString(tempDir.resolve("transactions.log"), "2026-07-02T12:00:00Z\tadmin_balance_set\tAda\t%s\t250.00\ttest%n".formatted(uuid));
    }
}
