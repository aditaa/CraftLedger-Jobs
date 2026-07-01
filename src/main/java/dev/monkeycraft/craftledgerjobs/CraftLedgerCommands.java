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
import net.minecraft.server.level.ServerPlayer;

import java.util.concurrent.CompletableFuture;

public final class CraftLedgerCommands {
    private CraftLedgerCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, Ledger ledger) {
        dispatcher.register(Commands.literal("balance")
                .requires(source -> source.getEntity() instanceof ServerPlayer)
                .executes(ctx -> balance(ctx.getSource().getPlayerOrException(), ledger)));

        dispatcher.register(Commands.literal("money")
                .requires(source -> source.getEntity() instanceof ServerPlayer)
                .executes(ctx -> balance(ctx.getSource().getPlayerOrException(), ledger)));

        dispatcher.register(Commands.literal("pay")
                .requires(source -> source.getEntity() instanceof ServerPlayer)
                .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.01D))
                                .executes(ctx -> pay(ctx.getSource().getPlayerOrException(), EntityArgument.getPlayer(ctx, "player"), DoubleArgumentType.getDouble(ctx, "amount"), ledger)))));

        dispatcher.register(Commands.literal("sell")
                .requires(source -> source.getEntity() instanceof ServerPlayer)
                .then(Commands.literal("hand")
                        .executes(ctx -> sellHand(ctx.getSource().getPlayerOrException(), ledger)))
                .then(Commands.literal("all")
                        .executes(ctx -> sellAll(ctx.getSource().getPlayerOrException(), ledger))));

        dispatcher.register(Commands.literal("shop")
                .requires(source -> source.getEntity() instanceof ServerPlayer)
                .then(Commands.literal("list")
                        .executes(ctx -> shopList(ctx.getSource().getPlayerOrException(), ledger)))
                .then(Commands.literal("buy")
                        .then(Commands.argument("item", StringArgumentType.word())
                                .suggests((ctx, builder) -> suggestShopItems(ledger, builder))
                                .executes(ctx -> shopBuy(ctx.getSource().getPlayerOrException(), StringArgumentType.getString(ctx, "item"), 1, ledger))
                                .then(Commands.argument("amount", IntegerArgumentType.integer(1, 2304))
                                        .executes(ctx -> shopBuy(ctx.getSource().getPlayerOrException(), StringArgumentType.getString(ctx, "item"), IntegerArgumentType.getInteger(ctx, "amount"), ledger))))));

        dispatcher.register(Commands.literal("jobs")
                .requires(source -> source.getEntity() instanceof ServerPlayer)
                .executes(ctx -> jobs(ctx.getSource().getPlayerOrException(), ledger)));

        dispatcher.register(Commands.literal("job")
                .requires(source -> source.getEntity() instanceof ServerPlayer)
                .then(Commands.literal("join")
                        .then(Commands.argument("job", StringArgumentType.word())
                                .suggests((ctx, builder) -> suggestJobs(ledger, builder))
                                .executes(ctx -> jobJoin(ctx.getSource().getPlayerOrException(), StringArgumentType.getString(ctx, "job"), ledger))))
                .then(Commands.literal("leave")
                        .executes(ctx -> jobLeave(ctx.getSource().getPlayerOrException(), ledger)))
                .then(Commands.literal("info")
                        .executes(ctx -> jobInfo(ctx.getSource().getPlayerOrException(), ledger.players().job(ctx.getSource().getPlayerOrException()), ledger))
                        .then(Commands.argument("job", StringArgumentType.word())
                                .suggests((ctx, builder) -> suggestJobs(ledger, builder))
                                .executes(ctx -> jobInfo(ctx.getSource().getPlayerOrException(), StringArgumentType.getString(ctx, "job"), ledger)))));

        dispatcher.register(Commands.literal("craftledger")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("reload")
                        .executes(ctx -> reload(ctx.getSource(), ledger)))
                .then(Commands.literal("shop")
                        .then(Commands.literal("reload")
                                .executes(ctx -> reload(ctx.getSource(), ledger))))
                .then(Commands.literal("jobs")
                        .then(Commands.literal("reload")
                                .executes(ctx -> reload(ctx.getSource(), ledger))))
                .then(Commands.literal("balance")
                        .then(Commands.literal("set")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0))
                                                .executes(ctx -> adminBalance(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), DoubleArgumentType.getDouble(ctx, "amount"), "set", ledger)))))
                        .then(Commands.literal("add")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.01D))
                                                .executes(ctx -> adminBalance(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), DoubleArgumentType.getDouble(ctx, "amount"), "add", ledger)))))
                        .then(Commands.literal("take")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.01D))
                                                .executes(ctx -> adminBalance(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), DoubleArgumentType.getDouble(ctx, "amount"), "take", ledger)))))));
    }

    private static int balance(ServerPlayer player, Ledger ledger) {
        player.sendSystemMessage(TextUtil.success("Balance: " + ledger.common().format(ledger.players().balance(player))));
        return 1;
    }

    private static int pay(ServerPlayer source, ServerPlayer target, double amount, Ledger ledger) {
        if (source.getUUID().equals(target.getUUID())) {
            source.sendSystemMessage(TextUtil.error("You cannot pay yourself."));
            return 0;
        }
        if (!ledger.players().withdraw(source, amount)) {
            source.sendSystemMessage(TextUtil.error("Insufficient funds."));
            return 0;
        }
        ledger.players().deposit(target, amount);
        ledger.transactions().write("pay_send", source, amount, "to " + target.getGameProfile().getName());
        ledger.transactions().write("pay_receive", target, amount, "from " + source.getGameProfile().getName());
        source.sendSystemMessage(TextUtil.success("Paid " + target.getGameProfile().getName() + " " + ledger.common().format(amount)));
        target.sendSystemMessage(TextUtil.success("Received " + ledger.common().format(amount) + " from " + source.getGameProfile().getName()));
        return 1;
    }

    private static int sellHand(ServerPlayer player, Ledger ledger) {
        double total = ledger.shop().sellHand(player);
        player.sendSystemMessage(total > 0 ? TextUtil.success("Sold hand for " + ledger.common().format(total)) : TextUtil.error("The item in your hand cannot be sold."));
        return total > 0 ? 1 : 0;
    }

    private static int sellAll(ServerPlayer player, Ledger ledger) {
        double total = ledger.shop().sellAll(player);
        player.sendSystemMessage(total > 0 ? TextUtil.success("Sold items for " + ledger.common().format(total)) : TextUtil.error("No configured sellable items found."));
        return total > 0 ? 1 : 0;
    }

    private static int shopList(ServerPlayer player, Ledger ledger) {
        player.sendSystemMessage(TextUtil.success(ledger.shop().list(ledger.common())));
        return 1;
    }

    private static int shopBuy(ServerPlayer player, String item, int amount, Ledger ledger) {
        ShopService.BuyResult result = ledger.shop().buy(player, item, amount);
        if (!result.success()) {
            player.sendSystemMessage(TextUtil.error(result.message()));
            return 0;
        }
        player.sendSystemMessage(TextUtil.success("Bought " + result.amount() + " " + item + " for " + ledger.common().format(result.total())));
        return 1;
    }

    private static int jobs(ServerPlayer player, Ledger ledger) {
        String current = ledger.players().job(player);
        player.sendSystemMessage(TextUtil.success(ledger.jobs().listJobs() + "\nCurrent job: " + (current == null ? "none" : current)));
        return 1;
    }

    private static int jobJoin(ServerPlayer player, String job, Ledger ledger) {
        if (!ledger.jobs().join(player, job)) {
            player.sendSystemMessage(TextUtil.error("Unknown job: " + job));
            return 0;
        }
        player.sendSystemMessage(TextUtil.success("Joined job: " + job.toLowerCase()));
        return 1;
    }

    private static int jobLeave(ServerPlayer player, Ledger ledger) {
        ledger.jobs().leave(player);
        player.sendSystemMessage(TextUtil.success("Left your job."));
        return 1;
    }

    private static int jobInfo(ServerPlayer player, String job, Ledger ledger) {
        if (job == null || job.isBlank()) {
            player.sendSystemMessage(TextUtil.error("You have not joined a job."));
            return 0;
        }
        player.sendSystemMessage(TextUtil.success(ledger.jobs().info(job)));
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

    private static int adminBalance(CommandSourceStack source, ServerPlayer player, double amount, String mode, Ledger ledger) {
        if ("set".equals(mode)) {
            ledger.players().set(player.getUUID(), player.getGameProfile().getName(), amount);
        } else if ("add".equals(mode)) {
            ledger.players().add(player.getUUID(), player.getGameProfile().getName(), amount);
        } else {
            ledger.players().take(player.getUUID(), player.getGameProfile().getName(), amount);
        }
        ledger.transactions().write("admin_balance_" + mode, player.getGameProfile().getName(), player.getUUID().toString(), amount, source.getTextName());
        source.sendSuccess(() -> TextUtil.success("Balance updated for " + player.getGameProfile().getName() + ": " + ledger.common().format(ledger.players().balance(player))), true);
        return 1;
    }

    private static CompletableFuture<Suggestions> suggestShopItems(Ledger ledger, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(ledger.shopConfig().buyPrices.keySet(), builder);
    }

    private static CompletableFuture<Suggestions> suggestJobs(Ledger ledger, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(ledger.jobsConfig().jobs.keySet(), builder);
    }

    private static String rootMessage(Throwable throwable) {
        Throwable cursor = throwable;
        while (cursor.getCause() != null) {
            cursor = cursor.getCause();
        }
        return cursor.getMessage() == null ? cursor.getClass().getSimpleName() : cursor.getMessage();
    }
}
