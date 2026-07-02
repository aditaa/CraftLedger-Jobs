package dev.monkeycraft.craftledgerjobs;

final class TestConfigs {
    private TestConfigs() {
    }

    static CommonConfig common(boolean currencyEnabled, double startingBalance, String currencyName, String currencySymbol) {
        return new CommonConfig(
                CommonConfig.CURRENT_VERSION,
                currencyEnabled,
                startingBalance,
                currencyName,
                currencySymbol,
                CommonConfig.STORAGE_JSON,
                "craftledger.sqlite"
        );
    }
}
