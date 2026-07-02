package dev.monkeycraft.craftledgerjobs;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class Ledger {
    private CommonConfig commonConfig;
    private ShopConfig shopConfig;
    private JobsConfig jobsConfig;
    private MessagesConfig messagesConfig;
    private PlayerDataStore playerStore;
    private JobPayoutDataStore jobPayoutStore;
    private TransactionStore transactionLog;
    private PlacedBlockStore placedBlockStore;
    private Path dataDir;
    private ShopService shopService;
    private JobsService jobsService;

    public void start(MinecraftServer server) {
        try {
            Path configDir = FMLPaths.CONFIGDIR.get().resolve("craftledger");
            dataDir = server.getWorldPath(LevelResource.ROOT).resolve("craftledger");
            Files.createDirectories(configDir);
            Files.createDirectories(dataDir);

            commonConfig = CommonConfig.load(configDir.resolve("common.toml"));
            shopConfig = ShopConfig.load(configDir.resolve("shop.json"));
            jobsConfig = JobsConfig.load(configDir.resolve("jobs.json"));
            messagesConfig = MessagesConfig.load(configDir.resolve("messages.json"));
            loadDataStores(dataDir);
            shopService = new ShopService(this);
            jobsService = new JobsService(this);
            CraftLedgerJobs.LOGGER.info("CraftLedger Jobs loaded with {} storage", commonConfig.storageBackend());
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to start CraftLedger Jobs", ex);
        }
    }

    public void stop() {
        if (playerStore != null) {
            playerStore.save();
        }
        if (jobPayoutStore instanceof JobPayoutStore jsonJobPayouts) {
            jsonJobPayouts.save();
        }
        if (placedBlockStore != null) {
            placedBlockStore.save();
        }
        if (playerStore instanceof AutoCloseable closeable) {
            try {
                closeable.close();
            } catch (Exception ex) {
                CraftLedgerJobs.LOGGER.warn("Failed to close CraftLedger storage", ex);
            }
        }
    }

    public void reload() {
        ensureStarted();
        try {
            Path configDir = FMLPaths.CONFIGDIR.get().resolve("craftledger");
            CommonConfig reloadedCommon = CommonConfig.load(configDir.resolve("common.toml"));
            ShopConfig reloadedShop = ShopConfig.load(configDir.resolve("shop.json"));
            JobsConfig reloadedJobs = JobsConfig.load(configDir.resolve("jobs.json"));
            MessagesConfig reloadedMessages = MessagesConfig.load(configDir.resolve("messages.json"));
            if (!commonConfig.storageBackend().equals(reloadedCommon.storageBackend())
                    || !commonConfig.sqliteFile().equals(reloadedCommon.sqliteFile())) {
                throw new ConfigValidationException("common.toml storageBackend/sqliteFile changes require a server restart.");
            }
            commonConfig = reloadedCommon;
            shopConfig = reloadedShop;
            jobsConfig = reloadedJobs;
            messagesConfig = reloadedMessages;
            CraftLedgerJobs.LOGGER.info("CraftLedger Jobs config reloaded");
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to reload CraftLedger Jobs", ex);
        }
    }

    public CommonConfig common() {
        ensureStarted();
        return commonConfig;
    }

    public ShopConfig shopConfig() {
        ensureStarted();
        return shopConfig;
    }

    public JobsConfig jobsConfig() {
        ensureStarted();
        return jobsConfig;
    }

    public MessagesConfig messages() {
        ensureStarted();
        return messagesConfig;
    }

    public PlayerDataStore players() {
        ensureStarted();
        return playerStore;
    }

    public boolean canDeposit(net.minecraft.server.level.ServerPlayer player, double amount) {
        ensureStarted();
        double balance = playerStore.balance(player);
        return playerStore.canDeposit(player, amount) && canAddWithinMaxBalance(balance, amount);
    }

    public boolean deposit(net.minecraft.server.level.ServerPlayer player, double amount) {
        ensureStarted();
        return canDeposit(player, amount) && playerStore.deposit(player, amount);
    }

    public boolean canSetBalance(double amount) {
        ensureStarted();
        return EconomyRules.nonNegativeFiniteOrZero(amount) == amount && withinMaxBalance(amount);
    }

    public boolean canAddBalance(java.util.UUID uuid, String name, double amount) {
        ensureStarted();
        return canAddWithinMaxBalance(playerStore.balance(uuid, name), amount);
    }

    public String maxBalanceMessage() {
        return commonConfig.maxBalance() <= 0
                ? "That balance amount is not allowed."
                : "That would exceed the configured max balance of " + commonConfig.format(commonConfig.maxBalance()) + ".";
    }

    public TransactionStore transactions() {
        ensureStarted();
        return transactionLog;
    }

    public JobPayoutDataStore jobPayouts() {
        ensureStarted();
        return jobPayoutStore;
    }

    public PlacedBlockStore placedBlocks() {
        ensureStarted();
        return placedBlockStore;
    }

    public Path dataDir() {
        ensureStarted();
        return dataDir;
    }

    public ShopService shop() {
        ensureStarted();
        return shopService;
    }

    public JobsService jobs() {
        ensureStarted();
        return jobsService;
    }

    private void ensureStarted() {
        if (playerStore == null) {
            throw new IllegalStateException("CraftLedger Jobs has not started yet");
        }
    }

    private void loadDataStores(Path dataDir) throws IOException {
        placedBlockStore = PlacedBlockStore.load(dataDir.resolve("placed_blocks.json"));
        if (CommonConfig.STORAGE_SQLITE.equals(commonConfig.storageBackend())) {
            SqliteLedgerStore sqlite = SqliteLedgerStore.load(dataDir.resolve(commonConfig.sqliteFile()), commonConfig.startingBalance());
            playerStore = sqlite;
            jobPayoutStore = sqlite;
            transactionLog = sqlite;
            return;
        }

        playerStore = PlayerStore.load(dataDir.resolve("players.json"), commonConfig.startingBalance());
        jobPayoutStore = JobPayoutStore.load(dataDir.resolve("job_payouts.json"));
        transactionLog = new TransactionLog(dataDir.resolve("transactions.log"));
    }

    private boolean canAddWithinMaxBalance(double balance, double amount) {
        if (!EconomyRules.canAddToBalance(balance, amount)) {
            return false;
        }
        return withinMaxBalance(EconomyRules.addToBalance(balance, amount));
    }

    private boolean withinMaxBalance(double balance) {
        return commonConfig.maxBalance() <= 0 || balance <= commonConfig.maxBalance();
    }
}
