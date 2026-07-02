package dev.monkeycraft.craftledgerjobs;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public record CommonConfig(boolean currencyEnabled, double startingBalance, String currencyName, String currencySymbol) {
    public static CommonConfig load(Path path) throws IOException {
        if (Files.notExists(path)) {
            Files.writeString(path, """
                    # CraftLedger Jobs common config
                    currencyEnabled = true
                    startingBalance = 100.0
                    currencyName = "coins"
                    currencySymbol = "$"
                    """, StandardCharsets.UTF_8);
        }

        boolean currencyEnabled = true;
        double startingBalance = 100.0D;
        String currencyName = "coins";
        String currencySymbol = "$";
        for (String rawLine : Files.readAllLines(path, StandardCharsets.UTF_8)) {
            String line = rawLine.split("#", 2)[0].trim();
            if (line.isEmpty() || !line.contains("=")) {
                continue;
            }
            String[] parts = line.split("=", 2);
            String key = parts[0].trim();
            String value = stripQuotes(parts[1].trim());
            if ("currencyEnabled".equals(key)) {
                currencyEnabled = parseBoolean("common.toml currencyEnabled", value);
            } else if ("startingBalance".equals(key)) {
                try {
                    startingBalance = Double.parseDouble(value);
                } catch (NumberFormatException ex) {
                    throw new ConfigValidationException("common.toml startingBalance must be a number: " + value);
                }
            } else if ("currencyName".equals(key)) {
                currencyName = value;
            } else if ("currencySymbol".equals(key)) {
                currencySymbol = value;
            }
        }
        CommonConfig config = new CommonConfig(currencyEnabled, startingBalance, currencyName, currencySymbol);
        config.validate();
        return config;
    }

    public String format(double amount) {
        return currencySymbol + String.format("%,.2f", amount) + " " + currencyName;
    }

    private static String stripQuotes(String value) {
        if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    private static boolean parseBoolean(String label, String value) {
        if ("true".equalsIgnoreCase(value)) {
            return true;
        }
        if ("false".equalsIgnoreCase(value)) {
            return false;
        }
        throw new ConfigValidationException(label + " must be true or false.");
    }

    private void validate() {
        if (!Double.isFinite(startingBalance) || startingBalance < 0) {
            throw new ConfigValidationException("common.toml startingBalance must be a finite number greater than or equal to 0.");
        }
        if (currencyName == null || currencyName.isBlank()) {
            throw new ConfigValidationException("common.toml currencyName must not be blank.");
        }
        if (currencySymbol == null || currencySymbol.isBlank()) {
            throw new ConfigValidationException("common.toml currencySymbol must not be blank.");
        }
    }
}
