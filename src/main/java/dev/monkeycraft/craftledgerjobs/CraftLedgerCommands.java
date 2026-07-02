package dev.monkeycraft.craftledgerjobs;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public final class CraftLedgerCommands {
    private CraftLedgerCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, Ledger ledger) {
        dispatcher.register(Commands.literal("balance")
                .requires(source -> source.getEntity() instanceof ServerPlayer)
                .executes(ctx -> balance(ctx.getSource().getPlayerOrException(), ledger))
                .then(Commands.argument("player", EntityArgument.player())
                        .requires(CraftLedgerPermissions::canViewOtherBalances)
                        .executes(ctx -> balanceOther(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), ledger))));

        dispatcher.register(Commands.literal("money")
                .requires(source -> source.getEntity() instanceof ServerPlayer)
                .executes(ctx -> balance(ctx.getSource().getPlayerOrException(), ledger)));

        dispatcher.register(Commands.literal("baltop")
                .requires(CraftLedgerPermissions::canViewBalanceTop)
                .executes(ctx -> balanceTop(ctx.getSource(), 1, ledger))
                .then(Commands.argument("page", IntegerArgumentType.integer(1))
                        .executes(ctx -> balanceTop(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "page"), ledger))));

        dispatcher.register(Commands.literal("pay")
                .requires(source -> source.getEntity() instanceof ServerPlayer)
                .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.01D))
                                .executes(ctx -> pay(ctx.getSource().getPlayerOrException(), EntityArgument.getPlayer(ctx, "player"), DoubleArgumentType.getDouble(ctx, "amount"), ledger)))));

        dispatcher.register(Commands.literal("sell")
                .requires(source -> source.getEntity() instanceof ServerPlayer)
                .then(Commands.literal("hand")
                        .executes(ctx -> sellHand(ctx.getSource().getPlayerOrException(), Integer.MAX_VALUE, ledger))
                        .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                .executes(ctx -> sellHand(ctx.getSource().getPlayerOrException(), IntegerArgumentType.getInteger(ctx, "amount"), ledger))))
                .then(Commands.literal("all")
                        .executes(ctx -> sellAll(ctx.getSource().getPlayerOrException(), null, ledger))
                        .then(Commands.argument("item_id", ResourceLocationArgument.id())
                                .suggests((ctx, builder) -> suggestSellItems(ledger, builder))
                                .executes(ctx -> sellAll(ctx.getSource().getPlayerOrException(), ResourceLocationArgument.getId(ctx, "item_id").toString(), ledger)))));

        dispatcher.register(Commands.literal("shop")
                .requires(source -> source.getEntity() instanceof ServerPlayer)
                .then(Commands.literal("list")
                        .executes(ctx -> shopList(ctx.getSource().getPlayerOrException(), 1, ledger))
                        .then(Commands.argument("page", IntegerArgumentType.integer(1))
                                .executes(ctx -> shopList(ctx.getSource().getPlayerOrException(), IntegerArgumentType.getInteger(ctx, "page"), ledger))))
                .then(Commands.literal("sell")
                        .executes(ctx -> shopSellList(ctx.getSource().getPlayerOrException(), 1, ledger))
                        .then(Commands.argument("page", IntegerArgumentType.integer(1))
                                .executes(ctx -> shopSellList(ctx.getSource().getPlayerOrException(), IntegerArgumentType.getInteger(ctx, "page"), ledger))))
                .then(Commands.literal("price")
                        .then(Commands.argument("item", ResourceLocationArgument.id())
                                .suggests((ctx, builder) -> suggestShopPriceItems(ledger, builder))
                                .executes(ctx -> shopPrice(ctx.getSource().getPlayerOrException(), ResourceLocationArgument.getId(ctx, "item").toString(), ledger))))
                .then(Commands.literal("buy")
                        .then(Commands.argument("item", ResourceLocationArgument.id())
                                .suggests((ctx, builder) -> suggestShopItems(ledger, builder))
                                .executes(ctx -> shopBuy(ctx.getSource().getPlayerOrException(), ResourceLocationArgument.getId(ctx, "item").toString(), 1, ledger))
                                .then(Commands.argument("amount", IntegerArgumentType.integer(1, 2304))
                                        .executes(ctx -> shopBuy(ctx.getSource().getPlayerOrException(), ResourceLocationArgument.getId(ctx, "item").toString(), IntegerArgumentType.getInteger(ctx, "amount"), ledger))))));

        dispatcher.register(Commands.literal("jobs")
                .requires(source -> source.getEntity() instanceof ServerPlayer)
                .executes(ctx -> jobs(ctx.getSource().getPlayerOrException(), 1, ledger))
                .then(Commands.argument("page", IntegerArgumentType.integer(1))
                        .executes(ctx -> jobs(ctx.getSource().getPlayerOrException(), IntegerArgumentType.getInteger(ctx, "page"), ledger))));

        dispatcher.register(Commands.literal("job")
                .requires(source -> source.getEntity() instanceof ServerPlayer)
                .then(Commands.literal("join")
                        .then(Commands.argument("job", StringArgumentType.word())
                                .suggests((ctx, builder) -> suggestJobs(ledger, builder))
                                .executes(ctx -> jobJoin(ctx.getSource().getPlayerOrException(), StringArgumentType.getString(ctx, "job"), ledger))))
                .then(Commands.literal("current")
                        .executes(ctx -> jobCurrent(ctx.getSource().getPlayerOrException(), ledger)))
                .then(Commands.literal("leave")
                        .executes(ctx -> jobLeave(ctx.getSource().getPlayerOrException(), ledger)))
                .then(Commands.literal("info")
                        .executes(ctx -> jobInfo(ctx.getSource().getPlayerOrException(), ledger.players().job(ctx.getSource().getPlayerOrException()), ledger))
                        .then(Commands.argument("job", StringArgumentType.word())
                                .suggests((ctx, builder) -> suggestJobs(ledger, builder))
                                .executes(ctx -> jobInfo(ctx.getSource().getPlayerOrException(), StringArgumentType.getString(ctx, "job"), 1, ledger))
                                .then(Commands.argument("page", IntegerArgumentType.integer(1))
                                        .executes(ctx -> jobInfo(ctx.getSource().getPlayerOrException(), StringArgumentType.getString(ctx, "job"), IntegerArgumentType.getInteger(ctx, "page"), ledger))))));

        dispatcher.register(Commands.literal("craftledger")
                .requires(CraftLedgerPermissions::canUseCraftLedgerRoot)
                .then(Commands.literal("reload")
                        .requires(CraftLedgerPermissions::canAdmin)
                        .executes(ctx -> reload(ctx.getSource(), ledger)))
                .then(Commands.literal("shop")
                        .requires(CraftLedgerPermissions::canAdmin)
                        .then(Commands.literal("reload")
                                .executes(ctx -> reload(ctx.getSource(), ledger))))
                .then(Commands.literal("jobs")
                                .requires(CraftLedgerPermissions::canAdmin)
                                .then(Commands.literal("reload")
                                        .executes(ctx -> reload(ctx.getSource(), ledger))))
                .then(Commands.literal("balance")
                        .requires(CraftLedgerPermissions::canAdmin)
                        .then(Commands.literal("top")
                                .executes(ctx -> balanceTop(ctx.getSource(), 1, ledger))
                                .then(Commands.argument("page", IntegerArgumentType.integer(1))
                                        .executes(ctx -> balanceTop(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "page"), ledger))))
                        .then(Commands.literal("get")
                                .then(Commands.argument("player", StringArgumentType.word())
                                        .suggests((ctx, builder) -> suggestBalanceTargets(ctx.getSource(), ledger, builder))
                                        .executes(ctx -> adminBalanceGet(ctx.getSource(), StringArgumentType.getString(ctx, "player"), ledger))))
                        .then(Commands.literal("set")
                                .then(Commands.argument("player", StringArgumentType.word())
                                        .suggests((ctx, builder) -> suggestBalanceTargets(ctx.getSource(), ledger, builder))
                                        .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0))
                                                .executes(ctx -> adminBalance(ctx.getSource(), StringArgumentType.getString(ctx, "player"), DoubleArgumentType.getDouble(ctx, "amount"), "set", ledger))))
                                .then(Commands.argument("selector", EntityArgument.player())
                                        .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0))
                                                .executes(ctx -> adminBalance(ctx.getSource(), EntityArgument.getPlayer(ctx, "selector"), DoubleArgumentType.getDouble(ctx, "amount"), "set", ledger)))))
                        .then(Commands.literal("add")
                                .then(Commands.argument("player", StringArgumentType.word())
                                        .suggests((ctx, builder) -> suggestBalanceTargets(ctx.getSource(), ledger, builder))
                                        .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.01D))
                                                .executes(ctx -> adminBalance(ctx.getSource(), StringArgumentType.getString(ctx, "player"), DoubleArgumentType.getDouble(ctx, "amount"), "add", ledger))))
                                .then(Commands.argument("selector", EntityArgument.player())
                                        .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.01D))
                                                .executes(ctx -> adminBalance(ctx.getSource(), EntityArgument.getPlayer(ctx, "selector"), DoubleArgumentType.getDouble(ctx, "amount"), "add", ledger)))))
                        .then(Commands.literal("take")
                                .then(Commands.argument("player", StringArgumentType.word())
                                        .suggests((ctx, builder) -> suggestBalanceTargets(ctx.getSource(), ledger, builder))
                                        .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.01D))
                                                .executes(ctx -> adminBalance(ctx.getSource(), StringArgumentType.getString(ctx, "player"), DoubleArgumentType.getDouble(ctx, "amount"), "take", ledger))))
                                .then(Commands.argument("selector", EntityArgument.player())
                                        .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.01D))
                                                .executes(ctx -> adminBalance(ctx.getSource(), EntityArgument.getPlayer(ctx, "selector"), DoubleArgumentType.getDouble(ctx, "amount"), "take", ledger))))))
                .then(Commands.literal("transactions")
                        .requires(CraftLedgerPermissions::canViewTransactions)
                        .then(Commands.literal("tail")
                                .executes(ctx -> transactionTail(ctx.getSource(), 10, ledger))
                                .then(Commands.argument("lines", IntegerArgumentType.integer(1, 50))
                                        .executes(ctx -> transactionTail(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "lines"), ledger))))));
    }

    private static int balance(ServerPlayer player, Ledger ledger) {
        player.sendSystemMessage(TextUtil.success("Balance: " + ledger.common().format(ledger.players().balance(player))));
        return 1;
    }

    private static int balanceTop(CommandSourceStack source, int page, Ledger ledger) {
        source.sendSuccess(() -> TextUtil.success(BalanceViews.topBalances(ledger.players().topBalances(), ledger.common(), page)), false);
        return 1;
    }

    private static int balanceOther(CommandSourceStack source, ServerPlayer player, Ledger ledger) {
        source.sendSuccess(() -> TextUtil.success(player.getGameProfile().getName() + " balance: " + ledger.common().format(ledger.players().balance(player))), false);
        return 1;
    }

    private static int pay(ServerPlayer source, ServerPlayer target, double amount, Ledger ledger) {
        if (source.getUUID().equals(target.getUUID())) {
            source.sendSystemMessage(TextUtil.error("You cannot pay yourself."));
            return 0;
        }
        if (!ledger.players().canDeposit(target, amount)) {
            source.sendSystemMessage(TextUtil.error("That player cannot receive that amount."));
            return 0;
        }
        if (!ledger.players().withdraw(source, amount)) {
            source.sendSystemMessage(TextUtil.error("Insufficient funds."));
            return 0;
        }
        if (!ledger.players().deposit(target, amount)) {
            ledger.players().deposit(source, amount);
            source.sendSystemMessage(TextUtil.error("Payment failed and your money was returned."));
            return 0;
        }
        ledger.transactions().write("pay_send", source, amount, "to " + target.getGameProfile().getName());
        ledger.transactions().write("pay_receive", target, amount, "from " + source.getGameProfile().getName());
        source.sendSystemMessage(TextUtil.success("Paid " + target.getGameProfile().getName() + " " + ledger.common().format(amount) + ". Balance: " + ledger.common().format(ledger.players().balance(source))));
        target.sendSystemMessage(TextUtil.success("Received " + ledger.common().format(amount) + " from " + source.getGameProfile().getName() + ". Balance: " + ledger.common().format(ledger.players().balance(target))));
        return 1;
    }

    private static int sellHand(ServerPlayer player, int amount, Ledger ledger) {
        ShopService.SellResult result = ledger.shop().sellHand(player, amount);
        player.sendSystemMessage(result.success()
                ? TextUtil.success("Sold " + result.itemCount() + " item(s) from hand for " + ledger.common().format(result.total()))
                : TextUtil.error(result.message()));
        return result.success() ? 1 : 0;
    }

    private static int sellAll(ServerPlayer player, String itemId, Ledger ledger) {
        ShopService.SellResult result = ledger.shop().sellAll(player, itemId);
        player.sendSystemMessage(result.success()
                ? TextUtil.success("Sold " + result.itemCount() + " item(s) for " + ledger.common().format(result.total()) + sellSummary(result))
                : TextUtil.error(result.message()));
        return result.success() ? 1 : 0;
    }

    private static int shopList(ServerPlayer player, int page, Ledger ledger) {
        player.sendSystemMessage(TextUtil.success(ledger.shop().listBuy(ledger.common(), page)));
        return 1;
    }

    private static int shopSellList(ServerPlayer player, int page, Ledger ledger) {
        player.sendSystemMessage(TextUtil.success(ledger.shop().listSell(ledger.common(), page)));
        return 1;
    }

    private static int shopPrice(ServerPlayer player, String item, Ledger ledger) {
        player.sendSystemMessage(TextUtil.success(ledger.shop().price(item, ledger.common())));
        return 1;
    }

    private static int shopBuy(ServerPlayer player, String item, int amount, Ledger ledger) {
        ShopService.BuyResult result = ledger.shop().buy(player, item, amount);
        if (!result.success()) {
            player.sendSystemMessage(TextUtil.error(result.message()));
            return 0;
        }
        String overflow = result.droppedItems() ? " Some items were dropped because your inventory was full." : "";
        player.sendSystemMessage(TextUtil.success("Bought " + result.amount() + " " + item + " for " + ledger.common().format(result.total()) + overflow));
        return 1;
    }

    private static int jobs(ServerPlayer player, int page, Ledger ledger) {
        String current = ledger.players().job(player);
        player.sendSystemMessage(TextUtil.success(ledger.jobs().listJobs(current, page)));
        return 1;
    }

    private static int jobJoin(ServerPlayer player, String job, Ledger ledger) {
        JobsService.JoinResult result = ledger.jobs().join(player, job);
        if (result == JobsService.JoinResult.UNKNOWN_JOB) {
            player.sendSystemMessage(TextUtil.error("Unknown job: " + job));
            return 0;
        }
        if (result == JobsService.JoinResult.ALREADY_IN_JOB) {
            player.sendSystemMessage(TextUtil.error("You already have that job."));
            return 0;
        }
        if (result == JobsService.JoinResult.SWITCHING_DISABLED) {
            player.sendSystemMessage(TextUtil.error("Leave your current job before joining another one."));
            return 0;
        }
        player.sendSystemMessage(TextUtil.success("Joined job: " + job.toLowerCase(Locale.ROOT)));
        return 1;
    }

    private static int jobCurrent(ServerPlayer player, Ledger ledger) {
        String current = ledger.players().job(player);
        player.sendSystemMessage(TextUtil.success("Current job: " + (current == null ? "none" : current)));
        return 1;
    }

    private static int jobLeave(ServerPlayer player, Ledger ledger) {
        ledger.jobs().leave(player);
        player.sendSystemMessage(TextUtil.success("Left your job."));
        return 1;
    }

    private static int jobInfo(ServerPlayer player, String job, Ledger ledger) {
        return jobInfo(player, job, 1, ledger);
    }

    private static int jobInfo(ServerPlayer player, String job, int page, Ledger ledger) {
        if (job == null || job.isBlank()) {
            player.sendSystemMessage(TextUtil.error("You have not joined a job."));
            return 0;
        }
        player.sendSystemMessage(TextUtil.success(ledger.jobs().info(job, page)));
        return 1;
    }

    private static int reload(CommandSourceStack source, Ledger ledger) {
        try {
            ledger.reload();
            source.sendSuccess(() -> TextUtil.success("CraftLedger Jobs reloaded."), true);
            return 1;
        } catch (RuntimeException ex) {
            source.sendFailure(TextUtil.error("CraftLedger reload failed: " + rootMessage(ex)));
            CraftLedgerJobs.LOGGER.warn("CraftLedger reload failed", ex);
            return 0;
        }
    }

    private static int adminBalanceGet(CommandSourceStack source, String playerTarget, Ledger ledger) {
        Optional<PlayerStore.KnownPlayer> target = resolveBalanceTarget(source, playerTarget, ledger);
        if (target.isEmpty()) {
            source.sendFailure(TextUtil.error("Unknown stored player: " + playerTarget + ". The player must join once before offline balance commands can target them."));
            return 0;
        }
        PlayerStore.KnownPlayer player = target.get();
        source.sendSuccess(() -> TextUtil.success(player.name() + " balance: " + ledger.common().format(ledger.players().balance(player.uuid(), player.name()))), false);
        return 1;
    }

    private static int adminBalance(CommandSourceStack source, String playerTarget, double amount, String mode, Ledger ledger) {
        Optional<PlayerStore.KnownPlayer> target = resolveBalanceTarget(source, playerTarget, ledger);
        if (target.isEmpty()) {
            source.sendFailure(TextUtil.error("Unknown stored player: " + playerTarget + ". The player must join once before offline balance commands can target them."));
            return 0;
        }
        return updateBalance(source, target.get(), amount, mode, ledger);
    }

    private static int adminBalance(CommandSourceStack source, ServerPlayer player, double amount, String mode, Ledger ledger) {
        return updateBalance(source, new PlayerStore.KnownPlayer(player.getUUID(), player.getGameProfile().getName()), amount, mode, ledger);
    }

    private static int updateBalance(CommandSourceStack source, PlayerStore.KnownPlayer player, double amount, String mode, Ledger ledger) {
        if ("set".equals(mode)) {
            ledger.players().set(player.uuid(), player.name(), amount);
        } else if ("add".equals(mode)) {
            ledger.players().add(player.uuid(), player.name(), amount);
        } else {
            ledger.players().take(player.uuid(), player.name(), amount);
        }
        ledger.transactions().write("admin_balance_" + mode, player.name(), player.uuid().toString(), amount, source.getTextName());
        source.sendSuccess(() -> TextUtil.success("Balance updated for " + player.name() + ": " + ledger.common().format(ledger.players().balance(player.uuid(), player.name()))), true);
        return 1;
    }

    private static int transactionTail(CommandSourceStack source, int lines, Ledger ledger) {
        List<String> entries = ledger.transactions().tail(lines);
        if (entries.isEmpty()) {
            source.sendSuccess(() -> TextUtil.success("No transactions logged."), false);
            return 1;
        }
        source.sendSuccess(() -> TextUtil.success("Recent transactions:\n" + String.join("\n", entries)), false);
        return 1;
    }

    private static CompletableFuture<Suggestions> suggestShopItems(Ledger ledger, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(ledger.shopConfig().buyPrices.keySet(), builder);
    }

    private static CompletableFuture<Suggestions> suggestSellItems(Ledger ledger, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(ledger.shopConfig().sellPrices.keySet(), builder);
    }

    private static CompletableFuture<Suggestions> suggestShopPriceItems(Ledger ledger, SuggestionsBuilder builder) {
        List<String> items = new ArrayList<>(ledger.shopConfig().buyPrices.keySet());
        for (String item : ledger.shopConfig().sellPrices.keySet()) {
            if (items.stream().noneMatch(existing -> existing.equalsIgnoreCase(item))) {
                items.add(item);
            }
        }
        return SharedSuggestionProvider.suggest(items, builder);
    }

    private static CompletableFuture<Suggestions> suggestJobs(Ledger ledger, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(ledger.jobsConfig().jobs.keySet(), builder);
    }

    private static CompletableFuture<Suggestions> suggestBalanceTargets(CommandSourceStack source, Ledger ledger, SuggestionsBuilder builder) {
        List<String> names = new ArrayList<>(ledger.players().knownPlayerNames());
        for (ServerPlayer player : source.getServer().getPlayerList().getPlayers()) {
            String name = player.getGameProfile().getName();
            if (names.stream().noneMatch(existing -> existing.equalsIgnoreCase(name))) {
                names.add(name);
            }
        }
        return SharedSuggestionProvider.suggest(names, builder);
    }

    private static Optional<PlayerStore.KnownPlayer> resolveBalanceTarget(CommandSourceStack source, String playerTarget, Ledger ledger) {
        ServerPlayer online = source.getServer().getPlayerList().getPlayerByName(playerTarget);
        if (online != null) {
            return Optional.of(new PlayerStore.KnownPlayer(online.getUUID(), online.getGameProfile().getName()));
        }
        return ledger.players().findKnownPlayer(playerTarget);
    }

    private static String rootMessage(Throwable throwable) {
        Throwable cursor = throwable;
        while (cursor.getCause() != null) {
            cursor = cursor.getCause();
        }
        return cursor.getMessage() == null ? cursor.getClass().getSimpleName() : cursor.getMessage();
    }

    private static String sellSummary(ShopService.SellResult result) {
        if (result.items().isEmpty()) {
            return "";
        }
        String first = result.items().get(0).itemId() + " x" + result.items().get(0).count();
        int remaining = result.items().size() - 1;
        return remaining > 0 ? " (" + first + ", +" + remaining + " more)" : " (" + first + ")";
    }
}
