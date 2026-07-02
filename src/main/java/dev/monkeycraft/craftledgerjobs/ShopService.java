package dev.monkeycraft.craftledgerjobs;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ShopService {
    private final Ledger ledger;

    public ShopService(Ledger ledger) {
        this.ledger = ledger;
    }

    public double sellHand(ServerPlayer player) {
        return sellHand(player, Integer.MAX_VALUE).total();
    }

    public SellResult sellHand(ServerPlayer player, int requestedAmount) {
        ItemStack hand = player.getMainHandItem();
        if (hand.isEmpty()) {
            return SellResult.none("The item in your hand cannot be sold.");
        }
        String itemId = itemId(hand.getItem());
        Double price = ledger.shopConfig().sellPrices.get(itemId);
        if (price == null || price <= 0) {
            return SellResult.none("The item in your hand cannot be sold.");
        }
        int count = Math.min(hand.getCount(), Math.max(1, requestedAmount));
        double total = price * count;
        if (!Double.isFinite(total)) {
            return SellResult.none("That sale total is too large.");
        }
        if (!ledger.players().deposit(player, total)) {
            return SellResult.none("Your balance cannot receive that sale total.");
        }
        hand.shrink(count);
        ledger.transactions().write("sell_hand", player, total, itemId + " x" + count);
        return SellResult.success(total, count, List.of(new SoldItem(itemId, count, total)));
    }

    public double sellAll(ServerPlayer player) {
        return sellAll(player, null).total();
    }

    public SellResult sellAll(ServerPlayer player, String requestedItemId) {
        String normalizedFilter = requestedItemId == null || requestedItemId.isBlank() ? null : requestedItemId.toLowerCase(Locale.ROOT);
        double total = 0;
        int itemCount = 0;
        List<SoldItem> soldItems = new ArrayList<>();
        List<PendingSale> pendingSales = new ArrayList<>();
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }
            String itemId = itemId(stack.getItem());
            if (normalizedFilter != null && !normalizedFilter.equals(itemId)) {
                continue;
            }
            Double price = ledger.shopConfig().sellPrices.get(itemId);
            if (price == null || price <= 0) {
                continue;
            }
            int count = stack.getCount();
            double lineTotal = price * count;
            if (!Double.isFinite(lineTotal) || !Double.isFinite(total + lineTotal)) {
                return SellResult.none("That sale total is too large.");
            }
            total += lineTotal;
            itemCount += count;
            soldItems.add(new SoldItem(itemId, count, lineTotal));
            pendingSales.add(new PendingSale(slot));
        }
        if (total > 0) {
            if (!ledger.players().deposit(player, total)) {
                return SellResult.none("Your balance cannot receive that sale total.");
            }
            for (PendingSale pendingSale : pendingSales) {
                player.getInventory().getItem(pendingSale.slot()).setCount(0);
            }
            ledger.transactions().write("sell_all", player, total, normalizedFilter == null ? "configured sellables" : normalizedFilter);
        }
        return total > 0 ? SellResult.success(total, itemCount, soldItems) : SellResult.none("No configured sellable items found.");
    }

    public BuyResult buy(ServerPlayer player, String itemId, int amount) {
        String normalizedItemId = itemId.toLowerCase(Locale.ROOT);
        ShopConfig.BuyOffer offer = ledger.shopConfig().buyPrices.get(normalizedItemId);
        if (offer == null || offer.price <= 0) {
            return BuyResult.notForSale();
        }
        ResourceLocation id = ResourceLocation.tryParse(normalizedItemId);
        if (id == null || !BuiltInRegistries.ITEM.containsKey(id)) {
            return BuyResult.invalidItem();
        }
        Item item = BuiltInRegistries.ITEM.get(id);
        int clampedAmount = Math.max(1, amount);
        int itemMaxStack = new ItemStack(item).getMaxStackSize();
        int maxStack = offer.maxStack <= 0 ? itemMaxStack : Math.min(offer.maxStack, itemMaxStack);
        double total = offer.price * clampedAmount;
        if (!ledger.players().withdraw(player, total)) {
            return BuyResult.insufficientFunds(total);
        }

        boolean droppedItems = false;
        int remaining = clampedAmount;
        while (remaining > 0) {
            int count = Math.min(maxStack, remaining);
            ItemStack stack = new ItemStack(item, count);
            if (!player.getInventory().add(stack)) {
                player.drop(stack, false);
                droppedItems = true;
            }
            remaining -= count;
        }
        ledger.transactions().write("shop_buy", player, total, itemId + " x" + clampedAmount);
        return BuyResult.success(total, clampedAmount, droppedItems);
    }

    public String list(CommonConfig common) {
        return listBuy(common, 1);
    }

    public String listBuy(CommonConfig common, int page) {
        List<String> rows = new ArrayList<>();
        for (Map.Entry<String, ShopConfig.BuyOffer> entry : ledger.shopConfig().buyPrices.entrySet()) {
            rows.add(entry.getKey() + " - buy " + common.format(entry.getValue().price));
        }
        return PagedText.format("Shop buy items", rows, page, 8);
    }

    public String listSell(CommonConfig common, int page) {
        List<String> rows = new ArrayList<>();
        for (Map.Entry<String, Double> entry : ledger.shopConfig().sellPrices.entrySet()) {
            rows.add(entry.getKey() + " - sell " + common.format(entry.getValue()));
        }
        return PagedText.format("Shop sell items", rows, page, 8);
    }

    public String price(String itemId, CommonConfig common) {
        String normalizedItemId = itemId.toLowerCase(Locale.ROOT);
        ShopConfig.BuyOffer buyOffer = ledger.shopConfig().buyPrices.get(normalizedItemId);
        Double sellPrice = ledger.shopConfig().sellPrices.get(normalizedItemId);
        if (buyOffer == null && sellPrice == null) {
            return normalizedItemId + " is not configured in the shop.";
        }

        StringBuilder builder = new StringBuilder(normalizedItemId);
        if (buyOffer != null) {
            builder.append("\nBuy: ").append(common.format(buyOffer.price));
        }
        if (sellPrice != null) {
            builder.append("\nSell: ").append(common.format(sellPrice));
        }
        return builder.toString();
    }

    private static String itemId(Item item) {
        return BuiltInRegistries.ITEM.getKey(item).toString();
    }

    public record BuyResult(boolean success, String message, double total, int amount, boolean droppedItems) {
        static BuyResult success(double total, int amount, boolean droppedItems) {
            return new BuyResult(true, "ok", total, amount, droppedItems);
        }

        static BuyResult notForSale() {
            return new BuyResult(false, "That item is not for sale.", 0, 0, false);
        }

        static BuyResult invalidItem() {
            return new BuyResult(false, "That configured item id is invalid.", 0, 0, false);
        }

        static BuyResult insufficientFunds(double total) {
            return new BuyResult(false, "You need more money for that purchase.", total, 0, false);
        }
    }

    public record SellResult(boolean success, String message, double total, int itemCount, List<SoldItem> items) {
        static SellResult success(double total, int itemCount, List<SoldItem> items) {
            return new SellResult(true, "ok", total, itemCount, List.copyOf(items));
        }

        static SellResult none(String message) {
            return new SellResult(false, message, 0, 0, List.of());
        }
    }

    public record SoldItem(String itemId, int count, double total) {
    }

    private record PendingSale(int slot) {
    }
}
