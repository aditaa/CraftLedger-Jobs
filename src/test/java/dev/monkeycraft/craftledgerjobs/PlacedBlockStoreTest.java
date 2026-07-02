package dev.monkeycraft.craftledgerjobs;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlacedBlockStoreTest {
    @TempDir
    Path tempDir;

    @Test
    void loadCreatesDefaultFile() throws Exception {
        Path path = tempDir.resolve("placed_blocks.json");

        PlacedBlockStore.load(path);

        assertTrue(Files.exists(path));
    }

    @Test
    void recordsConsumesAndPersistsPlacedBlocks() throws Exception {
        Path path = tempDir.resolve("placed_blocks.json");
        PlacedBlockStore store = PlacedBlockStore.load(path);

        store.record("minecraft:overworld:1", 10);
        store.record("minecraft:overworld:2", 10);

        PlacedBlockStore reloaded = PlacedBlockStore.load(path);

        assertTrue(reloaded.consume("minecraft:overworld:1"));
        assertFalse(reloaded.consume("minecraft:overworld:1"));
        assertTrue(reloaded.consume("minecraft:overworld:2"));
    }

    @Test
    void evictsOldestEntriesAtConfiguredLimit() throws Exception {
        PlacedBlockStore store = PlacedBlockStore.load(tempDir.resolve("placed_blocks.json"));

        store.record("one", 2);
        store.record("two", 2);
        store.record("three", 2);

        assertFalse(store.consume("one"));
        assertTrue(store.consume("two"));
        assertTrue(store.consume("three"));
    }
}
