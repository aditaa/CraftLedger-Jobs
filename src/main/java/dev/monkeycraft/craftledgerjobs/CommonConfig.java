package dev.monkeycraft.craftledgerjobs;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public record CommonConfig(double startingBalance, String currencyName, String currencySymbol) {
    public static CommonConfig load(Path path) throws IOException {
        if (Files.notExists(path)) {
            Files.writeString(path, """
                    # CraftLedger Jobs common config
                    startingBalance = 100.0
                    currencyName = "coins"
                    currencySymbol = "$"
                    """, StandardCharsets.UTF_8);
        }

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
            if ("startingBalance".equals(key)) {
                startingBalance = Double.parseDouble(value);
            } else if ("currencyName".equals(key)) {
                currencyName = value;
            } else if ("currencySymbol".equals(key)) {
                currencySymbol = value;
            }
        }
        return new CommonConfig(startingBalance, currencyName, currencySymbol);
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
}
