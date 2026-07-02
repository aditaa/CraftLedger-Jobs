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
        ItemStack hand = player.getMainHandItem();
        if (hand.isEmpty()) {
            return 0;
        }
        String itemId = itemId(hand.getItem());
        Double price = ledger.shopConfig().sellPrices.get(itemId);
        if (price == null || price <= 0) {
            return 0;
        }
        int count = hand.getCount();
        hand.setCount(0);
        double total = price * count;
        ledger.players().deposit(player, total);
        ledger.transactions().write("sell_hand", player, total, itemId + " x" + count);
        return total;
    }

    public double sellAll(ServerPlayer player) {
        double total = 0;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }
            String itemId = itemId(stack.getItem());
            Double price = ledger.shopConfig().sellPrices.get(itemId);
            if (price == null || price <= 0) {
                continue;
            }
            int count = stack.getCount();
            stack.setCount(0);
            total += price * count;
        }
        if (total > 0) {
            ledger.players().deposit(player, total);
            ledger.transactions().write("sell_all", player, total, "configured sellables");
        }
        return total;
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
}
