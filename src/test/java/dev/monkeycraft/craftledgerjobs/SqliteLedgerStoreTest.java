package dev.monkeycraft.craftledgerjobs;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SqliteLedgerStoreTest {
    @TempDir
    Path tempDir;

    @Test
    void persistsPlayerBalancesJobsPayoutsAndTransactions() throws Exception {
        Path path = tempDir.resolve("craftledger.sqlite");
        UUID uuid = UUID.randomUUID();

        try (SqliteLedgerStore store = SqliteLedgerStore.load(path, 25.0D)) {
            assertEquals(25.0D, store.balance(uuid, "Ada"));
            store.add(uuid, "Ada", 10.0D);
            store.setJob(uuid, "Ada", "miner");
            assertTrue(store.allowAndRecord(uuid, Instant.parse("2026-07-02T12:00:00Z"), 5.0D, 10.0D));
            store.write("admin_balance_add", "Ada", uuid.toString(), 10.0D, "test");
        }

        try (SqliteLedgerStore store = SqliteLedgerStore.load(path, 99.0D)) {
            assertEquals(35.0D, store.balance(uuid, "Ignored"));
            assertEquals("miner", store.job(uuid, "Ada"));
            assertEquals(5.0D, store.total(uuid, LocalDate.parse("2026-07-02")));
            assertEquals(List.of(new PlayerStore.BalanceEntry("Ada", 35.0D)), store.topBalances());
            assertEquals(uuid, store.findKnownPlayer("ada").orElseThrow().uuid());
            assertEquals(1, store.tail("Ada", 10).size());
        }
    }

    @Test
    void dailyPayoutLimitIsEnforced() throws Exception {
        UUID uuid = UUID.randomUUID();

        try (SqliteLedgerStore store = SqliteLedgerStore.load(tempDir.resolve("craftledger.sqlite"), 0.0D)) {
            assertTrue(store.allowAndRecord(uuid, Instant.parse("2026-07-02T12:00:00Z"), 5.0D, 10.0D));
            assertTrue(store.allowAndRecord(uuid, Instant.parse("2026-07-02T13:00:00Z"), 5.0D, 10.0D));
            assertTrue(!store.allowAndRecord(uuid, Instant.parse("2026-07-02T14:00:00Z"), 0.01D, 10.0D));
        }
    }
}
