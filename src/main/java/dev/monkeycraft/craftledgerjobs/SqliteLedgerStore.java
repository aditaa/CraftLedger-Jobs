package dev.monkeycraft.craftledgerjobs;

import net.minecraft.server.level.ServerPlayer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class SqliteLedgerStore implements PlayerDataStore, JobPayoutDataStore, TransactionStore, AutoCloseable {
    private static final int MAX_TAIL_LINES = 50;

    private final Connection connection;
    private final double startingBalance;

    private SqliteLedgerStore(Connection connection, double startingBalance) {
        this.connection = connection;
        this.startingBalance = startingBalance;
    }

    public static SqliteLedgerStore load(Path path, double startingBalance) throws IOException {
        Files.createDirectories(path.getParent());
        try {
            Connection connection = DriverManager.getConnection("jdbc:sqlite:" + path.toAbsolutePath());
            SqliteLedgerStore store = new SqliteLedgerStore(connection, startingBalance);
            store.initializeSchema();
            return store;
        } catch (SQLException ex) {
            throw new IOException("Failed to open SQLite storage at " + path, ex);
        }
    }

    @Override
    public synchronized PlayerStore.PlayerAccount get(ServerPlayer player) {
        return get(player.getUUID(), player.getGameProfile().getName());
    }

    @Override
    public synchronized PlayerStore.PlayerAccount get(UUID uuid, String fallbackName) {
        ensurePlayer(uuid, fallbackName);
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT last_known_name, balance, job, initialized
                FROM players
                WHERE uuid = ?
                """)) {
            statement.setString(1, uuid.toString());
            try (ResultSet results = statement.executeQuery()) {
                if (results.next()) {
                    PlayerStore.PlayerAccount account = new PlayerStore.PlayerAccount();
                    account.lastKnownName = displayName(results.getString("last_known_name"), fallbackName);
                    account.balance = results.getDouble("balance");
                    account.job = results.getString("job");
                    account.initialized = results.getBoolean("initialized");
                    return account;
                }
            }
        } catch (SQLException ex) {
            CraftLedgerJobs.LOGGER.error("Failed to read CraftLedger SQLite player data", ex);
        }
        PlayerStore.PlayerAccount fallback = new PlayerStore.PlayerAccount();
        fallback.lastKnownName = fallbackName;
        fallback.balance = startingBalance;
        fallback.initialized = true;
        return fallback;
    }

    @Override
    public synchronized double balance(ServerPlayer player) {
        return balance(player.getUUID(), player.getGameProfile().getName());
    }

    @Override
    public synchronized double balance(UUID uuid, String name) {
        return get(uuid, name).balance;
    }

    @Override
    public synchronized boolean withdraw(ServerPlayer player, double amount) {
        PlayerStore.PlayerAccount account = get(player);
        if (!EconomyRules.canWithdraw(account.balance, amount)) {
            return false;
        }
        set(player.getUUID(), player.getGameProfile().getName(), EconomyRules.subtractFromBalance(account.balance, amount));
        return true;
    }

    @Override
    public synchronized boolean canDeposit(ServerPlayer player, double amount) {
        return EconomyRules.canAddToBalance(get(player).balance, amount);
    }

    @Override
    public synchronized boolean deposit(ServerPlayer player, double amount) {
        PlayerStore.PlayerAccount account = get(player);
        if (!EconomyRules.canAddToBalance(account.balance, amount)) {
            return false;
        }
        set(player.getUUID(), player.getGameProfile().getName(), EconomyRules.addToBalance(account.balance, amount));
        return true;
    }

    @Override
    public synchronized void add(UUID uuid, String name, double amount) {
        set(uuid, name, EconomyRules.addToBalance(get(uuid, name).balance, amount));
    }

    @Override
    public synchronized void set(UUID uuid, String name, double amount) {
        ensurePlayer(uuid, name);
        try (PreparedStatement statement = connection.prepareStatement("""
                UPDATE players
                SET last_known_name = ?, balance = ?, initialized = 1
                WHERE uuid = ?
                """)) {
            statement.setString(1, name);
            statement.setDouble(2, EconomyRules.nonNegativeFiniteOrZero(amount));
            statement.setString(3, uuid.toString());
            statement.executeUpdate();
        } catch (SQLException ex) {
            CraftLedgerJobs.LOGGER.error("Failed to update CraftLedger SQLite balance", ex);
        }
    }

    @Override
    public synchronized void take(UUID uuid, String name, double amount) {
        set(uuid, name, EconomyRules.subtractFromBalance(get(uuid, name).balance, amount));
    }

    @Override
    public synchronized Optional<PlayerStore.KnownPlayer> findKnownPlayer(String nameOrUuid) {
        if (nameOrUuid == null || nameOrUuid.isBlank()) {
            return Optional.empty();
        }
        try {
            UUID uuid = UUID.fromString(nameOrUuid);
            try (PreparedStatement statement = connection.prepareStatement("""
                    SELECT last_known_name
                    FROM players
                    WHERE uuid = ?
                    """)) {
                statement.setString(1, uuid.toString());
                try (ResultSet results = statement.executeQuery()) {
                    if (results.next()) {
                        return Optional.of(new PlayerStore.KnownPlayer(uuid, displayName(results.getString("last_known_name"), uuid.toString())));
                    }
                }
            }
        } catch (IllegalArgumentException ignored) {
            // Not a UUID; fall through to name lookup.
        } catch (SQLException ex) {
            CraftLedgerJobs.LOGGER.error("Failed to find CraftLedger SQLite player by UUID", ex);
            return Optional.empty();
        }

        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT uuid, last_known_name
                FROM players
                WHERE lower(last_known_name) = lower(?)
                ORDER BY last_known_name
                LIMIT 1
                """)) {
            statement.setString(1, nameOrUuid);
            try (ResultSet results = statement.executeQuery()) {
                if (results.next()) {
                    return Optional.of(new PlayerStore.KnownPlayer(
                            UUID.fromString(results.getString("uuid")),
                            displayName(results.getString("last_known_name"), nameOrUuid)
                    ));
                }
            }
        } catch (SQLException ex) {
            CraftLedgerJobs.LOGGER.error("Failed to find CraftLedger SQLite player by name", ex);
        }
        return Optional.empty();
    }

    @Override
    public synchronized List<String> knownPlayerNames() {
        List<String> names = new ArrayList<>();
        try (Statement statement = connection.createStatement();
             ResultSet results = statement.executeQuery("""
                     SELECT DISTINCT last_known_name
                     FROM players
                     WHERE last_known_name IS NOT NULL AND trim(last_known_name) != ''
                     ORDER BY lower(last_known_name)
                     """)) {
            while (results.next()) {
                names.add(results.getString("last_known_name"));
            }
        } catch (SQLException ex) {
            CraftLedgerJobs.LOGGER.error("Failed to list CraftLedger SQLite player names", ex);
        }
        return names;
    }

    @Override
    public synchronized List<PlayerStore.BalanceEntry> topBalances() {
        List<PlayerStore.BalanceEntry> entries = new ArrayList<>();
        try (Statement statement = connection.createStatement();
             ResultSet results = statement.executeQuery("""
                     SELECT uuid, last_known_name, balance
                     FROM players
                     WHERE initialized = 1
                     ORDER BY balance DESC, lower(coalesce(last_known_name, uuid)) ASC
                     """)) {
            while (results.next()) {
                entries.add(new PlayerStore.BalanceEntry(
                        displayName(results.getString("last_known_name"), results.getString("uuid")),
                        results.getDouble("balance")
                ));
            }
        } catch (SQLException ex) {
            CraftLedgerJobs.LOGGER.error("Failed to list CraftLedger SQLite top balances", ex);
        }
        return entries;
    }

    @Override
    public synchronized void setJob(ServerPlayer player, String job) {
        setJob(player.getUUID(), player.getGameProfile().getName(), job);
    }

    @Override
    public synchronized void clearJob(ServerPlayer player) {
        clearJob(player.getUUID(), player.getGameProfile().getName());
    }

    @Override
    public synchronized String job(ServerPlayer player) {
        return job(player.getUUID(), player.getGameProfile().getName());
    }

    @Override
    public synchronized void setJob(UUID uuid, String name, String job) {
        ensurePlayer(uuid, name);
        try (PreparedStatement statement = connection.prepareStatement("""
                UPDATE players
                SET last_known_name = ?, job = ?
                WHERE uuid = ?
                """)) {
            statement.setString(1, name);
            statement.setString(2, job);
            statement.setString(3, uuid.toString());
            statement.executeUpdate();
        } catch (SQLException ex) {
            CraftLedgerJobs.LOGGER.error("Failed to update CraftLedger SQLite job", ex);
        }
    }

    @Override
    public synchronized void clearJob(UUID uuid, String name) {
        setJob(uuid, name, null);
    }

    @Override
    public synchronized String job(UUID uuid, String name) {
        return get(uuid, name).job;
    }

    @Override
    public void save() {
        // SQLite commits each statement immediately; no explicit save is needed.
    }

    @Override
    public synchronized boolean allowAndRecord(UUID playerUuid, Instant instant, double payout, double dailyLimit) {
        if (dailyLimit <= 0) {
            return true;
        }
        String date = LocalDate.ofInstant(instant, ZoneOffset.UTC).toString();
        double current = total(playerUuid, LocalDate.parse(date));
        double next = current + payout;
        if (!Double.isFinite(next) || next > dailyLimit) {
            return false;
        }
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO job_payouts(day, uuid, amount)
                VALUES(?, ?, ?)
                ON CONFLICT(day, uuid) DO UPDATE SET amount = excluded.amount
                """)) {
            statement.setString(1, date);
            statement.setString(2, playerUuid.toString());
            statement.setDouble(3, next);
            statement.executeUpdate();
            return true;
        } catch (SQLException ex) {
            CraftLedgerJobs.LOGGER.error("Failed to update CraftLedger SQLite job payout totals", ex);
            return false;
        }
    }

    @Override
    public synchronized double total(UUID playerUuid, LocalDate date) {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT amount
                FROM job_payouts
                WHERE day = ? AND uuid = ?
                """)) {
            statement.setString(1, date.toString());
            statement.setString(2, playerUuid.toString());
            try (ResultSet results = statement.executeQuery()) {
                if (results.next()) {
                    return results.getDouble("amount");
                }
            }
        } catch (SQLException ex) {
            CraftLedgerJobs.LOGGER.error("Failed to read CraftLedger SQLite job payout totals", ex);
        }
        return 0.0D;
    }

    @Override
    public synchronized void write(String type, ServerPlayer player, double amount, String detail) {
        write(type, player.getGameProfile().getName(), player.getUUID().toString(), amount, detail);
    }

    @Override
    public synchronized void write(String type, String playerName, String playerUuid, double amount, String detail) {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO transactions(created_at, type, player_name, player_uuid, amount, detail)
                VALUES(?, ?, ?, ?, ?, ?)
                """)) {
            statement.setString(1, Instant.now().toString());
            statement.setString(2, clean(type));
            statement.setString(3, clean(playerName));
            statement.setString(4, clean(playerUuid));
            statement.setDouble(5, Double.isFinite(amount) ? amount : 0.0D);
            statement.setString(6, clean(detail));
            statement.executeUpdate();
        } catch (SQLException ex) {
            CraftLedgerJobs.LOGGER.error("Failed to write CraftLedger SQLite transaction", ex);
        }
    }

    @Override
    public synchronized List<String> tail(int requestedLines) {
        return tail(null, requestedLines);
    }

    @Override
    public synchronized List<String> tail(String playerNameOrUuid, int requestedLines) {
        int lines = Math.max(1, Math.min(MAX_TAIL_LINES, requestedLines));
        List<String> entries = new ArrayList<>();
        String filter = playerNameOrUuid == null ? "" : playerNameOrUuid.trim();
        String sql = filter.isEmpty()
                ? """
                SELECT created_at, type, player_name, player_uuid, amount, detail
                FROM transactions
                ORDER BY id DESC
                LIMIT ?
                """
                : """
                SELECT created_at, type, player_name, player_uuid, amount, detail
                FROM transactions
                WHERE lower(player_name) = lower(?) OR lower(player_uuid) = lower(?)
                ORDER BY id DESC
                LIMIT ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            if (filter.isEmpty()) {
                statement.setInt(1, lines);
            } else {
                statement.setString(1, filter);
                statement.setString(2, filter);
                statement.setInt(3, lines);
            }
            try (ResultSet results = statement.executeQuery()) {
                while (results.next()) {
                    entries.add("%s\t%s\t%s\t%s\t%.2f\t%s".formatted(
                            results.getString("created_at"),
                            results.getString("type"),
                            results.getString("player_name"),
                            results.getString("player_uuid"),
                            results.getDouble("amount"),
                            results.getString("detail")
                    ));
                }
            }
        } catch (SQLException ex) {
            CraftLedgerJobs.LOGGER.error("Failed to read CraftLedger SQLite transactions", ex);
        }
        Collections.reverse(entries);
        return entries;
    }

    @Override
    public synchronized void close() throws SQLException {
        connection.close();
    }

    private void initializeSchema() throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("PRAGMA foreign_keys = ON");
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS players (
                        uuid TEXT PRIMARY KEY,
                        last_known_name TEXT,
                        balance REAL NOT NULL,
                        job TEXT,
                        initialized INTEGER NOT NULL DEFAULT 1
                    )
                    """);
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS job_payouts (
                        day TEXT NOT NULL,
                        uuid TEXT NOT NULL,
                        amount REAL NOT NULL,
                        PRIMARY KEY(day, uuid)
                    )
                    """);
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS transactions (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        created_at TEXT NOT NULL,
                        type TEXT NOT NULL,
                        player_name TEXT NOT NULL,
                        player_uuid TEXT NOT NULL,
                        amount REAL NOT NULL,
                        detail TEXT NOT NULL
                    )
                    """);
            statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_transactions_player_uuid ON transactions(player_uuid)");
            statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_transactions_player_name ON transactions(player_name)");
        }
    }

    private void ensurePlayer(UUID uuid, String fallbackName) {
        try (PreparedStatement insert = connection.prepareStatement("""
                INSERT OR IGNORE INTO players(uuid, last_known_name, balance, initialized)
                VALUES(?, ?, ?, 1)
                """)) {
            insert.setString(1, uuid.toString());
            insert.setString(2, fallbackName);
            insert.setDouble(3, startingBalance);
            insert.executeUpdate();
        } catch (SQLException ex) {
            CraftLedgerJobs.LOGGER.error("Failed to create CraftLedger SQLite player", ex);
            return;
        }
        if (fallbackName == null || fallbackName.isBlank()) {
            return;
        }
        try (PreparedStatement update = connection.prepareStatement("""
                UPDATE players
                SET last_known_name = ?
                WHERE uuid = ?
                """)) {
            update.setString(1, fallbackName);
            update.setString(2, uuid.toString());
            update.executeUpdate();
        } catch (SQLException ex) {
            CraftLedgerJobs.LOGGER.error("Failed to update CraftLedger SQLite player name", ex);
        }
    }

    private static String clean(String value) {
        return value == null ? "" : value.replace('\t', ' ').replace('\r', ' ').replace('\n', ' ');
    }

    private static String displayName(String name, String fallback) {
        return name == null || name.isBlank() ? fallback : name;
    }
}
