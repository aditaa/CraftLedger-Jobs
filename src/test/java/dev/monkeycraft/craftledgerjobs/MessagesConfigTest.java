package dev.monkeycraft.craftledgerjobs;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MessagesConfigTest {
    @TempDir
    Path tempDir;

    @Test
    void loadCreatesDefaults() throws Exception {
        Path path = tempDir.resolve("messages.json");

        MessagesConfig config = MessagesConfig.load(path);

        assertTrue(Files.exists(path));
        assertEquals("Currency is disabled.", config.get("currency.disabled"));
    }

    @Test
    void loadMergesMissingDefaultsAndFormatsPlaceholders() throws Exception {
        Path path = tempDir.resolve("messages.json");
        Files.writeString(path, """
                {
                  "version": 1,
                  "messages": {
                    "balance.self": "Wallet: {balance}"
                  }
                }
                """);

        MessagesConfig config = MessagesConfig.load(path);

        assertEquals("Wallet: $5.00 coins", config.format("balance.self", Map.of("balance", "$5.00 coins")));
        assertEquals("Jobs are disabled.", config.get("jobs.disabled"));
    }

    @Test
    void loadRejectsInvalidKeys() throws Exception {
        Path path = tempDir.resolve("messages.json");
        Files.writeString(path, """
                {
                  "version": 1,
                  "messages": {
                    "Bad Key": "no"
                  }
                }
                """);

        assertThrows(ConfigValidationException.class, () -> MessagesConfig.load(path));
    }
}
