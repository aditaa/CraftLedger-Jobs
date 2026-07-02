package dev.monkeycraft.craftledgerjobs;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommonConfigTest {
    @TempDir
    Path tempDir;

    @Test
    void loadCreatesDefaultConfigWhenMissing() throws Exception {
        Path path = tempDir.resolve("common.toml");

        CommonConfig config = CommonConfig.load(path);

        assertTrue(Files.exists(path));
        assertEquals(1, config.configVersion());
        assertTrue(config.currencyEnabled());
        assertEquals(100.0D, config.startingBalance());
        assertEquals("coins", config.currencyName());
        assertEquals("$", config.currencySymbol());
        assertEquals("json", config.storageBackend());
        assertEquals("craftledger.sqlite", config.sqliteFile());
        assertEquals(0.0D, config.maxBalance());
        assertEquals(0.0D, config.maxPayAmount());
        assertEquals(0, config.payCooldownSeconds());
        assertEquals("$1,234.50 coins", config.format(1234.5D));
    }

    @Test
    void loadParsesConfiguredValuesAndIgnoresComments() throws Exception {
        Path path = tempDir.resolve("common.toml");
        Files.writeString(path, """
                # comment
                currencyEnabled = false
                startingBalance = 42.5 # inline comment
                currencyName = "tokens"
                currencySymbol = "M$"
                storageBackend = "sqlite"
                sqliteFile = "ledger.sqlite"
                maxBalance = 1000.0
                maxPayAmount = 50.0
                payCooldownSeconds = 3
                """);

        CommonConfig config = CommonConfig.load(path);

        assertFalse(config.currencyEnabled());
        assertEquals(42.5D, config.startingBalance());
        assertEquals("tokens", config.currencyName());
        assertEquals("M$", config.currencySymbol());
        assertEquals("sqlite", config.storageBackend());
        assertEquals("ledger.sqlite", config.sqliteFile());
        assertEquals(1000.0D, config.maxBalance());
        assertEquals(50.0D, config.maxPayAmount());
        assertEquals(3, config.payCooldownSeconds());
        assertEquals("M$42.50 tokens", config.format(42.5D));
    }

    @Test
    void loadRejectsInvalidStartingBalance() throws Exception {
        Path path = tempDir.resolve("common.toml");
        Files.writeString(path, """
                startingBalance = -1
                currencyName = "coins"
                currencySymbol = "$"
                """);

        assertThrows(ConfigValidationException.class, () -> CommonConfig.load(path));
    }

    @Test
    void loadRejectsBlankCurrencyName() throws Exception {
        Path path = tempDir.resolve("common.toml");
        Files.writeString(path, """
                startingBalance = 1
                currencyName = ""
                currencySymbol = "$"
                """);

        assertThrows(ConfigValidationException.class, () -> CommonConfig.load(path));
    }

    @Test
    void loadRejectsNonNumericStartingBalance() throws Exception {
        Path path = tempDir.resolve("common.toml");
        Files.writeString(path, """
                startingBalance = many
                currencyName = "coins"
                currencySymbol = "$"
                """);

        assertThrows(ConfigValidationException.class, () -> CommonConfig.load(path));
    }

    @Test
    void loadRejectsBlankCurrencySymbol() throws Exception {
        Path path = tempDir.resolve("common.toml");
        Files.writeString(path, """
                startingBalance = 1
                currencyName = "coins"
                currencySymbol = ""
                """);

        assertThrows(ConfigValidationException.class, () -> CommonConfig.load(path));
    }

    @Test
    void loadRejectsInvalidCurrencyEnabledFlag() throws Exception {
        Path path = tempDir.resolve("common.toml");
        Files.writeString(path, """
                currencyEnabled = maybe
                startingBalance = 1
                currencyName = "coins"
                currencySymbol = "$"
                """);

        assertThrows(ConfigValidationException.class, () -> CommonConfig.load(path));
    }

    @Test
    void loadRejectsInvalidStorageBackend() throws Exception {
        Path path = tempDir.resolve("common.toml");
        Files.writeString(path, """
                storageBackend = "mysql"
                startingBalance = 1
                currencyName = "coins"
                currencySymbol = "$"
                """);

        assertThrows(ConfigValidationException.class, () -> CommonConfig.load(path));
    }

    @Test
    void loadRejectsSqlitePathTraversal() throws Exception {
        Path path = tempDir.resolve("common.toml");
        Files.writeString(path, """
                storageBackend = "sqlite"
                sqliteFile = "../ledger.sqlite"
                startingBalance = 1
                currencyName = "coins"
                currencySymbol = "$"
                """);

        assertThrows(ConfigValidationException.class, () -> CommonConfig.load(path));
    }

    @Test
    void loadRejectsInvalidEconomySafetyValues() throws Exception {
        Path path = tempDir.resolve("common.toml");
        Files.writeString(path, """
                maxBalance = -1
                currencyName = "coins"
                currencySymbol = "$"
                """);

        assertThrows(ConfigValidationException.class, () -> CommonConfig.load(path));

        Files.writeString(path, """
                maxPayAmount = nope
                currencyName = "coins"
                currencySymbol = "$"
                """);

        assertThrows(ConfigValidationException.class, () -> CommonConfig.load(path));

        Files.writeString(path, """
                payCooldownSeconds = -1
                currencyName = "coins"
                currencySymbol = "$"
                """);

        assertThrows(ConfigValidationException.class, () -> CommonConfig.load(path));
    }
}
