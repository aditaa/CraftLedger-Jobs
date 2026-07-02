package dev.monkeycraft.craftledgerjobs;

import net.minecraft.server.level.ServerPlayer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class PlayerStore implements PlayerDataStore {
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

    @Override
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

    @Override
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

    @Override
    public double balance(ServerPlayer player) {
        return get(player).balance;
    }

    @Override
    public double balance(UUID uuid, String name) {
        return get(uuid, name).balance;
    }

    @Override
    public boolean withdraw(ServerPlayer player, double amount) {
        PlayerAccount account = get(player);
        if (!EconomyRules.canWithdraw(account.balance, amount)) {
            return false;
        }
        account.balance = EconomyRules.subtractFromBalance(account.balance, amount);
        save();
        return true;
    }

    @Override
    public boolean canDeposit(ServerPlayer player, double amount) {
        PlayerAccount account = get(player);
        return EconomyRules.canAddToBalance(account.balance, amount);
    }

    @Override
    public boolean deposit(ServerPlayer player, double amount) {
        PlayerAccount account = get(player);
        if (!EconomyRules.canAddToBalance(account.balance, amount)) {
            return false;
        }
        account.balance = EconomyRules.addToBalance(account.balance, amount);
        save();
        return true;
    }

    @Override
    public void add(UUID uuid, String name, double amount) {
        PlayerAccount account = get(uuid, name);
        account.balance = EconomyRules.addToBalance(account.balance, amount);
        save();
    }

    @Override
    public void set(UUID uuid, String name, double amount) {
        PlayerAccount account = get(uuid, name);
        account.balance = EconomyRules.nonNegativeFiniteOrZero(amount);
        save();
    }

    @Override
    public void take(UUID uuid, String name, double amount) {
        PlayerAccount account = get(uuid, name);
        account.balance = EconomyRules.subtractFromBalance(account.balance, amount);
        save();
    }

    @Override
    public Optional<KnownPlayer> findKnownPlayer(String nameOrUuid) {
        if (nameOrUuid == null || nameOrUuid.isBlank()) {
            return Optional.empty();
        }
        try {
            UUID uuid = UUID.fromString(nameOrUuid);
            PlayerAccount account = players.get(uuid.toString());
            if (account != null) {
                return Optional.of(new KnownPlayer(uuid, displayName(account, uuid.toString())));
            }
        } catch (IllegalArgumentException ignored) {
            // Not a UUID; fall through to last-known-name lookup.
        }

        for (Map.Entry<String, PlayerAccount> entry : players.entrySet()) {
            PlayerAccount account = entry.getValue();
            if (account.lastKnownName != null && account.lastKnownName.equalsIgnoreCase(nameOrUuid)) {
                return Optional.of(new KnownPlayer(UUID.fromString(entry.getKey()), displayName(account, nameOrUuid)));
            }
        }
        return Optional.empty();
    }

    @Override
    public List<String> knownPlayerNames() {
        return players.values().stream()
                .map(account -> account.lastKnownName)
                .filter(name -> name != null && !name.isBlank())
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    @Override
    public List<BalanceEntry> topBalances() {
        return players.entrySet().stream()
                .filter(entry -> entry.getValue().initialized)
                .map(entry -> new BalanceEntry(displayName(entry.getValue(), entry.getKey()), entry.getValue().balance))
                .sorted((left, right) -> {
                    int balanceOrder = Double.compare(right.balance, left.balance);
                    return balanceOrder != 0 ? balanceOrder : left.name.compareToIgnoreCase(right.name);
                })
                .toList();
    }

    @Override
    public void setJob(ServerPlayer player, String job) {
        PlayerAccount account = get(player);
        account.job = job;
        save();
    }

    @Override
    public void clearJob(ServerPlayer player) {
        PlayerAccount account = get(player);
        account.job = null;
        save();
    }

    @Override
    public String job(ServerPlayer player) {
        return get(player).job;
    }

    @Override
    public void setJob(UUID uuid, String name, String job) {
        PlayerAccount account = get(uuid, name);
        account.job = job;
        save();
    }

    @Override
    public void clearJob(UUID uuid, String name) {
        PlayerAccount account = get(uuid, name);
        account.job = null;
        save();
    }

    @Override
    public String job(UUID uuid, String name) {
        return get(uuid, name).job;
    }

    @Override
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

    public record KnownPlayer(UUID uuid, String name) {
    }

    public record BalanceEntry(String name, double balance) {
    }

    private static final class PlayerFile {
        public int version = 1;
        public Map<String, PlayerAccount> players;

        PlayerFile(Map<String, PlayerAccount> players) {
            this.players = players;
        }
    }

    private static String displayName(PlayerAccount account, String fallback) {
        return account.lastKnownName == null || account.lastKnownName.isBlank() ? fallback : account.lastKnownName;
    }
}
