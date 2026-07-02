package dev.monkeycraft.craftledgerjobs;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public record CommonConfig(
        int configVersion,
        boolean currencyEnabled,
        double startingBalance,
        String currencyName,
        String currencySymbol,
        String storageBackend,
        String sqliteFile,
        double maxBalance,
        double maxPayAmount,
        int payCooldownSeconds
) {
    public static final int CURRENT_VERSION = 1;
    public static final String STORAGE_JSON = "json";
    public static final String STORAGE_SQLITE = "sqlite";

    public static CommonConfig load(Path path) throws IOException {
        if (Files.notExists(path)) {
            Files.writeString(path, """
                    # CraftLedger Jobs common config
                    configVersion = 1
                    currencyEnabled = true
                    startingBalance = 100.0
                    currencyName = "coins"
                    currencySymbol = "$"
                    storageBackend = "json"
                    sqliteFile = "craftledger.sqlite"
                    maxBalance = 0.0
                    maxPayAmount = 0.0
                    payCooldownSeconds = 0
                    """, StandardCharsets.UTF_8);
        }

        int configVersion = CURRENT_VERSION;
        boolean currencyEnabled = true;
        double startingBalance = 100.0D;
        String currencyName = "coins";
        String currencySymbol = "$";
        String storageBackend = STORAGE_JSON;
        String sqliteFile = "craftledger.sqlite";
        double maxBalance = 0.0D;
        double maxPayAmount = 0.0D;
        int payCooldownSeconds = 0;
        for (String rawLine : Files.readAllLines(path, StandardCharsets.UTF_8)) {
            String line = rawLine.split("#", 2)[0].trim();
            if (line.isEmpty() || !line.contains("=")) {
                continue;
            }
            String[] parts = line.split("=", 2);
            String key = parts[0].trim();
            String value = stripQuotes(parts[1].trim());
            if ("configVersion".equals(key)) {
                try {
                    configVersion = Integer.parseInt(value);
                } catch (NumberFormatException ex) {
                    throw new ConfigValidationException("common.toml configVersion must be a whole number: " + value);
                }
            } else if ("currencyEnabled".equals(key)) {
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
            } else if ("storageBackend".equals(key)) {
                storageBackend = value.toLowerCase(java.util.Locale.ROOT);
            } else if ("sqliteFile".equals(key)) {
                sqliteFile = value;
            } else if ("maxBalance".equals(key)) {
                maxBalance = parseDouble("common.toml maxBalance", value);
            } else if ("maxPayAmount".equals(key)) {
                maxPayAmount = parseDouble("common.toml maxPayAmount", value);
            } else if ("payCooldownSeconds".equals(key)) {
                try {
                    payCooldownSeconds = Integer.parseInt(value);
                } catch (NumberFormatException ex) {
                    throw new ConfigValidationException("common.toml payCooldownSeconds must be a whole number: " + value);
                }
            }
        }
        CommonConfig config = new CommonConfig(configVersion, currencyEnabled, startingBalance, currencyName, currencySymbol, storageBackend, sqliteFile, maxBalance, maxPayAmount, payCooldownSeconds);
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

    private static double parseDouble(String label, String value) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException ex) {
            throw new ConfigValidationException(label + " must be a number: " + value);
        }
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
        if (configVersion < 1) {
            throw new ConfigValidationException("common.toml configVersion must be greater than or equal to 1.");
        }
        if (!STORAGE_JSON.equals(storageBackend) && !STORAGE_SQLITE.equals(storageBackend)) {
            throw new ConfigValidationException("common.toml storageBackend must be \"json\" or \"sqlite\".");
        }
        if (sqliteFile == null || sqliteFile.isBlank()) {
            throw new ConfigValidationException("common.toml sqliteFile must not be blank.");
        }
        if (sqliteFile.contains("/") || sqliteFile.contains("\\") || sqliteFile.contains("..")) {
            throw new ConfigValidationException("common.toml sqliteFile must be a file name, not a path.");
        }
        if (!Double.isFinite(maxBalance) || maxBalance < 0) {
            throw new ConfigValidationException("common.toml maxBalance must be finite and greater than or equal to 0.");
        }
        if (!Double.isFinite(maxPayAmount) || maxPayAmount < 0) {
            throw new ConfigValidationException("common.toml maxPayAmount must be finite and greater than or equal to 0.");
        }
        if (payCooldownSeconds < 0) {
            throw new ConfigValidationException("common.toml payCooldownSeconds must be greater than or equal to 0.");
        }
    }
}
