package dev.monkeycraft.craftledgerjobs;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.server.permission.PermissionAPI;
import net.minecraftforge.server.permission.events.PermissionGatherEvent;
import net.minecraftforge.server.permission.nodes.PermissionNode;
import net.minecraftforge.server.permission.nodes.PermissionTypes;

public final class CraftLedgerPermissions {
    public static final PermissionNode<Boolean> ADMIN = node("admin", "Use CraftLedger admin commands", true);
    public static final PermissionNode<Boolean> BALANCE_OTHER = node("balance.other", "View another player's balance", true);
    public static final PermissionNode<Boolean> BALANCE_TOP = node("balance.top", "View balance leaderboards", false);
    public static final PermissionNode<Boolean> TRANSACTIONS = node("transactions", "View transaction log entries", true);

    private static final int OP_LEVEL = 2;

    private CraftLedgerPermissions() {
    }

    public static void register(PermissionGatherEvent.Nodes event) {
        event.addNodes(ADMIN, BALANCE_OTHER, BALANCE_TOP, TRANSACTIONS);
    }

    public static boolean canAdmin(CommandSourceStack source) {
        return has(source, ADMIN);
    }

    public static boolean canViewOtherBalances(CommandSourceStack source) {
        return has(source, BALANCE_OTHER);
    }

    public static boolean canViewBalanceTop(CommandSourceStack source) {
        return has(source, BALANCE_TOP);
    }

    public static boolean canViewTransactions(CommandSourceStack source) {
        return has(source, TRANSACTIONS);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static PermissionNode<Boolean> node(String name, String description, boolean requiresOp) {
        return new PermissionNode<>(
                CraftLedgerJobs.MOD_ID,
                name,
                PermissionTypes.BOOLEAN,
                (player, playerUuid, context) -> !requiresOp || player != null && player.hasPermissions(OP_LEVEL)
        ).setInformation(Component.literal("CraftLedger " + name), Component.literal(description));
    }

    private static boolean has(CommandSourceStack source, PermissionNode<Boolean> node) {
        if (source.getEntity() instanceof ServerPlayer player) {
            return PermissionAPI.getPermission(player, node);
        }
        return source.hasPermission(OP_LEVEL);
    }
}
