package dev.monkeycraft.craftledgerjobs;

import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlayerDataStore {
    PlayerStore.PlayerAccount get(ServerPlayer player);

    PlayerStore.PlayerAccount get(UUID uuid, String fallbackName);

    double balance(ServerPlayer player);

    double balance(UUID uuid, String name);

    boolean withdraw(ServerPlayer player, double amount);

    boolean canDeposit(ServerPlayer player, double amount);

    boolean deposit(ServerPlayer player, double amount);

    void add(UUID uuid, String name, double amount);

    void set(UUID uuid, String name, double amount);

    void take(UUID uuid, String name, double amount);

    Optional<PlayerStore.KnownPlayer> findKnownPlayer(String nameOrUuid);

    List<String> knownPlayerNames();

    List<PlayerStore.BalanceEntry> topBalances();

    void setJob(ServerPlayer player, String job);

    void clearJob(ServerPlayer player);

    String job(ServerPlayer player);

    void setJob(UUID uuid, String name, String job);

    void clearJob(UUID uuid, String name);

    String job(UUID uuid, String name);

    void save();
}
