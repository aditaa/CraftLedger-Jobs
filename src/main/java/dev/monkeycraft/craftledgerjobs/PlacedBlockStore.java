package dev.monkeycraft.craftledgerjobs;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class PlacedBlockStore {
    private final Path path;
    private final LinkedHashSet<String> placedBlocks;

    private PlacedBlockStore(Path path, LinkedHashSet<String> placedBlocks) {
        this.path = path;
        this.placedBlocks = placedBlocks;
    }

    public static PlacedBlockStore load(Path path) throws IOException {
        if (Files.notExists(path)) {
            PlacedBlockStore store = new PlacedBlockStore(path, new LinkedHashSet<>());
            store.save();
            return store;
        }
        PlacedBlockFile file = JsonFiles.read(path, PlacedBlockFile.class);
        LinkedHashSet<String> keys = new LinkedHashSet<>(file == null || file.placedBlocks == null ? List.of() : file.placedBlocks);
        return new PlacedBlockStore(path, keys);
    }

    public synchronized void record(ServerLevel level, BlockPos pos, int maxEntries) {
        record(key(level, pos), maxEntries);
    }

    synchronized void record(String key, int maxEntries) {
        if (maxEntries <= 0) {
            return;
        }
        placedBlocks.remove(key);
        placedBlocks.add(key);
        while (placedBlocks.size() > maxEntries) {
            String oldest = placedBlocks.iterator().next();
            placedBlocks.remove(oldest);
        }
        save();
    }

    public synchronized boolean consume(ServerLevel level, BlockPos pos) {
        return consume(key(level, pos));
    }

    synchronized boolean consume(String key) {
        boolean removed = placedBlocks.remove(key);
        if (removed) {
            save();
        }
        return removed;
    }

    public synchronized int size() {
        return placedBlocks.size();
    }

    public synchronized void save() {
        try {
            JsonFiles.writeAtomic(path, new PlacedBlockFile(List.copyOf(placedBlocks)));
        } catch (IOException ex) {
            CraftLedgerJobs.LOGGER.error("Failed to save CraftLedger placed block data", ex);
        }
    }

    private static String key(ServerLevel level, BlockPos pos) {
        return level.dimension().location() + ":" + pos.asLong();
    }

    private static final class PlacedBlockFile {
        public int version = 1;
        public List<String> placedBlocks;

        PlacedBlockFile(List<String> placedBlocks) {
            this.placedBlocks = placedBlocks;
        }
    }
}
