package dev.monkeycraft.craftledgerjobs;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public final class MessagesConfig {
    private MessagesConfig() {
    }

    public static void ensureExists(Path path) throws IOException {
        if (Files.notExists(path)) {
            JsonFiles.writeAtomic(path, Map.of(
                    "prefix", "[CraftLedger]",
                    "balance", "Balance: {balance}",
                    "paid", "Paid {target} {amount}",
                    "received", "Received {amount} from {source}"
            ));
        }
    }
}
