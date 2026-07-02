package dev.monkeycraft.craftledgerjobs;

import net.minecraft.server.level.ServerPlayer;

import java.util.List;

public interface TransactionStore {
    void write(String type, ServerPlayer player, double amount, String detail);

    void write(String type, String playerName, String playerUuid, double amount, String detail);

    List<String> tail(int requestedLines);

    List<String> tail(String playerNameOrUuid, int requestedLines);
}
