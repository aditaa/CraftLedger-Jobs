package dev.monkeycraft.craftledgerjobs;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BalanceViewsTest {
    @Test
    void formatsTopBalancesInRankOrderProvidedByStore() {
        String output = BalanceViews.topBalances(List.of(
                new PlayerStore.BalanceEntry("Ada", 250),
                new PlayerStore.BalanceEntry("Bert", 100)
        ), new CommonConfig(true, 0, "coins", "$"), 1);

        assertEquals("""
                Top balances (page 1/1):
                1. Ada - $250.00 coins
                2. Bert - $100.00 coins""", output);
    }

    @Test
    void handlesNoStoredBalances() {
        assertEquals("No top balances configured.", BalanceViews.topBalances(List.of(), new CommonConfig(true, 0, "coins", "$"), 1));
    }
}
