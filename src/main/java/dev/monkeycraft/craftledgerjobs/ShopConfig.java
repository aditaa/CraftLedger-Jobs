package dev.monkeycraft.craftledgerjobs;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ShopConfig {
    public Map<String, Double> sellPrices = new LinkedHashMap<>();
    public Map<String, BuyOffer> buyPrices = new LinkedHashMap<>();

    public static ShopConfig load(Path path) throws IOException {
        if (Files.notExists(path)) {
            ShopConfig config = defaults();
            JsonFiles.writeAtomic(path, config);
            return config;
        }
        ShopConfig config = JsonFiles.read(path, ShopConfig.class);
        return config == null ? defaults() : config.normalize();
    }

    public ShopConfig normalize() {
        if (sellPrices == null) {
            sellPrices = new LinkedHashMap<>();
        }
        if (buyPrices == null) {
            buyPrices = new LinkedHashMap<>();
        }
        return this;
    }

    private static ShopConfig defaults() {
        ShopConfig config = new ShopConfig();
        config.sellPrices.put("minecraft:cobblestone", 0.10D);
        config.sellPrices.put("minecraft:coal", 1.00D);
        config.sellPrices.put("minecraft:iron_ingot", 8.00D);
        config.sellPrices.put("minecraft:wheat", 0.50D);
        config.sellPrices.put("minecraft:oak_log", 0.75D);
        config.buyPrices.put("minecraft:bread", new BuyOffer(2.00D, 16));
        config.buyPrices.put("minecraft:torch", new BuyOffer(0.25D, 64));
        config.buyPrices.put("minecraft:iron_pickaxe", new BuyOffer(75.00D, 1));
        return config;
    }

    public static final class BuyOffer {
        public double price;
        public int maxStack;

        public BuyOffer(double price, int maxStack) {
            this.price = price;
            this.maxStack = maxStack;
        }
    }
}
