package dev.monkeycraft.craftledgerjobs;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShopConfigTest {
    @TempDir
    Path tempDir;

    @Test
    void loadCreatesDefaultShopWhenMissing() throws Exception {
        Path path = tempDir.resolve("shop.json");

        ShopConfig config = ShopConfig.load(path);

        assertTrue(Files.exists(path));
        assertEquals(0.10D, config.sellPrices.get("minecraft:cobblestone"));
        assertEquals(2.00D, config.buyPrices.get("minecraft:bread").price);
        assertEquals(16, config.buyPrices.get("minecraft:bread").maxStack);
    }

    @Test
    void loadNormalizesMissingMaps() throws Exception {
        Path path = tempDir.resolve("shop.json");
        Files.writeString(path, "{}");

        ShopConfig config = ShopConfig.load(path);

        assertNotNull(config.sellPrices);
        assertNotNull(config.buyPrices);
        assertTrue(config.sellPrices.isEmpty());
        assertTrue(config.buyPrices.isEmpty());
    }
}
