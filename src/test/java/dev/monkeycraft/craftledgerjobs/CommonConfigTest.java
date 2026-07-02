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
        assertTrue(config.currencyEnabled());
        assertEquals(100.0D, config.startingBalance());
        assertEquals("coins", config.currencyName());
        assertEquals("$", config.currencySymbol());
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
                """);

        CommonConfig config = CommonConfig.load(path);

        assertFalse(config.currencyEnabled());
        assertEquals(42.5D, config.startingBalance());
        assertEquals("tokens", config.currencyName());
        assertEquals("M$", config.currencySymbol());
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
}
