package dev.monkeycraft.craftledgerjobs;

import net.minecraft.server.level.ServerPlayer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class PlayerStore {
    private final Path path;
    private final double startingBalance;
    private final Map<String, PlayerAccount> players;

    private PlayerStore(Path path, double startingBalance, Map<String, PlayerAccount> players) {
        this.path = path;
        this.startingBalance = startingBalance;
        this.players = players;
    }

    public static PlayerStore load(Path path, double startingBalance) throws IOException {
        if (Files.notExists(path)) {
            PlayerStore store = new PlayerStore(path, startingBalance, new LinkedHashMap<>());
            store.save();
            return store;
        }
        PlayerFile file = JsonFiles.read(path, PlayerFile.class);
        Map<String, PlayerAccount> players = file == null || file.players == null ? new LinkedHashMap<>() : file.players;
        return new PlayerStore(path, startingBalance, players);
    }

    public PlayerAccount get(ServerPlayer player) {
        String uuid = player.getUUID().toString();
        PlayerAccount account = players.computeIfAbsent(uuid, ignored -> new PlayerAccount());
        account.lastKnownName = player.getGameProfile().getName();
        if (!account.initialized) {
            account.balance = startingBalance;
            account.initialized = true;
            save();
        }
        return account;
    }

    public PlayerAccount get(UUID uuid, String fallbackName) {
        PlayerAccount account = players.computeIfAbsent(uuid.toString(), ignored -> new PlayerAccount());
        if (account.lastKnownName == null || account.lastKnownName.isBlank()) {
            account.lastKnownName = fallbackName;
        }
        if (!account.initialized) {
            account.balance = startingBalance;
            account.initialized = true;
            save();
        }
        return account;
    }

    public double balance(ServerPlayer player) {
        return get(player).balance;
    }

    public boolean withdraw(ServerPlayer player, double amount) {
        PlayerAccount account = get(player);
        if (!EconomyRules.canWithdraw(account.balance, amount)) {
            return false;
        }
        account.balance = EconomyRules.subtractFromBalance(account.balance, amount);
        save();
        return true;
    }

    public void deposit(ServerPlayer player, double amount) {
        if (!EconomyRules.isPositiveFinite(amount)) {
            return;
        }
        PlayerAccount account = get(player);
        account.balance = EconomyRules.addToBalance(account.balance, amount);
        save();
    }

    public void add(UUID uuid, String name, double amount) {
        PlayerAccount account = get(uuid, name);
        account.balance = EconomyRules.addToBalance(account.balance, amount);
        save();
    }

    public void set(UUID uuid, String name, double amount) {
        PlayerAccount account = get(uuid, name);
        account.balance = EconomyRules.nonNegativeFiniteOrZero(amount);
        save();
    }

    public void take(UUID uuid, String name, double amount) {
        PlayerAccount account = get(uuid, name);
        account.balance = EconomyRules.subtractFromBalance(account.balance, amount);
        save();
    }

    public void setJob(ServerPlayer player, String job) {
        PlayerAccount account = get(player);
        account.job = job;
        save();
    }

    public void clearJob(ServerPlayer player) {
        PlayerAccount account = get(player);
        account.job = null;
        save();
    }

    public String job(ServerPlayer player) {
        return get(player).job;
    }

    public void save() {
        try {
            JsonFiles.writeAtomic(path, new PlayerFile(players));
        } catch (IOException ex) {
            CraftLedgerJobs.LOGGER.error("Failed to save CraftLedger player data", ex);
        }
    }

    public static final class PlayerAccount {
        public String lastKnownName;
        public double balance;
        public String job;
        public boolean initialized;
    }

    private record PlayerFile(Map<String, PlayerAccount> players) {
    }
}
