package dev.monkeycraft.craftledgerjobs;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

public final class ShopConfig {
    private static final Pattern RESOURCE_ID = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+");

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
        validate();
        return this;
    }

    private void validate() {
        sellPrices.forEach((itemId, price) -> {
            validateResourceId("shop.json sellPrices", itemId);
            validatePositiveFinite("shop.json sell price for " + itemId, price);
        });
        buyPrices.forEach((itemId, offer) -> {
            validateResourceId("shop.json buyPrices", itemId);
            if (offer == null) {
                throw new ConfigValidationException("shop.json buy offer for " + itemId + " must not be null.");
            }
            validatePositiveFinite("shop.json buy price for " + itemId, offer.price);
            if (offer.maxStack < 0) {
                throw new ConfigValidationException("shop.json maxStack for " + itemId + " must be greater than or equal to 0.");
            }
        });
    }

    private static void validateResourceId(String section, String value) {
        if (value == null || !RESOURCE_ID.matcher(value).matches()) {
            throw new ConfigValidationException(section + " contains invalid item id: " + value);
        }
    }

    private static void validatePositiveFinite(String label, double value) {
        if (!Double.isFinite(value) || value <= 0) {
            throw new ConfigValidationException(label + " must be a finite number greater than 0.");
        }
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
