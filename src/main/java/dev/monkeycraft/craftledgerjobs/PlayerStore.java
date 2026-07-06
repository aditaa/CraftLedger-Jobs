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
    public JobProgress jobProgress(ServerPlayer player, String job) {
        return jobProgress(player.getUUID(), player.getGameProfile().getName(), job);
    }

    @Override
    public JobProgress jobProgress(UUID uuid, String name, String job) {
        PlayerAccount account = get(uuid, name);
        account.normalizeProgress();
        String normalized = normalizeJob(job);
        return new JobProgress(
                account.jobLevels.getOrDefault(normalized, 1),
                account.jobExperience.getOrDefault(normalized, 0.0D)
        );
    }

    @Override
    public JobProgress addJobExperience(ServerPlayer player, String job, double amount, JobsConfig config) {
        PlayerAccount account = get(player);
        account.normalizeProgress();
        String normalized = normalizeJob(job);
        JobProgress current = jobProgress(player.getUUID(), player.getGameProfile().getName(), normalized);
        JobProgression.Progress updated = JobProgression.apply(current.level(), current.xp(), amount, config);
        account.jobLevels.put(normalized, updated.level());
        account.jobExperience.put(normalized, updated.xp());
        save();
        return new JobProgress(updated.level(), updated.xp(), updated.gainedXp(), updated.leveled());
    }

    @Override
    public void setJobProgress(UUID uuid, String name, String job, int level, double xp) {
        PlayerAccount account = get(uuid, name);
        account.normalizeProgress();
        account.jobLevels.put(normalizeJob(job), Math.max(1, level));
        account.jobExperience.put(normalizeJob(job), Math.max(0.0D, Double.isFinite(xp) ? xp : 0.0D));
        save();
    }

    @Override
    public void resetJobProgress(UUID uuid, String name, String job) {
        PlayerAccount account = get(uuid, name);
        account.normalizeProgress();
        account.jobLevels.remove(normalizeJob(job));
        account.jobExperience.remove(normalizeJob(job));
        save();
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
        public Map<String, Integer> jobLevels = new LinkedHashMap<>();
        public Map<String, Double> jobExperience = new LinkedHashMap<>();
        public boolean initialized;

        void normalizeProgress() {
            if (jobLevels == null) {
                jobLevels = new LinkedHashMap<>();
            }
            if (jobExperience == null) {
                jobExperience = new LinkedHashMap<>();
            }
        }
    }

    public record KnownPlayer(UUID uuid, String name) {
    }

    public record BalanceEntry(String name, double balance) {
    }

    public record JobProgress(int level, double xp, double gainedXp, boolean leveled) {
        public JobProgress(int level, double xp) {
            this(level, xp, 0.0D, false);
        }
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

    private static String normalizeJob(String job) {
        return job == null ? "" : job.toLowerCase(java.util.Locale.ROOT);
    }
}
