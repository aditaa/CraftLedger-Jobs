package dev.monkeycraft.craftledgerjobs;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerStoreTest {
    @TempDir
    Path tempDir;

    @Test
    void loadCreatesEmptyPlayerFile() throws Exception {
        Path path = tempDir.resolve("players.json");

        PlayerStore.load(path, 25.0D);

        assertTrue(Files.exists(path));
        assertTrue(Files.readString(path).contains("\"players\""));
    }

    @Test
    void newUuidAccountReceivesStartingBalanceAndPersists() throws Exception {
        Path path = tempDir.resolve("players.json");
        UUID uuid = UUID.randomUUID();

        PlayerStore store = PlayerStore.load(path, 25.0D);
        PlayerStore.PlayerAccount account = store.get(uuid, "Ada");
        account.job = "miner";
        store.save();

        PlayerStore reloaded = PlayerStore.load(path, 99.0D);
        PlayerStore.PlayerAccount reloadedAccount = reloaded.get(uuid, "Ignored");

        assertEquals("Ada", reloadedAccount.lastKnownName);
        assertEquals(25.0D, reloadedAccount.balance);
        assertEquals("miner", reloadedAccount.job);
    }

    @Test
    void adminBalanceOperationsRejectNegativeAndNonFiniteAmounts() throws Exception {
        PlayerStore store = PlayerStore.load(tempDir.resolve("players.json"), 10.0D);
        UUID uuid = UUID.randomUUID();

        store.add(uuid, "Ada", 5.0D);
        store.add(uuid, "Ada", -100.0D);
        store.add(uuid, "Ada", Double.NaN);
        store.add(uuid, "Ada", Double.POSITIVE_INFINITY);

        assertEquals(15.0D, store.get(uuid, "Ada").balance);

        store.set(uuid, "Ada", Double.NaN);
        assertEquals(0.0D, store.get(uuid, "Ada").balance);

        store.set(uuid, "Ada", 20.0D);
        store.take(uuid, "Ada", -5.0D);
        store.take(uuid, "Ada", Double.NaN);
        assertEquals(20.0D, store.get(uuid, "Ada").balance);

        store.take(uuid, "Ada", 200.0D);
        assertEquals(0.0D, store.get(uuid, "Ada").balance);
    }

    @Test
    void findsKnownPlayersByNameCaseInsensitivelyOrUuid() throws Exception {
        PlayerStore store = PlayerStore.load(tempDir.resolve("players.json"), 10.0D);
        UUID uuid = UUID.randomUUID();
        store.get(uuid, "Ada");

        assertEquals(uuid, store.findKnownPlayer("ada").orElseThrow().uuid());
        assertEquals(uuid, store.findKnownPlayer(uuid.toString()).orElseThrow().uuid());
        assertTrue(store.findKnownPlayer("missing").isEmpty());
    }

    @Test
    void knownPlayerNamesAreSortedAndUnique() throws Exception {
        PlayerStore store = PlayerStore.load(tempDir.resolve("players.json"), 10.0D);
        UUID adaOne = UUID.randomUUID();
        UUID adaTwo = UUID.randomUUID();
        UUID bert = UUID.randomUUID();
        store.get(bert, "Bert");
        store.get(adaOne, "Ada");
        store.get(adaTwo, "Ada");

        assertEquals(List.of("Ada", "Bert"), store.knownPlayerNames());
    }

    @Test
    void topBalancesSortsByBalanceThenName() throws Exception {
        PlayerStore store = PlayerStore.load(tempDir.resolve("players.json"), 10.0D);
        store.set(UUID.randomUUID(), "Bert", 100.0D);
        store.set(UUID.randomUUID(), "Ada", 100.0D);
        store.set(UUID.randomUUID(), "Cora", 250.0D);

        assertEquals(List.of(
                new PlayerStore.BalanceEntry("Cora", 250.0D),
                new PlayerStore.BalanceEntry("Ada", 100.0D),
                new PlayerStore.BalanceEntry("Bert", 100.0D)
        ), store.topBalances());
    }
}
