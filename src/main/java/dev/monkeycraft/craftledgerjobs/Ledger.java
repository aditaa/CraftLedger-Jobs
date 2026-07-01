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
    private PlayerStore playerStore;
    private TransactionLog transactionLog;
    private ShopService shopService;
    private JobsService jobsService;

    public void start(MinecraftServer server) {
        try {
            Path configDir = FMLPaths.CONFIGDIR.get().resolve("craftledger");
            Path dataDir = server.getWorldPath(LevelResource.ROOT).resolve("craftledger");
            Files.createDirectories(configDir);
            Files.createDirectories(dataDir);

            commonConfig = CommonConfig.load(configDir.resolve("common.toml"));
            shopConfig = ShopConfig.load(configDir.resolve("shop.json"));
            jobsConfig = JobsConfig.load(configDir.resolve("jobs.json"));
            MessagesConfig.ensureExists(configDir.resolve("messages.json"));
            playerStore = PlayerStore.load(dataDir.resolve("players.json"), commonConfig.startingBalance());
            transactionLog = new TransactionLog(dataDir.resolve("transactions.log"));
            shopService = new ShopService(this);
            jobsService = new JobsService(this);
            CraftLedgerJobs.LOGGER.info("CraftLedger Jobs loaded");
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to start CraftLedger Jobs", ex);
        }
    }

    public void stop() {
        if (playerStore != null) {
            playerStore.save();
        }
    }

    public void reload() {
        ensureStarted();
        try {
            Path configDir = FMLPaths.CONFIGDIR.get().resolve("craftledger");
            CommonConfig reloadedCommon = CommonConfig.load(configDir.resolve("common.toml"));
            ShopConfig reloadedShop = ShopConfig.load(configDir.resolve("shop.json"));
            JobsConfig reloadedJobs = JobsConfig.load(configDir.resolve("jobs.json"));
            commonConfig = reloadedCommon;
            shopConfig = reloadedShop;
            jobsConfig = reloadedJobs;
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

    public PlayerStore players() {
        ensureStarted();
        return playerStore;
    }

    public TransactionLog transactions() {
        ensureStarted();
        return transactionLog;
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
}
