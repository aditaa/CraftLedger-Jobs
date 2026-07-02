package dev.monkeycraft.craftledgerjobs;

import java.util.ArrayList;
import java.util.List;

final class BalanceViews {
    private static final int PAGE_SIZE = 10;

    private BalanceViews() {
    }

    static String topBalances(List<PlayerStore.BalanceEntry> balances, CommonConfig common, int page) {
        List<String> rows = new ArrayList<>();
        for (int index = 0; index < balances.size(); index++) {
            PlayerStore.BalanceEntry entry = balances.get(index);
            rows.add((index + 1) + ". " + entry.name() + " - " + common.format(entry.balance()));
        }
        return PagedText.format("Top balances", rows, page, PAGE_SIZE);
    }
}
