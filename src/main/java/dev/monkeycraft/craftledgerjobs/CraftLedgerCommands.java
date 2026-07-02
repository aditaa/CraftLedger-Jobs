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

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class CraftLedgerCommands {
    private static final Map<UUID, Instant> LAST_PAYMENTS = new HashMap<>();

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
                .executes(ctx -> sellHelp(ctx.getSource().getPlayerOrException()))
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
                .executes(ctx -> shopHelp(ctx.getSource().getPlayerOrException()))
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
                .executes(ctx -> jobCurrent(ctx.getSource().getPlayerOrException(), ledger))
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
                .executes(ctx -> craftLedgerHelp(ctx.getSource()))
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
                .then(Commands.literal("player")
                        .requires(CraftLedgerPermissions::canAdmin)
                        .then(Commands.literal("info")
                                .then(Commands.argument("player", StringArgumentType.word())
                                        .suggests((ctx, builder) -> suggestBalanceTargets(ctx.getSource(), ledger, builder))
                                        .executes(ctx -> adminPlayerInfo(ctx.getSource(), StringArgumentType.getString(ctx, "player"), ledger)))))
                .then(Commands.literal("job")
                        .requires(CraftLedgerPermissions::canAdmin)
                        .then(Commands.literal("set")
                                .then(Commands.argument("player", StringArgumentType.word())
                                        .suggests((ctx, builder) -> suggestBalanceTargets(ctx.getSource(), ledger, builder))
                                        .then(Commands.argument("job", StringArgumentType.word())
                                                .suggests((ctx, builder) -> suggestJobs(ledger, builder))
                                                .executes(ctx -> adminJobSet(ctx.getSource(), StringArgumentType.getString(ctx, "player"), StringArgumentType.getString(ctx, "job"), ledger)))))
                        .then(Commands.literal("clear")
                                .then(Commands.argument("player", StringArgumentType.word())
                                        .suggests((ctx, builder) -> suggestBalanceTargets(ctx.getSource(), ledger, builder))
                                        .executes(ctx -> adminJobClear(ctx.getSource(), StringArgumentType.getString(ctx, "player"), ledger)))))
                .then(Commands.literal("storage")
                        .requires(CraftLedgerPermissions::canAdmin)
                        .then(Commands.literal("migrate")
                                .then(Commands.literal("json-to-sqlite")
                                        .executes(ctx -> migrateJsonToSqlite(ctx.getSource(), false, ledger))
                                        .then(Commands.literal("dry-run")
                                                .executes(ctx -> migrateJsonToSqlite(ctx.getSource(), true, ledger))))))
                .then(Commands.literal("transactions")
                        .requires(CraftLedgerPermissions::canViewTransactions)
                        .then(Commands.literal("tail")
                                .executes(ctx -> transactionTail(ctx.getSource(), 10, ledger))
                                .then(Commands.argument("lines", IntegerArgumentType.integer(1, 50))
                                        .executes(ctx -> transactionTail(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "lines"), ledger)))
                                .then(Commands.literal("player")
                                        .then(Commands.argument("player", StringArgumentType.word())
                                                .suggests((ctx, builder) -> suggestBalanceTargets(ctx.getSource(), ledger, builder))
                                                .executes(ctx -> transactionTail(ctx.getSource(), StringArgumentType.getString(ctx, "player"), 10, ledger))
                                                .then(Commands.argument("lines", IntegerArgumentType.integer(1, 50))
                                                        .executes(ctx -> transactionTail(ctx.getSource(), StringArgumentType.getString(ctx, "player"), IntegerArgumentType.getInteger(ctx, "lines"), ledger))))))));
    }

    private static int balance(ServerPlayer player, Ledger ledger) {
        if (!ledger.common().currencyEnabled()) {
            player.sendSystemMessage(TextUtil.error(msg(ledger, "currency.disabled")));
            return 0;
        }
        player.sendSystemMessage(TextUtil.success(msg(ledger, "balance.self", "balance", ledger.common().format(ledger.players().balance(player)))));
        return 1;
    }

    private static int balanceTop(CommandSourceStack source, int page, Ledger ledger) {
        if (!ledger.common().currencyEnabled()) {
            source.sendFailure(TextUtil.error(msg(ledger, "currency.disabled")));
            return 0;
        }
        source.sendSuccess(() -> TextUtil.success(BalanceViews.topBalances(ledger.players().topBalances(), ledger.common(), page)), false);
        return 1;
    }

    private static int balanceOther(CommandSourceStack source, ServerPlayer player, Ledger ledger) {
        if (!ledger.common().currencyEnabled()) {
            source.sendFailure(TextUtil.error(msg(ledger, "currency.disabled")));
            return 0;
        }
        source.sendSuccess(() -> TextUtil.success(msg(ledger, "balance.other", Map.of(
                "player", player.getGameProfile().getName(),
                "balance", ledger.common().format(ledger.players().balance(player))
        ))), false);
        return 1;
    }

    private static int pay(ServerPlayer source, ServerPlayer target, double amount, Ledger ledger) {
        if (!ledger.common().currencyEnabled()) {
            source.sendSystemMessage(TextUtil.error(msg(ledger, "currency.disabled")));
            return 0;
        }
        if (source.getUUID().equals(target.getUUID())) {
            source.sendSystemMessage(TextUtil.error(msg(ledger, "pay.self")));
            return 0;
        }
        if (ledger.common().maxPayAmount() > 0 && amount > ledger.common().maxPayAmount()) {
            source.sendSystemMessage(TextUtil.error("Payments are capped at " + ledger.common().format(ledger.common().maxPayAmount()) + "."));
            return 0;
        }
        int cooldownSeconds = ledger.common().payCooldownSeconds();
        if (cooldownSeconds > 0) {
            Instant now = Instant.now();
            Instant lastPayment = LAST_PAYMENTS.get(source.getUUID());
            if (lastPayment != null && Duration.between(lastPayment, now).getSeconds() < cooldownSeconds) {
                long remaining = cooldownSeconds - Duration.between(lastPayment, now).getSeconds();
                source.sendSystemMessage(TextUtil.error("Wait " + remaining + " second(s) before using /pay again."));
                return 0;
            }
        }
        if (!ledger.canDeposit(target, amount)) {
            source.sendSystemMessage(TextUtil.error(msg(ledger, "pay.target_full")));
            return 0;
        }
        if (!ledger.players().withdraw(source, amount)) {
            source.sendSystemMessage(TextUtil.error(msg(ledger, "pay.insufficient")));
            return 0;
        }
        if (!ledger.deposit(target, amount)) {
            ledger.deposit(source, amount);
            source.sendSystemMessage(TextUtil.error(msg(ledger, "pay.rollback_failed")));
            return 0;
        }
        if (cooldownSeconds > 0) {
            LAST_PAYMENTS.put(source.getUUID(), Instant.now());
        }
        ledger.transactions().write("pay_send", source, amount, "to " + target.getGameProfile().getName());
        ledger.transactions().write("pay_receive", target, amount, "from " + source.getGameProfile().getName());
        source.sendSystemMessage(TextUtil.success(msg(ledger, "pay.sent", Map.of(
                "target", target.getGameProfile().getName(),
                "amount", ledger.common().format(amount),
                "balance", ledger.common().format(ledger.players().balance(source))
        ))));
        target.sendSystemMessage(TextUtil.success(msg(ledger, "pay.received", Map.of(
                "source", source.getGameProfile().getName(),
                "amount", ledger.common().format(amount),
                "balance", ledger.common().format(ledger.players().balance(target))
        ))));
        return 1;
    }

    private static int sellHelp(ServerPlayer player) {
        player.sendSystemMessage(TextUtil.success("Sell commands: /sell hand [amount], /sell all [item_id]"));
        return 1;
    }

    private static int sellHand(ServerPlayer player, int amount, Ledger ledger) {
        ShopService.SellResult result = ledger.shop().sellHand(player, amount);
        player.sendSystemMessage(result.success()
                ? TextUtil.success(msg(ledger, "sell.hand.success", Map.of("count", Integer.toString(result.itemCount()), "total", ledger.common().format(result.total()))))
                : TextUtil.error(result.message()));
        return result.success() ? 1 : 0;
    }

    private static int sellAll(ServerPlayer player, String itemId, Ledger ledger) {
        ShopService.SellResult result = ledger.shop().sellAll(player, itemId);
        player.sendSystemMessage(result.success()
                ? TextUtil.success(msg(ledger, "sell.all.success", Map.of(
                "count", Integer.toString(result.itemCount()),
                "total", ledger.common().format(result.total()),
                "summary", sellSummary(result)
        )))
                : TextUtil.error(result.message()));
        return result.success() ? 1 : 0;
    }

    private static int shopHelp(ServerPlayer player) {
        player.sendSystemMessage(TextUtil.success("Shop commands: /shop list [page], /shop sell [page], /shop price <item>, /shop buy <item> [amount]"));
        return 1;
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
        String overflow = result.droppedItems() ? msg(ledger, "shop.buy.overflow") : "";
        player.sendSystemMessage(TextUtil.success(msg(ledger, "shop.buy.success", Map.of(
                "count", Integer.toString(result.amount()),
                "item", item,
                "total", ledger.common().format(result.total()),
                "overflow", overflow
        ))));
        return 1;
    }

    private static int jobs(ServerPlayer player, int page, Ledger ledger) {
        String current = ledger.players().job(player);
        player.sendSystemMessage(TextUtil.success(ledger.jobs().listJobs(current, page)));
        return 1;
    }

    private static int jobJoin(ServerPlayer player, String job, Ledger ledger) {
        JobsService.JoinResult result = ledger.jobs().join(player, job);
        if (result == JobsService.JoinResult.JOBS_DISABLED) {
            player.sendSystemMessage(TextUtil.error(msg(ledger, "jobs.disabled")));
            return 0;
        }
        if (result == JobsService.JoinResult.UNKNOWN_JOB) {
            player.sendSystemMessage(TextUtil.error(msg(ledger, "job.unknown", "job", job)));
            return 0;
        }
        if (result == JobsService.JoinResult.ALREADY_IN_JOB) {
            player.sendSystemMessage(TextUtil.error(msg(ledger, "job.already")));
            return 0;
        }
        if (result == JobsService.JoinResult.SWITCHING_DISABLED) {
            player.sendSystemMessage(TextUtil.error(msg(ledger, "job.switching_disabled")));
            return 0;
        }
        player.sendSystemMessage(TextUtil.success(msg(ledger, "job.joined", "job", job.toLowerCase(Locale.ROOT))));
        return 1;
    }

    private static int jobCurrent(ServerPlayer player, Ledger ledger) {
        if (!ledger.jobsConfig().enabled) {
            player.sendSystemMessage(TextUtil.error(msg(ledger, "jobs.disabled")));
            return 0;
        }
        String current = ledger.players().job(player);
        player.sendSystemMessage(TextUtil.success(msg(ledger, "job.current", "job", current == null ? "none" : current)));
        return 1;
    }

    private static int jobLeave(ServerPlayer player, Ledger ledger) {
        if (!ledger.jobsConfig().enabled) {
            player.sendSystemMessage(TextUtil.error(msg(ledger, "jobs.disabled")));
            return 0;
        }
        ledger.jobs().leave(player);
        player.sendSystemMessage(TextUtil.success(msg(ledger, "job.left")));
        return 1;
    }

    private static int jobInfo(ServerPlayer player, String job, Ledger ledger) {
        if (!ledger.jobsConfig().enabled) {
            player.sendSystemMessage(TextUtil.error(msg(ledger, "jobs.disabled")));
            return 0;
        }
        return jobInfo(player, job, 1, ledger);
    }

    private static int jobInfo(ServerPlayer player, String job, int page, Ledger ledger) {
        if (!ledger.jobsConfig().enabled) {
            player.sendSystemMessage(TextUtil.error(msg(ledger, "jobs.disabled")));
            return 0;
        }
        if (job == null || job.isBlank()) {
            player.sendSystemMessage(TextUtil.error(msg(ledger, "job.none")));
            return 0;
        }
        player.sendSystemMessage(TextUtil.success(ledger.jobs().info(job, page)));
        return 1;
    }

    private static int reload(CommandSourceStack source, Ledger ledger) {
        try {
            ledger.reload();
            source.sendSuccess(() -> TextUtil.success(msg(ledger, "admin.reload_success")), true);
            return 1;
        } catch (RuntimeException ex) {
            source.sendFailure(TextUtil.error(msg(ledger, "admin.reload_failed", "error", rootMessage(ex))));
            CraftLedgerJobs.LOGGER.warn("CraftLedger reload failed", ex);
            return 0;
        }
    }

    private static int adminBalanceGet(CommandSourceStack source, String playerTarget, Ledger ledger) {
        if (!ledger.common().currencyEnabled()) {
            source.sendFailure(TextUtil.error(msg(ledger, "currency.disabled")));
            return 0;
        }
        Optional<PlayerStore.KnownPlayer> target = resolveBalanceTarget(source, playerTarget, ledger);
        if (target.isEmpty()) {
            source.sendFailure(TextUtil.error(msg(ledger, "admin.unknown_player", "player", playerTarget)));
            return 0;
        }
        PlayerStore.KnownPlayer player = target.get();
        source.sendSuccess(() -> TextUtil.success(msg(ledger, "balance.other", Map.of(
                "player", player.name(),
                "balance", ledger.common().format(ledger.players().balance(player.uuid(), player.name()))
        ))), false);
        return 1;
    }

    private static int adminBalance(CommandSourceStack source, String playerTarget, double amount, String mode, Ledger ledger) {
        if (!ledger.common().currencyEnabled()) {
            source.sendFailure(TextUtil.error(msg(ledger, "currency.disabled")));
            return 0;
        }
        Optional<PlayerStore.KnownPlayer> target = resolveBalanceTarget(source, playerTarget, ledger);
        if (target.isEmpty()) {
            source.sendFailure(TextUtil.error(msg(ledger, "admin.unknown_player", "player", playerTarget)));
            return 0;
        }
        return updateBalance(source, target.get(), amount, mode, ledger);
    }

    private static int adminBalance(CommandSourceStack source, ServerPlayer player, double amount, String mode, Ledger ledger) {
        if (!ledger.common().currencyEnabled()) {
            source.sendFailure(TextUtil.error(msg(ledger, "currency.disabled")));
            return 0;
        }
        return updateBalance(source, new PlayerStore.KnownPlayer(player.getUUID(), player.getGameProfile().getName()), amount, mode, ledger);
    }

    private static int updateBalance(CommandSourceStack source, PlayerStore.KnownPlayer player, double amount, String mode, Ledger ledger) {
        if ("set".equals(mode)) {
            if (!ledger.canSetBalance(amount)) {
                source.sendFailure(TextUtil.error(ledger.maxBalanceMessage()));
                return 0;
            }
            ledger.players().set(player.uuid(), player.name(), amount);
        } else if ("add".equals(mode)) {
            if (!ledger.canAddBalance(player.uuid(), player.name(), amount)) {
                source.sendFailure(TextUtil.error(ledger.maxBalanceMessage()));
                return 0;
            }
            ledger.players().add(player.uuid(), player.name(), amount);
        } else {
            ledger.players().take(player.uuid(), player.name(), amount);
        }
        ledger.transactions().write("admin_balance_" + mode, player.name(), player.uuid().toString(), amount, source.getTextName());
        source.sendSuccess(() -> TextUtil.success(msg(ledger, "balance.updated", Map.of(
                "player", player.name(),
                "balance", ledger.common().format(ledger.players().balance(player.uuid(), player.name()))
        ))), true);
        return 1;
    }

    private static int adminPlayerInfo(CommandSourceStack source, String playerTarget, Ledger ledger) {
        Optional<PlayerStore.KnownPlayer> target = resolveBalanceTarget(source, playerTarget, ledger);
        if (target.isEmpty()) {
            source.sendFailure(TextUtil.error(msg(ledger, "admin.unknown_player", "player", playerTarget)));
            return 0;
        }
        PlayerStore.KnownPlayer player = target.get();
        String balance = ledger.common().currencyEnabled() ? ledger.common().format(ledger.players().balance(player.uuid(), player.name())) : "disabled";
        String job = ledger.players().job(player.uuid(), player.name());
        source.sendSuccess(() -> TextUtil.success(msg(ledger, "admin.player_info", Map.of(
                "player", player.name(),
                "balance", balance,
                "job", job == null ? "none" : job
        ))), false);
        return 1;
    }

    private static int adminJobSet(CommandSourceStack source, String playerTarget, String job, Ledger ledger) {
        if (!ledger.jobsConfig().enabled) {
            source.sendFailure(TextUtil.error(msg(ledger, "jobs.disabled")));
            return 0;
        }
        String normalized = job.toLowerCase(Locale.ROOT);
        if (!ledger.jobsConfig().jobs.containsKey(normalized)) {
            source.sendFailure(TextUtil.error(msg(ledger, "job.unknown", "job", job)));
            return 0;
        }
        Optional<PlayerStore.KnownPlayer> target = resolveBalanceTarget(source, playerTarget, ledger);
        if (target.isEmpty()) {
            source.sendFailure(TextUtil.error(msg(ledger, "admin.unknown_player", "player", playerTarget)));
            return 0;
        }
        PlayerStore.KnownPlayer player = target.get();
        ledger.players().setJob(player.uuid(), player.name(), normalized);
        ledger.transactions().write("admin_job_set", player.name(), player.uuid().toString(), 0, source.getTextName() + " -> " + normalized);
        source.sendSuccess(() -> TextUtil.success(msg(ledger, "admin.job_set", Map.of("player", player.name(), "job", normalized))), true);
        return 1;
    }

    private static int adminJobClear(CommandSourceStack source, String playerTarget, Ledger ledger) {
        Optional<PlayerStore.KnownPlayer> target = resolveBalanceTarget(source, playerTarget, ledger);
        if (target.isEmpty()) {
            source.sendFailure(TextUtil.error(msg(ledger, "admin.unknown_player", "player", playerTarget)));
            return 0;
        }
        PlayerStore.KnownPlayer player = target.get();
        ledger.players().clearJob(player.uuid(), player.name());
        ledger.transactions().write("admin_job_clear", player.name(), player.uuid().toString(), 0, source.getTextName());
        source.sendSuccess(() -> TextUtil.success(msg(ledger, "admin.job_cleared", "player", player.name())), true);
        return 1;
    }

    private static int transactionTail(CommandSourceStack source, int lines, Ledger ledger) {
        List<String> entries = ledger.transactions().tail(lines);
        return sendTransactionTail(source, entries, ledger);
    }

    private static int migrateJsonToSqlite(CommandSourceStack source, boolean dryRun, Ledger ledger) {
        if (!CommonConfig.STORAGE_JSON.equals(ledger.common().storageBackend())) {
            source.sendFailure(TextUtil.error("JSON-to-SQLite migration can only run while storageBackend is \"json\"."));
            return 0;
        }
        ledger.players().save();
        if (ledger.jobPayouts() instanceof JobPayoutStore jsonPayouts) {
            jsonPayouts.save();
        }
        try {
            StorageMigrationService.MigrationResult result = new StorageMigrationService()
                    .migrateJsonToSqlite(ledger.dataDir(), ledger.common().sqliteFile(), ledger.common().startingBalance(), dryRun);
            source.sendSuccess(() -> TextUtil.success(result.summary()), true);
            return 1;
        } catch (IOException | RuntimeException ex) {
            source.sendFailure(TextUtil.error("Storage migration failed: " + rootMessage(ex)));
            CraftLedgerJobs.LOGGER.warn("CraftLedger storage migration failed", ex);
            return 0;
        }
    }

    private static int transactionTail(CommandSourceStack source, String player, int lines, Ledger ledger) {
        List<String> entries = ledger.transactions().tail(player, lines);
        return sendTransactionTail(source, entries, ledger);
    }

    private static int sendTransactionTail(CommandSourceStack source, List<String> entries, Ledger ledger) {
        if (entries.isEmpty()) {
            source.sendSuccess(() -> TextUtil.success(msg(ledger, "transactions.empty")), false);
            return 1;
        }
        source.sendSuccess(() -> TextUtil.success(msg(ledger, "transactions.header") + "\n" + String.join("\n", entries)), false);
        return 1;
    }

    private static int craftLedgerHelp(CommandSourceStack source) {
        source.sendSuccess(() -> TextUtil.success("CraftLedger admin commands: reload, balance, player info, job set/clear, storage migrate, transactions tail"), false);
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

    private static String msg(Ledger ledger, String key) {
        return ledger.messages().get(key);
    }

    private static String msg(Ledger ledger, String key, String placeholder, String value) {
        return ledger.messages().format(key, placeholder, value);
    }

    private static String msg(Ledger ledger, String key, Map<String, String> placeholders) {
        return ledger.messages().format(key, placeholders);
    }
}
